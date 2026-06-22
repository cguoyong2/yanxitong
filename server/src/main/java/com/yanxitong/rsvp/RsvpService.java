package com.yanxitong.rsvp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.common.PageResult;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.rsvp.dto.RsvpStatsResult;
import com.yanxitong.rsvp.dto.RsvpSubmitRequest;
import com.yanxitong.rsvp.entity.RsvpRecord;
import com.yanxitong.rsvp.mapper.RsvpRecordMapper;
import com.yanxitong.tenant.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RsvpService {
    private final RsvpRecordMapper rsvpRecordMapper;
    private final OperationLogService operationLogService;

    public RsvpService(RsvpRecordMapper rsvpRecordMapper, OperationLogService operationLogService) {
        this.rsvpRecordMapper = rsvpRecordMapper;
        this.operationLogService = operationLogService;
    }

    public RsvpRecord submit(RsvpSubmitRequest request) {
        if (!isSupportedStatus(request.attendanceStatus)) {
            throw new IllegalArgumentException("Unsupported RSVP attendance status");
        }
        RsvpRecord record = findExisting(request);
        boolean created = record == null;
        if (created) {
            record = new RsvpRecord();
            record.tenantId = TenantContext.getTenantId();
            record.banquetId = request.banquetId;
            record.invitationId = request.invitationId;
        }
        record.guestName = request.guestName;
        record.phone = request.phone;
        record.attendanceStatus = request.attendanceStatus;
        record.mealRequired = request.mealRequired == null ? 0 : request.mealRequired;
        record.accommodationRequired = request.accommodationRequired == null ? 0 : request.accommodationRequired;
        record.guestCount = request.guestCount == null ? 1 : request.guestCount;
        record.message = request.message;
        if (created) {
            rsvpRecordMapper.insert(record);
        } else {
            rsvpRecordMapper.updateById(record);
        }
        record.created = created;
        operationLogService.record(OperationModule.INVITATION, created ? "SUBMIT_RSVP" : "UPDATE_RSVP", "rsvp_record", record.id, "submit rsvp");
        return record;
    }

    public List<RsvpRecord> list(Long banquetId) {
        return rsvpRecordMapper.selectList(new QueryWrapper<RsvpRecord>()
                .eq("banquet_id", banquetId)
                .orderByDesc("created_at"));
    }

    public PageResult<RsvpRecord> adminPage(Long banquetId, String status, String keyword, Integer page, Integer pageSize) {
        QueryWrapper<RsvpRecord> countQuery = adminQuery(banquetId, status, keyword);
        long total = rsvpRecordMapper.selectCount(countQuery);
        QueryWrapper<RsvpRecord> query = adminQuery(banquetId, status, keyword);
        int normalizedPageSize = PageResult.normalizePageSize(pageSize);
        query.orderByDesc("created_at").last("LIMIT " + PageResult.offset(page, pageSize) + ", " + normalizedPageSize);
        return PageResult.of(rsvpRecordMapper.selectList(query), total, page, pageSize);
    }

    private QueryWrapper<RsvpRecord> adminQuery(Long banquetId, String status, String keyword) {
        QueryWrapper<RsvpRecord> query = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        if (banquetId != null) {
            query.eq("banquet_id", banquetId);
        }
        if (status != null && !status.isBlank()) {
            query.eq("attendance_status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like("guest_name", keyword).or().like("phone", keyword));
        }
        return query;
    }

    public RsvpStatsResult stats(Long banquetId) {
        List<RsvpRecord> records = list(banquetId);
        long attendingRecords = records.stream().filter(record -> isAttending(record.attendanceStatus)).count();
        long pendingRecords = records.stream().filter(record -> "PENDING".equals(record.attendanceStatus)).count();
        long declinedRecords = records.stream().filter(record -> "DECLINED".equals(record.attendanceStatus)).count();
        long totalGuests = records.stream()
                .filter(record -> isAttending(record.attendanceStatus))
                .mapToLong(record -> record.guestCount == null ? 1 : record.guestCount)
                .sum();
        long mealRequiredGuests = records.stream()
                .filter(record -> Integer.valueOf(1).equals(record.mealRequired))
                .mapToLong(record -> record.guestCount == null ? 1 : record.guestCount)
                .sum();
        long accommodationRequiredGuests = records.stream()
                .filter(record -> Integer.valueOf(1).equals(record.accommodationRequired))
                .mapToLong(record -> record.guestCount == null ? 1 : record.guestCount)
                .sum();
        return new RsvpStatsResult(
                banquetId,
                records.size(),
                attendingRecords,
                pendingRecords,
                declinedRecords,
                totalGuests,
                mealRequiredGuests,
                accommodationRequiredGuests
        );
    }

    private boolean isAttending(String attendanceStatus) {
        return "ATTEND".equals(attendanceStatus) || "ATTENDING".equals(attendanceStatus);
    }

    private boolean isSupportedStatus(String attendanceStatus) {
        return isAttending(attendanceStatus) || "PENDING".equals(attendanceStatus) || "DECLINED".equals(attendanceStatus);
    }

    private RsvpRecord findExisting(RsvpSubmitRequest request) {
        QueryWrapper<RsvpRecord> query = new QueryWrapper<RsvpRecord>()
                .eq("banquet_id", request.banquetId)
                .last("LIMIT 1");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.eq("tenant_id", tenantId);
        }
        if (request.phone != null && !request.phone.isBlank()) {
            query.eq("phone", request.phone);
        } else {
            query.eq("guest_name", request.guestName);
            if (request.invitationId != null) {
                query.eq("invitation_id", request.invitationId);
            }
        }
        return rsvpRecordMapper.selectOne(query);
    }
}
