package kr.co.victoryfairy.storage.db.core.dto;


import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryDto {

    // 필수 입력값
    private String teamName;                    // 응원팀
    private String viewType;                    // 관람 방식
    private String imgUrl;                      // 사진 url
    private String gameMatchId;                 // 경기 식별자

    // 선택 입력값
    private String weather;                     // 날씨
    private SeatUseHistoryDto seat;             // 좌석
    private List<String> foodNameList;          // 음식 리스트
    private List<PartnerDto> partnerList;       // 함께한 사람 리스트
    private List<String> moodList;              // 기분 리스트

}
