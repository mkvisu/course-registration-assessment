package com.example.enrollment;
import jakarta.persistence.*;
@Entity class Offering { @Id String id; int capacity; int waitlistLimit; protected Offering(){} Offering(String id,int capacity,int waitlistLimit){this.id=id;this.capacity=capacity;this.waitlistLimit=waitlistLimit;} }
