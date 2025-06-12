package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

@Entity(name = "team")
public class TeamEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Comment("팀명")
    private String name;

    @Column
    @Comment("kbo 팀명")
    private String kboNm;

    @Column
    @Comment("스폰서 명")
    private String sponsorNm;

    @Column
    private String label;

    public TeamEntity() {
    }

    public TeamEntity(Long id, String name, String kboNm) {
        this.id = id;
        this.name = name;
        this.kboNm = kboNm;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKboNm() {
        return kboNm;
    }

    public String getLabel() {
        return label;
    }
}
