package com.yanxitong.miniapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("miniapp_user")
public class MiniappUser {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public String openId;
    public String unionId;
    public String nickname;
    public String avatarUrl;
    public String phone;
    public String roleCode;
    public String status;
    public LocalDateTime lastLoginAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;
}
