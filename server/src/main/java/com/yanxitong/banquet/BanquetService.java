package com.yanxitong.banquet;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.banquet.dto.BanquetCreateResult;
import com.yanxitong.banquet.dto.BanquetDetailResult;
import com.yanxitong.banquet.dto.CreateBanquetRequest;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.entity.BanquetMember;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.banquet.mapper.BanquetMemberMapper;
import com.yanxitong.invitation.InvitationService;
import com.yanxitong.invitation.entity.Invitation;
import com.yanxitong.miniapp.MiniappPrincipalContext;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.tenant.TenantContext;
import com.yanxitong.theme.ThemeResolutionService;
import com.yanxitong.theme.entity.EventType;
import com.yanxitong.theme.entity.Theme;
import com.yanxitong.theme.mapper.ThemeMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BanquetService {
    private final BanquetMapper banquetMapper;
    private final BanquetMemberMapper banquetMemberMapper;
    private final ThemeMapper themeMapper;
    private final ThemeResolutionService themeResolutionService;
    private final InvitationService invitationService;
    private final OperationLogService operationLogService;

    public BanquetService(
            BanquetMapper banquetMapper,
            BanquetMemberMapper banquetMemberMapper,
            ThemeMapper themeMapper,
            ThemeResolutionService themeResolutionService,
            InvitationService invitationService,
            OperationLogService operationLogService
    ) {
        this.banquetMapper = banquetMapper;
        this.banquetMemberMapper = banquetMemberMapper;
        this.themeMapper = themeMapper;
        this.themeResolutionService = themeResolutionService;
        this.invitationService = invitationService;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public BanquetCreateResult create(CreateBanquetRequest request) {
        EventType eventType = themeResolutionService.requireEventType(request.eventTypeCode);
        Banquet banquet = new Banquet();
        banquet.tenantId = TenantContext.getTenantId();
        banquet.ownerUserId = MiniappPrincipalContext.requireUserId();
        banquet.banquetNo = nextBanquetNo();
        banquet.name = request.name;
        banquet.eventTypeCode = request.eventTypeCode;
        banquet.themeCode = eventType.defaultThemeCode;
        banquet.banquetTime = request.banquetTime;
        banquet.location = request.location;
        banquet.customCopywriting = request.customCopywriting;
        banquet.favorBookScope = "FAMILY".equals(request.favorBookScope) && request.favorFamilyBookId != null ? "FAMILY" : "PERSONAL";
        banquet.favorFamilyBookId = "FAMILY".equals(banquet.favorBookScope) ? request.favorFamilyBookId : null;
        banquet.status = "DRAFT";
        banquetMapper.insert(banquet);

        BanquetMember owner = new BanquetMember();
        owner.tenantId = banquet.tenantId;
        owner.banquetId = banquet.id;
        owner.userId = banquet.ownerUserId;
        owner.roleCode = "HOST";
        owner.permissions = "MANAGE,WRITE,READ";
        owner.status = "ACTIVE";
        banquetMemberMapper.insert(owner);

        Invitation invitation = invitationService.createBaseInvitation(banquet, request.templateId);
        operationLogService.record(OperationModule.BANQUET, "CREATE", "banquet", banquet.id, "create banquet");
        operationLogService.record(OperationModule.INVITATION, "CREATE", "invitation", invitation.id, "create base invitation");
        return new BanquetCreateResult(banquet, invitation, "/invite/" + invitation.shareSlug);
    }

    public List<Banquet> list() {
        QueryWrapper<Banquet> query = new QueryWrapper<>();
        Long userId = MiniappPrincipalContext.currentUserId();
        if (userId != null) {
            List<Long> memberBanquetIds = banquetMemberMapper.selectList(new QueryWrapper<BanquetMember>()
                            .eq("user_id", userId)
                            .eq("status", "ACTIVE"))
                    .stream()
                    .map(member -> member.banquetId)
                    .distinct()
                    .toList();
            query.and(wrapper -> {
                wrapper.eq("owner_user_id", userId);
                if (!memberBanquetIds.isEmpty()) {
                    wrapper.or().in("id", memberBanquetIds);
                }
            });
        }
        Long tenantId = TenantContext.getTenantId();
        if (userId == null && tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        query.orderByDesc("created_at");
        return banquetMapper.selectList(query);
    }

    public BanquetDetailResult detail(Long id) {
        Banquet banquet = banquetMapper.selectById(id);
        if (banquet == null || !canCurrentUserAccess(banquet)) {
            throw new IllegalArgumentException("Banquet not found");
        }
        Invitation invitation = invitationService.findByBanquetId(id);
        Theme theme = themeMapper.selectOne(new QueryWrapper<Theme>()
                .eq("theme_code", banquet.themeCode)
                .last("LIMIT 1"));
        return new BanquetDetailResult(
                banquet,
                invitation,
                theme,
                themeResolutionService.resolveGiftSuccess(banquet.customCopywriting, banquet.themeCode, banquet.eventTypeCode)
        );
    }

    @Transactional
    public BanquetDetailResult publish(Long id) {
        Banquet banquet = banquetMapper.selectById(id);
        if (banquet == null || !canCurrentUserAccess(banquet)) {
            throw new IllegalArgumentException("Banquet not found");
        }
        if (!"PUBLISHED".equals(banquet.status)) {
            String previousStatus = banquet.status;
            banquet.status = "PUBLISHED";
            banquetMapper.updateById(banquet);
            operationLogService.record(
                    OperationModule.BANQUET,
                    "PUBLISH",
                    "banquet",
                    banquet.id,
                    "publish banquet invitation",
                    java.util.Map.of("previousStatus", previousStatus == null ? "" : previousStatus, "status", banquet.status)
            );
        }
        invitationService.activateByBanquetId(banquet.id);
        return detail(id);
    }

    private String nextBanquetNo() {
        return "BQ" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private boolean canCurrentUserAccess(Banquet banquet) {
        Long userId = MiniappPrincipalContext.currentUserId();
        if (userId == null) {
            return true;
        }
        if (userId.equals(banquet.ownerUserId)) {
            return true;
        }
        return banquetMemberMapper.selectCount(new QueryWrapper<BanquetMember>()
                .eq("banquet_id", banquet.id)
                .eq("user_id", userId)
                .eq("status", "ACTIVE")) > 0;
    }
}
