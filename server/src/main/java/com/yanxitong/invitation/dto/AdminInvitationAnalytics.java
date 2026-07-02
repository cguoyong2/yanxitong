package com.yanxitong.invitation.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminInvitationAnalytics(
        Long invitationId,
        Long banquetId,
        long visitCount,
        long uniqueIpCount,
        long rsvpCount,
        long rsvpGuestCount,
        long giftCount,
        BigDecimal giftAmount,
        double rsvpConversionRate,
        double giftConversionRate,
        List<TrendPoint> visitTrend,
        List<BreakdownItem> sourceBreakdown,
        List<BreakdownItem> shareChannelBreakdown,
        List<BreakdownItem> rsvpBreakdown,
        List<RecentVisit> recentVisits
) {
    public record TrendPoint(String date, long count) {
    }

    public record BreakdownItem(String label, long count) {
    }

    public record RecentVisit(String visitedAt, String ipAddress, String source, String userAgent) {
    }
}
