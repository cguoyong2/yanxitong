package com.yanxitong.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yanxitong.common.BaseConfigEntity;
import java.math.BigDecimal;

@TableName("invitation_template")
public class InvitationTemplate extends BaseConfigEntity {
    public String templateCode;
    public String typeCode;
    public String name;
    public String coverUrl;
    public String priceType;
    public BigDecimal price;
    public Integer sortOrder;
    public String status;
}

