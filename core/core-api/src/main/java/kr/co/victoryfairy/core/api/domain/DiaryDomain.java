package kr.co.victoryfairy.core.api.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.victoryfairy.storage.db.core.dto.PartnerDto;
import kr.co.victoryfairy.storage.db.core.dto.SeatUseHistoryDto;

import java.util.List;

public interface DiaryDomain {

    @Schema(name = "Diary.DiaryDto")
    record DiaryDto(
            // 필수 입력값
            @Schema(description = "응원팀", example = "키움")
            String teamName,                    // 응원팀

            @Schema(description = "관람 방식", example = "직관")
            String viewType,                    // 관람 방식

            @Schema(description = "이미지 링크", example = "img_url")
            String imgUrl,                      // 사진 url

            @Schema(description = "경기 식별자", example = "20240309HTNC0")
            String gameMatchId,                 // 경기 식별자

            // 선택 입력값
            @Schema(description = "날씨", example = "흐림")
            String weather,                     // 날씨

            @Schema(description = "좌석 정보", example = "SeatUseHistoryDto")
            SeatUseHistoryDto seat,             // 좌석

            @Schema(description = "음식 리스트", example = "[치킨,맥주]")
            List<String> foodNameList,          // 음식 리스트

            @Schema(description = "함께한 사람 리스트", example = "[PartnerDto]")
            List<PartnerDto> partnerList,       // 함께한 사람 리스트

            @Schema(description = "기분 리스트", example = "[좋음]")
            List<String> moodList               // 기분 리스트
    ) {}


}
