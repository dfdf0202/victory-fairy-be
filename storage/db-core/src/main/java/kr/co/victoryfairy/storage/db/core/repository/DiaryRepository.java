package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.DiaryEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaryRepository extends JpaRepository<DiaryEntity,Long> {
    DiaryEntity findByMemberAndGameMatchEntity(MemberEntity memberEntity, GameMatchEntity gameMatchEntity);
    List<DiaryEntity> findByGameMatchEntityAndIsRatedFalse(GameMatchEntity gameMatchEntity);
}
