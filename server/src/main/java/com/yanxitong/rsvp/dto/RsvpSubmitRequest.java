package com.yanxitong.rsvp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RsvpSubmitRequest {
    @NotNull
    public Long banquetId;
    public Long invitationId;
    @NotBlank
    public String guestName;
    public String phone;
    @NotBlank
    public String attendanceStatus;
    public Integer mealRequired = 0;
    public Integer accommodationRequired = 0;
    @Min(1)
    public Integer guestCount = 1;
    public String message;
}

