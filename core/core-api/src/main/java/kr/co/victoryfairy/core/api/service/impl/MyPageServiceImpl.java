package kr.co.victoryfairy.core.api.service.impl;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.api.domain.MyPageDomain;
import kr.co.victoryfairy.core.api.service.MyPageService;
import kr.co.victoryfairy.storage.db.core.repository.GameRecordRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberCustomRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberRepository;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {

    private final MemberRepository memberRepository;
    private final MemberCustomRepository memberCustomRepository;
    private final GameRecordRepository gameRecordRepository;

    @Override
    public MyPageDomain.MemberInfoForMyPageResponse findMemberInfoForMyPage() {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        var member = memberCustomRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var teamDto = member.getTeamId() != null ? new MyPageDomain.TeamDto(member.getTeamId(), member.getTeamName()) : null;
        var fileDto = member.getFileId() != null ? new MyPageDomain.ImageDto(member.getFileId(), member.getPath(), member.getSaveName(), member.getExt()) : null;

        return new MyPageDomain.MemberInfoForMyPageResponse(
                member.getId(),
                fileDto,
                member.getNickNm(),
                member.getSnsType(),
                teamDto
        );
    }

    @Override
    public MyPageDomain.VictoryPowerResponse findVictoryPower() {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        var memberEntity = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var year = String.valueOf(LocalDate.now().getYear());

        var recordList = gameRecordRepository.findByMemberAndSeason(memberEntity, year);

        short stadiumWinAvg = 0;
        short homeWinAvg = 0;

        var stadiumRecord = recordList.stream()
                .filter(record -> record.getViewType() == DiaryEnum.ViewType.STADIUM)
                .toList();

        if (!stadiumRecord.isEmpty()) {
            var winCount = (short) stadiumRecord.stream()
                    .filter(record -> record.getResultType() == MatchEnum.ResultType.WIN)
                    .count();

            var loseCount = (short) stadiumRecord.stream()
                    .filter(record -> record.getResultType() == MatchEnum.ResultType.LOSS)
                    .count();

            var validGameCount = winCount + loseCount;

            if (validGameCount > 0) {
                double avg = (double) winCount / validGameCount * 100;
                stadiumWinAvg = (short) Math.round(avg);  // 소수점 첫째자리 반올림
            }
        }


        var homeRecord = recordList.stream()
                .filter(record -> record.getViewType() == DiaryEnum.ViewType.HOME)
                .toList();

        if (!homeRecord.isEmpty()) {
            var winCount = (short) homeRecord.stream()
                    .filter(record -> record.getResultType() == MatchEnum.ResultType.WIN)
                    .count();

            var loseCount = (short) homeRecord.stream()
                    .filter(record -> record.getResultType() == MatchEnum.ResultType.LOSS)
                    .count();

            var validGameCount = winCount + loseCount;

            if (validGameCount > 0) {
                double avg = (double) winCount / validGameCount * 100;
                homeWinAvg = (short) Math.round(avg);  // 소수점 첫째자리 반올림
            }
        }

        double avg = (double) (stadiumWinAvg + homeWinAvg) / 2;
        var power = (short) Math.round(avg);

        short level = 0;
        if (power >= 0 && power < 20) {
            level = 1; // Lv 1
        } else if (power >= 20 && power < 40) {
            level = 2; // Lv 2
        } else if (power >= 40 && power < 60) {
            level = 3; // Lv 3
        } else if (power >= 60 && power < 80) {
            level = 4; // Lv 4
        } else if (power >= 80) {
            level = 5; // 만렙
        }

        return new MyPageDomain.VictoryPowerResponse(level, power);
    }
}
