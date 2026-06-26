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
@Table(name = "xuehai_book_favorite")
@Getter
@Setter
@NoArgsConstructor
public class XuehaiBookFavorite {

    @EmbeddedId
    private XuehaiUserBookKey id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
