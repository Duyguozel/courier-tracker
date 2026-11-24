package com.couriertracker.dto;

import com.couriertracker.domain.StoreEntranceLog;

import java.time.Instant;

public record EntranceLogResponse(
        String courierId,
        String storeName,
        Instant enteredAt,
        double distanceMeters
) {
    public static EntranceLogResponse from(StoreEntranceLog log) {
        return new EntranceLogResponse(
                log.courierId(),
                log.storeName(),
                log.enteredAt(),
                log.distanceMeters()
        );
    }
}