package com.yanxitong.miniapp.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.common.ApiResponse;
import com.yanxitong.miniapp.LegacyOwnershipClaimResult;
import com.yanxitong.miniapp.LegacyOwnershipClaimService;
import com.yanxitong.miniapp.entity.MiniappUser;
import com.yanxitong.miniapp.mapper.MiniappUserMapper;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/miniapp-users")
public class AdminMiniappUserController {
    private final MiniappUserMapper userMapper;
    private final LegacyOwnershipClaimService ownershipClaimService;

    public AdminMiniappUserController(
            MiniappUserMapper userMapper,
            LegacyOwnershipClaimService ownershipClaimService
    ) {
        this.userMapper = userMapper;
        this.ownershipClaimService = ownershipClaimService;
    }

    @GetMapping
    public ApiResponse<List<MiniappUser>> list() {
        return ApiResponse.ok(userMapper.selectList(new QueryWrapper<MiniappUser>()
                .orderByDesc("last_login_at")
                .orderByDesc("id")));
    }

    @PostMapping("/{userId}/claim-legacy")
    public ApiResponse<LegacyOwnershipClaimResult> claimLegacy(@PathVariable Long userId) {
        return ApiResponse.ok(ownershipClaimService.claimForUser(userId));
    }
}
