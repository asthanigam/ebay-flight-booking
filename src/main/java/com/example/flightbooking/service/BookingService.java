package com.example.flightbooking.service;

import com.example.flightbooking.dto.BookFlightRequest;
import com.example.flightbooking.exception.FlightFullException;
import com.example.flightbooking.model.Booking;
import com.example.flightbooking.model.Flight;
import com.example.flightbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BookingService {

    private final FlightService flightService;
    private final BookingRepository bookingRepository;

    public BookingService(FlightService flightService, BookingRepository bookingRepository) {
        this.flightService = flightService;
        this.bookingRepository = bookingRepository;
    }

    public Booking bookFlight(BookFlightRequest request) {
        Flight flight = flightService.getFlight(request.flightNumber());

        // reserveSeats is synchronized on the Flight instance, so concurrent
        // bookings against the same flight are serialized and can never
        // oversell it, while bookings on different flights don't contend.
        Flight.SeatReservationResult result = flight.reserveSeats(request.seatCount());
        if (!result.successful()) {
            throw new FlightFullException(request.flightNumber(), request.seatCount(),
                    result.availableSeats());
        }

        Booking booking = new Booking(
                UUID.randomUUID().toString(),
                request.flightNumber(),
                request.passengerName(),
                request.seatCount(),
                LocalDateTime.now()
        );
        return bookingRepository.save(booking);
    }
}
