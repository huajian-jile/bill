package org.example.bill.repo;

import org.example.bill.domain.WxPhoneSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface WxPhoneSessionRepository extends JpaRepository<WxPhoneSession, Long> {

    Optional<WxPhoneSession> findByCode(String code);

    @Modifying
    @Query("DELETE FROM WxPhoneSession s WHERE s.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}
