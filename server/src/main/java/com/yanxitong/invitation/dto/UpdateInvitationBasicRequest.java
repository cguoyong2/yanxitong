package com.yanxitong.invitation.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateInvitationBasicRequest {
    @NotBlank
    public String title;
    public String coverUrl;
    public String hostName;
    public String contactPhone;
    public String addressDetail;
    public String scheduleText;
    public String greeting;
    public Boolean showGiftEntry;
    public Boolean showDeviceEntry;
}
