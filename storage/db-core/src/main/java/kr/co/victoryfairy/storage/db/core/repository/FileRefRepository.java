package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.FileRefEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRefRepository extends JpaRepository<FileRefEntity, Long> {
    Optional<FileRefEntity> findByFileEntityIdAndIsUseTrue(Long fileId);
}
