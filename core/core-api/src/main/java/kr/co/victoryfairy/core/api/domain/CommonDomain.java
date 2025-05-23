package kr.co.victoryfairy.core.api.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public interface CommonDomain {

    @Schema(name = "Common.TeamListResponse")
    record TeamListResponse(
            @Schema(description = "team id")
            Long id,

            @Schema(description = "팀명")
            String name,

            @Schema(description = "라벨")
            String label
    ) {}

    record SeatListResponse(
            @Schema(description = "seat id")
            Long id,

            @Schema(description = "좌석명")
            String name
    ) {}
}
