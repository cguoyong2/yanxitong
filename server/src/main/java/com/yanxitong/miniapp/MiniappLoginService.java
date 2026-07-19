package com.yanxitong.miniapp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.miniapp.dto.MiniappSessionResult;
import com.yanxitong.miniapp.entity.MiniappUser;
import com.yanxitong.miniapp.mapper.MiniappUserMapper;
import com.yanxitong.wechat.WechatMiniappAuthService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiniappLoginService {
    private final WechatMiniappAuthService wechatAuthService;
    private final MiniappUserMapper userMapper;
    private final MiniappTokenService tokenService;

    public MiniappLoginService(
            WechatMiniappAuthService wechatAuthService,
            MiniappUserMapper userMapper,
            MiniappTokenService tokenService
    ) {
        this.wechatAuthService = wechatAuthService;
        this.userMapper = userMapper;
        this.tokenService = tokenService;
    }

    @Transactional
    public MiniappSessionResult login(String code) {
        String openId = wechatAuthService.resolveOpenId(code);
        MiniappUser user = userMapper.selectOne(new QueryWrapper<MiniappUser>()
                .eq("open_id", openId)
                .last("LIMIT 1"));
        if (user == null) {
            user = new MiniappUser();
            user.openId = openId;
            user.roleCode = "USER";
            user.status = "ACTIVE";
            user.lastLoginAt = LocalDateTime.now();
            userMapper.insert(user);
        } else {
            if (!"ACTIVE".equals(user.status)) {
                throw new IllegalStateException("当前小程序账号已停用");
            }
            user.lastLoginAt = LocalDateTime.now();
            userMapper.updateById(user);
        }
        MiniappPrincipal principal = new MiniappPrincipal(user.id, user.tenantId, user.openId, user.roleCode);
        String token = tokenService.issue(principal);
        return new MiniappSessionResult(token, MiniappTokenService.TOKEN_TTL.toSeconds(), user.id, user.openId, user.roleCode);
    }
}
