package com.example.flightbooking.repository;

import com.example.flightbooking.model.Booking;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

@Repository
public class BookingRepository {

    private final ConcurrentHashMap<String, Booking> bookingsById = new ConcurrentHashMap<>();

    public Booking save(Booking booking) {
        bookingsById.put(booking.bookingId(), booking);
        return booking;
    }
}
