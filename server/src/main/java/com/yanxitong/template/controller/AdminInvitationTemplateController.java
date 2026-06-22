package com.yanxitong.template.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.admin.AbstractAdminCrudController;
import com.yanxitong.invitation.entity.Invitation;
import com.yanxitong.invitation.mapper.InvitationMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.template.entity.InvitationTemplate;
import com.yanxitong.template.mapper.InvitationTemplateMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/invitation-templates")
public class AdminInvitationTemplateController extends AbstractAdminCrudController<InvitationTemplate> {
    private final InvitationMapper invitationMapper;

    public AdminInvitationTemplateController(InvitationTemplateMapper mapper, OperationLogService operationLogService,
            InvitationMapper invitationMapper) {
        super(mapper, operationLogService);
        this.invitationMapper = invitationMapper;
    }

    @Override
    protected OperationModule module() {
        return OperationModule.TEMPLATE;
    }

    @Override
    protected String targetType() {
        return "invitation_template";
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("templateCode", "typeCode", "name", "priceType", "status");
    }

    @Override
    protected List<String> uniqueFields() {
        return List.of("templateCode");
    }

    @Override
    protected void beforeSave(InvitationTemplate entity) {
        if (entity.price == null) {
            entity.price = BigDecimal.ZERO;
        }
        if (entity.price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must be greater than or equal to 0");
        }
    }

    @Override
    protected void beforeDelete(Long id) {
        if (invitationMapper.selectCount(new QueryWrapper<Invitation>().eq("template_id", id)) > 0) {
            throw new IllegalArgumentException("Template is used by invitations and cannot be deleted");
        }
    }
}
