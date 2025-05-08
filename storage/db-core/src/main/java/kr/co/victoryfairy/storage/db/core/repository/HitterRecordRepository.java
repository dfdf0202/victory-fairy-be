package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.HitterRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HitterRecordRepository extends JpaRepository<HitterRecordEntity, Integer> {
}
