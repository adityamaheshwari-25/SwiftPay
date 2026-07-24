package com.example.demo.service.impl;

import com.example.demo.dto.KycDocumentUploadResponseDto;
import com.example.demo.dto.KycDocumentViewDto;
import com.example.demo.dto.KycFileDataDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.KycDocument;
import com.example.demo.entity.enums.KycStatus;
import com.example.demo.exception.*;

import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.KycDocumentRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.storage.DocumentStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *How do you test file uploads?
 *I mock MultipartFile and avoid touching real filesystem. For file write logic, I simulate byte arrays 
 *and validate repository interactions. For deeper validation, I may use @TempDir in JUnit 5.
 */
@ExtendWith(MockitoExtension.class)
class KycDocumentServiceImplTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private KycDocumentRepository kycDocumentRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private DocumentStorageService documentStorageService;
    @Mock private Authentication authentication;
    @Mock private MultipartFile file;

    @InjectMocks
    private KycDocumentServiceImpl service;

    private AppUser user;

    @BeforeEach
    void setup() {
        user = new AppUser();
        user.setId(1L);
        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);
    }

    // --------------------------------------------
    // uploadKyc - SUCCESS (new upload)
    // --------------------------------------------

    @Test
    void uploadKyc_shouldSaveDocument_whenValidAndNoExisting() throws Exception {

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("aadhaar.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getBytes()).thenReturn("dummy".getBytes());

        when(kycDocumentRepository.findByUser(user)).thenReturn(Optional.empty());

        KycDocumentUploadResponseDto response =
                service.uploadKyc(authentication, file);

        assertEquals(KycStatus.PENDING, response.getStatus());

        ArgumentCaptor<KycDocument> documentCaptor = ArgumentCaptor.forClass(KycDocument.class);
        verify(kycDocumentRepository).save(documentCaptor.capture());
        KycDocument savedDocument = documentCaptor.getValue();
        assertNotNull(savedDocument.getStorageKey());
        assertTrue(savedDocument.getStorageKey().startsWith("kyc/user-1/"));
        verify(documentStorageService).store(
                eq(savedDocument.getStorageKey()),
                eq("dummy".getBytes()),
                eq("application/pdf"));
        verify(appUserRepository).save(user);
        assertFalse(user.isKycVerified());
    }

    // --------------------------------------------
    // uploadKyc - already pending
    // --------------------------------------------

    @Test
    void uploadKyc_shouldThrow_whenAlreadyPending() throws Exception {
        KycDocument existing = new KycDocument();
        existing.setStatus(KycStatus.PENDING);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("aadhaar.pdf");
        when(file.getContentType()).thenReturn("application/pdf");

        when(kycDocumentRepository.findByUser(user))
                .thenReturn(Optional.of(existing));

        assertThrows(KycAlreadySubmittedException.class,
                () -> service.uploadKyc(authentication, file));
    }

    // --------------------------------------------
    // uploadKyc - file empty
    // --------------------------------------------

    @Test
    void uploadKyc_shouldThrow_whenFileEmpty() {
        when(file.isEmpty()).thenReturn(true);

        assertThrows(FileNotUploadedException.class,
                () -> service.uploadKyc(authentication, file));
    }

    // --------------------------------------------
    // uploadKyc - invalid file type
    // --------------------------------------------

    @Test
    void uploadKyc_shouldThrow_whenInvalidFileType() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("aadhaar.exe");
        when(file.getContentType()).thenReturn("application/octet-stream");

        assertThrows(InvalidFileTypeException.class,
                () -> service.uploadKyc(authentication, file));
    }

    // --------------------------------------------
    // uploadKyc - too large
    // --------------------------------------------

    @Test
    void uploadKyc_shouldThrow_whenFileTooLarge() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(3 * 1024 * 1024L);
        when(file.getOriginalFilename()).thenReturn("aadhaar.pdf");
//        when(file.getContentType()).thenReturn("application/pdf"); // if there is unnecessary stabbing you should remove that because that was not even used in the service only, then why in test.

        assertThrows(LargerFileSizeException.class,
                () -> service.uploadKyc(authentication, file));
    }

    // --------------------------------------------
    // viewMyKyc
    // --------------------------------------------

    @Test
    void viewMyKyc_shouldReturnMetadata_whenExists() throws Exception {
        KycDocument doc = new KycDocument();
        doc.setUser(user);
        doc.setDocumentType("AADHAR");
        doc.setFileName("aadhaar.pdf");
        doc.setContentType("application/pdf");
        doc.setFileSize(1024L);
        doc.setStatus(KycStatus.PENDING);

        when(kycDocumentRepository.findByUser(user))
                .thenReturn(Optional.of(doc));

        KycDocumentViewDto result =
                service.viewMyKyc(authentication);

        assertEquals("AADHAR", result.getDocumentType());
        assertEquals("aadhaar.pdf", result.getFileName());
    }

    @Test
    void viewMyKyc_shouldThrow_whenNotFound() {
        when(kycDocumentRepository.findByUser(user))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.viewMyKyc(authentication));
    }

    // --------------------------------------------
    // getMyKycFile
    // --------------------------------------------

    @Test
    void getMyKycFile_shouldReturnFileData_whenExists() throws Exception {
        KycDocument doc = new KycDocument();
        doc.setUser(user);
        doc.setStorageKey("kyc/user-1/document.pdf");
        doc.setFileName("test.pdf");
        doc.setContentType("application/pdf");

        when(kycDocumentRepository.findByUser(user))
                .thenReturn(Optional.of(doc));
        when(documentStorageService.load(doc.getStorageKey()))
                .thenReturn(Optional.of("document".getBytes()));

        KycFileDataDto result =
                service.getMyKycFile(authentication);

        assertArrayEquals("document".getBytes(), result.getFileData());
        assertEquals("test.pdf", result.getFileName());
    }

    @Test
    void getMyKycFile_shouldThrow_whenNotFound() {
        when(kycDocumentRepository.findByUser(user))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getMyKycFile(authentication));
    }
}
