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
@Table(name = "xuehai_read_progress")
@Getter
@Setter
@NoArgsConstructor
public class XuehaiReadProgress {

    @EmbeddedId
    private XuehaiUserBookKey id;

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "offset_pos", nullable = false)
    private int offsetPos;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
