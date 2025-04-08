package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "partner")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // 함께한 사람 식별자

    @ManyToOne
    @JoinColumn(name = "diary_id")
    private Diary diary;                // 일기 식별자

    private String name;                // 함께한 사람 이름

    @Column(name = "team_name")
    private String teamName;            // 함께한 사람의 응원팀

}
