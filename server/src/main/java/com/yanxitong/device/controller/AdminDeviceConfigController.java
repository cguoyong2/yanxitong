package com.yanxitong.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.admin.AbstractAdminCrudController;
import com.yanxitong.device.entity.DeviceConfig;
import com.yanxitong.device.entity.DeviceOrder;
import com.yanxitong.device.mapper.DeviceConfigMapper;
import com.yanxitong.device.mapper.DeviceOrderMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/device-configs")
public class AdminDeviceConfigController extends AbstractAdminCrudController<DeviceConfig> {
    private final DeviceConfigMapper deviceConfigMapper;
    private final DeviceOrderMapper deviceOrderMapper;

    public AdminDeviceConfigController(DeviceConfigMapper mapper, OperationLogService operationLogService,
            DeviceOrderMapper deviceOrderMapper) {
        super(mapper, operationLogService);
        this.deviceConfigMapper = mapper;
        this.deviceOrderMapper = deviceOrderMapper;
    }

    @Override
    protected OperationModule module() {
        return OperationModule.DEVICE;
    }

    @Override
    protected String targetType() {
        return "device_config";
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("deviceType", "name", "price", "priceUnit", "deliveryMethod");
    }

    @Override
    protected void beforeSave(DeviceConfig entity) {
        QueryWrapper<DeviceConfig> query = new QueryWrapper<DeviceConfig>()
                .eq("device_type", entity.deviceType)
                .eq("delivery_method", entity.deliveryMethod);
        if (entity.id != null) {
            query.ne("id", entity.id);
        }
        if (deviceConfigMapper.selectCount(query) > 0) {
            throw new IllegalArgumentException("deviceType,deliveryMethod already exists");
        }
        if (entity.price == null || entity.price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must be greater than or equal to 0");
        }
    }

    @Override
    protected void beforeDelete(Long id) {
        DeviceConfig entity = deviceConfigMapper.selectById(id);
        if (entity != null && deviceOrderMapper.selectCount(new QueryWrapper<DeviceOrder>().eq("device_type", entity.deviceType)) > 0) {
            throw new IllegalArgumentException("Device config is used by device orders and cannot be deleted");
        }
    }
}
