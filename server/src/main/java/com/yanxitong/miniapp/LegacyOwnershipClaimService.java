package com.yanxitong.miniapp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.entity.BanquetMember;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.banquet.mapper.BanquetMemberMapper;
import com.yanxitong.favor.entity.FavorContact;
import com.yanxitong.favor.entity.FavorFamilyBook;
import com.yanxitong.favor.mapper.FavorContactMapper;
import com.yanxitong.favor.mapper.FavorFamilyBookMapper;
import com.yanxitong.miniapp.entity.MiniappUser;
import com.yanxitong.miniapp.mapper.MiniappUserMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacyOwnershipClaimService {
    private final MiniappUserMapper userMapper;
    private final BanquetMapper banquetMapper;
    private final BanquetMemberMapper banquetMemberMapper;
    private final FavorContactMapper favorContactMapper;
    private final FavorFamilyBookMapper familyBookMapper;
    private final OperationLogService operationLogService;

    public LegacyOwnershipClaimService(
            MiniappUserMapper userMapper,
            BanquetMapper banquetMapper,
            BanquetMemberMapper banquetMemberMapper,
            FavorContactMapper favorContactMapper,
            FavorFamilyBookMapper familyBookMapper,
            OperationLogService operationLogService
    ) {
        this.userMapper = userMapper;
        this.banquetMapper = banquetMapper;
        this.banquetMemberMapper = banquetMemberMapper;
        this.favorContactMapper = favorContactMapper;
        this.familyBookMapper = familyBookMapper;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public LegacyOwnershipClaimResult claimForUser(Long userId) {
        MiniappUser user = userMapper.selectById(userId);
        if (user == null || !"ACTIVE".equals(user.status)) {
            throw new IllegalArgumentException("小程序用户不存在或已停用");
        }
        int banquetCount = banquetMapper.update(null, new UpdateWrapper<Banquet>()
                .isNull("owner_user_id")
                .set("owner_user_id", user.id));
        int memberCount = 0;
        for (Banquet banquet : banquetMapper.selectList(new QueryWrapper<Banquet>().eq("owner_user_id", user.id))) {
            if (banquetMemberMapper.selectCount(new QueryWrapper<BanquetMember>()
                    .eq("banquet_id", banquet.id)
                    .eq("user_id", user.id)) == 0) {
                BanquetMember member = new BanquetMember();
                member.tenantId = banquet.tenantId;
                member.banquetId = banquet.id;
                member.userId = user.id;
                member.roleCode = "HOST";
                member.permissions = "MANAGE,WRITE,READ";
                member.status = "ACTIVE";
                banquetMemberMapper.insert(member);
                memberCount++;
            }
        }
        int contactCount = favorContactMapper.update(null, new UpdateWrapper<FavorContact>()
                .isNull("owner_user_id")
                .set("owner_user_id", user.id));
        int familyBookCount = familyBookMapper.update(null, new UpdateWrapper<FavorFamilyBook>()
                .isNull("creator_user_id")
                .set("creator_user_id", user.id));
        LegacyOwnershipClaimResult result = new LegacyOwnershipClaimResult(
                user.id,
                banquetCount,
                memberCount,
                contactCount,
                familyBookCount
        );
        operationLogService.record(
                OperationModule.SECURITY,
                "CLAIM_LEGACY_MINIAPP_DATA",
                "miniapp_user",
                user.id,
                "assign legacy miniapp data ownership",
                Map.of(
                        "banquetCount", banquetCount,
                        "memberCount", memberCount,
                        "contactCount", contactCount,
                        "familyBookCount", familyBookCount
                )
        );
        return result;
    }
}
