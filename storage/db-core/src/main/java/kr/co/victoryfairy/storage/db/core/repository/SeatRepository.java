package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatRepository extends JpaRepository<SeatEntity,Long> {

}
