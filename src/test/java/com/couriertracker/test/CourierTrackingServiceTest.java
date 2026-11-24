package com.couriertracker.test;


import com.couriertracker.domain.CourierLocation;
import com.couriertracker.domain.StoreEntranceLog;
import com.couriertracker.pattern.observer.LoggingCourierEntranceListener;
import com.couriertracker.pattern.strategy.HaversineDistanceStrategy;
import com.couriertracker.repository.StoreRepository;
import com.couriertracker.service.CourierTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierTrackingServiceTest {

    private CourierTrackingService service;

    @BeforeEach
    void setUp() {
        StoreRepository storeRepository = new StoreRepository(
                new ObjectMapper(),
                new DefaultResourceLoader(),
                "classpath:stores.json"
        );
        storeRepository.loadStores();

        service = new CourierTrackingService(
                storeRepository,
                new HaversineDistanceStrategy(),
                List.of(new LoggingCourierEntranceListener()),
                100.0,
                60L
        );
    }

    @Test
    void shouldLogEntranceWithin100MetersAndIgnoreReentryWithinOneMinute() {
        Instant t0 = Instant.parse("2026-11-23T10:00:00Z");

        List<StoreEntranceLog> first = service.processLocation(
                new CourierLocation(t0, "courier-1", 40.9924000, 29.1244500)
        );
        assertEquals(1, first.size());
        assertEquals("Ataşehir MMM Migros", first.get(0).storeName());

        List<StoreEntranceLog> withinMinute = service.processLocation(
                new CourierLocation(t0.plusSeconds(30), "courier-1", 40.9923500, 29.1244300)
        );
        assertTrue(withinMinute.isEmpty());

        List<StoreEntranceLog> afterMinute = service.processLocation(
                new CourierLocation(t0.plusSeconds(65), "courier-1", 40.9923400, 29.1244250)
        );
        assertEquals(1, afterMinute.size());
        assertEquals("Ataşehir MMM Migros", afterMinute.get(0).storeName());
    }

    @Test
    void shouldAccumulateTotalTravelDistance() {
        Instant t0 = Instant.parse("2026-11-23T10:00:00Z");

        service.processLocation(new CourierLocation(t0, "courier-2", 40.9923307, 29.1244229));
        service.processLocation(new CourierLocation(t0.plusSeconds(120), "courier-2", 40.986106, 29.1161293));

        Double totalDistance = service.getTotalTravelDistance("courier-2");
        assertTrue(totalDistance > 900.0 && totalDistance < 1100.0);
    }
}