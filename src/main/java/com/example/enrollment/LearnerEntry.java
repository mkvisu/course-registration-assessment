package com.example.enrollment;
import jakarta.persistence.*;
@Entity @Table(name="learner_entry",uniqueConstraints=@UniqueConstraint(columnNames="email"))
class LearnerEntry { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(nullable=false,unique=true) String email; @Column(nullable=false) String name; @Column(nullable=false) String degree; double marks; double experience; @Enumerated(EnumType.STRING) @Column(nullable=false) Status status; @Column(nullable=false) long queueNumber;
 protected LearnerEntry(){} LearnerEntry(EnrollmentRequest r,Status status,long queueNumber){this.email=r.email().trim().toLowerCase(java.util.Locale.ROOT);this.name=r.name();this.degree=r.degree();this.marks=r.aggregateMarks();this.experience=r.experienceYears();this.status=status;this.queueNumber=queueNumber;}
 enum Status {ENROLLED,WAITLISTED}
 boolean eligible(){return (degree.equalsIgnoreCase("BE")||degree.equalsIgnoreCase("BTech")||degree.equalsIgnoreCase("MCA")||degree.equalsIgnoreCase("MSc"))&&marks>=60&&marks<=100&&experience>=1;}
}
