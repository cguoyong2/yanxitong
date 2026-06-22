package com.yanxitong.gift.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("gift_record")
public class GiftRecord {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long banquetId;
    public Long paymentOrderId;
    public String giftSource;
    public String guestName;
    public BigDecimal amount;
    public String blessing;
    public LocalDateTime receivedAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;
}

