package com.yanxitong.theme.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.admin.AbstractAdminCrudController;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.theme.entity.ThemeCopywriting;
import com.yanxitong.theme.mapper.ThemeCopywritingMapper;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/theme-copywriting")
public class AdminThemeCopywritingController extends AbstractAdminCrudController<ThemeCopywriting> {
    private final ThemeCopywritingMapper themeCopywritingMapper;

    public AdminThemeCopywritingController(ThemeCopywritingMapper mapper, OperationLogService operationLogService) {
        super(mapper, operationLogService);
        this.themeCopywritingMapper = mapper;
    }

    @Override
    protected OperationModule module() {
        return OperationModule.THEME;
    }

    @Override
    protected String targetType() {
        return "theme_copywriting";
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("themeCode", "eventTypeCode", "sceneCode");
    }

    @Override
    protected void beforeSave(ThemeCopywriting entity) {
        QueryWrapper<ThemeCopywriting> query = new QueryWrapper<ThemeCopywriting>()
                .eq("theme_code", entity.themeCode)
                .eq("event_type_code", entity.eventTypeCode)
                .eq("scene_code", entity.sceneCode);
        if (entity.id != null) {
            query.ne("id", entity.id);
        }
        if (themeCopywritingMapper.selectCount(query) > 0) {
            throw new IllegalArgumentException("themeCode,eventTypeCode,sceneCode already exists");
        }
    }
}
