package com.couriertracker.controller;


import com.couriertracker.domain.CourierLocation;
import com.couriertracker.domain.StoreEntranceLog;
import com.couriertracker.dto.EntranceLogResponse;
import com.couriertracker.dto.LocationEventRequest;
import com.couriertracker.dto.TravelDistanceResponse;
import com.couriertracker.service.CourierTrackingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CourierTrackingController {

    private final CourierTrackingService trackingService;

    public CourierTrackingController(CourierTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/locations")
    public ResponseEntity<List<EntranceLogResponse>> ingestLocation(@Valid @RequestBody LocationEventRequest request) {
        Instant timestamp = request.time() != null ? request.time() : Instant.now();
        CourierLocation location = new CourierLocation(
                timestamp,
                request.courier().trim(),
                request.lat(),
                request.lng()
        );

        List<StoreEntranceLog> entrances = trackingService.processLocation(location);
        List<EntranceLogResponse> response = entrances.stream()
                .map(EntranceLogResponse::from)
                .toList();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/couriers/{courierId}/distance")
    public ResponseEntity<TravelDistanceResponse> getTotalTravelDistance(@PathVariable String courierId) {
        Double totalMeters = trackingService.getTotalTravelDistance(courierId);
        return ResponseEntity.ok(new TravelDistanceResponse(
                courierId,
                totalMeters,
                totalMeters / 1000.0
        ));
    }

    @GetMapping("/entrances")
    public ResponseEntity<List<EntranceLogResponse>> getEntrances(
            @RequestParam(required = false) String courierId) {
        List<EntranceLogResponse> logs = trackingService.getEntranceLogs(courierId).stream()
                .map(EntranceLogResponse::from)
                .toList();
        return ResponseEntity.ok(logs);
    }
}