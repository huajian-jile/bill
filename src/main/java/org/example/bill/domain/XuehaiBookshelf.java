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
@Table(name = "xuehai_bookshelf")
@Getter
@Setter
@NoArgsConstructor
public class XuehaiBookshelf {

    @EmbeddedId
    private XuehaiUserBookKey id;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt = Instant.now();

    @Column(nullable = false)
    private boolean pinned;

    @Column(name = "pin_order", nullable = false)
    private int pinOrder;

    @Column(name = "last_read_chapter_id")
    private Long lastReadChapterId;

    @Column(name = "last_read_at")
    private Instant lastReadAt;
}
