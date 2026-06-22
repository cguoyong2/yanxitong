package com.yanxitong.favor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("favor_contact")
public class FavorContact {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long ownerUserId;
    public String contactName;
    public String phone;
    public String remark;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;
}

