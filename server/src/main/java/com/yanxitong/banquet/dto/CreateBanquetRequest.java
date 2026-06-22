package com.yanxitong.banquet.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class CreateBanquetRequest {
    @NotBlank
    public String name;
    @NotBlank
    public String eventTypeCode;
    public LocalDateTime banquetTime;
    public String location;
    public String customCopywriting;
    public Long templateId;
}

