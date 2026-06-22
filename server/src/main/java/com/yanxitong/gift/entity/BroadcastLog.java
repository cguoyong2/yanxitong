package com.yanxitong.gift.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("broadcast_log")
public class BroadcastLog {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long banquetId;
    public Long giftRecordId;
    public String deviceType;
    public String eventType;
    public String content;
    public String status;
    public LocalDateTime createdAt;
}

