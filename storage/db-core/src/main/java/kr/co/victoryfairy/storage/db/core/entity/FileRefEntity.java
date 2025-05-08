package kr.co.victoryfairy.storage.db.core.entity;

import io.dodn.springboot.core.enums.RefType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@DynamicUpdate
@Entity(name = "file_ref")
public class FileRefEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * optional 을 붙이면 해당 관계가 반드시 존재해야 함을 의미해서
     * left 로 날라가던 query 가 inner 로 바뀌게 됨
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id")
    private FileEntity fileEntity;

    @Comment("참조 ID")
    @Column(name = "ref_id")
    private Long refId;

    @Comment("참조 구분")
    @Enumerated(EnumType.STRING)
    private RefType refType;

    //@CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    //@UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP", insertable = false)
    private LocalDateTime updatedAt;

    @Comment("테스트 후 삭제")
    @Builder.Default
    private Boolean isTest = true;
}
