package com.couriertracker.dto;

public record TravelDistanceResponse(
        String courierId,
        double totalDistanceMeters,
        double totalDistanceKilometers
) {}