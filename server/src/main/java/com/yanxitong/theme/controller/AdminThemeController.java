package com.yanxitong.theme.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.admin.AbstractAdminCrudController;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.theme.entity.Theme;
import com.yanxitong.theme.mapper.ThemeMapper;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/themes")
public class AdminThemeController extends AbstractAdminCrudController<Theme> {
    private final ThemeMapper themeMapper;
    private final BanquetMapper banquetMapper;

    public AdminThemeController(ThemeMapper mapper, OperationLogService operationLogService, BanquetMapper banquetMapper) {
        super(mapper, operationLogService);
        this.themeMapper = mapper;
        this.banquetMapper = banquetMapper;
    }

    @Override
    protected OperationModule module() {
        return OperationModule.THEME;
    }

    @Override
    protected String targetType() {
        return "theme";
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("themeCode", "name", "primaryColor");
    }

    @Override
    protected List<String> uniqueFields() {
        return List.of("themeCode");
    }

    @Override
    protected void beforeSave(Theme entity) {
        if (isChanged(entity, "themeCode")) {
            Theme existing = themeMapper.selectById(entity.id);
            if (existing != null && banquetMapper.selectCount(new QueryWrapper<Banquet>().eq("theme_code", existing.themeCode)) > 0) {
                throw new IllegalArgumentException("Theme code is used by banquets and cannot be changed");
            }
        }
    }

    @Override
    protected void beforeDelete(Long id) {
        Theme entity = themeMapper.selectById(id);
        if (entity != null && banquetMapper.selectCount(new QueryWrapper<Banquet>().eq("theme_code", entity.themeCode)) > 0) {
            throw new IllegalArgumentException("Theme is used by banquets and cannot be deleted");
        }
    }
}
