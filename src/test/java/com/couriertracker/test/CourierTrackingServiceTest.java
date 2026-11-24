package com.couriertracker.test;


import com.couriertracker.domain.CourierLocation;
import com.couriertracker.domain.Store;
import com.couriertracker.domain.StoreEntranceLog;
import com.couriertracker.pattern.observer.CourierEntranceEvent;
import com.couriertracker.pattern.observer.CourierEntranceListener;
import com.couriertracker.pattern.strategy.DistanceCalculationStrategy;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourierTrackingServiceTest {

    private CourierTrackingService service;
    private CourierEntranceListener entranceListener;

    @BeforeEach
    void setUp() {
        StoreRepository storeRepository = new StoreRepository(
                new ObjectMapper(),
                new DefaultResourceLoader(),
                "classpath:stores.json"
        );
        storeRepository.loadStores();
        entranceListener = mock(CourierEntranceListener.class);

        service = new CourierTrackingService(
                storeRepository,
                new HaversineDistanceStrategy(),
                List.of(entranceListener),
                100.0,
                60L
        );
    }

    @Test
    void shouldLogFirstEntryAndNotifyListener() {
        Instant t0 = Instant.parse("2026-11-23T10:00:00Z");
        List<StoreEntranceLog> first = service.processLocation(
                new CourierLocation(t0, "courier-1", 40.9924000, 29.1244500)
        );

        assertEquals(1, first.size());
        assertEquals("Ataşehir MMM Migros", first.get(0).storeName());
        assertEquals("courier-1", first.get(0).courierId());
        assertEquals(t0, first.get(0).enteredAt());
        assertTrue(first.get(0).distanceMeters() <= 100.0);
        verify(entranceListener).onCourierEnteredStore(new CourierEntranceEvent(first.get(0)));
    }

    @Test
    void shouldNotLogAgainWhileCourierRemainsInsideAfterCooldown() {
        Instant t0 = Instant.parse("2026-11-23T10:00:00Z");
        service.processLocation(new CourierLocation(t0, "courier-1", 40.9924000, 29.1244500));

        List<StoreEntranceLog> stillInside = service.processLocation(
                new CourierLocation(t0.plusSeconds(120), "courier-1", 40.9923400, 29.1244250)
        );

        assertTrue(stillInside.isEmpty());
        assertEquals(1, service.getEntranceLogs("courier-1").size());
        verify(entranceListener).onCourierEnteredStore(any(CourierEntranceEvent.class));
    }

    @Test
    void shouldIgnoreReentryBeforeCooldownAndLogAfterCooldown() {
        Instant t0 = Instant.parse("2026-11-23T10:00:00Z");
        service.processLocation(new CourierLocation(t0, "courier-1", 40.9924000, 29.1244500));
        service.processLocation(new CourierLocation(t0.plusSeconds(10), "courier-1", 41.0100000, 29.1244500));

        List<StoreEntranceLog> quickReentry = service.processLocation(
                new CourierLocation(t0.plusSeconds(30), "courier-1", 40.9924000, 29.1244500)
        );
        assertTrue(quickReentry.isEmpty());

        service.processLocation(new CourierLocation(t0.plusSeconds(40), "courier-1", 41.0100000, 29.1244500));
        List<StoreEntranceLog> reentryAfterCooldown = service.processLocation(
                new CourierLocation(t0.plusSeconds(60), "courier-1", 40.9924000, 29.1244500)
        );
        assertEquals(1, reentryAfterCooldown.size());
        assertEquals("Ataşehir MMM Migros", reentryAfterCooldown.get(0).storeName());
    }

    @Test
    void shouldNotLogWhenCourierIsOutsideStoreRadius() {
        Instant t0 = Instant.parse("2026-11-23T10:00:00Z");
        List<StoreEntranceLog> logs = service.processLocation(
                new CourierLocation(t0, "courier-1", 41.0100000, 29.1244500)
        );

        assertTrue(logs.isEmpty());
        verify(entranceListener, never()).onCourierEnteredStore(any(CourierEntranceEvent.class));
    }

    @Test
    void shouldIncludeLocationExactlyAtConfiguredRadius() {
        StoreRepository storeRepository = mock(StoreRepository.class);
        DistanceCalculationStrategy distanceStrategy = mock(DistanceCalculationStrategy.class);
        when(storeRepository.findAll()).thenReturn(List.of(new Store("Test Store", 0.0, 0.0)));
        when(distanceStrategy.calculateDistanceMeters(1.0, 2.0, 0.0, 0.0)).thenReturn(100.0);
        CourierTrackingService boundaryService = new CourierTrackingService(
                storeRepository,
                distanceStrategy,
                List.of(),
                100.0,
                60L
        );

        List<StoreEntranceLog> logs = boundaryService.processLocation(
                new CourierLocation(Instant.parse("2026-11-23T10:00:00Z"), "courier-1", 1.0, 2.0)
        );

        assertEquals(1, logs.size());
        assertEquals(100.0, logs.get(0).distanceMeters());
    }

    @Test
    void shouldTrackEntryCooldownIndependentlyForEachCourier() {
        Instant t0 = Instant.parse("2026-11-23T10:00:00Z");
        service.processLocation(new CourierLocation(t0, "courier-1", 40.9924000, 29.1244500));

        List<StoreEntranceLog> secondCourierEntry = service.processLocation(
                new CourierLocation(t0.plusSeconds(1), "courier-2", 40.9924000, 29.1244500)
        );

        assertEquals(1, secondCourierEntry.size());
        assertEquals("courier-2", secondCourierEntry.get(0).courierId());
    }

    @Test
    void shouldAccumulateDistancePerCourierAndReturnZeroForUnknownCourier() {
        Instant t0 = Instant.parse("2026-11-23T10:00:00Z");
        service.processLocation(new CourierLocation(t0, "courier-1", 40.9923307, 29.1244229));
        service.processLocation(new CourierLocation(t0.plusSeconds(60), "courier-1", 40.986106, 29.1161293));
        service.processLocation(new CourierLocation(t0.plusSeconds(120), "courier-1", 41.0066851, 28.6552262));

        Double totalDistance = service.getTotalTravelDistance("courier-1");
        assertTrue(totalDistance > 30_000.0);
        assertEquals(0.0, service.getTotalTravelDistance("unknown-courier"));
        assertEquals(0.0, service.getTotalTravelDistance("courier-2"));
    }

    @Test
    void shouldFilterEntranceLogsByCourierAndReturnAllForNullOrBlankFilter() {
        Instant t0 = Instant.parse("2026-11-23T10:00:00Z");
        service.processLocation(new CourierLocation(t0, "courier-1", 40.9924000, 29.1244500));
        service.processLocation(new CourierLocation(t0.plusSeconds(1), "courier-2", 40.986106, 29.1161293));

        assertEquals(1, service.getEntranceLogs("courier-1").size());
        assertEquals(1, service.getEntranceLogs("courier-2").size());
        assertEquals(2, service.getEntranceLogs(null).size());
        assertEquals(2, service.getEntranceLogs(" ").size());
        assertTrue(service.getEntranceLogs("unknown-courier").isEmpty());
    }
}