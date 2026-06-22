package com.yanxitong.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yanxitong.common.BaseConfigEntity;
import java.math.BigDecimal;

@TableName("plan")
public class Plan extends BaseConfigEntity {
    public String planCode;
    public String name;
    public BigDecimal price;
    public String priceUnit;
    public Integer recommended;
    public Integer sortOrder;
    public String status;
}

