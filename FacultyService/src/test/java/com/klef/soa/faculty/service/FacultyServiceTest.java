package com.klef.soa.faculty.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import com.klef.soa.faculty.dto.CreateFacultyRequest;
import com.klef.soa.faculty.entity.Faculty;
import com.klef.soa.faculty.repository.FacultyRepository;

@ExtendWith(MockitoExtension.class)
class FacultyServiceTest {
 @Mock FacultyRepository repository; @Mock PasswordEncoder encoder; @InjectMocks FacultyService service;
 @Test void createsHashedActiveFacultyWithMfaDisabled(){
  when(repository.findByEmailIgnoreCase("ravi@school.com")).thenReturn(Optional.empty());when(repository.findByEmployeeIdIgnoreCase("FAC001")).thenReturn(Optional.empty());when(encoder.encode("temporary-password")).thenReturn("bcrypt-hash");when(repository.save(any())).thenAnswer(call->{Faculty f=call.getArgument(0);f.setId(1L);return f;});
  var response=service.create(new CreateFacultyRequest("Ravi Kumar","Ravi@school.com","FAC001","Science","Professor","9000000000","temporary-password"));
  assertEquals("ravi@school.com",response.email());assertFalse(response.mfaEnabled());verify(repository).save(argThat(f->f.getPasswordHash().equals("bcrypt-hash")&&f.getStatus().equals("ACTIVE")));
 }
 @Test void rejectsDuplicateEmail(){
  Faculty exists=new Faculty();exists.setId(4L);when(repository.findByEmailIgnoreCase("ravi@school.com")).thenReturn(Optional.of(exists));
  assertThrows(ResponseStatusException.class,()->service.create(new CreateFacultyRequest("Ravi","ravi@school.com","FAC001","Science",null,null,"temporary-password")));
  verify(repository,never()).save(any());
 }
}
