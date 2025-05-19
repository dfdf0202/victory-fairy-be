package kr.co.victoryfairy.core.api.service.impl;

import io.dodn.springboot.core.enums.EventType;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.api.domain.DiaryDomain;
import kr.co.victoryfairy.core.api.service.DiaryService;
import kr.co.victoryfairy.storage.db.core.entity.*;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.handler.RedisHandler;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryFoodRepository diaryFoodRepository;
    private final SeatRepository seatRepository;
    private final SeatUseHistoryRepository seatUseHistoryRepository;
    private final SeatReviewRepository seatReviewRepository;
    private final PartnerRepository partnerRepository;
    private final GameMatchRepository gameMatchRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    private final FileRepository fileRepository;
    private final FileRefRepository fileRefRepository;
    private final RedisHandler redisHandler;

    @Transactional
    public void writeDiary(DiaryDomain.DiaryDto diaryDto){
        // 로그인한 회원 조회
        var id = RequestUtils.getId();
        MemberEntity member = memberRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        // 일기를 작성할 경기 조회
        GameMatchEntity gameMatchEntity = gameMatchRepository.findById(diaryDto.gameMatchId())
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var teamEntity = teamRepository.findById(diaryDto.teamId())
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        if (diaryRepository.findByMemberAndGameMatchEntity(member, gameMatchEntity) != null) {
            throw new CustomException(MessageEnum.Data.FAIL_DUPLICATE);
        }

        DiaryEntity diaryEntity = DiaryEntity.builder()
                .member(member)
                .teamName(teamEntity.getName())
                .teamEntity(teamEntity)
                .viewType(diaryDto.viewType())
                .gameMatchEntity(gameMatchEntity)
                .weatherType(diaryDto.weather())
                .moodType(diaryDto.mood())
                .memo(diaryDto.memo())
                .build();
        diaryRepository.save(diaryEntity);

        if (!diaryDto.fileId().isEmpty()) {
            var fileEntities = fileRepository.findAllById(diaryDto.fileId());
            var fileRefEntities = fileEntities.stream()
                    .map(file -> FileRefEntity.builder()
                            .fileEntity(file)
                            .refId(diaryEntity.getId())
                            .refType(RefType.DIARY)
                            .build()
                    )
                    .toList();
            fileRefRepository.saveAll(fileRefEntities);
        }

        // 선택 입력값인 음식 리스트가 비어있지 않는 경우
        if (!diaryDto.foodNameList().isEmpty()) {
            List<DiaryFoodEntity> foodList = new ArrayList<>();
            for (String food : diaryDto.foodNameList()) {
                DiaryFoodEntity diaryFoodEntity = DiaryFoodEntity.builder()
                        .diaryEntity(diaryEntity)
                        .foodName(food)
                        .build();
                foodList.add(diaryFoodEntity);

            }
            diaryFoodRepository.saveAll(foodList);
        }

        // 선택 입력값인 함께한 사람 리스트가 비어있지 않는 경우
        if (!diaryDto.partnerList().isEmpty()) {
            List<PartnerEntity> partnerEntityList = new ArrayList<>();
            for (DiaryDomain.PartnerDto partnerDto : diaryDto.partnerList()) {
                var partnerTeamEntity = teamRepository.findById(partnerDto.teamId())
                        .orElse(null);

                var teamNm = partnerTeamEntity != null ? partnerTeamEntity.getName() : null;

                PartnerEntity partnerEntity = PartnerEntity.builder()
                        .diaryEntity(diaryEntity)
                        .name(partnerDto.name())
                        .teamName(teamNm)
                        .teamEntity(teamEntity)
                        .build();
                partnerEntityList.add(partnerEntity);
            }
            partnerRepository.saveAll(partnerEntityList);
        }


        //
        DiaryDomain.SeatUseHistoryDto diaryDtoSeat = diaryDto.seat();
        if (diaryDtoSeat != null) {
            // 좌석 조회
            SeatEntity seatEntity = seatRepository.findById(diaryDtoSeat.id())
                    .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

            // 좌석 이용 내역 저장
            SeatUseHistoryEntity seatUseHistoryEntity = SeatUseHistoryEntity.builder()
                    .diaryEntity(diaryEntity)
                    .seatEntity(seatEntity)
                    .seatName(diaryDtoSeat.name())
                    .build();
            seatUseHistoryRepository.save(seatUseHistoryEntity);

            // 좌석 리뷰 저장
            List<SeatReviewEntity> reviewList = new ArrayList<>();
            for (String review : diaryDtoSeat.desc()) {
                SeatReviewEntity seatReviewEntity = SeatReviewEntity.builder()
                        .seatUseHistoryEntity(seatUseHistoryEntity)
                        .seatReview(review)
                        .build();
                reviewList.add(seatReviewEntity);
            }
            seatReviewRepository.saveAll(reviewList);
        }

        // Event 발급
        if (gameMatchEntity.getStatus().equals(MatchEnum.MatchStatus.END)) {
            var writeEventDto = new DiaryDomain.WriteEventDto(diaryDto.gameMatchId(), id, diaryEntity.getId(), EventType.DIARY);
            redisHandler.pushEvent("write_diary", writeEventDto);
        }
    }

}
