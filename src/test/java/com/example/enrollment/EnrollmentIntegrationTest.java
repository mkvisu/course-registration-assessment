package com.example.enrollment;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EnrollmentIntegrationTest {
	@Autowired
	EnrollmentService service;
	@Autowired
	EntryRepository entries;

	@BeforeEach
	void reset() {
		entries.deleteAll();
	}

	EnrollmentRequest request(int i) {
		return new EnrollmentRequest("Learner " + i, "learner" + i + "@example.com", "BTech", 75.0, 2.0);
	}

	void fill() {
		for (int i = 0; i < 60; i++)
			assertEquals("ENROLLED", service.enroll(request(i)).status());
	}

	@Test
	void capacityAndDuplicate() {
		fill();
		assertEquals(60, service.status().enrolled());
		assertEquals("Already enrolled", service.enroll(request(0)).message());
		assertEquals("WAITLISTED", service.enroll(request(60)).status());
		assertEquals("Already in waitlist", service.enroll(request(60)).message());
		assertEquals(1, service.status().waitlisted());
	}

	@Test
	void fifoAndDrop() {
		fill();
		service.enroll(request(60));
		service.enroll(request(61));
		service.drop(request(0).email());
		assertEquals("Already enrolled", service.enroll(request(60)).message());
		assertEquals("Already in waitlist", service.enroll(request(61)).message());
		assertEquals(60, service.status().enrolled());
		assertEquals(1, service.status().waitlisted());
	}

	@Test
	void eligibilityAndInvalidDrops() {
		assertThrows(ResponseStatusException.class,
				() -> service.enroll(new EnrollmentRequest("X", "student@example.com", "BTech", 59.0, 2.0)));
		assertEquals(0, service.status().enrolled());
		assertThrows(ResponseStatusException.class, () -> service.drop("student@example.com"));
		service.enroll(request(0));
		service.drop(request(0).email());
		assertThrows(ResponseStatusException.class, () -> service.drop(request(0).email()));
	}

	@Test
	void waitlistLimitAndWithdrawal() {
		fill();
		for (int i = 60; i < 70; i++)
			service.enroll(request(i));
		assertEquals(10, service.status().waitlisted());
		assertThrows(ResponseStatusException.class, () -> service.enroll(request(70)));
		service.drop(request(60).email());
		assertEquals(9, service.status().waitlisted());
	}

	@Test
	void emailsAreDistinctAndDuplicateSafe() {
		var india = new EnrollmentRequest("India", "student@example.com", "BTech", 70.0, 2.0);
		var usa = new EnrollmentRequest("USA", "student.us@example.com", "MCA", 70.0, 2.0);
		assertEquals("ENROLLED", service.enroll(india).status());
		assertEquals("ENROLLED", service.enroll(usa).status());
		assertEquals("Already enrolled", service.enroll(india).message());
		assertEquals(2, service.status().enrolled());
		service.drop(usa.email());
		assertEquals(1, service.status().enrolled());
	}

	@Test
	void concurrentCapacity() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(8);
		try {
			List<Callable<String>> jobs = new ArrayList<>();
			for (int i = 100; i < 170; i++) {
				final int n = i;
				jobs.add(() -> service.enroll(request(n)).status());
			}
			List<Future<String>> results = pool.invokeAll(jobs);
			int enrolled = 0, waitlisted = 0;
			for (var f : results) {
				String status = f.get();
				if (status.equals("ENROLLED"))
					enrolled++;
				else if (status.equals("WAITLISTED"))
					waitlisted++;
			}
			assertEquals(60, enrolled);
			assertEquals(10, waitlisted);
		} finally {
			pool.shutdownNow();
		}
	}
}
