package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.FileRefEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRefRepository extends JpaRepository<FileRefEntity, Long> {
}
