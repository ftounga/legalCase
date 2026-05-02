package fr.ailegalcase.backlog;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BacklogSyncRunRepository extends JpaRepository<BacklogSyncRunEntity, UUID> {

    List<BacklogSyncRunEntity> findAllByOrderByStartedAtDesc(Pageable pageable);

    Optional<BacklogSyncRunEntity> findFirstByOrderByStartedAtDesc();

    Optional<BacklogSyncRunEntity> findFirstBySuccessTrueOrderByStartedAtDesc();
}
