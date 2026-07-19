package com.yanxitong.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateInvitationBasicRequest {
    @NotBlank
    public String title;
    public String coverUrl;
    public String hostName;
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
    public String contactPhone;
    public String addressDetail;
    public String scheduleText;
    public String greeting;
    public Boolean showGiftEntry;
    public Boolean showDeviceEntry;
}
