# Course Registration Assessment — MTech Artificial Intelligence Enrollment Microservice

> A bounded, single-course REST microservice built with Java 17, Spring Boot 3.x, Spring Data JPA and H2 in-memory database. It enforces **60 seats**, a **10-person FIFO waitlist**, eligibility requirements, email-based uniqueness and atomic drop-and-promotion.

## 1. Quick start

**Project name / Maven artifactId:** `course-registration-assessment`

The course offering remains **MTech Artificial Intelligence** (`mtech-ai`); only the project name has changed.


**Prerequisites:** JDK 17, Maven 3.9+.

```bash
java -version
mvn -version
mvn clean test
mvn spring-boot:run
```

Base URL: `http://localhost:8080`. No frontend, login, course catalog or external database is required. H2 data is lost when the application stops.

**API contract:** [`openapi.yaml`](openapi.yaml) (OpenAPI 3.0.3). Open it in [Swagger Editor](https://editor.swagger.io/) or import into Postman. This project does **not** host Swagger UI or `/v3/api-docs` at runtime.

## 2. Business problem and scope

The service manages enrollment for **one** offering: **MTech Artificial Intelligence** (`mtech-ai`). A qualifying learner gets an available seat; otherwise they may enter the waitlist. Dropping an enrolled learner triggers promotion of the earliest eligible waitlisted learner.

| Rule | Implementation |
| --- | --- |
| Seat limit | 60 active `ENROLLED` entries |
| Waitlist limit | 10 active `WAITLISTED` entries |
| Accepted degrees | BE, BTech, MCA, MSc (case-insensitive) |
| Minimum marks | 60% aggregate; input must be between 0 and 100 |
| Minimum experience | 1 year |
| Unique identity | Normalized email address, not mobile number |
| Duplicates | Existing status returned; no duplicate record or queue-position change |
| Promotion | Oldest eligible waitlisted entry, after an enrolled learner drops |
| Ineligible waitlisted entry | Removed/skipped when encountered during promotion |

**Out of scope:** frontend, multi-course registration, authentication/authorization, email ownership verification, payments, external identity or qualification verification, deployment and durable persistence.

## 3. Architecture and responsibilities

```text
HTTP client
   |
   v
EnrollmentController  (routing, @Valid request validation, HTTP status)
   |
   v
EnrollmentService     (@Transactional, eligibility, duplicate/capacity checks,
   |                    pessimistic offering lock, drop, FIFO promotion)
   |
   +--> OfferingRepository --> Offering (one row: id, capacity, waitlistLimit)
   |
   +--> EntryRepository ----> LearnerEntry (email, qualifications, status, queueNumber)
                                  |
                                  v
                            H2 in-memory DB

ErrorHandler: maps invalid requests and business errors to JSON {"error": "..."}
```

All mutations are serialized through the same course offering row. The database is the source of truth for current enrollment and waitlist counts. The service has no distributed cache or external dependency.

### Main source files

| File | Responsibility |
| --- | --- |
| `EnrollmentApplication.java` | Spring Boot entry point |
| `Startup.java` | Seeds the one course offering |
| `EnrollmentController.java` | Three REST routes |
| `EnrollmentRequest.java` | Input DTO and Bean Validation |
| `EnrollmentService.java` | Enrollment, drop, promotion, status |
| `Offering.java`, `OfferingRepository.java` | Course configuration and locking query |
| `LearnerEntry.java`, `EntryRepository.java` | Learner persistence and FIFO retrieval |
| `ErrorHandler.java` | API error mapping |
| `src/test/...` | Integration and API tests |
| `openapi.yaml` | Machine-readable API contract |

## 4. Data model and identity

**Offering** (`id` = `mtech-ai`): `capacity=60`, `waitlistLimit=10`.

**LearnerEntry**: generated numeric `id`; unique, non-null `email`; `name`; `degree`; `marks`; `experience`; `status` (`ENROLLED` or `WAITLISTED`); `queueNumber` (0 for enrolled, increasing number for waitlisted).

Email is trimmed and lowercased with `Locale.ROOT` before lookup and persistence. Thus `Ravi@Example.com` and `ravi@example.com` are the same application identity. This is an explicit simplification: it does not establish email ownership and can collapse email local parts that some providers consider case-sensitive. Mobile numbers are no longer required or stored.

A database uniqueness constraint on `email` provides an additional defense against duplicate active records. Dropping deletes the record; the same email can subsequently reapply.

## 5. REST API reference

All request and response bodies are JSON. See [`openapi.yaml`](openapi.yaml) for formal schema definitions and examples.

### POST `/api/v1/enrollments`

```bash
curl -i -X POST 'http://localhost:8080/api/v1/enrollments' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ravi Kumar","email":"Ravi.Kumar@example.com","degree":"BTech","aggregateMarks":75.5,"experienceYears":2}'
```

**201 Created — seat available**

```json
{"status":"ENROLLED","message":"Enrollment successful","email":"ravi.kumar@example.com"}
```

**202 Accepted — course full, waitlist has space**

```json
{"status":"WAITLISTED","message":"Added to waitlist","email":"ravi.kumar@example.com"}
```

**200 OK — duplicate request:** returns `ENROLLED`/`Already enrolled` or `WAITLISTED`/`Already in waitlist`; the original record and queue order remain unchanged. Duplicate detection precedes eligibility validation for existing entries.

**409 Conflict — waitlist full:** `{"error":"Waitlist is full"}`.

**422 Unprocessable Entity — ineligible:**

```json
{"error":"Learner is not eligible as per eligibility criteria"}
```

**400 Bad Request — malformed or invalid payload:**

```json
{"error":"Invalid request: check required fields, email format and numeric values"}
```

Validation requires nonblank name/degree/email, email-like syntax, non-null marks in `[0,100]`, and nonnegative experience. Eligibility checks apply stricter business thresholds. `@Email` validates syntax, not deliverability.

### DELETE `/api/v1/enrollments/{email}`

```bash
curl -i -X DELETE 'http://localhost:8080/api/v1/enrollments/ravi.kumar%40example.com'
```

**200 OK — enrolled learner dropped:**

```json
{"status":"DROPPED","message":"Enrollment dropped successfully","email":"ravi.kumar@example.com"}
```

If the email belongs to a waitlisted learner, the response message is `Removed from waitlist`; no promotion is triggered. **404 Not Found** is returned for unknown or repeated drops: `{"error":"No active enrollment or waitlist entry found"}`. Invalid email format is intended to return **400**. URL-encode path values, particularly `@` and reserved characters.

### GET `/api/v1/courses/mtech-ai`

```bash
curl -i 'http://localhost:8080/api/v1/courses/mtech-ai'
```

Example initial state:

```json
{"course":"MTech Artificial Intelligence","capacity":60,"enrolled":0,"available":60,"waitlisted":0,"waitlistLimit":10}
```

This endpoint reads counts; it does not reserve seats. Concurrent mutations can change counts immediately after the response.

### HTTP response summary

| Code | When |
| --- | --- |
| 200 | Duplicate enrollment, successful drop/withdrawal, course status |
| 201 | New enrollment |
| 202 | New waitlist entry |
| 400 | Request/parameter validation error |
| 404 | Unknown or already-dropped email |
| 409 | Waitlist full |
| 422 | Eligibility requirements not met |

## 6. Algorithms and invariants

### Enrollment

1. Spring starts a database transaction.
2. Acquire a `PESSIMISTIC_WRITE` lock on the `Offering` row.
3. Normalize email and look for an existing active entry. If found, return its status without mutation.
4. Validate degree, marks and experience. Reject an ineligible applicant with 422.
5. Count enrolled entries. If fewer than 60, insert `ENROLLED` and return 201.
6. Otherwise count waitlisted entries. If 10, reject with 409.
7. Otherwise allocate a monotonically increasing queue number, insert `WAITLISTED` and return 202.
8. Commit the transaction and release the database lock.

### Drop and FIFO promotion

1. Start a transaction and lock the same offering row.
2. Normalize email, load the entry or return 404.
3. Delete the entry and flush the deletion.
4. If the deleted entry was `WAITLISTED`, finish without promotion.
5. If it was `ENROLLED`, read the waitlisted entry with the **smallest** queue number.
6. Recheck its stored qualifications. If ineligible, **delete it and repeat**; do not leave it blocking the queue.
7. Promote the first eligible entry to `ENROLLED`, set its queue number to 0, and finish. If none remain, leave the seat open.
8. Commit deletion and promotion together; release the lock.

**Invariants:** `enrolled <= 60`, `waitlisted <= 10`, at most one active entry per normalized email, and no ineligible learner newly enrolled or promoted. A drop and its promotion are atomic within the database transaction.

**Assumption:** No learner-profile update endpoint exists. Eligibility is rechecked at promotion time using the stored qualifications; the skip/remove rule also protects against inconsistent or externally modified records.

## 7. Concurrency, pessimistic locking and release

`OfferingRepository.lockById` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Hibernate requests a database write lock on the **shared course row** (often via `SELECT ... FOR UPDATE`). Both `enroll` and `drop` call this method inside `@Transactional`.

Example: with 59 occupied seats, transactions A and B attempt to enroll simultaneously. A acquires the lock, observes 59, enrolls the 60th learner and commits. B waits for the lock; after A commits, B acquires it, observes 60, and joins the waitlist instead. This prevents oversubscription.

**When is the lock released?** At **transaction completion**: successful commit or rollback. It is **not** released when `lockById()` returns or merely when a helper method ends. Spring manages the transaction; no explicit `unlock()` call is needed. Database lock waits can time out depending on database settings.

Locking the offering row, rather than individual learner rows, protects the shared seat-count decision. All code paths that mutate these records must follow the same locking convention for the invariant to hold.

**Important limitations:** The `AtomicLong.incrementAndGet()` FIFO counter is process-local, not persisted or rolled back, and may have gaps after failed transactions. H2 memory state is not durable. Multiple application instances with separate in-memory databases would **not** coordinate. This design is intentionally for one running instance.

## 8. Tests and verification

```bash
mvn clean compile
mvn clean test
mvn spring-boot:run
```

Review the test classes `EnrollmentIntegrationTest.java` and `EmailApiTest.java`. The intended verification matrix is:

| Scenario | Expected result |
| --- | --- |
| Eligible learner, available seat | 201 and enrolled count +1 |
| 60 seats filled | Next eligible learner waitlisted, 202 |
| 10 waitlist slots filled | Additional eligible learner rejected, 409 |
| Same email, including casing differences | 200, no duplicate or reorder |
| Invalid degree / marks below 60 / experience below 1 | 422, no state change |
| Enrolled learner drops | Seat released, earliest eligible waitlisted learner promoted |
| Waitlisted learner withdraws | Entry removed, no promotion |
| Unknown or repeated drop | 404, state unchanged |
| Invalid email or missing fields | 400 |
| Concurrent requests for final seat | No oversubscription |

**Verification status:** The project files and OpenAPI YAML can be inspected statically. Maven tests have **not** been run in this environment; the table describes expected outcomes, not claimed passing results. Run `mvn clean test` locally and include the resulting Surefire reports (`target/surefire-reports/`) when submitting verification evidence.

## 9. Design decisions and trade-offs

| Decision | Rationale / trade-off |
| --- | --- |
| H2 in-memory + JPA | Minimal setup and transactional database semantics; data disappears on restart |
| One offering row locked for mutations | Simple correctness model; serializes writes and limits throughput |
| Email as unique identifier | Meets current requirement; not proof of identity or ownership |
| Skip **and remove** ineligible waitlisted entries | Avoids head-of-line blocking; removal is permanent |
| FIFO by increasing queue number | Deterministic within one running instance; not restart-safe |
| Physical deletion on drop | Simple reapplication semantics; no audit history |
| Static OpenAPI file | Easy review without starting server; must be kept in sync manually |

### Risks and potential follow-ups (outside this assignment)

- Use a persistent shared database and database-generated ordering for durable/multi-instance operation.
- Add authorization and email ownership verification before exposing real learner records.
- Define a durable audit trail and learner qualification update workflow if requirements expand.
- Add standardized structured validation errors, observability and load tests for production readiness.
- Keep `openapi.yaml` and controller DTOs synchronized; consider contract tests.

