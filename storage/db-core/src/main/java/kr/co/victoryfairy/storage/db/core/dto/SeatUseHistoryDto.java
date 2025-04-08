package kr.co.victoryfairy.storage.db.core.dto;


import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatUseHistoryDto {

    private Long seatId;                    // 좌석 식별자
    private String seatBlock;               // 좌석 블록
    private String seatRow;                 // 좌석 열
    private String seatNumber;              // 좌석 번호
    private List<String> seatReview;        // 좌석 리뷰


}
