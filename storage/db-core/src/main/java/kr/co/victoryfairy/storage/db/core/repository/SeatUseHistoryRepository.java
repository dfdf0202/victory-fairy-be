package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.SeatUseHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatUseHistoryRepository extends JpaRepository<SeatUseHistoryEntity,Long> {

}
