package com.yanxitong.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yanxitong.common.BaseConfigEntity;

@TableName("template_type")
public class TemplateType extends BaseConfigEntity {
    public String typeCode;
    public String name;
    public Integer sortOrder;
    public Integer enabled;
}

