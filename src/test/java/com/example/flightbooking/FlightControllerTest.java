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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FlightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String createFlightRequestJson(String flightNumber) {
        return """
                {
                  "flightNumber": "%s",
                  "origin": "JFK",
                  "destination": "LAX",
                  "departureTime": "%s",
                  "totalSeats": 10
                }
                """.formatted(flightNumber, LocalDateTime.now().plusDays(10));
    }

    @Test
    void createFlight_duplicateNumber_returns409() throws Exception {
        String flightNumber = "FC101";

        mockMvc.perform(post("/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createFlightRequestJson(flightNumber)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createFlightRequestJson(flightNumber)))
                .andExpect(status().isConflict());
    }

    /**
     * Guards against the check-then-act race that used to exist in
     * FlightService.createFlight (exists check + save were two separate
     * steps): concurrent creates for the same flight number must yield
     * exactly one 201 and 409 for every other attempt, never two 201s.
     */
    @Test
    void concurrentCreateFlight_sameNumber_onlyOneSucceeds() throws Exception {
        String flightNumber = "FC102";
        int requestCount = 20;

        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<Integer>> tasks = IntStream.range(0, requestCount)
                .<Callable<Integer>>mapToObj(i -> () -> {
                    startLatch.await();
                    var result = mockMvc.perform(post("/flights")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createFlightRequestJson(flightNumber)));
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

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(requestCount - 1);
    }
}
