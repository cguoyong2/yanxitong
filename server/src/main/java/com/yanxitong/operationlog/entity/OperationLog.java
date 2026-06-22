package com.yanxitong.operationlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long operatorId;
    public String operatorType;
    public String module;
    public String action;
    public String targetType;
    public Long targetId;
    public String summary;
    public String detail;
    public String ipAddress;
    public String userAgent;
    public LocalDateTime createdAt;
}

