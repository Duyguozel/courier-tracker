package com.couriertracker.pattern.strategy;

public interface DistanceCalculationStrategy {
    double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2);
}