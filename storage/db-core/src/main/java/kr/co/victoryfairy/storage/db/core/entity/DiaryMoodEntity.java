package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "diary_mood")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryMoodEntity extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                // 일기 기분 식별자

    @ManyToOne
    @JoinColumn(name = "diary_id")
    private DiaryEntity diaryEntity;            // 일기 식별자

    private String mood;            // 기분

}
