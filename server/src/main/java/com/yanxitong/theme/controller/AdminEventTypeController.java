package com.yanxitong.theme.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.admin.AbstractAdminCrudController;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.theme.entity.EventType;
import com.yanxitong.theme.mapper.EventTypeMapper;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/event-types")
public class AdminEventTypeController extends AbstractAdminCrudController<EventType> {
    private final EventTypeMapper eventTypeMapper;
    private final BanquetMapper banquetMapper;

    public AdminEventTypeController(EventTypeMapper mapper, OperationLogService operationLogService, BanquetMapper banquetMapper) {
        super(mapper, operationLogService);
        this.eventTypeMapper = mapper;
        this.banquetMapper = banquetMapper;
    }

    @Override
    protected OperationModule module() {
        return OperationModule.EVENT_TYPE;
    }

    @Override
    protected String targetType() {
        return "event_type";
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("eventTypeCode", "name", "defaultThemeCode");
    }

    @Override
    protected List<String> uniqueFields() {
        return List.of("eventTypeCode");
    }

    @Override
    protected void beforeSave(EventType entity) {
        if (isChanged(entity, "eventTypeCode")) {
            EventType existing = eventTypeMapper.selectById(entity.id);
            long banquetCount = banquetMapper.selectCount(new QueryWrapper<Banquet>().eq("event_type_code", existing.eventTypeCode));
            if (banquetCount > 0) {
                throw new IllegalArgumentException("Event type code is used by banquets and cannot be changed");
            }
        }
    }

    @Override
    protected void beforeDelete(Long id) {
        EventType entity = eventTypeMapper.selectById(id);
        if (entity != null && banquetMapper.selectCount(new QueryWrapper<Banquet>().eq("event_type_code", entity.eventTypeCode)) > 0) {
            throw new IllegalArgumentException("Event type is used by banquets and cannot be deleted");
        }
    }
}
