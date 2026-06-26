package org.example.bill.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admin_local")
@Getter
@Setter
@NoArgsConstructor
public class AdminLocal {

    @Id
    @Column(length = 12)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private short level;

    @Column(name = "parent_code", nullable = false, length = 12)
    private String parentCode;

    @Column(name = "area_code", nullable = false, length = 12)
    private String areaCode;

    @Column(name = "city_code", nullable = false, length = 12)
    private String cityCode;

    @Column(name = "province_code", nullable = false, length = 12)
    private String provinceCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
