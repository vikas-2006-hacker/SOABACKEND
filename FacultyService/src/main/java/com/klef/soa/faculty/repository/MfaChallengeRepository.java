package com.klef.soa.faculty.repository;
import java.time.Instant;import java.util.Optional;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import jakarta.persistence.LockModeType;
import com.klef.soa.faculty.entity.MfaChallenge;
public interface MfaChallengeRepository extends JpaRepository<MfaChallenge,String>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from MfaChallenge c where c.id=:id") Optional<MfaChallenge> lock(@Param("id")String id);
 @Modifying @Query("delete from MfaChallenge c where c.expiresAt<=:now") int deleteExpired(@Param("now")Instant now);
}
