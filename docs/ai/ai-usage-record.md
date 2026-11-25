# Course Registration Assessment — Consolidated AI Implementation Prompt

## Role and objective

Act as a senior Java software engineer experienced in REST API design, transactional systems, and automated testing.

Implement a minimal, runnable course registration microservice for a single offering, **MTech Artificial Intelligence**, in the project `course-registration-assessment`. The solution must satisfy the business requirements below, remain proportionate to the assessment's scope, and be straightforward for an interviewer to build, test, and review.

## 1. Business requirements

**Eligibility:** A learner is eligible only when all three conditions are satisfied:

- Degree is BE, BTech, MCA, or MSc.
- Aggregate marks are at least 60%.
- Professional experience is at least one year.

Reject an ineligible enrollment request with the exact message:

`Learner is not eligible as per eligibility criteria`

**Enrollment and waitlisting:**

- Allow no more than 60 active enrollments.
- Enroll an eligible learner when a seat is available.
- When all 60 seats are occupied, add an eligible learner to a FIFO waitlist containing no more than 10 learners.
- Reject a new eligible request when both enrollment and waitlist capacities are full.
- Use email address as the unique learner identifier. Duplicate requests must return a clear `Already enrolled` or `Already in waitlist` result without creating a record, consuming capacity, or changing FIFO position.

**Dropping and promotion:**

- Allow an enrolled learner to drop and a waitlisted learner to withdraw.
- When an enrolled learner drops, release the seat and promote the earliest eligible waitlisted learner in the same transaction.
- Recheck eligibility before promotion. If a learner is no longer eligible, remove that waitlist entry and continue to the next learner. Apply this policy consistently in code, tests, and documentation.
- Invalid, unknown, or repeated drops must return a clear result without changing valid enrollment or waitlist state.

## 2. Explicit assumptions and design decisions

Use the following decisions unless the supplied starter project or assessment instructions require otherwise. Document any conflict before changing them.

- Trim email addresses and compare them case-insensitively using a consistent canonical representation. Enforce uniqueness in the database as well as in application logic.
- Treat the submitted qualifications as learner-provided data; external qualification or email verification is outside scope.
- Store aggregate marks as a decimal percentage between 0 and 100. Treat one year of experience as the inclusive minimum.
- Accept degree values case-insensitively and normalize them to the four supported values.
- Maintain FIFO using a stable, monotonically ordered waitlist sequence. Duplicate requests must not obtain a new sequence.
- Permit a learner to apply again after dropping or withdrawing.
- Use H2 in-memory storage. Data loss on application restart is an accepted limitation.
- Support one application instance. Do not claim distributed coordination across independent in-memory databases.

If an assumption is necessary but not covered here, choose the simplest defensible behavior, state it explicitly, and add a test when it affects correctness.

## 3. Technology and scope constraints

Use Java 17, Spring Boot 3.x, Maven, Spring Web, Spring Data JPA, Bean Validation, H2, and JUnit 5. Use RESTful JSON APIs and centralized error handling.

Implement only this offering's enrollment, drop, withdrawal, status, and waitlist-promotion slice. Do not introduce a frontend, course catalog, payments, identity integration, multi-term registration, external database, deployment infrastructure, or unnecessary framework. If a starter project is supplied, inspect it first and preserve its established conventions and transaction or locking pattern.

## 4. API contract

Provide and document these endpoints:

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/enrollments` | Enroll or waitlist a learner |
| DELETE | `/api/v1/enrollments/{email}` | Drop or withdraw a learner |
| GET | `/api/v1/courses/mtech-ai` | Return enrollment and waitlist counts |

Use `201` for a new enrollment, `202` for a waitlist admission, `200` for a duplicate request or successful drop, `400` for malformed input, `404` for an unknown or repeated drop, `409` when the waitlist is full, and `422` for prerequisite failure. Ensure the OpenAPI specification matches the implemented payloads and status codes. Document URL encoding for email path parameters.

## 5. State integrity and concurrency

Maintain these invariants after every committed operation:

- Enrolled count is between 0 and 60.
- Waitlisted count is between 0 and 10.
- A canonical email has at most one active record and one active status.
- Promotion respects FIFO among learners who remain eligible.
- A failed operation leaves valid state unchanged.

Protect capacity checks and mutations from concurrent requests. For this bounded single-offering design, use a transaction and a pessimistic write lock on the offering record, unless the starter already supplies an appropriate alternative. Acquire the lock before making state-dependent decisions, and keep drop and promotion atomic. Explain when the lock is released and what happens on rollback. Do not rely on a thread-safe sequence generator alone to protect capacity.

## 6. Acceptance tests

Write focused automated tests for:

- Eligibility boundaries, including exactly 60% marks and exactly one year of experience.
- Available capacity, the 60th enrollment, and the next eligible learner joining the waitlist.
- The 10th waitlist entry and rejection of an 11th entry.
- Duplicate enrolled and waitlisted requests, including email case variations.
- Prerequisite failure with the exact required message and no state mutation.
- FIFO promotion after a drop, including removal of an ineligible earlier entry.
- Waitlist withdrawal, invalid requests, unknown drops, and repeated drops.
- Concurrent requests competing for the last seat, verifying capacity and uniqueness invariants.

Use deterministic test data and avoid timing-dependent concurrency assertions where practical.

## 7. Deliverables and verification

Produce:

1. Runnable source code and focused automated tests.
2. A concise README covering setup, architecture, API examples, invariants, assumptions, locking, limitations, and test commands.
3. An OpenAPI specification consistent with the implementation.
4. An accurate AI-use record identifying prompts, relevant suggestions, accepted or rejected decisions, affected files, and verification evidence.

Run `mvn clean test` if the environment permits. Report the actual command, result, and any failures. If execution is unavailable, mark tests as **not executed** and provide reproduction commands; do not claim a successful build or test run.

Finish with a brief requirement-to-test summary and any unresolved risks. Prefer a small, correct, explainable solution over additional features.
