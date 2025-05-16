package kr.co.victoryfairy.core.event.service;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.event.model.EventDomain;
import kr.co.victoryfairy.storage.db.core.entity.GameRecordEntity;
import kr.co.victoryfairy.storage.db.core.entity.WinningRateEntity;
import kr.co.victoryfairy.storage.db.core.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final MemberRepository memberRepository;
    private final DiaryRepository diaryRepository;
    private final TeamRepository teamRepository;
    private final GameMatchRepository matchRepository;
    private final GameRecordRepository gameRecordRepository;

    private final WinningRateRepository winningRateRepository;

    public EventService(MemberRepository memberRepository, DiaryRepository diaryRepository,
                        GameMatchRepository matchRepository, TeamRepository teamRepository,
                        GameRecordRepository gameRecordRepository, WinningRateRepository winningRateRepository) {
        this.memberRepository = memberRepository;
        this.diaryRepository = diaryRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.gameRecordRepository = gameRecordRepository;
        this.winningRateRepository = winningRateRepository;
    }

    @Transactional
    public void processDiary(EventDomain.WriteEventDto eventDto) {
        var memberEntity = memberRepository.findById(eventDto.memberId())
                .orElse(null);

        var matchEntity = matchRepository.findById(eventDto.gameId())
                .orElse(null);

        var diaryEntity = diaryRepository.findById(eventDto.diaryId())
                .orElse(null);

        if (diaryEntity.getIsRated()) {
            return;
        }

        var teamEntity = teamRepository.findById(diaryEntity.getTeamEntity().getId())
                .orElse(null);

        if (memberEntity == null || matchEntity == null || diaryEntity == null || teamEntity == null) {
            return;
        }

        var awayTeam = matchEntity.getAwayTeamEntity();
        var homeTeam = matchEntity.getHomeTeamEntity();

        var awayScore = matchEntity.getAwayScore();
        var homeScore = matchEntity.getHomeScore();

        var isAway = awayTeam.getId().equals(teamEntity.getId());

        var myScore = isAway ? awayScore : homeScore;
        var opponentScore = isAway ? homeScore : awayScore;

        var isWin = myScore > opponentScore;

        var gameRecordEntity = GameRecordEntity.builder()
                .member(memberEntity)
                .teamEntity(teamEntity)
                .teamName(awayTeam.getName())
                .opponentTeamEntity(isAway ? homeTeam : awayTeam)
                .opponentTeamName(homeTeam.getName())
                .stadium(matchEntity.getStadium())
                .status(matchEntity.getStatus())
                .isWin(isWin)
                .build();

        gameRecordRepository.save(gameRecordEntity);

        // 종료된 경기에 대해서만 승률 반영
        if (matchEntity.getStatus().equals(MatchEnum.MatchStatus.END)) {
            var memberWinRateEntity = winningRateRepository.findByMemberAndSeason(memberEntity, matchEntity.getSeason())
                    .orElseGet(() -> {
                        WinningRateEntity newEntity = new WinningRateEntity(memberEntity, matchEntity.getSeason());
                        return newEntity;
                    });

            short totalCnt =  (short) (defaultShort(memberWinRateEntity.getTotalCnt()) + 1);
            short totalWinCnt = (short) (defaultShort(memberWinRateEntity.getTotalWinCnt()) + (isWin ? 1 : 0));

            short homeCnt = (short) 0;
            short homeWinCnt = (short) 0;

            short stadiumCnt = (short) 0;
            short stadiumWinCnt = (short) 0;

            if (diaryEntity.getViewType().equals(DiaryEnum.ViewType.HOME)) {
                homeCnt = (short) (defaultShort(memberWinRateEntity.getHomeCnt()) + 1);
                homeWinCnt = (short) (defaultShort(memberWinRateEntity.getHomeWinCnt()) + (isWin ? 1 : 0));
            } else {
                stadiumCnt = (short) (defaultShort(memberWinRateEntity.getStadiumCnt()) + 1);
                stadiumWinCnt = (short) (defaultShort(memberWinRateEntity.getStadiumWinCnt()) + (isWin ? 1 : 0));
            }

            Float totalAvg = calcRate(totalWinCnt, totalCnt);
            Float homeAvg = calcRate(homeWinCnt, homeCnt);
            Float stadiumAvg = calcRate(stadiumWinCnt, stadiumCnt);

            memberWinRateEntity.updateWinningRate(totalCnt, totalWinCnt, totalAvg, homeCnt, homeWinCnt, homeAvg, stadiumCnt, stadiumWinCnt, stadiumAvg);
            winningRateRepository.save(memberWinRateEntity);
        }

        // 이벤트 적용 여부 업데이트
        diaryEntity.updateRated();
        diaryRepository.save(diaryEntity);
    }

    private float calcRate(Short win, int total) {
        if (total == 0) return 0.0f;
        return Math.round((win * 100.0f) / total);
    }

    private short defaultShort(Short val) {
        return val != null ? val : 0;
    }

    @Transactional
    public void processBatch(EventDomain.WriteEventDto eventDto) {
        var matchEntity = matchRepository.findById(eventDto.gameId())
                .orElse(null);

        if (!matchEntity.getStatus().equals(MatchEnum.MatchStatus.END)) {
            return;
        }

        var diaryEntities = diaryRepository.findByGameMatchEntityAndIsRatedFalse(matchEntity);

        if (diaryEntities.isEmpty()) {
            return;
        }

        diaryEntities.forEach(diaryEntity -> {
            if (diaryEntity.getIsRated()) {
                return;
            }

            var memberEntity = memberRepository.findById(diaryEntity.getMember().getId())
                    .orElse(null);

            var teamEntity = teamRepository.findById(diaryEntity.getTeamEntity().getId())
                    .orElse(null);

            var awayTeam = matchEntity.getAwayTeamEntity();
            var homeTeam = matchEntity.getHomeTeamEntity();

            var awayScore = matchEntity.getAwayScore();
            var homeScore = matchEntity.getHomeScore();

            var isAway = awayTeam.getId().equals(teamEntity.getId());

            var myScore = isAway ? awayScore : homeScore;
            var opponentScore = isAway ? homeScore : awayScore;

            var isWin = myScore > opponentScore;

            var gameRecordEntity = GameRecordEntity.builder()
                    .member(memberEntity)
                    .teamEntity(teamEntity)
                    .teamName(awayTeam.getName())
                    .opponentTeamEntity(isAway ? homeTeam : awayTeam)
                    .opponentTeamName(homeTeam.getName())
                    .stadium(matchEntity.getStadium())
                    .status(matchEntity.getStatus())
                    .isWin(isWin)
                    .build();

            gameRecordRepository.save(gameRecordEntity);

            var memberWinRateEntity = winningRateRepository.findByMemberAndSeason(memberEntity, matchEntity.getSeason())
                    .orElseGet(() -> {
                        WinningRateEntity newEntity = new WinningRateEntity(memberEntity, matchEntity.getSeason());
                        return newEntity;
                    });

            short totalCnt =  (short) (defaultShort(memberWinRateEntity.getTotalCnt()) + 1);
            short totalWinCnt = (short) (defaultShort(memberWinRateEntity.getTotalWinCnt()) + (isWin ? 1 : 0));

            short homeCnt = (short) 0;
            short homeWinCnt = (short) 0;

            short stadiumCnt = (short) 0;
            short stadiumWinCnt = (short) 0;

            if (diaryEntity.getViewType().equals(DiaryEnum.ViewType.HOME)) {
                homeCnt = (short) (defaultShort(memberWinRateEntity.getHomeCnt()) + 1);
                homeWinCnt = (short) (defaultShort(memberWinRateEntity.getHomeWinCnt()) + (isWin ? 1 : 0));
            } else {
                stadiumCnt = (short) (defaultShort(memberWinRateEntity.getStadiumCnt()) + 1);
                stadiumWinCnt = (short) (defaultShort(memberWinRateEntity.getStadiumWinCnt()) + (isWin ? 1 : 0));
            }

            Float totalAvg = calcRate(totalWinCnt, totalCnt);
            Float homeAvg = calcRate(homeWinCnt, homeCnt);
            Float stadiumAvg = calcRate(stadiumWinCnt, stadiumCnt);

            memberWinRateEntity.updateWinningRate(totalCnt, totalWinCnt, totalAvg, homeCnt, homeWinCnt, homeAvg, stadiumCnt, stadiumWinCnt, stadiumAvg);
            winningRateRepository.save(memberWinRateEntity);

            memberWinRateEntity.updateWinningRate(totalCnt, totalWinCnt, totalAvg, homeCnt, homeWinCnt, homeAvg, stadiumCnt, stadiumWinCnt, stadiumAvg);
            winningRateRepository.save(memberWinRateEntity);

            diaryEntity.updateRated();
            diaryRepository.save(diaryEntity);
        });

    }
}
