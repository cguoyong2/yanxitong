package com.yanxitong.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("device_order")
public class DeviceOrder {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long banquetId;
    public String orderNo;
    public Integer needDevice;
    public String deviceType;
    public LocalDateTime rentStartAt;
    public LocalDateTime rentEndAt;
    public BigDecimal price;
    public String priceUnit;
    public String deliveryMethod;
    public String payStatus;
    public String orderStatus;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;
}

