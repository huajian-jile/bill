package org.example.bill.repo;

import java.util.List;
import java.util.Optional;
import org.example.bill.domain.AppUserPhone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserPhoneRepository extends JpaRepository<AppUserPhone, Long> {

    List<AppUserPhone> findByUserIdOrderByCreatedAtAsc(Long userId);

    boolean existsByMobileCn(String mobileCn);

    Optional<AppUserPhone> findByUserIdAndMobileCn(Long userId, String mobileCn);
}
