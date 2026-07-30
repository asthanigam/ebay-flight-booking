package com.example.flightbooking.model;

import java.time.LocalDateTime;

public class Flight {

    private final String flightNumber;
    private final String origin;
    private final String destination;
    private final LocalDateTime departureTime;
    private final int totalSeats;
    private int availableSeats;

    public Flight(String flightNumber, String origin, String destination,
                  LocalDateTime departureTime, int totalSeats) {
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
    }

    /**
     * Atomically reserves {@code seatCount} seats if enough are available.
     * Synchronized per-flight so concurrent bookings on the same flight
     * can never oversell it, while bookings on different flights don't
     * contend with each other. Returns the available-seats count as of
     * the same atomic decision, so a caller building a "not enough seats"
     * message never has to re-read availableSeats separately - a second,
     * unsynchronized read could reflect a different value by the time
     * it runs under concurrent bookings.
     */
    public synchronized SeatReservationResult reserveSeats(int seatCount) {
        if (seatCount <= 0 || seatCount > availableSeats) {
            return new SeatReservationResult(false, availableSeats);
        }
        availableSeats -= seatCount;
        return new SeatReservationResult(true, availableSeats);
    }

    public record SeatReservationResult(boolean successful, int availableSeats) {
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public synchronized int getAvailableSeats() {
        return availableSeats;
    }
}
