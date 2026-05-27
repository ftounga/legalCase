package fr.ailegalcase.jurisprudencemapping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-10 — repository des jobs de bootstrap super-admin.
 */
public interface JurisprudenceBootstrapJobRepository extends JpaRepository<JurisprudenceBootstrapJob, UUID> {
}
