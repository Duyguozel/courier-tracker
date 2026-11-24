package com.couriertracker.domain;

import java.time.Instant;

public record CourierLocation(
        Instant time,
        String courierId,
        double lat,
        double lng
) {}