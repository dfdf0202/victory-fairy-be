package kr.co.victoryfairy.core.api.domain;

import io.dodn.springboot.core.enums.MatchEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public interface MatchDomain {

    record MatchListResponse(
            @Schema(description = "경기일자")
            LocalDate date,
            List<MatchListDto> matchList
    ) {}

    record MatchListDto(
            String id,
            LocalDate date,
            String time,
            String stadium,
            MatchEnum.MatchStatus status,
            String statusDetail,
            TeamDto awayTeam,
            TeamDto homeTeam
    ) {}

    record TeamDto(
            Long id,
            String name,
            Short score,
            MatchEnum.ResultType result
    ) {}

    record MatchInfoResponse(
            String id,
            LocalDate date,
            String time,
            StadiumDto stadium,
            MatchEnum.MatchStatus status,
            String statusDetail,
            TeamDto awayTeam,
            TeamDto homeTeam
    ) {}

    record StadiumDto(
            Long id,
            String shortName,
            String fullName
    ) {}
}
