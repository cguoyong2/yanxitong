package com.yanxitong.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("payment_order")
public class PaymentOrder {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long banquetId;
    public String orderNo;
    public String clientRequestId;
    public String provider;
    public String scene;
    public String entrySource;
    public BigDecimal amount;
    public String currency;
    public String subject;
    public String payerName;
    public String payerOpenId;
    public String blessing;
    public String providerTradeNo;
    public String prepayId;
    public String payPayload;
    public String providerStatus;
    public LocalDateTime expiresAt;
    public String notifyUrl;
    public String payStatus;
    public LocalDateTime paidAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;
}
