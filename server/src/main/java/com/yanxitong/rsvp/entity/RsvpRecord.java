package com.yanxitong.rsvp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("rsvp_record")
public class RsvpRecord {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long tenantId;
    public Long banquetId;
    public Long invitationId;
    public String guestName;
    public String phone;
    public String attendanceStatus;
    public Integer mealRequired;
    public Integer accommodationRequired;
    public Integer guestCount;
    public String message;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    @TableField(exist = false)
    public Boolean created;
    @TableLogic
    public Integer deleted;
}
