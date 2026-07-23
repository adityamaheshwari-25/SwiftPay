package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SseEventDto {
    private String type;   // e.g. split.created / split.updated
    private Object data;   // e.g. {splitId:123}
}
