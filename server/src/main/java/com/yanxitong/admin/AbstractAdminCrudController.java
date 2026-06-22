package com.yanxitong.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanxitong.common.ApiResponse;
import com.yanxitong.common.BaseConfigEntity;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.tenant.TenantContext;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public abstract class AbstractAdminCrudController<T extends BaseConfigEntity> {
    private final BaseMapper<T> mapper;
    private final OperationLogService operationLogService;

    protected AbstractAdminCrudController(BaseMapper<T> mapper, OperationLogService operationLogService) {
        this.mapper = mapper;
        this.operationLogService = operationLogService;
    }

    protected abstract OperationModule module();

    protected abstract String targetType();

    protected List<String> requiredFields() {
        return Collections.emptyList();
    }

    protected List<String> uniqueFields() {
        return Collections.emptyList();
    }

    protected void beforeSave(T entity) {
    }

    protected void beforeDelete(Long id) {
    }

    @GetMapping
    public ApiResponse<List<T>> list() {
        QueryWrapper<T> query = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        query.orderByDesc("updated_at").orderByDesc("created_at");
        return ApiResponse.ok(mapper.selectList(query));
    }

    @PostMapping
    public ApiResponse<T> save(@RequestBody T entity) {
        validateRequired(entity);
        validateUnique(entity);
        beforeSave(entity);
        entity.setTenantId(TenantContext.getTenantId());
        if (entity.id == null) {
            mapper.insert(entity);
            operationLogService.record(module(), "CREATE", targetType(), entity.id, "create " + targetType());
        } else {
            mapper.updateById(entity);
            operationLogService.record(module(), "UPDATE", targetType(), entity.id, "update " + targetType());
        }
        return ApiResponse.ok(entity);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        beforeDelete(id);
        mapper.deleteById(id);
        operationLogService.record(module(), "DELETE", targetType(), id, "delete " + targetType());
        return ApiResponse.ok(null);
    }

    private void validateRequired(T entity) {
        for (String field : requiredFields()) {
            Object value = fieldValue(entity, field);
            if (value == null || (value instanceof String text && text.isBlank())) {
                throw new IllegalArgumentException(field + " is required");
            }
        }
    }

    private void validateUnique(T entity) {
        if (uniqueFields().isEmpty()) {
            return;
        }
        QueryWrapper<T> query = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            query.isNull("tenant_id");
        } else {
            query.eq("tenant_id", tenantId);
        }
        for (String field : uniqueFields()) {
            query.eq(camelToSnake(field), fieldValue(entity, field));
        }
        if (entity.id != null) {
            query.ne("id", entity.id);
        }
        if (mapper.selectCount(query) > 0) {
            throw new IllegalArgumentException(String.join(",", uniqueFields()) + " already exists");
        }
    }

    protected Object fieldValue(Object entity, String fieldName) {
        try {
            Field field = entity.getClass().getField(fieldName);
            return field.get(entity);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Unknown field: " + fieldName);
        }
    }

    protected boolean isChanged(T entity, String fieldName) {
        if (entity.id == null) {
            return false;
        }
        T existing = mapper.selectById(entity.id);
        return existing != null && !Objects.equals(fieldValue(existing, fieldName), fieldValue(entity, fieldName));
    }

    protected String camelToSnake(String value) {
        return value.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
