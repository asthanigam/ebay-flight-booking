package com.example.flightbooking.dto;

import com.example.flightbooking.model.Booking;

import java.time.LocalDateTime;

public record BookingResponse(
        String bookingId,
        String flightNumber,
        String passengerName,
        int seatCount,
        LocalDateTime bookedAt
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.bookingId(),
                booking.flightNumber(),
                booking.passengerName(),
                booking.seatCount(),
                booking.bookedAt()
        );
    }
}
