package kr.co.victoryfairy.core.api.domain;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public interface DiaryDomain {

    @Schema(name = "Diary.DiaryDto")
    record DiaryDto(
            @Schema(description = "응원팀 id", requiredMode = Schema.RequiredMode.REQUIRED)
            Long teamId,

            @Schema(description = "관람 방식", implementation = DiaryEnum.ViewType.class, requiredMode = Schema.RequiredMode.REQUIRED)
            DiaryEnum.ViewType viewType,                    // 관람 방식

            @Schema(description = "경기 식별자", example = "20240309HTNC0" , requiredMode = Schema.RequiredMode.REQUIRED)
            String gameMatchId,

            @Schema(description = "업로드 file id", requiredMode = Schema.RequiredMode.REQUIRED)
            List<Long> fileId,

            // 선택 입력값
            @Schema(description = "날씨", implementation = DiaryEnum.WeatherType.class)
            DiaryEnum.WeatherType weather,                     // 날씨

            @Schema(description = "기분", implementation = DiaryEnum.MoodType.class)
            DiaryEnum.MoodType mood,

            @Schema(description = "음식 리스트", example = "[\"맥주\", \"치킨\"]")
            List<String> foodNameList,          // 음식 리스트

            @Schema(description = "좌석 정보")
            SeatUseHistoryDto seat,             // 좌석

            @Schema(description = "메모")
            String memo,

            @Schema(description = "함께한 사람 리스트")
            List<PartnerDto> partnerList       // 함께한 사람 리스트
    ) {}

    record PartnerDto(
            @Schema(description = "함께한 사람 이름", example = "홍길동")
            String name,

            @Schema(description = "함께한 사람의 응원 팀 id")
            Long teamId
    ) {}

    record SeatUseHistoryDto(
            @Schema(description = "좌석 식별자", example = "1")
            Long id,           // 좌석 식별자

            @Schema(description = "좌석 이름")
            Short name,     // 좌석 번호

            @Schema(description = "좌석 리뷰", example = "[\"탁 트인 시야\", \"넓은 공간\"]")
            List<String> desc // 좌석 리뷰
    ) {
    }

    record WriteEventDto(
            String gameId,
            Long memberId,
            Long diaryId,
            EventType type
    ) {}
}
