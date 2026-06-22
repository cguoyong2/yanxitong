package com.yanxitong.theme.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yanxitong.common.BaseConfigEntity;

@TableName("theme")
public class Theme extends BaseConfigEntity {
    public String themeCode;
    public String name;
    public String primaryColor;
    public String secondaryColor;
    public String iconStyle;
    public String confirmScreenTemplate;
    public Integer enabled;
}

