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
@Table(name = "xuehai_book")
@Getter
@Setter
@NoArgsConstructor
public class XuehaiBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(length = 256)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "cover_storage_path")
    private String coverStoragePath;

    @Column(name = "main_file_storage_path")
    private String mainFileStoragePath;

    @Column(name = "main_file_original_name")
    private String mainFileOriginalName;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "favorite_count", nullable = false)
    private int favoriteCount;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType = "other";

    @Column(nullable = false, length = 32)
    private String status = "ongoing";

    @Column(length = 512)
    private String tags;

    @Column(name = "word_count", nullable = false)
    private int wordCount;

    @Column(name = "avg_score", nullable = false)
    private double avgScore;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "read_count", nullable = false)
    private int readCount;

    @Column(name = "publish_at")
    private Instant publishAt;

    @Column(name = "recommend_weight", nullable = false)
    private int recommendWeight;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
