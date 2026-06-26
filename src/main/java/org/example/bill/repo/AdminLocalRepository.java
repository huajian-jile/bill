package org.example.bill.repo;

import java.util.List;
import org.example.bill.domain.AdminLocal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLocalRepository extends JpaRepository<AdminLocal, String> {

    List<AdminLocal> findByParentCodeAndLevelOrderByNameAsc(String parentCode, short level);

    List<AdminLocal> findByParentCodeOrderByNameAsc(String parentCode);

    boolean existsByParentCodeAndLevel(String parentCode, short level);
}
