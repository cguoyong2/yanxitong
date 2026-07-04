package com.yanxitong.favor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("favor_family_book")
public class FavorFamilyBook {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long creatorUserId;
    public String bookName;
    public String description;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableLogic
    public Integer deleted;
}
