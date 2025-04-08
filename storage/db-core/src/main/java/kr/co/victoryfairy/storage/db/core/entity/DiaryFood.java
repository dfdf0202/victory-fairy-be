package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "diary_food")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                // 일기 음식 식별자

    @ManyToOne
    @JoinColumn(name = "diary_id")
    private Diary diary;            // 일기 식별자

    @Column(name = "food_name")
    private String foodName;        // 음식 이름

}
