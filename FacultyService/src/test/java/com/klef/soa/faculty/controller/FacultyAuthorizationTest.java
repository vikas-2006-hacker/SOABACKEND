package com.klef.soa.faculty.controller;
import static org.junit.jupiter.api.Assertions.*;
import java.security.Principal;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import com.klef.soa.faculty.dto.FacultyResponse;
import com.klef.soa.faculty.service.FacultyService;
class FacultyAuthorizationTest {

 @Test void authenticatedFacultyCanResolveOnlyPrincipalEmailThroughMe() throws Exception {
  var service=new StubFacultyService(false);
  var controller=new FacultyController(service);
  Principal principal=()->"saketh@gmail.com";
  FacultyResponse response=controller.me(principal);
  assertEquals("saketh@gmail.com",service.requestedEmail);
  assertEquals("saketh@gmail.com",response.email());
  assertEquals("26000001",response.employeeId());
  assertEquals("hasRole('FACULTY')",FacultyController.class.getMethod("me",Principal.class).getAnnotation(PreAuthorize.class).value());
 }

 @Test void facultySelfProfileIsProtectedByFacultyRoleAndAuthentication() throws Exception {
  var method=FacultyController.class.getMethod("me",Principal.class);
  assertEquals("/api/faculty",FacultyController.class.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class).value()[0]);
  assertEquals("/me",method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value()[0]);
  assertEquals("hasRole('FACULTY')",method.getAnnotation(PreAuthorize.class).value());
  assertNotNull(com.klef.soa.faculty.security.SecurityConfig.class.getAnnotation(org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity.class));
 }

 @Test void authenticatedPrincipalWithoutFacultyRecordReceivesNotFound() {
  var controller=new FacultyController(new StubFacultyService(true));
  var error=assertThrows(ResponseStatusException.class,()->controller.me(()->"missing@example.invalid"));
  assertEquals(HttpStatus.NOT_FOUND,error.getStatusCode());
 }

 @Test void responsePreservesNullOptionalFieldsWithoutLeakingSecrets() {
  FacultyResponse response=new FacultyResponse(1L,"Saketh","saketh@gmail.com","26000001","CSE",null,null,"ACTIVE",true,null,null);
  assertNull(response.designation());
  assertNull(response.phone());
  assertFalse(response.toString().contains("password"));
  assertFalse(response.toString().contains("secret"));
 }

 @Test void allManagementMethodsRequireAdmin() throws Exception {
  assertAdmin("list");assertAdmin("get",Long.class);assertAdmin("create",com.klef.soa.faculty.dto.CreateFacultyRequest.class);assertAdmin("update",Long.class,com.klef.soa.faculty.dto.UpdateFacultyRequest.class);assertAdmin("status",Long.class,String.class);assertAdmin("delete",Long.class);
 }
 private void assertAdmin(String name,Class<?>... parameters)throws Exception{Method method=FacultyController.class.getMethod(name,parameters);assertEquals("hasRole('ADMIN')",method.getAnnotation(PreAuthorize.class).value());}

 private static class StubFacultyService extends FacultyService {
  private final boolean missing;
  private String requestedEmail;
  StubFacultyService(boolean missing){super(null,null);this.missing=missing;}
  @Override public FacultyResponse byEmail(String email){requestedEmail=email;if(missing)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty not found");return new FacultyResponse(1L,"Saketh",email,"26000001","CSE",null,null,"ACTIVE",true,null,null);}
 }
}
