package com.klef.soa.faculty.service;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.klef.soa.faculty.dto.*;
import com.klef.soa.faculty.entity.Faculty;
import com.klef.soa.faculty.repository.FacultyRepository;

@Service @Transactional
public class FacultyService {
 private final FacultyRepository repository; private final PasswordEncoder encoder;
 public FacultyService(FacultyRepository repository,PasswordEncoder encoder){this.repository=repository;this.encoder=encoder;}
 @Transactional(readOnly=true) public List<FacultyResponse> list(){return repository.findAll().stream().map(FacultyResponse::from).toList();}
 @Transactional(readOnly=true) public FacultyResponse get(Long id){return FacultyResponse.from(find(id));}
 @Transactional(readOnly=true) public FacultyResponse byEmail(String email){return FacultyResponse.from(repository.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty not found")));}
 @Transactional(readOnly=true) public FacultyDirectoryResponse directory(String email){return FacultyDirectoryResponse.from(repository.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty not found")));}
 public FacultyResponse create(CreateFacultyRequest request){
  String email=request.email().trim().toLowerCase(Locale.ROOT), employeeId=request.employeeId().trim().toUpperCase(Locale.ROOT);
  ensureUnique(email,employeeId,null); Faculty f=new Faculty(); f.setName(request.name().trim());f.setEmail(email);f.setEmployeeId(employeeId);f.setDepartment(request.department().trim());f.setDesignation(request.designation());f.setPhone(request.phone());f.setStatus("ACTIVE");f.setPasswordHash(encoder.encode(request.password()));f.setMfaEnabled(false);f.setMfaSecretEncrypted(null);return FacultyResponse.from(repository.save(f));
 }
 public FacultyResponse update(Long id,UpdateFacultyRequest request){Faculty f=find(id);String email=request.email().trim().toLowerCase(Locale.ROOT), employeeId=request.employeeId().trim().toUpperCase(Locale.ROOT);ensureUnique(email,employeeId,id);f.setName(request.name().trim());f.setEmail(email);f.setEmployeeId(employeeId);f.setDepartment(request.department().trim());if(request.designation()!=null)f.setDesignation(request.designation());if(request.phone()!=null)f.setPhone(request.phone());return FacultyResponse.from(repository.save(f));}
 public FacultyResponse status(Long id,String status){String normalized=status==null?"":status.trim().toUpperCase(Locale.ROOT);if(!Set.of("ACTIVE","INACTIVE").contains(normalized))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Status must be ACTIVE or INACTIVE");Faculty f=find(id);f.setStatus(normalized);return FacultyResponse.from(repository.save(f));}
 public FacultyResponse deactivate(Long id){return status(id,"INACTIVE");}
 private Faculty find(Long id){return repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty not found"));}
 private void ensureUnique(String email,String employeeId,Long except){repository.findByEmailIgnoreCase(email).filter(f->!Objects.equals(f.getId(),except)).ifPresent(f->{throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already exists");});repository.findByEmployeeIdIgnoreCase(employeeId).filter(f->!Objects.equals(f.getId(),except)).ifPresent(f->{throw new ResponseStatusException(HttpStatus.CONFLICT,"Employee ID already exists");});}
}
