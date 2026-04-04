package fr.ailegalcase.referential;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReferentialReportRepository extends JpaRepository<ReferentialReport, UUID> {

    Optional<ReferentialReport> findByEntry_IdAndReporter_IdAndStatus(
            UUID entryId, UUID reporterId, ReferentialReport.Status status);
}
