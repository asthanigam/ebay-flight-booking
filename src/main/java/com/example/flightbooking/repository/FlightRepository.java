package com.example.flightbooking.repository;

import com.example.flightbooking.model.Flight;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FlightRepository {

    private final ConcurrentHashMap<String, Flight> flightsByNumber = new ConcurrentHashMap<>();

    /**
     * Unconditional insert, for startup seed data only. Deliberately kept
     * separate from putIfAbsent (the one used by the create-flight API path)
     * so a future caller can't accidentally overwrite - and silently reset
     * the seat count of - an existing, possibly already-booked flight.
     */
    public Flight seedFlight(Flight flight) {
        flightsByNumber.put(flight.getFlightNumber(), flight);
        return flight;
    }

    /**
     * Atomically inserts the flight only if its number isn't already taken,
     * returning the pre-existing flight on conflict (empty on success).
     * Using putIfAbsent instead of a separate exists-check-then-save avoids
     * a race where two concurrent creates for the same flight number could
     * both pass the check and the second would silently overwrite the first.
     */
    public Optional<Flight> putIfAbsent(Flight flight) {
        return Optional.ofNullable(flightsByNumber.putIfAbsent(flight.getFlightNumber(), flight));
    }

    public Optional<Flight> findByFlightNumber(String flightNumber) {
        return Optional.ofNullable(flightsByNumber.get(flightNumber));
    }
}
