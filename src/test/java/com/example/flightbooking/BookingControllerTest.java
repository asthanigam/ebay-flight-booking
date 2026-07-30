package com.example.flightbooking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String createFlight(String flightNumber, int totalSeats) throws Exception {
        String requestJson = """
                {
                  "flightNumber": "%s",
                  "origin": "JFK",
                  "destination": "LAX",
                  "departureTime": "%s",
                  "totalSeats": %d
                }
                """.formatted(flightNumber, LocalDateTime.now().plusDays(10), totalSeats);

        mockMvc.perform(post("/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
        return flightNumber;
    }

    private String bookingBody(String flightNumber, String passenger, int seatCount) {
        return """
                {
                  "flightNumber": "%s",
                  "passengerName": "%s",
                  "seatCount": %d
                }
                """.formatted(flightNumber, passenger, seatCount);
    }

    @Test
    void bookFlight_success_returnsConfirmation() throws Exception {
        String flightNumber = createFlight("TC101", 5);

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(flightNumber, "Jane Doe", 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").isNotEmpty())
                .andExpect(jsonPath("$.flightNumber").value(flightNumber))
                .andExpect(jsonPath("$.seatCount").value(2));

        mockMvc.perform(get("/flights/" + flightNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats").value(3));
    }

    @Test
    void bookFlight_unknownFlight_returns404() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody("NO-SUCH-FLIGHT", "Jane Doe", 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookFlight_notEnoughSeats_returns409() throws Exception {
        String flightNumber = createFlight("TC102", 1);

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(flightNumber, "First Passenger", 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(flightNumber, "Second Passenger", 1)))
                .andExpect(status().isConflict());
    }

    @Test
    void bookFlight_invalidRequest_returns400() throws Exception {
        String flightNumber = createFlight("TC103", 5);

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(flightNumber, "", 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookFlight_malformedJson_returns400NotServerError() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightNumber\": \"AA100\", \"passengerName\": "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookFlight_wrongFieldType_returns400NotServerError() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightNumber\":\"AA100\",\"passengerName\":\"Jane\",\"seatCount\":\"two\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentBookings_neverOversellTheFlight() throws Exception {
        int totalSeats = 10;
        int requestCount = 30;
        String flightNumber = createFlight("TC104", totalSeats);

        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<Integer>> tasks = IntStream.range(0, requestCount)
                .<Callable<Integer>>mapToObj(i -> () -> {
                    startLatch.await();
                    var result = mockMvc.perform(post("/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingBody(flightNumber, "Passenger " + i, 1)));
                    return result.andReturn().getResponse().getStatus();
                })
                .collect(Collectors.toList());

        List<Future<Integer>> futures = tasks.stream().map(executor::submit).collect(Collectors.toList());
        startLatch.countDown();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        for (Future<Integer> future : futures) {
            int status = future.get();
            if (status == 201) {
                successCount.incrementAndGet();
            } else if (status == 409) {
                conflictCount.incrementAndGet();
            }
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(totalSeats);
        assertThat(conflictCount.get()).isEqualTo(requestCount - totalSeats);

        mockMvc.perform(get("/flights/" + flightNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats").value(0));
    }
}
