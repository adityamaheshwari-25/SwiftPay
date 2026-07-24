package com.example.demo.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.KycDocumentUploadResponseDto;
import com.example.demo.dto.KycDocumentViewDto;
import com.example.demo.dto.KycFileDataDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.KycDocument;
import com.example.demo.entity.enums.KycStatus;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.DocumentStorageException;
import com.example.demo.exception.FileNotUploadedException;
import com.example.demo.exception.InvalidFileTypeException;
import com.example.demo.exception.KycAlreadySubmittedException;
import com.example.demo.exception.LargerFileSizeException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.KycDocumentRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.KycDocumentService;
import com.example.demo.service.storage.DocumentStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class KycDocumentServiceImpl implements KycDocumentService {

	private static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024;

	private final CurrentUserService currentUserService;
	private final KycDocumentRepository kycDocumentRepository;
	private final AppUserRepository appUserRepository;
	private final DocumentStorageService documentStorageService;

	@Override
	public KycDocumentUploadResponseDto uploadKyc(
			Authentication authentication,
			MultipartFile file
	) throws UserNotFoundException, FileNotUploadedException, LargerFileSizeException,
			InvalidFileTypeException, KycAlreadySubmittedException {

		AppUser user = currentUserService.getCurrentUser(authentication);
		log.info(
				"User ID: {} is initiating KYC upload. File: {}, Size: {} bytes",
				user.getId(),
				file.getOriginalFilename(),
				file.getSize()
		);

		validateFile(file);

		KycDocument existingKyc = kycDocumentRepository.findByUser(user).orElse(null);
		if (existingKyc != null) {
			if (existingKyc.getStatus() == KycStatus.PENDING) {
				throw new KycAlreadySubmittedException(ErrorMessage.KYC_ALREADY_SUBMITTED);
			}

			if (existingKyc.getStatus() == KycStatus.APPROVED) {
				throw new KycAlreadySubmittedException(ErrorMessage.KYC_ALREADY_APPROVED);
			}

			log.info("Replacing previously rejected KYC for user ID: {}", user.getId());
		}

		String extension = getFileExtension(file.getOriginalFilename());
		String fileName = "aadhaar_" + System.currentTimeMillis() + extension;
		String storageKey = "kyc/user-" + user.getId() + "/" + UUID.randomUUID() + extension;
		String previousStorageKey = existingKyc == null ? null : existingKyc.getStorageKey();

		try {
			byte[] fileData = file.getBytes();
			documentStorageService.store(storageKey, fileData, file.getContentType());

			KycDocument kyc = existingKyc != null ? existingKyc : new KycDocument();
			kyc.setUser(user);
			kyc.setDocumentType("AADHAR");
			kyc.setFileName(fileName);
			kyc.setStorageKey(storageKey);
			kyc.setContentType(file.getContentType());
			kyc.setFileSize(file.getSize());
			kyc.setStatus(KycStatus.PENDING);
			kyc.setRejectionReason(null);
			kyc.setSubmittedAt(LocalDateTime.now());
			kyc.setReviewedAt(null);

			kycDocumentRepository.save(kyc);

			user.setKycVerified(false);
			appUserRepository.save(user);
			registerBlobCleanup(storageKey, previousStorageKey);

			log.info("KYC upload completed successfully for user ID: {}", user.getId());
			return new KycDocumentUploadResponseDto(
					"KYC document uploaded successfully",
					KycStatus.PENDING
			);
		} catch (IOException | DocumentStorageException ex) {
			safeDelete(storageKey);
			log.error("Unable to read KYC upload for user {}: {}", user.getId(), ex.getMessage());
			throw new FileNotUploadedException(ErrorMessage.FAIL_TO_UPLOAD_KYC_DOCUMENT);
		} catch (RuntimeException ex) {
			safeDelete(storageKey);
			throw ex;
		}
	}

	private void registerBlobCleanup(String newStorageKey, String previousStorageKey) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			deletePreviousBlob(previousStorageKey, newStorageKey);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				deletePreviousBlob(previousStorageKey, newStorageKey);
			}

			@Override
			public void afterCompletion(int status) {
				if (status != TransactionSynchronization.STATUS_COMMITTED) {
					safeDelete(newStorageKey);
				}
			}
		});
	}

	private void deletePreviousBlob(String previousStorageKey, String newStorageKey) {
		if (previousStorageKey != null && !previousStorageKey.equals(newStorageKey)) {
			safeDelete(previousStorageKey);
		}
	}

	private void safeDelete(String storageKey) {
		if (storageKey == null || storageKey.isBlank()) {
			return;
		}
		try {
			documentStorageService.delete(storageKey);
		} catch (DocumentStorageException cleanupError) {
			log.warn("Unable to clean up KYC blob {}: {}", storageKey, cleanupError.getMessage());
		}
	}

	private void validateFile(MultipartFile file)
			throws FileNotUploadedException, LargerFileSizeException, InvalidFileTypeException {

		if (file == null || file.isEmpty()) {
			throw new FileNotUploadedException(ErrorMessage.FILE_IS_REQUIRED);
		}

		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new LargerFileSizeException(ErrorMessage.FILE_SIZE_MUST_BE_LESS_THAN_2MB);
		}

		String contentType = file.getContentType();
		if (!"image/jpeg".equals(contentType)
				&& !"image/png".equals(contentType)
				&& !"application/pdf".equals(contentType)) {
			log.warn("Invalid KYC content type: {}", contentType);
			throw new InvalidFileTypeException(ErrorMessage.INVALID_FILE_TYPE);
		}

		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null) {
			throw new InvalidFileTypeException(ErrorMessage.INVALID_FILE_TYPE);
		}

		String filename = originalFilename.toLowerCase(Locale.ROOT);
		if (!filename.endsWith(".jpg")
				&& !filename.endsWith(".jpeg")
				&& !filename.endsWith(".png")
				&& !filename.endsWith(".pdf")) {
			throw new InvalidFileTypeException(ErrorMessage.INVALID_FILE_TYPE);
		}
	}

	private String getFileExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "";
		}
		return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
	}

	@Override
	@Transactional(readOnly = true)
	public KycDocumentViewDto viewMyKyc(Authentication authentication)
			throws UserNotFoundException, ResourceNotFoundException {

		AppUser user = currentUserService.getCurrentUser(authentication);
		KycDocument kyc = kycDocumentRepository.findByUser(user)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.KYC_NOT_FOUND));

		return new KycDocumentViewDto(
				kyc.getDocumentType(),
				kyc.getFileName(),
				kyc.getContentType(),
				kyc.getFileSize(),
				kyc.getStatus(),
				kyc.getRejectionReason(),
				kyc.getSubmittedAt()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public KycFileDataDto getMyKycFile(Authentication authentication)
			throws ResourceNotFoundException, UserNotFoundException {

		AppUser user = currentUserService.getCurrentUser(authentication);
		KycDocument kyc = kycDocumentRepository.findByUser(user)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.KYC_NOT_FOUND));

		Optional<byte[]> fileData = documentStorageService.load(kyc.getStorageKey());
		if (fileData.isEmpty() || fileData.get().length == 0) {
			throw new ResourceNotFoundException(ErrorMessage.KYC_NOT_FOUND);
		}

		return new KycFileDataDto(
				fileData.get(),
				kyc.getFileName(),
				kyc.getContentType()
		);
	}
}
