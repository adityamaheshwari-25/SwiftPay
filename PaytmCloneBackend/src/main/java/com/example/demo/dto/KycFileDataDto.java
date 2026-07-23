package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class KycFileDataDto {
	private String filePath;
	private String fileName;
	private String contentType;
}
