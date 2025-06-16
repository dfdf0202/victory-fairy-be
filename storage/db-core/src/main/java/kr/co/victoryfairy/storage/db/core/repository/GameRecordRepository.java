package kr.co.victoryfairy.storage.db.core.repository;

import io.dodn.springboot.core.enums.DiaryEnum;
import kr.co.victoryfairy.storage.db.core.entity.DiaryEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameRecordEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRecordRepository extends JpaRepository<GameRecordEntity, Long> {
    List<GameRecordEntity> findByMemberAndSeason(MemberEntity member, String season);
    List<GameRecordEntity> findByMemberId(Long memberId);
    GameRecordEntity findByMemberAndDiaryEntityId(MemberEntity member, DiaryEntity diary);
}
