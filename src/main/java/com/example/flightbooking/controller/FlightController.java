package com.example.flightbooking.controller;

import com.example.flightbooking.dto.CreateFlightRequest;
import com.example.flightbooking.dto.FlightResponse;
import com.example.flightbooking.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlightResponse createFlight(@Valid @RequestBody CreateFlightRequest request) {
        return FlightResponse.from(flightService.createFlight(request));
    }

    @GetMapping("/{flightNumber}")
    public FlightResponse getFlight(@PathVariable String flightNumber) {
        return FlightResponse.from(flightService.getFlight(flightNumber));
    }
}
