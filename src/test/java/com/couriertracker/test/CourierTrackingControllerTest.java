package com.couriertracker.test;

import com.couriertracker.domain.CourierLocation;
import com.couriertracker.domain.StoreEntranceLog;
import com.couriertracker.service.CourierTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class CourierTrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourierTrackingService trackingService;

    @Test
    void shouldAcceptValidLocationAndTrimCourierId() throws Exception {
        when(trackingService.processLocation(any(CourierLocation.class))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "time": "2026-11-23T10:00:00Z",
                                  "courier": " courier-1 ",
                                  "lat": 40.9924,
                                  "lng": 29.12445
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(trackingService).processLocation(new CourierLocation(
                Instant.parse("2026-11-23T10:00:00Z"),
                "courier-1",
                40.9924,
                29.12445
        ));
    }

    @Test
    void shouldRejectMissingTimeAndOutOfRangeCoordinates() throws Exception {
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courier": "courier-1",
                                  "lat": 40.0,
                                  "lng": 29.0
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "time": "2026-11-23T10:00:00Z",
                                  "courier": "courier-1",
                                  "lat": 91.0,
                                  "lng": 29.0
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "time": "2026-11-23T10:00:00Z",
                                  "courier": " ",
                                  "lat": -91.0,
                                  "lng": 181.0
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(trackingService, never()).processLocation(any(CourierLocation.class));
    }

    @Test
    void shouldReturnCourierDistanceInMetersAndKilometers() throws Exception {
        when(trackingService.getTotalTravelDistance("courier-1")).thenReturn(1250.0);

        mockMvc.perform(get("/api/v1/couriers/courier-1/distance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value("courier-1"))
                .andExpect(jsonPath("$.totalDistanceMeters").value(1250.0))
                .andExpect(jsonPath("$.totalDistanceKilometers").value(1.25));
    }

    @Test
    void shouldReturnEntranceLogsWithOptionalCourierFilter() throws Exception {
        StoreEntranceLog log = new StoreEntranceLog(
                "courier-1",
                "Ataşehir MMM Migros",
                Instant.parse("2026-11-23T10:00:00Z"),
                42.5
        );
        when(trackingService.getEntranceLogs("courier-1")).thenReturn(List.of(log));

        mockMvc.perform(get("/api/v1/entrances").param("courierId", "courier-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courierId").value("courier-1"))
                .andExpect(jsonPath("$[0].storeName").value("Ataşehir MMM Migros"))
                .andExpect(jsonPath("$[0].distanceMeters").value(42.5));

        verify(trackingService).getEntranceLogs(eq("courier-1"));
    }

    @Test
    void shouldReturnAllEntranceLogsWhenFilterIsOmitted() throws Exception {
        when(trackingService.getEntranceLogs(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/entrances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(trackingService).getEntranceLogs(null);
    }
}
