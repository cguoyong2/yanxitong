package com.yanxitong.template.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.admin.AbstractAdminCrudController;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.template.entity.InvitationTemplate;
import com.yanxitong.template.entity.TemplateType;
import com.yanxitong.template.mapper.InvitationTemplateMapper;
import com.yanxitong.template.mapper.TemplateTypeMapper;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/template-types")
public class AdminTemplateTypeController extends AbstractAdminCrudController<TemplateType> {
    private final TemplateTypeMapper templateTypeMapper;
    private final InvitationTemplateMapper invitationTemplateMapper;

    public AdminTemplateTypeController(TemplateTypeMapper mapper, OperationLogService operationLogService,
            InvitationTemplateMapper invitationTemplateMapper) {
        super(mapper, operationLogService);
        this.templateTypeMapper = mapper;
        this.invitationTemplateMapper = invitationTemplateMapper;
    }

    @Override
    protected OperationModule module() {
        return OperationModule.TEMPLATE;
    }

    @Override
    protected String targetType() {
        return "template_type";
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("typeCode", "name");
    }

    @Override
    protected List<String> uniqueFields() {
        return List.of("typeCode");
    }

    @Override
    protected void beforeSave(TemplateType entity) {
        if (isChanged(entity, "typeCode")) {
            TemplateType existing = templateTypeMapper.selectById(entity.id);
            if (existing != null && invitationTemplateMapper.selectCount(new QueryWrapper<InvitationTemplate>().eq("type_code", existing.typeCode)) > 0) {
                throw new IllegalArgumentException("Template type is used by templates and cannot be changed");
            }
        }
    }

    @Override
    protected void beforeDelete(Long id) {
        TemplateType entity = templateTypeMapper.selectById(id);
        if (entity != null && invitationTemplateMapper.selectCount(new QueryWrapper<InvitationTemplate>().eq("type_code", entity.typeCode)) > 0) {
            throw new IllegalArgumentException("Template type is used by templates and cannot be deleted");
        }
    }
}
