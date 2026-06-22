package com.yanxitong.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yanxitong.common.BaseConfigEntity;

@TableName("config_item")
public class ConfigItem extends BaseConfigEntity {
    public String configKey;
    public String configValue;
    public String valueType;
    public String description;
    public Integer enabled;
}

