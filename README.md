# Flight Ticket Booking API

A small Spring Boot REST API for booking flight tickets. Single instance, in-memory
storage, no auth. Built as a take-home exercise: Step 1 was generated with an AI
coding agent (Claude Code / Claude Sonnet 5), with every prompt recorded in the
commit messages; Step 2 is a manual-style review/improvement pass on top of that
(see the final commit message and the "AI vs. manual" section below).

## What's here

- **Flights**: `POST /flights` to register a flight, `GET /flights/{flightNumber}` to
  look one up by its number. There is no search/list endpoint by design (the spec
  assumes the client already knows the flight number) - three sample flights
  (`AA100`, `BA200`, `DL300`) are seeded at startup so you can try booking without
  creating one first.
- **Bookings**: `POST /bookings` to book seats on a flight. No GET/list/cancel
  endpoints, per the spec ("no APIs to retrieve bookings, only to book").
- **No overbooking**: seat reservation is synchronized per-flight, so concurrent
  requests against the same flight can never push it past capacity. This is covered
  by an automated concurrency test (30 simultaneous requests against a 10-seat
  flight; exactly 10 succeed, the rest get 409).

## Running it

Requires a JDK (17+; developed against JDK 26) and network access on first run so
the Maven Wrapper can download Maven + dependencies. No local Maven install needed.

```bash
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`.

Run the tests:

```bash
./mvnw test
```

## Example requests

Look up a seeded flight:

```bash
curl http://localhost:8080/flights/AA100
```

```json
{
  "flightNumber": "AA100",
  "origin": "JFK",
  "destination": "LAX",
  "departureTime": "2026-07-30T12:00:00",
  "totalSeats": 3,
  "availableSeats": 3
}
```

Register a new flight:

```bash
curl -X POST http://localhost:8080/flights \
  -H 'Content-Type: application/json' \
  -d '{
        "flightNumber": "UA400",
        "origin": "SFO",
        "destination": "ORD",
        "departureTime": "2026-08-15T09:30:00",
        "totalSeats": 4
      }'
```

Book seats on a flight:

```bash
curl -X POST http://localhost:8080/bookings \
  -H 'Content-Type: application/json' \
  -d '{
        "flightNumber": "AA100",
        "passengerName": "Jane Doe",
        "seatCount": 2
      }'
```

```json
{
  "bookingId": "b3b3f5b0-...",
  "flightNumber": "AA100",
  "passengerName": "Jane Doe",
  "seatCount": 2,
  "bookedAt": "2026-07-29T16:00:00"
}
```

Book past capacity (seeded `AA100` only has 3 seats total) → `409 Conflict`:

```json
{
  "timestamp": "2026-07-29T16:00:01",
  "status": 409,
  "error": "Conflict",
  "message": "Not enough seats on flight AA100: requested 2, available 1"
}
```

Unknown flight number → `404 Not Found`. Invalid input (blank passenger name,
`seatCount <= 0`, missing fields) → `400 Bad Request` with the failing field names.

## Design decisions / liberties taken

The spec deliberately left business rules open. The calls made here:

- Flights need *some* way to exist before they can be booked. Added `POST /flights`
  plus startup seed data, rather than only seed data, so the API is fully
  self-contained and testable. This doesn't add search/filtering - it's a direct
  lookup by the same key the client is assumed to already know.
- One booking can request multiple seats (`seatCount`) rather than always booking
  exactly one seat, so a single request can cover a family/group.
- No cancellation endpoint - out of scope per "only to book," and out of the 60-minute
  budget.
- Bookings are still stored in-memory (`BookingRepository`) even though nothing reads
  them back, since a real booking system would need the record for other purposes
  (payments, check-in, etc.) even if this exercise doesn't expose it.

## AI vs. manual (Step 1 vs Step 2)

Step 1 (scaffold through tests, README) was produced entirely through an AI coding
agent - every commit message contains the literal prompt that drove it. Step 2 is a
single final commit where the same agent did a review pass - covering both a first
round of fixes (the flight-creation race condition, removing redundant
`@ResponseStatus` annotations, adding a catch-all exception handler) and a follow-up
specifically auditing SOLID adherence and concurrency correctness (closing a minor
error-message race, splitting seed-only vs. API-driven repository writes) - and that
commit message spells out exactly what changed and why. Because both steps were
AI-performed, Step 2 doesn't literally satisfy "manual coding" in the letter of the
exercise, but it aims to satisfy it in spirit: a deliberate review pass looking for
real issues rather than more feature generation.

