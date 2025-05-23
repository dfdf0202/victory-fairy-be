package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.GameRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRecordRepository extends JpaRepository<GameRecordEntity, Long> {

}
