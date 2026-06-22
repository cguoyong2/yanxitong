package com.yanxitong.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yanxitong.common.BaseConfigEntity;

@TableName("plan_right")
public class PlanRight extends BaseConfigEntity {
    public Long planId;
    public String rightCode;
    public String rightName;
    public String rightValue;
}

