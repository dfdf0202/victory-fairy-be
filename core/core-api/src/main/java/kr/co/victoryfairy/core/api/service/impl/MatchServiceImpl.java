package kr.co.victoryfairy.core.api.service.impl;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.api.domain.MatchDomain;
import kr.co.victoryfairy.core.api.service.MatchService;
import kr.co.victoryfairy.storage.db.core.entity.StadiumEntity;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.handler.RedisHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final TeamRepository teamRepository;
    private final StadiumRepository stadiumRepository;
    private final GameMatchRepository gameMatchRepository;
    private final GameMatchCustomRepository gameMatchCustomRepository;
    private final GameRecordRepository gameRecordRepository;
    private final RedisHandler redisHandler;

    @Override
    public MatchDomain.MatchListResponse findList(LocalDate date) {

        var formatDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 당일 경기 경우 redis 에서 가져오기
        List<MatchDomain.MatchListDto> matchList = new ArrayList();

        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        if (matchRedis.isEmpty()) {
            var matchEntities = gameMatchCustomRepository.findByMatchAt(date);

            if (matchEntities.isEmpty()) {
                return new MatchDomain.MatchListResponse(date, matchList);
            }

            matchList = matchEntities.stream()
                    .map(entity -> {
                        var matchAt = entity.getMatchAt();
                        var awayTeamEntity = teamRepository.findById(entity.getAwayTeamEntity().getId()).orElse(null);
                        var homeTeamEntity = teamRepository.findById(entity.getHomeTeamEntity().getId()).orElse(null);
                        var stadiumEntity = stadiumRepository.findById(entity.getStadiumEntity().getId()).orElse(null);

                        var awayScore = entity.getAwayScore();
                        var homeScore = entity.getHomeScore();

                        MatchEnum.ResultType awayResult = awayScore == null ? null :
                                (awayScore == homeScore ? MatchEnum.ResultType.DRAW :
                                (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
                        MatchEnum.ResultType homeResult = homeScore == null ? null :
                                (homeScore == awayScore ? MatchEnum.ResultType.DRAW :
                                (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);


                        var awayTeamDto = awayTeamEntity != null ? new MatchDomain.TeamDto(awayTeamEntity.getId(), awayTeamEntity.getName(),
                                awayScore, awayResult) : null;

                        var homeTeamDto = homeTeamEntity != null ? new MatchDomain.TeamDto(homeTeamEntity.getId(), homeTeamEntity.getName(),
                                homeScore, homeResult) : null;

                        return new MatchDomain.MatchListDto(
                                        entity.getId(),
                                        matchAt.toLocalDate(),
                                        matchAt.format(DateTimeFormatter.ofPattern("HH:mm")),
                                        stadiumEntity.getShortName(),
                                        entity.getStatus(),
                                        entity.getStatus().getDesc(),
                                        awayTeamDto,
                                        homeTeamDto);
                    }).toList();

            return new MatchDomain.MatchListResponse(date, matchList);
        }

        for (Map.Entry<String, Map<String, Object>> entry : matchRedis.entrySet()) {
            Map<String, Object> matchData = entry.getValue();

            String id = entry.getKey();
            String time = (String) matchData.get("time");
            String stadium = (String) matchData.get("stadium");
            MatchEnum.MatchStatus status = MatchEnum.MatchStatus.valueOf((String) matchData.get("status"));
            String statusDetail = (String) matchData.get("statusDetail");

            Long awayId = Long.valueOf(String.valueOf(matchData.get("awayId")));
            Long homeId = Long.valueOf(String.valueOf(matchData.get("homeId")));

            Object awayScoreObj = matchData.get("awayScore");
            Object homeScoreObj = matchData.get("homeScore");

            var awayEntity = teamRepository.findById(awayId).orElse(null);
            var homeEntity = teamRepository.findById(homeId).orElse(null);

            var awayScore = awayScoreObj != null ? Short.valueOf(String.valueOf(awayScoreObj)) : null;
            var homeScore = homeScoreObj != null ? Short.valueOf(String.valueOf(homeScoreObj)) : null;

            var awayResult = status.equals(MatchEnum.MatchStatus.END) ?
                    (awayScore == homeScore ? MatchEnum.ResultType.DRAW : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

            var homeResult = status.equals(MatchEnum.MatchStatus.END) ?
                    (homeScore == awayScore ? MatchEnum.ResultType.DRAW : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

            var awayTeamDto = awayEntity != null ? new MatchDomain.TeamDto(awayEntity.getId(), awayEntity.getName(),
                    awayScore, awayResult) : null;

            var homeTeamDto = homeEntity != null ? new MatchDomain.TeamDto(homeEntity.getId(), homeEntity.getName(),
                    homeScore, homeResult) : null;

            var matchDto = new MatchDomain.MatchListDto(id, date, time, stadium, status, statusDetail, awayTeamDto, homeTeamDto);

            matchList.add(matchDto);
        }

        return new MatchDomain.MatchListResponse(date, matchList);
    }

    @Override
    public MatchDomain.MatchInfoResponse findById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new CustomException(MessageEnum.Common.REQUEST_PARAMETER);
        }

        var formatDate = id.substring(0, 8);

        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        if (matchRedis.isEmpty()) {
            var matchEntity = gameMatchRepository.findById(id)
                    .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));


            var matchAt = matchEntity.getMatchAt();
            var awayTeamEntity = teamRepository.findById(matchEntity.getAwayTeamEntity().getId()).orElse(null);
            var homeTeamEntity = teamRepository.findById(matchEntity.getHomeTeamEntity().getId()).orElse(null);
            var stadiumEntity = stadiumRepository.findById(matchEntity.getStadiumEntity().getId()).orElse(null);

            var awayScore = matchEntity.getAwayScore();
            var homeScore = matchEntity.getHomeScore();

            MatchEnum.ResultType awayResult = awayScore == null ? null :
                    (awayScore == homeScore ? MatchEnum.ResultType.DRAW :
                            (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
            MatchEnum.ResultType homeResult = homeScore == null ? null :
                    (homeScore == awayScore ? MatchEnum.ResultType.DRAW :
                            (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);


            var awayTeamDto = awayTeamEntity != null ? new MatchDomain.TeamDto(awayTeamEntity.getId(), awayTeamEntity.getName(),
                    awayScore, awayResult) : null;

            var homeTeamDto = homeTeamEntity != null ? new MatchDomain.TeamDto(homeTeamEntity.getId(), homeTeamEntity.getName(),
                    homeScore, homeResult) : null;

            var stadiumDto = stadiumEntity != null ? new MatchDomain.StadiumDto(stadiumEntity.getId(), stadiumEntity.getShortName(), stadiumEntity.getFullName()) : null;

            return new MatchDomain.MatchInfoResponse(
                    matchEntity.getId(),
                    matchAt.toLocalDate(),
                    matchAt.format(DateTimeFormatter.ofPattern("HH:mm")),
                    stadiumDto,
                    matchEntity.getStatus(),
                    matchEntity.getStatus().getDesc(),
                    awayTeamDto,
                    homeTeamDto);
        }

        var matchData = matchRedis.get(id);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(formatDate, formatter);

        String time = (String) matchData.get("time");
        MatchEnum.MatchStatus status = MatchEnum.MatchStatus.valueOf((String) matchData.get("status"));
        String statusDetail = (String) matchData.get("statusDetail");

        Long awayId = Long.valueOf(String.valueOf(matchData.get("awayId")));
        Long homeId = Long.valueOf(String.valueOf(matchData.get("homeId")));
        Long stadiumId = Long.valueOf(String.valueOf(matchData.get("stadiumId")));

        Object awayScoreObj = matchData.get("awayScore");
        Object homeScoreObj = matchData.get("homeScore");

        var awayEntity = teamRepository.findById(awayId).orElse(null);
        var homeEntity = teamRepository.findById(homeId).orElse(null);
        var stadiumEntity = stadiumRepository.findById(stadiumId).orElse(null);

        var awayScore = awayScoreObj != null ? Short.valueOf(String.valueOf(awayScoreObj)) : null;
        var homeScore = homeScoreObj != null ? Short.valueOf(String.valueOf(homeScoreObj)) : null;

        var awayResult = status.equals(MatchEnum.MatchStatus.END) ?
                (awayScore == homeScore ? MatchEnum.ResultType.DRAW : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

        var homeResult = status.equals(MatchEnum.MatchStatus.END) ?
                (homeScore == awayScore ? MatchEnum.ResultType.DRAW : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

        var awayTeamDto = awayEntity != null ? new MatchDomain.TeamDto(awayEntity.getId(), awayEntity.getName(),
                awayScore, awayResult) : null;

        var homeTeamDto = homeEntity != null ? new MatchDomain.TeamDto(homeEntity.getId(), homeEntity.getName(),
                homeScore, homeResult) : null;

        var stadiumDto = stadiumEntity != null ? new MatchDomain.StadiumDto(stadiumEntity.getId(), stadiumEntity.getShortName(), stadiumEntity.getFullName()) : null;

        return new MatchDomain.MatchInfoResponse(
                id,
                date,
                time,
                stadiumDto,
                status,
                statusDetail,
                awayTeamDto,
                homeTeamDto
        );
    }
}
