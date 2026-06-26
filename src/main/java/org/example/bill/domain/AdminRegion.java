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
@Table(name = "admin_region")
@Getter
@Setter
@NoArgsConstructor
public class AdminRegion {

    @Id
    @Column(length = 12)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private short level;

    @Column(name = "parent_code", length = 12)
    private String parentCode;

    @Column(name = "province_code", nullable = false, length = 12)
    private String provinceCode;

    @Column(name = "city_code", length = 12)
    private String cityCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
