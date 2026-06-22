package com.yanxitong.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("plan_order")
public class PlanOrder {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long banquetId;
    public Long planId;
    public String orderNo;
    public BigDecimal amount;
    public String priceUnit;
    public String payStatus;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;
}

