package com.example.flightbooking.dto;

import com.example.flightbooking.model.Flight;

import java.time.LocalDateTime;

public record FlightResponse(
        String flightNumber,
        String origin,
        String destination,
        LocalDateTime departureTime,
        int totalSeats,
        int availableSeats
) {
    public static FlightResponse from(Flight flight) {
        return new FlightResponse(
                flight.getFlightNumber(),
                flight.getOrigin(),
                flight.getDestination(),
                flight.getDepartureTime(),
                flight.getTotalSeats(),
                flight.getAvailableSeats()
        );
    }
}
