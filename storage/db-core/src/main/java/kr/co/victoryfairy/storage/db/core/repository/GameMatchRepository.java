package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface GameMatchRepository extends JpaRepository<GameMatchEntity, String> {
    /**
     * 시즌 별 경기 일정 불러오기
     * @param sYear
     * @return
     */
    List<GameMatchEntity> findBySeason(String sYear);

    /**
     *
     * @param matchAt
     * @return
     */
    List<GameMatchEntity> findByMatchAt(LocalDateTime matchAt);
}
