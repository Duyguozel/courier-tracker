package com.couriertracker.pattern.observer;

import com.couriertracker.domain.StoreEntranceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingCourierEntranceListener implements CourierEntranceListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingCourierEntranceListener.class);

    @Override
    public void onCourierEnteredStore(CourierEntranceEvent event) {
        StoreEntranceLog entry = event.entranceLog();
        log.info("COURIER_STORE_ENTRANCE | courierId={} | store={} | enteredAt={} | distanceMeters={}",
                entry.courierId(),
                entry.storeName(),
                entry.enteredAt(),
                String.format("%.2f", entry.distanceMeters()));
    }
}