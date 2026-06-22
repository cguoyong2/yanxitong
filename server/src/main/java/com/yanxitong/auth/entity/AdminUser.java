package com.yanxitong.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("admin_user")
public class AdminUser {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public String username;
    public String passwordHash;
    public String displayName;
    public String status;
    public LocalDateTime lastLoginAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;
}
