package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameMatchEntityRepository extends JpaRepository<GameMatchEntity, String> {
}
