package com.yanxitong.theme.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yanxitong.common.BaseConfigEntity;

@TableName("event_type")
public class EventType extends BaseConfigEntity {
    public String eventTypeCode;
    public String name;
    public String alias;
    public String defaultThemeCode;
    public String defaultCopywriting;
    public Integer sortOrder;
    public Integer enabled;
}

