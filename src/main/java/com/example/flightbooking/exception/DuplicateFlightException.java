package com.example.flightbooking.exception;

public class DuplicateFlightException extends RuntimeException {

    public DuplicateFlightException(String flightNumber) {
        super("Flight already exists: " + flightNumber);
    }
}
