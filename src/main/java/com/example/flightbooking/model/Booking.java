package com.example.flightbooking.model;

import java.time.LocalDateTime;

public class Booking {

    private final String bookingId;
    private final String flightNumber;
    private final String passengerName;
    private final int seatCount;
    private final LocalDateTime bookedAt;

    public Booking(String bookingId, String flightNumber, String passengerName,
                   int seatCount, LocalDateTime bookedAt) {
        this.bookingId = bookingId;
        this.flightNumber = flightNumber;
        this.passengerName = passengerName;
        this.seatCount = seatCount;
        this.bookedAt = bookedAt;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }
}
