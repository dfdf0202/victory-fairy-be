package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GameMatchCustomRepository {

    List<GameMatchEntity> findByMatchAt(LocalDate matchAt);

    Optional<GameMatchEntity> findByTeamId(Long teamId, LocalDate matchAt);

    List<GameMatchEntity> findByTeamIdIn(Long teamId, LocalDate matchAt);

    List<GameMatchEntity> findByYearAndMonth(String year, String month);
}
