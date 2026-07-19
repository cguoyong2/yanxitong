package com.yanxitong.banquet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class CreateBanquetRequest {
    @NotBlank
    public String name;
    @NotBlank
    public String eventTypeCode;
    @NotNull(message = "请选择宴席日期和时间")
    public LocalDateTime banquetTime;
    @NotBlank(message = "请选择或填写宴席地点")
    @Size(max = 255, message = "宴席地点不能超过255个字")
    public String location;
    public String customCopywriting;
    public Long templateId;
    public String favorBookScope;
    public Long favorFamilyBookId;
}
