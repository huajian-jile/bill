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
@Table(name = "xuehai_chapter")
@Getter
@Setter
@NoArgsConstructor
public class XuehaiChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "file_storage_path", nullable = false)
    private String fileStoragePath;

    @Column(name = "file_original_name")
    private String fileOriginalName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
