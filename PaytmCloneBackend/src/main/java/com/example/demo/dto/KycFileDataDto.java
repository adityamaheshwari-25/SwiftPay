package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class KycFileDataDto {
	private byte[] fileData;
	private String fileName;
	private String contentType;
}
