package com.yanxitong.banquet;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.entity.BanquetMember;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.banquet.mapper.BanquetMemberMapper;
import com.yanxitong.miniapp.MiniappPrincipalContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BanquetAccessService {
    private final BanquetMapper banquetMapper;
    private final BanquetMemberMapper memberMapper;

    public BanquetAccessService(BanquetMapper banquetMapper, BanquetMemberMapper memberMapper) {
        this.banquetMapper = banquetMapper;
        this.memberMapper = memberMapper;
    }

    public Banquet requireAccessible(Long banquetId) {
        Banquet banquet = banquetMapper.selectById(banquetId);
        Long userId = MiniappPrincipalContext.requireUserId();
        if (banquet == null || !canAccess(banquet, userId)) {
            throw notFound();
        }
        return banquet;
    }

    public Banquet requirePublished(Long banquetId) {
        Banquet banquet = banquetMapper.selectById(banquetId);
        if (banquet == null || !"PUBLISHED".equals(banquet.status)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "宴席不存在或尚未发布");
        }
        return banquet;
    }

    public boolean currentUserCanAccess(Banquet banquet) {
        Long userId = MiniappPrincipalContext.currentUserId();
        return userId != null && canAccess(banquet, userId);
    }

    private boolean canAccess(Banquet banquet, Long userId) {
        if (userId.equals(banquet.ownerUserId)) {
            return true;
        }
        return memberMapper.selectCount(new QueryWrapper<BanquetMember>()
                .eq("banquet_id", banquet.id)
                .eq("user_id", userId)
                .eq("status", "ACTIVE")) > 0;
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "宴席不存在或无权访问");
    }
}
