package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;

import java.time.LocalDate;
import java.util.List;

public interface GameMatchEntityCustomRepository {

    List<GameMatchEntity> findByMatchAt(LocalDate matchAt);

}
