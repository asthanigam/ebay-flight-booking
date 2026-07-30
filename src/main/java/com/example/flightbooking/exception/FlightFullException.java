package com.example.flightbooking.exception;

public class FlightFullException extends RuntimeException {

    public FlightFullException(String flightNumber, int requestedSeats, int availableSeats) {
        super("Not enough seats on flight " + flightNumber + ": requested " + requestedSeats
                + ", available " + availableSeats);
    }
}
