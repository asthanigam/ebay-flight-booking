package com.example.flightbooking.controller;

import com.example.flightbooking.dto.BookFlightRequest;
import com.example.flightbooking.dto.BookingResponse;
import com.example.flightbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse bookFlight(@Valid @RequestBody BookFlightRequest request) {
        return BookingResponse.from(bookingService.bookFlight(request));
    }
}
