package com.yanxitong.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("payment_callback_log")
public class PaymentCallbackLog {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public String provider;
    public String requestId;
    public String orderNo;
    public String providerTradeNo;
    public String providerEventId;
    public String providerSerialNo;
    public String eventType;
    public String resourceType;
    public String headers;
    public String rawBody;
    public String decryptedBody;
    public String signature;
    public String verifyStatus;
    public String processStatus;
    public String errorMessage;
    public String handleRemark;
    public LocalDateTime handledAt;
    public LocalDateTime createdAt;
}
