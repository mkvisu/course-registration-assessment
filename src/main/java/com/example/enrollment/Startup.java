package com.example.enrollment;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
class Startup {
	@Bean
	CommandLineRunner seed(OfferingRepository repo) {
		return args -> {
			if (!repo.existsById(EnrollmentService.COURSE))
				repo.save(new Offering(EnrollmentService.COURSE, 60, 10));
		};
	}
}
