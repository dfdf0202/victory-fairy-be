package kr.co.victoryfairy.core.api.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public interface TeamDomain {

    @Schema(name = "Team.TeamListResponse")
    record TeamListResponse(
            @Schema(description = "team id")
            Long id,

            @Schema(description = "팀명")
            String name,

            @Schema(description = "라벨")
            String label
    ) {}
}
