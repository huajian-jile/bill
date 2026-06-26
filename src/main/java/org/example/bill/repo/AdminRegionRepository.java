package org.example.bill.repo;

import java.util.List;
import org.example.bill.domain.AdminRegion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRegionRepository extends JpaRepository<AdminRegion, String> {

    List<AdminRegion> findByLevelOrderByCodeAsc(short level);

    List<AdminRegion> findByParentCodeOrderByCodeAsc(String parentCode);
}
