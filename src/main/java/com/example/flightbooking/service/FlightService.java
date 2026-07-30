package com.example.flightbooking.service;

import com.example.flightbooking.dto.CreateFlightRequest;
import com.example.flightbooking.exception.DuplicateFlightException;
import com.example.flightbooking.exception.FlightNotFoundException;
import com.example.flightbooking.model.Flight;
import com.example.flightbooking.repository.FlightRepository;
import org.springframework.stereotype.Service;

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public Flight createFlight(CreateFlightRequest request) {
        Flight flight = new Flight(
                request.flightNumber(),
                request.origin(),
                request.destination(),
                request.departureTime(),
                request.totalSeats()
        );
        if (flightRepository.putIfAbsent(flight).isPresent()) {
            throw new DuplicateFlightException(request.flightNumber());
        }
        return flight;
    }

    public Flight getFlight(String flightNumber) {
        return flightRepository.findByFlightNumber(flightNumber)
                .orElseThrow(() -> new FlightNotFoundException(flightNumber));
    }
}
