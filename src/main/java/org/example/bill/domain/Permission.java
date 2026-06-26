package org.example.bill.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permissions")
@TableName("permissions")
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;
}
