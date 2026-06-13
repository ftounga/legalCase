package fr.ailegalcase.casefile.conclusion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * F-261 / SF-261-05 — accès aux moyens adverses persistés d'un dossier.
 *
 * <p>Lecture ordonnée ({@code ordre} croissant) pour restituer le set dans l'ordre
 * d'extraction figé ; {@link #existsByCaseFileId(UUID)} sert au court-circuit
 * « set déjà persisté → pas de ré-extraction LLM » ;
 * {@link #deleteByCaseFileId(UUID)} prépare le replace-set lors d'un {@code refresh}.</p>
 */
public interface AdverseMoyenRepository extends JpaRepository<AdverseMoyenEntity, UUID> {

    List<AdverseMoyenEntity> findByCaseFileIdOrderByOrdreAsc(UUID caseFileId);

    boolean existsByCaseFileId(UUID caseFileId);

    @Transactional
    void deleteByCaseFileId(UUID caseFileId);
}
