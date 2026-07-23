package com.example.demo.service.impl;

import com.example.demo.dto.AdminKycListResponseDto;
import com.example.demo.dto.KycAdminActionResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.KycDocument;
import com.example.demo.entity.enums.KycStatus;
import com.example.demo.exception.*;

import com.example.demo.mapper.AdminKycMapper;
import com.example.demo.repository.KycDocumentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *How do you test @Transactional and @CacheEvict?
 *Unit tests verify business logic. Transaction and caching behavior require SpringBootTest integration tests with cache manager and database. 
 */

@ExtendWith(MockitoExtension.class)
class KycAdminServiceImplTest {

    @Mock private KycDocumentRepository kycDocumentRepository;
    @Mock private AdminKycMapper adminKycMapper;

    @InjectMocks
    private KycAdminServiceImpl service;

    // ---------------------------------------
    // approveKyc
    // ---------------------------------------

    @Test
    void approveKyc_shouldApprove_whenPending() throws Exception {
        Long userId = 1L;

        AppUser user = new AppUser();
        user.setId(userId);
        user.setKycVerified(false);

        KycDocument kyc = new KycDocument();
        kyc.setStatus(KycStatus.PENDING);
        kyc.setUser(user);

        when(kycDocumentRepository.findByUserId(userId))
                .thenReturn(Optional.of(kyc));

        KycAdminActionResponseDto response = service.approveKyc(userId);

        assertEquals(KycStatus.APPROVED, kyc.getStatus());
        assertNull(kyc.getRejectionReason());
        assertTrue(user.isKycVerified());
        assertEquals(KycStatus.APPROVED, response.getStatus());
    }

    @Test
    void approveKyc_shouldThrow_whenNotPending() {
        Long userId = 1L;

        KycDocument kyc = new KycDocument();
        kyc.setStatus(KycStatus.APPROVED);

        when(kycDocumentRepository.findByUserId(userId))
                .thenReturn(Optional.of(kyc));

        assertThrows(KycNotPendingException.class,
                () -> service.approveKyc(userId));
    }

    @Test
    void approveKyc_shouldThrow_whenNotFound() {
        when(kycDocumentRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.approveKyc(1L));
    }

    // ---------------------------------------
    // rejectKyc
    // ---------------------------------------

    @Test
    void rejectKyc_shouldReject_whenPendingAndReasonProvided() {
        Long userId = 2L;

        AppUser user = new AppUser();
        user.setId(userId);
        user.setKycVerified(true);

        KycDocument kyc = new KycDocument();
        kyc.setStatus(KycStatus.PENDING);
        kyc.setUser(user);

        when(kycDocumentRepository.findByUserId(userId))
                .thenReturn(Optional.of(kyc));

        KycAdminActionResponseDto response =
                service.rejectKyc(userId, "Invalid document");

        assertEquals(KycStatus.REJECTED, kyc.getStatus());
        assertEquals("Invalid document", kyc.getRejectionReason());
        assertFalse(user.isKycVerified());
        assertEquals(KycStatus.REJECTED, response.getStatus());
    }

    @Test
    void rejectKyc_shouldThrow_whenReasonNull() {
        Long userId = 2L;

        KycDocument kyc = new KycDocument();
        kyc.setStatus(KycStatus.PENDING);

        when(kycDocumentRepository.findByUserId(userId))
                .thenReturn(Optional.of(kyc));

        assertThrows(RejectWithoutReasonException.class,
                () -> service.rejectKyc(userId, null));
    }

    @Test
    void rejectKyc_shouldThrow_whenNotPending() {
        Long userId = 2L;

        KycDocument kyc = new KycDocument();
        kyc.setStatus(KycStatus.APPROVED);

        when(kycDocumentRepository.findByUserId(userId))
                .thenReturn(Optional.of(kyc));

        assertThrows(KycNotPendingException.class,
                () -> service.rejectKyc(userId, "reason"));
    }

    // ---------------------------------------
    // getPendingKycList
    // ---------------------------------------

    @Test
    void getPendingKycList_shouldReturnMappedList() {
        KycDocument doc = new KycDocument();
        doc.setStatus(KycStatus.PENDING);

        AdminKycListResponseDto dto = mock(AdminKycListResponseDto.class);

        when(kycDocumentRepository.findByStatus(KycStatus.PENDING))
                .thenReturn(List.of(doc));

        when(adminKycMapper.toAdminKycListDto(doc))
                .thenReturn(dto);

        List<AdminKycListResponseDto> result =
                service.getPendingKycList();

        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    // ---------------------------------------
    // viewKycByUserId (file streaming)
    // ---------------------------------------

    @Test
    void viewKycByUserId_shouldReturnResponseEntity_whenFileExists() throws Exception {
        Long userId = 10L;

        AppUser user = new AppUser();
        user.setId(userId);

        Path tempFile = Files.createTempFile("kyc-test", ".pdf");

        KycDocument kyc = new KycDocument();
        kyc.setUser(user);
        kyc.setFilePath(tempFile.toString());
        kyc.setContentType("application/pdf");
        kyc.setFileName("test.pdf");

        when(kycDocumentRepository.findByUserId(userId))
                .thenReturn(Optional.of(kyc));

        when(kycDocumentRepository.findByUser(user))
                .thenReturn(Optional.of(kyc));

        ResponseEntity<Resource> response =
                service.viewKycByUserId(userId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/pdf",
                response.getHeaders().getContentType().toString());
        assertNotNull(response.getBody());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void viewKycByUserId_shouldThrow_whenNotFound() {
        when(kycDocumentRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.viewKycByUserId(1L));
    }
}

