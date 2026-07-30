package com.example.flightbooking.model;

import java.time.LocalDateTime;

public record Booking(
        String bookingId,
        String flightNumber,
        String passengerName,
        int seatCount,
        LocalDateTime bookedAt
) {
}
