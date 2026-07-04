package com.yanxitong.favor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("favor_family_member")
public class FavorFamilyMember {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long familyBookId;
    public String memberName;
    public String phone;
    public String relationship;
    public String role;
    public String permissions;
    public String inviteStatus;
    public LocalDateTime joinedAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;
}
