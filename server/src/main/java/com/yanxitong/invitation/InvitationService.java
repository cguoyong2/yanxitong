package com.yanxitong.invitation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.invitation.dto.UpdateInvitationBasicRequest;
import com.yanxitong.invitation.entity.Invitation;
import com.yanxitong.invitation.entity.InvitationShare;
import com.yanxitong.invitation.entity.InvitationVisitLog;
import com.yanxitong.invitation.mapper.InvitationMapper;
import com.yanxitong.invitation.mapper.InvitationShareMapper;
import com.yanxitong.invitation.mapper.InvitationVisitLogMapper;
import com.yanxitong.banquet.BanquetAccessService;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InvitationService {
    private final InvitationMapper invitationMapper;
    private final InvitationShareMapper invitationShareMapper;
    private final InvitationVisitLogMapper invitationVisitLogMapper;
    private final ObjectMapper objectMapper;
    private final OperationLogService operationLogService;
    private final BanquetAccessService banquetAccessService;

    public InvitationService(
            InvitationMapper invitationMapper,
            InvitationShareMapper invitationShareMapper,
            InvitationVisitLogMapper invitationVisitLogMapper,
            ObjectMapper objectMapper,
            OperationLogService operationLogService,
            BanquetAccessService banquetAccessService
    ) {
        this.invitationMapper = invitationMapper;
        this.invitationShareMapper = invitationShareMapper;
        this.invitationVisitLogMapper = invitationVisitLogMapper;
        this.objectMapper = objectMapper;
        this.operationLogService = operationLogService;
        this.banquetAccessService = banquetAccessService;
    }

    public Invitation createBaseInvitation(Banquet banquet, Long templateId) {
        Invitation invitation = new Invitation();
        invitation.tenantId = banquet.tenantId;
        invitation.banquetId = banquet.id;
        invitation.templateId = templateId;
        invitation.title = banquet.name;
        invitation.basicFields = toBasicFields("", "", "", "", "诚邀您拨冗赴宴，共同见证这份重要时刻", true, true);
        invitation.shareSlug = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        invitation.status = "DRAFT";
        invitationMapper.insert(invitation);

        InvitationShare share = new InvitationShare();
        share.tenantId = banquet.tenantId;
        share.invitationId = invitation.id;
        share.shareChannel = "PUBLIC_LINK";
        share.shareUrl = "/invite/" + invitation.shareSlug;
        invitationShareMapper.insert(share);
        return invitation;
    }

    public Invitation findByBanquetId(Long banquetId) {
        return invitationMapper.selectOne(new QueryWrapper<Invitation>()
                .eq("banquet_id", banquetId)
                .last("LIMIT 1"));
    }

    public Invitation requireById(Long id) {
        Invitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            throw new IllegalArgumentException("Invitation not found");
        }
        banquetAccessService.requireAccessible(invitation.banquetId);
        return invitation;
    }

    public Invitation findByShareSlug(String shareSlug) {
        return invitationMapper.selectOne(new QueryWrapper<Invitation>()
                .eq("share_slug", shareSlug)
                .last("LIMIT 1"));
    }

    public Invitation requireByShareSlug(String shareSlug) {
        Invitation invitation = invitationMapper.selectOne(new QueryWrapper<Invitation>()
                .eq("share_slug", shareSlug)
                .eq("status", "ACTIVE")
                .last("LIMIT 1"));
        if (invitation == null) {
            throw new IllegalArgumentException("Invitation not found");
        }
        return invitation;
    }

    public Invitation updateBasic(Long id, UpdateInvitationBasicRequest request) {
        Invitation invitation = requireById(id);
        invitation.title = request.title;
        invitation.coverUrl = request.coverUrl;
        invitation.basicFields = toBasicFields(
                request.hostName,
                request.contactPhone,
                request.addressDetail,
                request.scheduleText,
                request.greeting,
                request.showGiftEntry,
                request.showDeviceEntry
        );
        invitationMapper.updateById(invitation);
        operationLogService.record(OperationModule.INVITATION, "UPDATE_BASIC", "invitation", invitation.id, "update invitation basic fields");
        return invitation;
    }

    public void activateByBanquetId(Long banquetId) {
        Invitation invitation = findByBanquetId(banquetId);
        if (invitation != null && !"ACTIVE".equals(invitation.status)) {
            invitation.status = "ACTIVE";
            invitationMapper.updateById(invitation);
        }
    }

    public void recordVisit(Invitation invitation, HttpServletRequest request) {
        InvitationVisitLog log = new InvitationVisitLog();
        log.tenantId = TenantContext.getTenantId();
        log.invitationId = invitation.id;
        log.ipAddress = request.getRemoteAddr();
        log.userAgent = request.getHeader("User-Agent");
        log.visitedAt = LocalDateTime.now();
        invitationVisitLogMapper.insert(log);
    }

    public Map<String, String> parseBasicFields(Invitation invitation) {
        if (invitation.basicFields == null || invitation.basicFields.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(invitation.basicFields, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private String toBasicFields(
            String hostName,
            String contactPhone,
            String addressDetail,
            String scheduleText,
            String greeting,
            Boolean showGiftEntry,
            Boolean showDeviceEntry
    ) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("hostName", hostName == null ? "" : hostName);
        fields.put("contactPhone", contactPhone == null ? "" : contactPhone);
        fields.put("addressDetail", addressDetail == null ? "" : addressDetail);
        fields.put("scheduleText", scheduleText == null ? "" : scheduleText);
        fields.put("greeting", greeting == null ? "" : greeting);
        fields.put("showGiftEntry", Boolean.FALSE.equals(showGiftEntry) ? "0" : "1");
        fields.put("showDeviceEntry", Boolean.FALSE.equals(showDeviceEntry) ? "0" : "1");
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize invitation basic fields", e);
        }
    }
}
