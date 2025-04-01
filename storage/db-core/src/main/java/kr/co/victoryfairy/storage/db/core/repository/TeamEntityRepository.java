package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamEntityRepository extends JpaRepository<TeamEntity, Long> {
}
