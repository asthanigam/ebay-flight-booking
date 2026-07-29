package com.example.flightbooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookFlightRequest(
        @NotBlank String flightNumber,
        @NotBlank String passengerName,
        @Min(1) int seatCount
) {
}