## SOLID & concurrency audit

A follow-up review pass, specifically checking design principles and concurrency
correctness:

- **SRP**: each class has one job - `Flight` owns its own seat-count invariant
  (`reserveSeats`), the two `*Service` classes each own one use case, controllers are
  thin adapters, `GlobalExceptionHandler` owns status-code mapping. No God classes.
- **DIP in practice**: every collaborator is constructor-injected (`FlightService`
  receives `FlightRepository`, `BookingService` receives `FlightService` +
  `BookingRepository`, controllers receive services) - nothing does `new` on its own
  dependencies, which is the actual substance of DIP. What's **not** here: Java
  `interface`s in front of the repositories/services. That's a deliberate call, not an
  oversight - there's exactly one implementation of each, no swappable backend to
  target, and adding an interface with a single implementer would be ceremony with no
  present benefit (classic YAGNI). If a real datastore replaced the in-memory maps,
  that's when the interface would earn its keep.
- **ISP/OCP**: not much surface area to violate at this size; adding a new error type
  only means adding one `@ExceptionHandler` method, nothing else changes.
- **Concurrency, re-verified**: exactly two places mutate shared state -
  `Flight.reserveSeats` (booking) and `FlightRepository.putIfAbsent` (flight creation).
  Both are single-lock-per-operation with no nesting, so there's no lock-ordering /
  deadlock risk. `availableSeats` is a plain (non-volatile) `int`, which is safe here
  only because every read and write of it goes through a `synchronized` method on the
  same monitor - the JMM guarantees visibility across synchronized boundaries, so no
  extra `volatile`/`Atomic*` is needed on top.
- **Fixed in this pass**: `Flight.reserveSeats` used to return a plain `boolean`, and
  `BookingService` would call the separate, unsynchronized `getAvailableSeats()`
  afterward to build the 409 message - a harmless-but-sloppy gap where, under load,
  the reported "available" count could reflect a different moment than the actual
  decision. `reserveSeats` now returns a `SeatReservationResult(successful,
  availableSeats)` computed atomically inside the same synchronized block, so the
  error message is always exact. Also split `FlightRepository.save` into a
  seed-only `seedFlight` versus the API-driven `putIfAbsent`, so a future caller can't
  accidentally reuse the unconditional-overwrite path and silently reset an
  already-booked flight's seat count.

## What I'd improve with more time

- **Idempotency**: `POST /bookings` isn't idempotent - a client retry after a
  network blip could double-book. Would add an idempotency key header.
- **Persistence**: everything is lost on restart. Fine per the spec ("in-memory
  storage only"), but would be the first thing to swap for a real system.
- **Cancellation / refund path**: no way to release seats once booked.
- **Optimistic bulk booking fairness**: seat reservation is correct but simplistic
  (first-come-first-served under a single lock per flight); for very high-traffic
  flights a lock-free structure (e.g. `AtomicInteger.updateAndGet`) would scale
  better than `synchronized`, though at this scale it doesn't matter.
- **Input validation depth**: e.g. rejecting a `departureTime` that isn't actually in
  the future is covered (`@Future`), but there's no validation that origin/destination
  aren't the same, or airport-code format checks.
- **Observability**: no structured logging or metrics around booking attempts/
  rejections, which you'd want in production to monitor sell-through and failed
  booking rates.
- **API docs**: no OpenAPI/Swagger UI generated - would add `springdoc-openapi` given
  more time.
- **Error message detail on framework exceptions**: `GlobalExceptionHandler` passes
  `ex.getMessage()` straight through for Spring MVC's own exceptions (malformed JSON,
  wrong field type, missing body). This gives genuinely useful detail for legitimate
  client mistakes, but for a couple of these Spring's default message includes the
  full internal method signature (e.g. "Required request body is missing: public
  com.example...BookingResponse ...bookFlight(...)"), which leaks implementation
  detail that a hardened public API shouldn't expose. Didn't sanitize this - a safe
  fix needs per-exception-type message shaping rather than a blanket string trim,
  which risked cutting real detail off other messages.
