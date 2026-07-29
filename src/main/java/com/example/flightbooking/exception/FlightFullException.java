package com.example.flightbooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class FlightFullException extends RuntimeException {

    public FlightFullException(String flightNumber, int requestedSeats, int availableSeats) {
        super("Not enough seats on flight " + flightNumber + ": requested " + requestedSeats
                + ", available " + availableSeats);
    }
}
