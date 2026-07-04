package com.yanxitong.favor.dto;

import jakarta.validation.constraints.NotBlank;

public class InviteFamilyMemberRequest {
    @NotBlank
    public String memberName;
    public String phone;
    public String relationship;
    public String role;
}
