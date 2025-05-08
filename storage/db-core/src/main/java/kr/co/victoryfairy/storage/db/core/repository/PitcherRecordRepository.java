package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.PitcherRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PitcherRecordRepository extends JpaRepository<PitcherRecordEntity, Integer> {
}
