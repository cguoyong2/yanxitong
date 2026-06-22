package com.yanxitong.rsvp.dto;

public record RsvpStatsResult(
        Long banquetId,
        long totalRecords,
        long attendingRecords,
        long pendingRecords,
        long declinedRecords,
        long totalGuests,
        long mealRequiredGuests,
        long accommodationRequiredGuests
) {
}
