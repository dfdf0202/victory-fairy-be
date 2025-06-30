package kr.co.victoryfairy.core.api.service.impl;

import io.dodn.springboot.core.enums.EventType;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.api.domain.DiaryDomain;
import kr.co.victoryfairy.core.api.domain.MatchDomain;
import kr.co.victoryfairy.core.api.service.DiaryService;
import kr.co.victoryfairy.storage.db.core.entity.*;
import kr.co.victoryfairy.storage.db.core.model.DiaryModel;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.handler.RedisHandler;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryCustomRepository diaryCustomRepository;
    private final DiaryFoodRepository diaryFoodRepository;
    private final SeatRepository seatRepository;
    private final SeatUseHistoryRepository seatUseHistoryRepository;
    private final SeatReviewRepository seatReviewRepository;
    private final PartnerRepository partnerRepository;
    private final GameMatchRepository gameMatchRepository;
    private final GameRecordRepository gameRecordRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    private final FileRepository fileRepository;
    private final FileRefRepository fileRefRepository;
    private final RedisHandler redisHandler;

    @Transactional
    public void writeDiary(DiaryDomain.WriteRequest diaryDto){
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
                .content(diaryDto.content())
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
                        .teamEntity(partnerTeamEntity)
                        .build();
                partnerEntityList.add(partnerEntity);
            }
            partnerRepository.saveAll(partnerEntityList);
        }


        //
        DiaryDomain.SeatUseHistoryDto diaryDtoSeat = diaryDto.seat();
        if (diaryDtoSeat != null) {
            // 좌석 조회
            SeatEntity seatEntity = seatRepository.findById(diaryDtoSeat.id()).orElse(null);

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

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (gameMatchEntity.getStatus().equals(MatchEnum.MatchStatus.END) || gameMatchEntity.getStatus().equals(MatchEnum.MatchStatus.CANCELED)) {
                    var writeEventDto = new DiaryDomain.WriteEventDto(
                            diaryDto.gameMatchId(), id, diaryEntity.getId(), EventType.DIARY
                    );
                    redisHandler.pushEvent("write_diary", writeEventDto);
                }
            }
        });
    }

    @Override
    @Transactional
    public void updateDiary(Long diaryId, DiaryDomain.UpdateRequest request) {
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        MemberEntity member = memberRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var teamEntity = teamRepository.findById(request.teamId())
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));


        var diaryEntity = diaryRepository.findByMemberIdAndId(id, diaryId)
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var gameRecordEntity = gameRecordRepository.findByMemberAndDiaryEntityId(member, diaryId);

        diaryEntity.updateDiary(
                teamEntity.getName(),
                teamEntity,
                request.viewType(),
                request.mood(),
                request.weather(),
                request.content()
        );
        diaryRepository.save(diaryEntity);

        var bfFileRefEntity = fileRefRepository.findAllByRefTypeAndRefIdAndIsUseTrue(RefType.DIARY, diaryId);
        fileRefRepository.deleteAll(bfFileRefEntity);

        if (!request.fileId().isEmpty()) {
            // 기존 이미지 삭제 처리

            var fileEntities = fileRepository.findAllById(request.fileId());
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
        if (!request.foodNameList().isEmpty()) {
            // 기존 데이터 삭제 처리
            var bfFoodEntities = diaryFoodRepository.findByDiaryEntityId(diaryId);
            diaryFoodRepository.deleteAll(bfFoodEntities);

            List<DiaryFoodEntity> foodList = new ArrayList<>();
            for (String food : request.foodNameList()) {
                DiaryFoodEntity diaryFoodEntity = DiaryFoodEntity.builder()
                        .diaryEntity(diaryEntity)
                        .foodName(food)
                        .build();
                foodList.add(diaryFoodEntity);

            }
            diaryFoodRepository.saveAll(foodList);
        }

        // 선택 입력값인 함께한 사람 리스트가 비어있지 않는 경우
        if (!request.partnerList().isEmpty()) {
            // 기존 데이터 삭제 처리
            var bfPartnerEntities = partnerRepository.findByDiaryEntityId(diaryId);
            partnerRepository.deleteAll(bfPartnerEntities);

            List<PartnerEntity> partnerEntityList = new ArrayList<>();
            for (DiaryDomain.PartnerDto partnerDto : request.partnerList()) {
                TeamEntity partnerTeamEntity = null;

                if (partnerDto.teamId() != null) {
                    partnerTeamEntity = teamRepository.findById(partnerDto.teamId()).orElse(null);
                }

                var teamNm = partnerTeamEntity != null ? partnerTeamEntity.getName() : null;

                PartnerEntity partnerEntity = PartnerEntity.builder()
                        .diaryEntity(diaryEntity)
                        .name(partnerDto.name())
                        .teamName(teamNm)
                        .teamEntity(partnerTeamEntity)
                        .build();
                partnerEntityList.add(partnerEntity);
            }
            partnerRepository.saveAll(partnerEntityList);
        }

        //
        DiaryDomain.SeatUseHistoryDto diaryDtoSeat = request.seat();
        if (diaryDtoSeat != null) {
            // 기존 데이터 삭제 처리
            var bfSeatUseHistoryEntity = seatUseHistoryRepository.findByDiaryEntityId(diaryId);
            var bfSeatReviewEntities = seatReviewRepository.findBySeatUseHistoryEntity(bfSeatUseHistoryEntity);

            if (!bfSeatReviewEntities.isEmpty()) {
                seatReviewRepository.deleteAll(bfSeatReviewEntities);
            }
            seatUseHistoryRepository.delete(bfSeatUseHistoryEntity);

            // 좌석 조회
            SeatEntity seatEntity = seatRepository.findById(diaryDtoSeat.id()).orElse(null);

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

        // 경기 결과 수정 반영
        if (gameRecordEntity != null && !gameRecordEntity.getTeamEntity().getId().equals(teamEntity.getId())) {
            var bfMyTeamEntity = gameRecordEntity.getTeamEntity();
            var bfResult = gameRecordEntity.getResultType();
            gameRecordEntity.updateRecord(
                    teamEntity,
                    bfMyTeamEntity,
                    bfResult.equals(MatchEnum.ResultType.WIN) ? MatchEnum.ResultType.LOSS :
                            bfResult.equals(MatchEnum.ResultType.LOSS) ? MatchEnum.ResultType.WIN : bfResult
            );
            gameRecordRepository.save(gameRecordEntity);
        }
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }
        MemberEntity member = memberRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var diaryEntity = diaryRepository.findByMemberIdAndId(id, diaryId)
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var gameRecordEntity = gameRecordRepository.findByMemberAndDiaryEntityId(member, diaryId);
        gameRecordRepository.delete(gameRecordEntity);

        var bfFileRefEntity = fileRefRepository.findAllByRefTypeAndRefIdAndIsUseTrue(RefType.DIARY, diaryId);
        if (!bfFileRefEntity.isEmpty()) {
            fileRefRepository.deleteAll(bfFileRefEntity);
        }

        var bfFoodEntities = diaryFoodRepository.findByDiaryEntityId(diaryId);
        if (!bfFoodEntities.isEmpty()) {
            diaryFoodRepository.deleteAll(bfFoodEntities);
        }

        var bfPartnerEntities = partnerRepository.findByDiaryEntityId(diaryId);
        if (!bfPartnerEntities.isEmpty()) {
            partnerRepository.deleteAll(bfPartnerEntities);
        }

        var bfSeatUseHistoryEntity = seatUseHistoryRepository.findByDiaryEntityId(diaryId);
        if (bfSeatUseHistoryEntity != null) {
            seatUseHistoryRepository.delete(bfSeatUseHistoryEntity);
        }

        var bfSeatReviewEntities = seatReviewRepository.findBySeatUseHistoryEntity(bfSeatUseHistoryEntity);
        if (!bfSeatReviewEntities.isEmpty()) {
            seatReviewRepository.deleteAll(bfSeatReviewEntities);
        }

        diaryRepository.delete(diaryEntity);
    }

    @Override
    public List<DiaryDomain.ListResponse> findList(YearMonth date) {
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        var startDate = date.atDay(1);
        var endDate = date.atEndOfMonth();

        var request = new DiaryModel.ListRequest(id, startDate, endDate);

        var diaryList = diaryCustomRepository.findList(request);

        if (diaryList.isEmpty()) {
            return new ArrayList<>();
        }

        var diaryIds = diaryList.stream()
                .map(DiaryModel.DiaryDto :: getId).toList();

        var fileMap = fileRefRepository.findByRefTypeAndRefIdInAndIsUseTrue(RefType.DIARY, diaryIds).stream()
                .collect(Collectors.toMap(
                        entity -> entity.getRefId(),
                        entity -> entity,
                        (existing, replacement) -> existing
                ));

        var diaryMap = diaryList.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getMatchAt().toLocalDate(),
                        dto -> dto,
                        (existing, replacement) -> existing
                ));

        var monthOfDays = IntStream.rangeClosed(1, date.lengthOfMonth())
                .mapToObj(day -> date.atDay(day))
                .toList();

        return monthOfDays.stream()
                .map(day -> {
                    var dto = diaryMap.get(day);

                    if (dto == null) {
                        return new DiaryDomain.ListResponse(null, day, null, null);
                    }

                    var fileRefEntity = fileMap.get(dto.getId());
                    DiaryDomain.ImageDto imageDto = null;

                    if (fileRefEntity != null) {
                        var fileEntity = fileRefEntity.getFileEntity();
                        imageDto = new DiaryDomain.ImageDto(fileEntity.getId(), fileEntity.getPath(), fileEntity.getSaveName(), fileEntity.getExt());
                    }

                    return new DiaryDomain.ListResponse(dto.getId(), day, imageDto, dto.getResultType());
                })
                .toList();
    }

    @Override
    public List<DiaryDomain.DailyListResponse> findDailyList(LocalDate date) {
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        var diaryEntities = diaryCustomRepository.findDailyList(new DiaryModel.DailyListRequest(id, date));

        if (diaryEntities.isEmpty()) {
            return new ArrayList<>();
        }

        var diaryIds = diaryEntities.stream()
                .map(DiaryModel.DiaryDto :: getId).toList();

        var fileMap = fileRefRepository.findByRefTypeAndRefIdInAndIsUseTrue(RefType.DIARY, diaryIds).stream()
                .collect(Collectors.toMap(
                        entity -> entity.getRefId(),
                        entity -> entity,
                        (existing, replacement) -> existing
                ));

        return diaryEntities.stream()
                    .map(entity -> {
                        var fileRefEntity = fileMap.get(entity.getId());
                        DiaryDomain.ImageDto imageDto = null;

                        if (fileRefEntity != null) {
                            var fileEntity = fileRefEntity.getFileEntity();
                            imageDto = new DiaryDomain.ImageDto(fileEntity.getId(), fileEntity.getPath(), fileEntity.getSaveName(), fileEntity.getExt());
                        }

                        var awayScore = entity.getAwayScore();
                        var homeScore = entity.getHomeScore();

                        MatchEnum.ResultType awayResult = awayScore == null ? null :
                                (awayScore == homeScore ? MatchEnum.ResultType.DRAW :
                                        (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
                        MatchEnum.ResultType homeResult = homeScore == null ? null :
                                (homeScore == awayScore ? MatchEnum.ResultType.DRAW :
                                        (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

                        var awayTeamDto = new MatchDomain.TeamDto(entity.getAwayTeamId(), entity.getAwayTeamName(), awayScore, awayResult);

                        var homeTeamDto = new MatchDomain.TeamDto(entity.getHomeTeamId(), entity.getHomeTeamName(), homeScore, homeResult);

                        return new DiaryDomain.DailyListResponse(entity.getId(), entity.getShortName(),
                                                                entity.getMatchAt().toLocalDate(), entity.getMatchAt().format(DateTimeFormatter.ofPattern("HH:mm")),
                                                                entity.getTeamId(), awayTeamDto, homeTeamDto, entity.getContent(),
                                                                imageDto, entity.getCreatedAt()
                        );
                    })
                    .toList();
    }

    @Override
    public DiaryDomain.DiaryDetailResponse findById(Long diaryId) {
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        var diaryEntity = diaryRepository.findByMemberIdAndId(id, diaryId)
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var foodList = diaryFoodRepository.findByDiaryEntityId(diaryId).stream()
                .map(entity -> {
                    if (entity == null) {
                        return null;
                    }

                    return entity.getFoodName();
                })
                .toList();

        var fileDto = fileRefRepository.findAllByRefTypeAndRefIdAndIsUseTrue(RefType.DIARY, diaryId).stream()
                .map(entity -> {
                    if (entity == null) {
                        return null;
                    }
                    var fileEntity = entity.getFileEntity();
                    return new DiaryDomain.ImageDto(fileEntity.getId(), fileEntity.getPath(), fileEntity.getSaveName(), fileEntity.getExt());
                })
                .toList();

        DiaryDomain.SeatUseHistoryDto seatUseHistoryDto = null;

        var seatUseHistoryEntity = seatUseHistoryRepository.findByDiaryEntityId(diaryId);
        if (seatUseHistoryEntity != null) {
            var seatReviewList = seatReviewRepository.findBySeatUseHistoryEntity(seatUseHistoryEntity).stream()
                    .map(entity -> {
                        if (entity == null) {
                            return null;
                        }
                        return entity.getSeatReview();
                    })
                    .toList();

            seatUseHistoryDto = new DiaryDomain.SeatUseHistoryDto(
                    seatUseHistoryDto != null ? seatUseHistoryEntity.getId() : null,
                    seatUseHistoryEntity.getSeatName(),
                    seatReviewList
            );
        }

        var partnerList = partnerRepository.findByDiaryEntityId(diaryId).stream()
                .map(entity -> {
                    if (entity == null) {
                        return null;
                    }
                    return new DiaryDomain.PartnerDto(entity.getName(), entity.getTeamEntity() != null ? entity.getTeamEntity().getId() : null);
                })
                .toList();

        return new DiaryDomain.DiaryDetailResponse(
                diaryEntity.getTeamEntity().getId(),
                diaryEntity.getViewType(),
                diaryEntity.getGameMatchEntity().getId(),
                fileDto,
                diaryEntity.getWeatherType(),
                diaryEntity.getMoodType(),
                foodList,
                seatUseHistoryDto,
                diaryEntity.getContent(),
                partnerList,
                diaryEntity.getCreatedAt(),
                diaryEntity.getUpdatedAt()
        );
    }

}
