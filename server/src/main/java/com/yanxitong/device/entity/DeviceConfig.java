package com.yanxitong.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yanxitong.common.BaseConfigEntity;
import java.math.BigDecimal;

@TableName("device_config")
public class DeviceConfig extends BaseConfigEntity {
    public String deviceType;
    public String name;
    public BigDecimal price;
    public String priceUnit;
    public String deliveryMethod;
    public Integer enabled;
}

