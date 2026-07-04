package com.yanxitong.favor.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateFamilyBookRequest {
    @NotBlank
    public String bookName;
    public String description;
    public String ownerName;
    public String ownerPhone;
}
