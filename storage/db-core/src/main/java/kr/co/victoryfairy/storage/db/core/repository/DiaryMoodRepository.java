package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.DiaryMoodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiaryMoodRepository extends JpaRepository<DiaryMoodEntity,Long> {

}
