package org.example.bill.repo;

import java.util.Optional;
import org.example.bill.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("select count(distinct u.id) from AppUser u join u.roles r where r.code = :roleCode")
    long countDistinctUsersHavingRole(@Param("roleCode") String roleCode);
}
