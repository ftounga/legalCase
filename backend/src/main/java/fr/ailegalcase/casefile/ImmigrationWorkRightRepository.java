package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImmigrationWorkRightRepository extends JpaRepository<ImmigrationWorkRight, UUID> {

    Optional<ImmigrationWorkRight> findByCaseFileId(UUID caseFileId);
}
