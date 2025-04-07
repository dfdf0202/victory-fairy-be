package kr.co.victoryfairy.storage.db.core.entity;

import io.dodn.springboot.core.enums.MatchEnum;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity(name = "game_match")
public class GameMatchEntity extends BaseEntity {

    @Id
    private String id;

    @Column
    @Comment("경기 타입")
    @Enumerated(EnumType.STRING)
    private MatchEnum.MatchType type;

    @Column
    @Comment("시리즈 타입")
    @Enumerated(EnumType.STRING)
    private MatchEnum.SeriesType series;

    @Comment("시즌")
    private String season;

    @Column
    @Comment("경기 일자")
    private LocalDateTime matchAt;

    @Comment("어웨이")
    @JoinColumn(name = "away_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private TeamEntity awayTeamEntity;

    @Column
    @Comment("어웨이 팀명")
    private String awayNm;

    @Column
    @Comment("어웨이 점수")
    private Short awayScore;

    @Comment("홈")
    @JoinColumn(name = "home_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private TeamEntity homeTeamEntity;

    @Column
    @Comment("홈 팀명")
    private String homeNm;

    @Column
    @Comment("홈 스코어")
    private Short homeScore;

    @Column
    @Comment("경기장")
    private String stadium;

    @Column
    @Comment("경기 상태")
    @Enumerated(EnumType.STRING)
    private MatchEnum.MatchStatus status;

    @Column
    @Comment("사유")
    private String reason;

    public void updateMatchStatus(MatchEnum.MatchStatus matchStatus) {
        this.status = matchStatus;
    }

    public void updateMatchStatusAndScore(Short awayScore, Short homeScore, MatchEnum.MatchStatus status) {
        this.awayScore = awayScore;
        this.homeScore = homeScore;
        this.status = status;
        this.reason = "-";
    }

    public void cancelMatchStatus(String reason, MatchEnum.MatchStatus status) {
        this.reason = reason;
        this.status = status;
    }

    public GameMatchEntity() {
    }

    public GameMatchEntity(String id, String sYear, MatchEnum.MatchType matchType, MatchEnum.SeriesType seriesType, LocalDateTime matchAt, TeamEntity awayTeamEntity, String awayNm, Short awayScore, TeamEntity homeTeamEntity, String homeNm, Short homeScore, MatchEnum.MatchStatus status, String stadium, String reason) {
        this.id = id;
        this.season = sYear;
        this.type = matchType;
        this.series = seriesType;
        this.matchAt = matchAt;
        this.awayTeamEntity = awayTeamEntity;
        this.awayNm = awayNm;
        this.awayScore = awayScore;
        this.homeTeamEntity = homeTeamEntity;
        this.homeNm = homeNm;
        this.homeScore = homeScore;
        this.status = status;
        this.stadium = stadium;
        this.reason = reason;
    }

    public String getId() {
        return id;
    }

    public MatchEnum.MatchType getType() {
        return type;
    }

    public MatchEnum.SeriesType getSeries() {
        return series;
    }

    public String getSeason() {
        return season;
    }

    public LocalDateTime getMatchAt() {
        return matchAt;
    }

    public TeamEntity getAwayTeamEntity() {
        return awayTeamEntity;
    }

    public String getAwayNm() {
        return awayNm;
    }

    public Short getAwayScore() {
        return awayScore;
    }

    public TeamEntity getHomeTeamEntity() {
        return homeTeamEntity;
    }

    public String getHomeNm() {
        return homeNm;
    }

    public Short getHomeScore() {
        return homeScore;
    }

    public String getStadium() {
        return stadium;
    }

    public MatchEnum.MatchStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
