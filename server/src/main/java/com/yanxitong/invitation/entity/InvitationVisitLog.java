package com.yanxitong.invitation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("invitation_visit_log")
public class InvitationVisitLog {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long invitationId;
    public String visitorOpenId;
    public String ipAddress;
    public String userAgent;
    public LocalDateTime visitedAt;
}

