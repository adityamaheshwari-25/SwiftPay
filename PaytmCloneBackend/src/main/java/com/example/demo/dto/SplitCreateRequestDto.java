package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.example.demo.entity.enums.SplitType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//@Data
//public class SplitCreateRequestDto {
//	
//	@NotNull
//	@Min(1)
//	private BigDecimal amount;
//	
//	@NotEmpty
//	private List<String> memberMobiles; // users only
//	
//	private String note;
//}

@Data
public class SplitCreateRequestDto {

    @NotNull
    private BigDecimal amount;

    @NotEmpty
    private List<String> memberMobiles;

    @NotNull
    private SplitType splitType;

    // Only used when CUSTOM
    private Map<String, BigDecimal> customShares;

    private String note;
}

