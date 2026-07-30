package com.example.flightbooking.config;

import com.example.flightbooking.model.Flight;
import com.example.flightbooking.repository.FlightRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final FlightRepository flightRepository;

    public DataSeeder(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public void run(String... args) {
        flightRepository.seedFlight(new Flight("AA100", "JFK", "LAX",
                LocalDateTime.now().plusDays(1), 3));
        flightRepository.seedFlight(new Flight("BA200", "LHR", "JFK",
                LocalDateTime.now().plusDays(2), 50));
        flightRepository.seedFlight(new Flight("DL300", "ATL", "SEA",
                LocalDateTime.now().plusDays(3), 120));
    }
}
