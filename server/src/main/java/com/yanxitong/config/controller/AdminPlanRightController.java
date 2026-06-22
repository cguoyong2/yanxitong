package com.yanxitong.config.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.admin.AbstractAdminCrudController;
import com.yanxitong.config.entity.PlanRight;
import com.yanxitong.config.mapper.PlanRightMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/plan-rights")
public class AdminPlanRightController extends AbstractAdminCrudController<PlanRight> {
    private final PlanRightMapper planRightMapper;

    public AdminPlanRightController(PlanRightMapper mapper, OperationLogService operationLogService) {
        super(mapper, operationLogService);
        this.planRightMapper = mapper;
    }

    @Override
    protected OperationModule module() {
        return OperationModule.PLAN;
    }

    @Override
    protected String targetType() {
        return "plan_right";
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("planId", "rightCode", "rightName");
    }

    @Override
    protected void beforeSave(PlanRight entity) {
        QueryWrapper<PlanRight> query = new QueryWrapper<PlanRight>()
                .eq("plan_id", entity.planId)
                .eq("right_code", entity.rightCode);
        if (entity.id != null) {
            query.ne("id", entity.id);
        }
        if (planRightMapper.selectCount(query) > 0) {
            throw new IllegalArgumentException("planId,rightCode already exists");
        }
    }
}
