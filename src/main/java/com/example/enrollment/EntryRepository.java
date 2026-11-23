package com.example.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

interface EntryRepository extends JpaRepository<LearnerEntry, Long> {
	Optional<LearnerEntry> findByEmail(String email);

	long countByStatus(LearnerEntry.Status status);

	Optional<LearnerEntry> findFirstByStatusOrderByQueueNumberAsc(LearnerEntry.Status status);
}
