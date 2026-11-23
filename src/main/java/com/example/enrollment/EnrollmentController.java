package com.example.enrollment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@org.springframework.validation.annotation.Validated
@RestController @RequestMapping("/api/v1")
public class EnrollmentController {
 private final EnrollmentService service;
 EnrollmentController(EnrollmentService service){this.service=service;}
 @PostMapping("/enrollments")
 ResponseEntity<EnrollmentService.Result> enroll(@Valid @RequestBody EnrollmentRequest r){
  var result=service.enroll(r);
  return ResponseEntity.status(result.message().equals("Enrollment successful")?HttpStatus.CREATED:result.message().equals("Added to waitlist")?HttpStatus.ACCEPTED:HttpStatus.OK).body(result);
 }
 @DeleteMapping("/enrollments/{email:.+}")
 EnrollmentService.Result drop(@PathVariable @NotBlank @Email String email){return service.drop(email);}
 @GetMapping("/courses/mtech-ai") EnrollmentService.CourseStatus status(){return service.status();}
}
