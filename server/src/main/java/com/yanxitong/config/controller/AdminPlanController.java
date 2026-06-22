package com.yanxitong.config.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.admin.AbstractAdminCrudController;
import com.yanxitong.config.entity.Plan;
import com.yanxitong.config.mapper.PlanMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.order.entity.PlanOrder;
import com.yanxitong.order.mapper.PlanOrderMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/plans")
public class AdminPlanController extends AbstractAdminCrudController<Plan> {
    private final PlanOrderMapper planOrderMapper;

    public AdminPlanController(PlanMapper mapper, OperationLogService operationLogService, PlanOrderMapper planOrderMapper) {
        super(mapper, operationLogService);
        this.planOrderMapper = planOrderMapper;
    }

    @Override
    protected OperationModule module() {
        return OperationModule.PLAN;
    }

    @Override
    protected String targetType() {
        return "plan";
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("planCode", "name", "price", "priceUnit", "status");
    }

    @Override
    protected List<String> uniqueFields() {
        return List.of("planCode");
    }

    @Override
    protected void beforeSave(Plan entity) {
        if (entity.price == null || entity.price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must be greater than or equal to 0");
        }
    }

    @Override
    protected void beforeDelete(Long id) {
        long orderCount = planOrderMapper.selectCount(new QueryWrapper<PlanOrder>().eq("plan_id", id));
        if (orderCount > 0) {
            throw new IllegalArgumentException("Plan is used by orders and cannot be deleted");
        }
    }
}
