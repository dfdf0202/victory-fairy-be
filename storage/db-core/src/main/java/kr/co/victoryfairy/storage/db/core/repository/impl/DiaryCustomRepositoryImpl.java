package kr.co.victoryfairy.storage.db.core.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.co.victoryfairy.storage.db.core.entity.DiaryEntity;
import kr.co.victoryfairy.storage.db.core.entity.QTeamEntity;
import kr.co.victoryfairy.storage.db.core.model.DiaryModel;
import kr.co.victoryfairy.storage.db.core.repository.DiaryCustomRepository;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static kr.co.victoryfairy.storage.db.core.entity.QDiaryEntity.diaryEntity;
import static kr.co.victoryfairy.storage.db.core.entity.QGameMatchEntity.gameMatchEntity;
import static kr.co.victoryfairy.storage.db.core.entity.QGameRecordEntity.gameRecordEntity;
import static kr.co.victoryfairy.storage.db.core.entity.QStadiumEntity.stadiumEntity;

@Repository
public class DiaryCustomRepositoryImpl extends QuerydslRepositorySupport implements DiaryCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public DiaryCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        super(DiaryEntity.class);
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<DiaryModel.DiaryDto> findList(DiaryModel.ListRequest request) {
        return jpaQueryFactory
                .select(Projections.fields(DiaryModel.DiaryDto.class
                        , diaryEntity.id
                        , gameMatchEntity.matchAt
                        , gameRecordEntity.resultType
                ))
                .from(diaryEntity)
                .leftJoin(gameMatchEntity).on(gameMatchEntity.id.eq(diaryEntity.gameMatchEntity.id))
                .leftJoin(gameRecordEntity).on(gameRecordEntity.diaryEntity.id.eq(diaryEntity.id))
                .where(diaryEntity.member.id.eq(request.memberId())
                        .and(this.betweenMatchAt(request.startDate(), request.endDate()))
                )
                .fetch();
    }

    @Override
    public List<DiaryModel.DiaryDto> findDailyList(DiaryModel.DailyListRequest request) {
        var awayEntity = new QTeamEntity("awayTeamEntity");
        var homeEntity = new QTeamEntity("homeTeamEntity");

        return jpaQueryFactory
                .select(Projections.fields(DiaryModel.DiaryDto.class
                        , diaryEntity.id
                        , diaryEntity.teamEntity.id.as("teamId")
                        , diaryEntity.content
                        , gameMatchEntity.matchAt
                        , awayEntity.id.as("awayTeamId")
                        , awayEntity.name.as("awayTeamName")
                        , gameMatchEntity.awayScore
                        , homeEntity.id.as("homeTeamId")
                        , homeEntity.name.as("homeTeamName")
                        , gameMatchEntity.homeScore
                        , gameRecordEntity.resultType
                        , gameMatchEntity.status
                        , stadiumEntity.shortName
                        , stadiumEntity.fullName
                        , diaryEntity.createdAt
                ))
                .from(diaryEntity)
                .leftJoin(gameMatchEntity).on(gameMatchEntity.id.eq(diaryEntity.gameMatchEntity.id))
                .leftJoin(awayEntity).on(awayEntity.id.eq(gameMatchEntity.awayTeamEntity.id))
                .leftJoin(homeEntity).on(homeEntity.id.eq(gameMatchEntity.homeTeamEntity.id))
                .leftJoin(stadiumEntity).on(gameMatchEntity.stadiumEntity.id.eq(stadiumEntity.id))
                .leftJoin(gameRecordEntity).on(gameRecordEntity.diaryEntity.id.eq(diaryEntity.id))
                .where(diaryEntity.member.id.eq(request.memberId())
                        .and(this.eqMatchAt(request.date()))
                )
                .fetch();
    }

    private BooleanExpression betweenMatchAt(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return gameMatchEntity.matchAt.between(start, end);
    }

    private BooleanExpression eqMatchAt(LocalDate matchAt) {
        if (matchAt == null) {
            return null;
        }

        String matchAtStr = matchAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        StringTemplate dbDate = Expressions.stringTemplate(
                "DATE_FORMAT({0}, '%Y-%m-%d')", gameMatchEntity.matchAt);

        return dbDate.eq(matchAtStr);
    }
}
