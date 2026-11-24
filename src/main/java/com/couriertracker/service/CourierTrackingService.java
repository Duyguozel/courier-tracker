package com.couriertracker.service;

import com.couriertracker.domain.CourierLocation;
import com.couriertracker.domain.Store;
import com.couriertracker.domain.StoreEntranceLog;
import com.couriertracker.pattern.observer.CourierEntranceEvent;
import com.couriertracker.pattern.observer.CourierEntranceListener;
import com.couriertracker.pattern.strategy.DistanceCalculationStrategy;
import com.couriertracker.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CourierTrackingService {

    private final StoreRepository storeRepository;
    private final DistanceCalculationStrategy distanceStrategy;
    private final List<CourierEntranceListener> entranceListeners;
    private final double storeRadiusMeters;
    private final Duration reentryCooldown;

    private final Map<String, CourierLocation> lastLocationByCourier = new ConcurrentHashMap<>();
    private final Map<String, Double> totalDistanceMetersByCourier = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastEntranceTimeByCourierAndStore = new ConcurrentHashMap<>();
    private final List<StoreEntranceLog> entranceLogs = new CopyOnWriteArrayList<>();

    public CourierTrackingService(
            StoreRepository storeRepository,
            DistanceCalculationStrategy distanceStrategy,
            List<CourierEntranceListener> entranceListeners,
            @Value("${courier.tracking.store-radius-meters:100.0}") double storeRadiusMeters,
            @Value("${courier.tracking.reentry-cooldown-seconds:60}") long reentryCooldownSeconds) {
        this.storeRepository = storeRepository;
        this.distanceStrategy = distanceStrategy;
        this.entranceListeners = entranceListeners != null ? entranceListeners : List.of();
        this.storeRadiusMeters = storeRadiusMeters;
        this.reentryCooldown = Duration.ofSeconds(reentryCooldownSeconds);
    }

    public List<StoreEntranceLog> processLocation(CourierLocation currentLocation) {
        String courierId = currentLocation.courierId();
        List<StoreEntranceLog> newlyLoggedEntrances = new ArrayList<>();

        Object courierLock = getCourierLock(courierId);
        synchronized (courierLock) {
            CourierLocation previousLocation = lastLocationByCourier.put(courierId, currentLocation);
            totalDistanceMetersByCourier.putIfAbsent(courierId, 0.0);

            if (previousLocation != null) {
                double segmentDistance = distanceStrategy.calculateDistanceMeters(
                        previousLocation.lat(),
                        previousLocation.lng(),
                        currentLocation.lat(),
                        currentLocation.lng()
                );
                totalDistanceMetersByCourier.compute(courierId, (id, currentTotal) ->
                        (currentTotal == null ? 0.0 : currentTotal) + segmentDistance);
            }

            for (Store store : storeRepository.findAll()) {
                double distanceToStore = distanceStrategy.calculateDistanceMeters(
                        currentLocation.lat(),
                        currentLocation.lng(),
                        store.lat(),
                        store.lng()
                );

                if (distanceToStore <= storeRadiusMeters && canLogEntrance(courierId, store.name(), currentLocation.time())) {
                    StoreEntranceLog logEntry = new StoreEntranceLog(
                            courierId,
                            store.name(),
                            currentLocation.time(),
                            distanceToStore
                    );
                    entranceLogs.add(logEntry);
                    newlyLoggedEntrances.add(logEntry);
                    notifyListeners(new CourierEntranceEvent(logEntry));
                }
            }
        }

        return newlyLoggedEntrances;
    }

    private boolean canLogEntrance(String courierId, String storeName, Instant eventTime) {
        String key = courierId + "::" + storeName;
        Instant lastEntrance = lastEntranceTimeByCourierAndStore.get(key);

        if (lastEntrance == null || Duration.between(lastEntrance, eventTime).compareTo(reentryCooldown) >= 0) {
            lastEntranceTimeByCourierAndStore.put(key, eventTime);
            return true;
        }
        return false;
    }

    private void notifyListeners(CourierEntranceEvent event) {
        for (CourierEntranceListener listener : entranceListeners) {
            listener.onCourierEnteredStore(event);
        }
    }

    public Double getTotalTravelDistance(String courierId) {
        return totalDistanceMetersByCourier.getOrDefault(courierId, 0.0);
    }

    public List<StoreEntranceLog> getEntranceLogs(String courierId) {
        if (courierId == null || courierId.isBlank()) {
            return Collections.unmodifiableList(entranceLogs);
        }
        return entranceLogs.stream()
                .filter(entry -> entry.courierId().equals(courierId))
                .toList();
    }

    private final Map<String, Object> courierLocks = new ConcurrentHashMap<>();

    private Object getCourierLock(String courierId) {
        return courierLocks.computeIfAbsent(courierId, id -> new Object());
    }
}