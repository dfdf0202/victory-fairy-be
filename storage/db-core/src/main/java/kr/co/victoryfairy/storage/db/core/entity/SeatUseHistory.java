package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "seat_use_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatUseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // 좌석 이용 내역 식별자

    @ManyToOne
    @JoinColumn(name = "seat_id")
    private Seat seat;                  // 좌석 식별자

    @OneToOne
    @JoinColumn(name = "diary_id")
    private Diary diary;                // 일기 식별자

    @Column(name = "seat_block")
    private String seatBlock;           // 좌석 블록

    @Column(name = "seat_row")
    private String seatRow;             // 좌석 열

    @Column(name = "seat_number")
    private String seatNumber;          // 좌석 번호

}
