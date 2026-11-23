package com.example.enrollment;
import jakarta.validation.constraints.*;
public record EnrollmentRequest(
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank String degree,
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double aggregateMarks,
    @NotNull @DecimalMin("0.0") Double experienceYears
) {}
