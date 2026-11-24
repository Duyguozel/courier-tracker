package com.couriertracker.domain;

import java.time.Instant;

public record StoreEntranceLog(
        String courierId,
        String storeName,
        Instant enteredAt,
        double distanceMeters
) {}