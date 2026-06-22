package com.yanxitong.invitation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("invitation_share")
public class InvitationShare {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long invitationId;
    public String shareChannel;
    public String shareUrl;
    public LocalDateTime createdAt;
}

