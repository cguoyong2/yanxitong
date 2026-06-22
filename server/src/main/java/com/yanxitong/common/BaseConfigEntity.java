package com.yanxitong.common;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;

public abstract class BaseConfigEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
