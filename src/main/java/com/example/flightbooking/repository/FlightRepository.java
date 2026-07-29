package com.example.flightbooking.repository;

import com.example.flightbooking.model.Flight;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FlightRepository {

    private final ConcurrentHashMap<String, Flight> flightsByNumber = new ConcurrentHashMap<>();

    public Flight save(Flight flight) {
        flightsByNumber.put(flight.getFlightNumber(), flight);
        return flight;
    }

    public Optional<Flight> findByFlightNumber(String flightNumber) {
        return Optional.ofNullable(flightsByNumber.get(flightNumber));
    }

    public boolean existsByFlightNumber(String flightNumber) {
        return flightsByNumber.containsKey(flightNumber);
    }
}
