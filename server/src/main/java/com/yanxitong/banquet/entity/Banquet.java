package com.yanxitong.banquet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("banquet")
public class Banquet {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long ownerUserId;
    public String banquetNo;
    public String name;
    public String eventTypeCode;
    public String themeCode;
    public LocalDateTime banquetTime;
    public String location;
    public String customCopywriting;
    public String favorBookScope;
    public Long favorFamilyBookId;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;
}
