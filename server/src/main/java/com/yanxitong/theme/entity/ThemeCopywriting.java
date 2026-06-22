package com.yanxitong.theme.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yanxitong.common.BaseConfigEntity;

@TableName("theme_copywriting")
public class ThemeCopywriting extends BaseConfigEntity {
    public String themeCode;
    public String eventTypeCode;
    public String sceneCode;
    public String title;
    public String content;
    public String speakerText;
    public Integer enabled;
}

