package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.DiaryFoodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaryFoodRepository extends JpaRepository<DiaryFoodEntity,Long> {
    List<DiaryFoodEntity> findByDiaryEntityId(Long diaryId);
    List<DiaryFoodEntity> findAllByDiaryEntityIdIn(List<Long> diaryId);
}
