package com.klef.soa.faculty.controller;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.klef.soa.faculty.dto.*;
import com.klef.soa.faculty.service.FacultyService;
import com.klef.soa.faculty.client.NotificationClient;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
@RestController @RequestMapping("/api/faculty")
public class FacultyController {
 private static final System.Logger LOGGER=System.getLogger(FacultyController.class.getName());
 private final FacultyService service; public FacultyController(FacultyService service){this.service=service;}
 @Autowired(required=false) @org.springframework.context.annotation.Lazy private NotificationClient notificationClient;
 @GetMapping("/me") @PreAuthorize("hasRole('FACULTY')") public FacultyResponse me(Principal principal){return service.byEmail(principal.getName());}
 @GetMapping("/directory/{email}") @PreAuthorize("hasAnyRole('ADMIN','FACULTY','STUDENT')") public FacultyDirectoryResponse directory(@PathVariable String email){return service.directory(email);}
 @GetMapping @PreAuthorize("hasRole('ADMIN')") public List<FacultyResponse> list(){return service.list();}
 @GetMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public FacultyResponse get(@PathVariable Long id){return service.get(id);}
 @GetMapping("/email/{email}") @PreAuthorize("hasRole('ADMIN')") public FacultyResponse byEmail(@PathVariable String email){return service.byEmail(email);}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<FacultyResponse> create(@Valid @RequestBody CreateFacultyRequest request){FacultyResponse created=service.create(request);publishAdminEvent("FACULTY_CREATED",created.name());return ResponseEntity.status(HttpStatus.CREATED).body(created);}
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public FacultyResponse update(@PathVariable Long id,@Valid @RequestBody UpdateFacultyRequest request){return service.update(id,request);}
 @PatchMapping("/{id}/status") @PreAuthorize("hasRole('ADMIN')") public FacultyResponse status(@PathVariable Long id,@RequestParam String status){FacultyResponse current=service.get(id);FacultyResponse updated=service.status(id,status);if("ACTIVE".equalsIgnoreCase(current.status())&&"INACTIVE".equalsIgnoreCase(updated.status()))publishAdminEvent("FACULTY_DEACTIVATED",updated.name());return updated;}
 @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<Map<String,Object>> delete(@PathVariable Long id){FacultyResponse current=service.get(id);FacultyResponse faculty=service.deactivate(id);if("ACTIVE".equalsIgnoreCase(current.status()))publishAdminEvent("FACULTY_DEACTIVATED",faculty.name());return ResponseEntity.ok(Map.of("message","Faculty deactivated to preserve account history","faculty",faculty));}
 private void publishAdminEvent(String eventType,String detail){if(notificationClient==null)return;try{notificationClient.publishAdminEvent(Map.of("eventType",eventType,"detail",detail));}catch(RuntimeException exception){LOGGER.log(System.Logger.Level.WARNING,"Faculty change saved but notification delivery failed ({0}).",exception.getClass().getSimpleName());}}
}
