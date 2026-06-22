package com.yanxitong.config.controller;

import com.yanxitong.admin.AbstractAdminCrudController;
import com.yanxitong.config.entity.ConfigItem;
import com.yanxitong.config.mapper.ConfigItemMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/config-items")
public class AdminConfigItemController extends AbstractAdminCrudController<ConfigItem> {
    public AdminConfigItemController(ConfigItemMapper mapper, OperationLogService operationLogService) {
        super(mapper, operationLogService);
    }

    @Override
    protected OperationModule module() {
        return OperationModule.CONFIG;
    }

    @Override
    protected String targetType() {
        return "config_item";
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("configKey", "valueType");
    }

    @Override
    protected List<String> uniqueFields() {
        return List.of("configKey");
    }
}
