package com.example.enrollment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;
@SpringBootTest @AutoConfigureMockMvc class EmailApiTest {
 @Autowired MockMvc mvc;
 private String json(String email){return "{\"name\":\"Test\",\"email\":\""+email+"\",\"degree\":\"BE\",\"aggregateMarks\":70,\"experienceYears\":2}";}
 @Test void acceptsAndNormalizesEmail() throws Exception {
  mvc.perform(post("/api/v1/enrollments").contentType(MediaType.APPLICATION_JSON).content(json("Student@Example.COM"))).andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("student@example.com"));
  mvc.perform(post("/api/v1/enrollments").contentType(MediaType.APPLICATION_JSON).content(json("student@example.com"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Already enrolled"));
  mvc.perform(delete("/api/v1/enrollments/{email}","STUDENT@example.com")).andExpect(status().isOk());
 }
 @Test void rejectsInvalidEmail() throws Exception {
  for(String email:new String[]{"not-an-email","missing-at.example.com",""}){
   mvc.perform(post("/api/v1/enrollments").contentType(MediaType.APPLICATION_JSON).content(json(email))).andExpect(status().isBadRequest());
  }
  mvc.perform(delete("/api/v1/enrollments/{email}","not-an-email")).andExpect(status().isBadRequest());
 }
}
