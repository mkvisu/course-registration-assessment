package com.example.enrollment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class EnrollmentService {
	static final String COURSE = "mtech-ai";
	private final OfferingRepository offerings;
	private final EntryRepository entries;

	public EnrollmentService(OfferingRepository offerings, EntryRepository entries) {
		this.offerings = offerings;
		this.entries = entries;
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private Offering lock() {
		return offerings.lockById(COURSE).orElseThrow();
	}

	private static boolean eligible(EnrollmentRequest r) {
		return (Set.of("BE", "BTECH", "MCA", "MSC").contains(r.degree().toUpperCase(Locale.ROOT)))
				&& r.aggregateMarks() >= 60 && r.experienceYears() >= 1;
	}

	public record Result(String status, String message, String email) {
	}

	public record CourseStatus(String course, int capacity, long enrolled, int available, long waitlisted,
			int waitlistLimit) {
	}

	@Transactional
	public Result enroll(EnrollmentRequest r) {
		Offering o = lock();
		Optional<LearnerEntry> existing = entries.findByEmail(normalizeEmail(r.email()));
		if (existing.isPresent()) {
			LearnerEntry e = existing.get();
			return new Result(e.status.name(),
					e.status == LearnerEntry.Status.ENROLLED ? "Already enrolled" : "Already in waitlist",
					normalizeEmail(r.email()));
		}
		if (!eligible(r))
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
					"Learner is not eligible as per eligibility criteria");
		long enrolled = entries.countByStatus(LearnerEntry.Status.ENROLLED);
		if (enrolled < o.capacity) {
			entries.save(new LearnerEntry(r, LearnerEntry.Status.ENROLLED, 0));
			return new Result("ENROLLED", "Enrollment successful", normalizeEmail(r.email()));
		}
		if (entries.countByStatus(LearnerEntry.Status.WAITLISTED) >= o.waitlistLimit)
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Waitlist is full");
		// IDENTITY ids increase monotonically; sequence assigned separately to avoid
		// reordering after deletes.
		long order = sequence.incrementAndGet();
		entries.save(new LearnerEntry(r, LearnerEntry.Status.WAITLISTED, order));
		return new Result("WAITLISTED", "Added to waitlist", normalizeEmail(r.email()));
	}

	private final java.util.concurrent.atomic.AtomicLong sequence = new java.util.concurrent.atomic.AtomicLong();

	@Transactional
	public Result drop(String email) {
		String normalizedEmail = normalizeEmail(email);
		lock();
		LearnerEntry e = entries.findByEmail(normalizedEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No active enrollment or waitlist entry found"));
		boolean occupied = e.status == LearnerEntry.Status.ENROLLED;
		entries.delete(e);
		entries.flush();
		if (occupied)
			promote();
		return new Result("DROPPED", occupied ? "Enrollment dropped successfully" : "Removed from waitlist",
				normalizedEmail);
	}

	private void promote() {
		while (true) {
			Optional<LearnerEntry> next = entries
					.findFirstByStatusOrderByQueueNumberAsc(LearnerEntry.Status.WAITLISTED);
			if (next.isEmpty())
				return;
			LearnerEntry e = next.get();
			if (!e.eligible()) {
				entries.delete(e);
				entries.flush();
				continue;
			}
			e.status = LearnerEntry.Status.ENROLLED;
			e.queueNumber = 0;
			entries.save(e);
			return;
		}
	}

	@Transactional(readOnly = true)
	public CourseStatus status() {
		Offering o = offerings.findById(COURSE).orElseThrow();
		long enrolled = entries.countByStatus(LearnerEntry.Status.ENROLLED);
		return new CourseStatus("MTech Artificial Intelligence", o.capacity, enrolled, (int) (o.capacity - enrolled),
				entries.countByStatus(LearnerEntry.Status.WAITLISTED), o.waitlistLimit);
	}
}
