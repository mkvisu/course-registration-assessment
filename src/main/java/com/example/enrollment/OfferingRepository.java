package com.example.enrollment;

import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;

interface OfferingRepository extends JpaRepository<Offering, String> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Offering o where o.id=:id")
	Optional<Offering> lockById(String id);
}
