package org.example.bill.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_user_phones")
@Getter
@Setter
@NoArgsConstructor
public class AppUserPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "mobile_cn", nullable = false, length = 11)
    private String mobileCn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
