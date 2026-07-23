package com.example.demo.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.KycDocumentUploadResponseDto;
import com.example.demo.dto.KycDocumentViewDto;
import com.example.demo.dto.KycFileDataDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.KycDocument;
import com.example.demo.entity.enums.KycStatus;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.FileNotUploadedException;
import com.example.demo.exception.InvalidFileTypeException;
import com.example.demo.exception.KycAlreadySubmittedException;
import com.example.demo.exception.LargerFileSizeException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.KycDocumentRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.KycDocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * User uploads KYC
│
├── No existing KYC
│   └── Save → PENDING
│
├── Existing KYC = PENDING
│   └── ❌ Block upload (wait for admin)
│
├── Existing KYC = APPROVED
│   └── ❌ Block upload (final)
│
└── Existing KYC = REJECTED
    └── ✅ Delete old file → Save new → PENDING
 * 
 * */


@Slf4j
@Service 
@RequiredArgsConstructor
public class KycDocumentServiceImpl implements KycDocumentService{
	
	private final CurrentUserService currentUserService;
	private final KycDocumentRepository kycDocumentRepository;
	private final AppUserRepository appUserRepository;

	
	private static final String UPLOAD_DIR = "uploads/kyc/";
	
	@Override
	public KycDocumentUploadResponseDto uploadKyc(Authentication authetication, MultipartFile file) throws UserNotFoundException, FileNotUploadedException, LargerFileSizeException, InvalidFileTypeException, KycAlreadySubmittedException, IOException {
		
		AppUser user = currentUserService.getCurrentUser(authetication);
		log.info("User ID: {} is initiating KYC upload. File: {}, Size: {} bytes", 
				user.getId(), file.getOriginalFilename(), file.getSize());
		
		// validate file
		validateFile(file);
		
		 // 2️⃣ Check existing KYC record
        KycDocument existingKyc = kycDocumentRepository.findByUser(user).orElse(null);

        if (existingKyc != null) {
        	
        	log.debug("Existing KYC record found for user {} with status: {}", user.getId(), existingKyc.getStatus());

            if (existingKyc.getStatus() == KycStatus.PENDING) {
                throw new KycAlreadySubmittedException(
                        ErrorMessage.KYC_ALREADY_SUBMITTED
                );
            }

            if (existingKyc.getStatus() == KycStatus.APPROVED) {
                throw new KycAlreadySubmittedException(
                        ErrorMessage.KYC_ALREADY_APPROVED
                );
            }
            
            if (existingKyc.getStatus() == KycStatus.REJECTED) {
            	log.info("Replacing previously REJECTED KYC for user ID: {}", user.getId());
            	if (existingKyc.getFilePath() != null) {
                    Files.deleteIfExists(Paths.get(existingKyc.getFilePath()));
                    log.debug("Old file deletion successful");
                }
            }

        }

        try {
            // 3️⃣ Create user-specific directory
            String userDir = UPLOAD_DIR + "user_" + user.getId();
            Files.createDirectories(Paths.get(userDir));

            // 4️⃣ Save file
            String fileName = "aadhaar_" + System.currentTimeMillis()
                    + getFileExtension(file.getOriginalFilename());

            Path filePath = Paths.get(userDir, fileName);
            
            log.debug("Writing file to disk at: {}", filePath);
            Files.write(filePath, file.getBytes());

            // 5️⃣ Create or update KYC entity
            KycDocument kyc = (existingKyc != null) ? existingKyc : new KycDocument();

            kyc.setUser(user);
            kyc.setDocumentType("AADHAR");
            kyc.setFileName(fileName);
            kyc.setFilePath(filePath.toString());
            kyc.setContentType(file.getContentType());
            kyc.setFileSize(file.getSize());
            kyc.setStatus(KycStatus.PENDING);
            kyc.setRejectionReason(null); 
            kyc.setReviewedAt(null);

            kycDocumentRepository.save(kyc);

            // 6️⃣ Reset user KYC flag (important for re-upload case)
            user.setKycVerified(false);
            appUserRepository.save(user);
            
            log.info("KYC upload completed successfully for user ID: {}. New status: PENDING", user.getId());

            return new KycDocumentUploadResponseDto(
                    "KYC document uploaded successfully",
                    KycStatus.PENDING
            );

        } catch (IOException ex) {
        	log.error("IO Exception during KYC upload for user {}: {}", user.getId(), ex.getMessage());
            throw new FileNotUploadedException(
                    ErrorMessage.FAIL_TO_UPLOAD_KYC_DOCUMENT
            );
        }
	}
	
	private void validateFile(MultipartFile file) throws FileNotUploadedException, LargerFileSizeException, InvalidFileTypeException {
		log.debug("Validating file: {}", file.getOriginalFilename());
		
		if (file.isEmpty()) {
			throw new FileNotUploadedException(ErrorMessage.FILE_IS_REQUIRED);
		} 
		
		if (file.getSize() > 2 * 1024 * 1024) {
			throw new LargerFileSizeException(ErrorMessage.FILE_SIZE_MUST_BE_LESS_THAN_2MB);
		}
		
		String contentType = file.getContentType();
		
		if (!(
				"image/jpeg".equals(contentType) || 
				"image/png".equals(contentType) || 
				"application/pdf".equals(contentType)
				)) {
			log.warn("Invalid content type attempt: {}", contentType);
			throw new InvalidFileTypeException(ErrorMessage.INVALID_FILE_TYPE);
		}
		
		String filename = file.getOriginalFilename().toLowerCase();

		if (!(filename.endsWith(".jpg") || filename.endsWith(".jpeg")
		   || filename.endsWith(".png") || filename.endsWith(".pdf"))) {
		    throw new InvalidFileTypeException(ErrorMessage.INVALID_FILE_TYPE);
		}

	}
	
	private String getFileExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
            return "";
        }
		return filename.substring(filename.lastIndexOf("."));
	}

	@Override
	public KycDocumentViewDto viewMyKyc(Authentication authentication) throws UserNotFoundException, ResourceNotFoundException {
		AppUser user = currentUserService.getCurrentUser(authentication);
		log.debug("User ID: {} is viewing their KYC metadata", user.getId());
		
		KycDocument kyc = kycDocumentRepository
					.findByUser(user)
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
	public KycFileDataDto getMyKycFile(Authentication authentication) throws ResourceNotFoundException, UserNotFoundException {
		AppUser user = currentUserService.getCurrentUser(authentication);
		log.debug("Fetching physical file path for user ID: {}", user.getId());
		
		KycDocument kyc = kycDocumentRepository
					.findByUser(user)
					.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.KYC_NOT_FOUND));
		
		return new KycFileDataDto(
					kyc.getFilePath(),
					kyc.getFileName(),
					kyc.getContentType()
				);
	}
	
}
