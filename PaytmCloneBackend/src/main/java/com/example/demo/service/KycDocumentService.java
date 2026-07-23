package com.example.demo.service;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.AdminKycListResponseDto;
import com.example.demo.dto.KycDocumentUploadResponseDto;
import com.example.demo.dto.KycDocumentViewDto;
import com.example.demo.dto.KycFileDataDto;
import com.example.demo.exception.FileNotUploadedException;
import com.example.demo.exception.InvalidFileTypeException;
import com.example.demo.exception.KycAlreadySubmittedException;
import com.example.demo.exception.LargerFileSizeException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;

public interface KycDocumentService {
	KycDocumentUploadResponseDto uploadKyc(Authentication authetication, MultipartFile file) throws UserNotFoundException, FileNotUploadedException, LargerFileSizeException, InvalidFileTypeException, KycAlreadySubmittedException, IOException;
	
	KycDocumentViewDto viewMyKyc(Authentication authentication) throws UserNotFoundException, ResourceNotFoundException;

	KycFileDataDto getMyKycFile(Authentication authentication) throws ResourceNotFoundException, UserNotFoundException;

}
