package org.example.bill.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "xuehai_book_rating")
@Getter
@Setter
@NoArgsConstructor
public class XuehaiBookRating {

    @EmbeddedId
    private XuehaiUserBookKey id;

    @Column(nullable = false)
    private int score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
