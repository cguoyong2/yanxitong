package com.yanxitong.favor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("favor_entry")
public class FavorEntry {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long contactId;
    public Long banquetId;
    public Long giftRecordId;
    public String direction;
    public String sourceType;
    public BigDecimal amount;
    public LocalDateTime occurredAt;
    public String note;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;
}

