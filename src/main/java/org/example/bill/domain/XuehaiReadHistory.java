package org.example.bill.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "xuehai_read_history")
@Getter
@Setter
@NoArgsConstructor
public class XuehaiReadHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(nullable = false)
    private double progress;

    @Column(name = "read_at", nullable = false)
    private Instant readAt = Instant.now();
}
