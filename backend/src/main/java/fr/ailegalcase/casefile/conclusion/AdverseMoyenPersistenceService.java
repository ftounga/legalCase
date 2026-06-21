package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * F-261 / SF-261-05 — persistance des moyens de la partie adverse.
 *
 * <p>Auparavant, {@code CaseConclusionService.prepare} ré-extrayait les moyens par
 * LLM ({@link AdverseMoyensExtractor}) à chaque génération (record éphémère, sans id).
 * Désormais ce service fige le set extrait dans la table {@code adverse_moyen} avec un
 * id <strong>stable</strong> par dossier, et la génération lit ce set persisté plutôt
 * que de ré-extraire — donnant un identifiant réutilisable par la curation des moyens
 * (F-288 vague 3) et garantissant que modal et génération lisent les MÊMES moyens.</p>
 *
 * <p><strong>Replace-set</strong> : lors d'une (ré-)extraction, on supprime puis
 * réinsère le set ordonné ({@code ordre} = 0..N). Les id ne changent donc que sur
 * {@link #refresh(UUID, String, String)} explicite → les exclusions F-288-03 restent
 * stables entre deux générations.</p>
 *
 * <p><strong>Fail-open total</strong> (invariant « silence &gt; erreur », cohérent
 * F-179 / SF-98-56) : toute exception de lecture, d'extraction ou de persistance
 * aboutit à une liste vide journalisée, jamais à une exception propagée — la
 * génération se poursuit sans section « moyens adverses ». Aucun document adverse /
 * aucun moyen ⇒ liste vide, sans appel LLM inutile et sans ligne persistée.</p>
 */
@Service
public class AdverseMoyenPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(AdverseMoyenPersistenceService.class);

    private static final TypeReference<List<String>> STRING_LIST =
            new TypeReference<List<String>>() {};

    private final AdverseMoyenRepository adverseMoyenRepository;
    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final AdverseMoyensExtractor adverseMoyensExtractor;
    private final ObjectMapper objectMapper;

    public AdverseMoyenPersistenceService(AdverseMoyenRepository adverseMoyenRepository,
                                          DocumentRepository documentRepository,
                                          DocumentExtractionRepository documentExtractionRepository,
                                          AdverseMoyensExtractor adverseMoyensExtractor,
                                          ObjectMapper objectMapper) {
        this.adverseMoyenRepository = adverseMoyenRepository;
        this.documentRepository = documentRepository;
        this.documentExtractionRepository = documentExtractionRepository;
        this.adverseMoyensExtractor = adverseMoyensExtractor;
        this.objectMapper = objectMapper;
    }

    /**
     * Retourne les moyens adverses persistés du dossier, en les extrayant + persistant
     * à la première fois.
     *
     * <ul>
     *   <li>Set déjà persisté pour le dossier → le retourne tel quel (ordre figé),
     *       <strong>sans appel LLM</strong> ;</li>
     *   <li>sinon, lit les écritures adverses ({@code adverse_pleadings}), délègue
     *       l'extraction LLM à {@link AdverseMoyensExtractor}, puis persiste le set
     *       (replace-set) et le retourne avec ses id stables ;</li>
     *   <li>aucun document adverse / aucun moyen extrait → liste vide, rien persisté,
     *       pas d'appel inutile.</li>
     * </ul>
     *
     * <p><strong>Fail-open</strong> : toute exception ⇒ liste vide journalisée.</p>
     *
     * @param caseFileId dossier
     * @param domain     domaine juridique (clé {@code DROIT_DU_TRAVAIL} / …)
     * @param country    pays du workspace (clé {@code FRANCE} / …)
     * @return les moyens persistés (jamais {@code null} ; vide si aucun / en cas d'échec)
     */
    @Transactional
    public List<AdverseMoyenWithId> loadOrExtract(UUID caseFileId, String domain, String country) {
        try {
            if (adverseMoyenRepository.existsByCaseFileId(caseFileId)) {
                // SF-261-06 — un acte adverse plus récent que le set figé (round adverse
                // ajouté depuis la 1ʳᵉ extraction) → ré-extraire (replace-set) pour que la
                // réplique réfute les VRAIS nouveaux moyens. Sinon set conservé (curation
                // F-288 préservée, aucun appel LLM, aucun non-déterminisme).
                if (hasAdverseDocumentNewerThanPersistedSet(caseFileId)) {
                    return extractAndPersist(caseFileId, domain, country);
                }
                return findPersisted(caseFileId);
            }
            return extractAndPersist(caseFileId, domain, country);
        } catch (RuntimeException ex) {
            log.warn("F-261 — chargement des moyens adverses indisponible pour caseFile {} — "
                    + "génération sans section : {}", caseFileId, ex.getMessage());
            return List.of();
        }
    }

    /**
     * SF-261-06 — un document marqué « écritures adverses » a-t-il une {@code createdAt}
     * postérieure au set de moyens persisté ? (= acte adverse ajouté depuis l'extraction
     * figée). Limite assumée : un document marqué adverse longtemps APRÈS son upload n'est
     * pas détecté ({@code Document} n'a pas de timestamp de modification) — cas rare.
     */
    private boolean hasAdverseDocumentNewerThanPersistedSet(UUID caseFileId) {
        Instant extractedAt = adverseMoyenRepository.findByCaseFileIdOrderByOrdreAsc(caseFileId).stream()
                .map(AdverseMoyenEntity::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        if (extractedAt == null) {
            return false;
        }
        for (Document d : documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId)) {
            if (d.isAdversePleadings() && d.getCreatedAt() != null && d.getCreatedAt().isAfter(extractedAt)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Force la ré-extraction + le remplacement du set persisté (replace-set), quel que
     * soit l'état actuel. Usage futur (déclencheur explicite à l'ajout d'une écriture
     * adverse — limite MVP : pas d'auto-refresh). <strong>Fail-open</strong>.
     *
     * @return le set ré-extrait avec ses nouveaux id stables (vide si aucun / en cas d'échec)
     */
    @Transactional
    public List<AdverseMoyenWithId> refresh(UUID caseFileId, String domain, String country) {
        try {
            return extractAndPersist(caseFileId, domain, country);
        } catch (RuntimeException ex) {
            log.warn("F-261 — ré-extraction des moyens adverses indisponible pour caseFile {} — "
                    + "set inchangé : {}", caseFileId, ex.getMessage());
            return List.of();
        }
    }

    /**
     * Lit le set de moyens persistés du dossier (lecture seule, <strong>sans</strong>
     * extraction ni persistance). Destiné à la curation des moyens (F-288 vague 3), qui
     * référence chaque moyen par son id.
     *
     * @return les moyens persistés ordonnés (jamais {@code null} ; vide si aucun)
     */
    public List<AdverseMoyenWithId> findPersisted(UUID caseFileId) {
        List<AdverseMoyenWithId> moyens = new ArrayList<>();
        for (AdverseMoyenEntity entity : adverseMoyenRepository.findByCaseFileIdOrderByOrdreAsc(caseFileId)) {
            moyens.add(toWithId(entity));
        }
        return List.copyOf(moyens);
    }

    /**
     * Reproduit la lecture des écritures adverses → extraction LLM, puis persiste le set
     * (replace : suppression de l'ancien set + insertion ordonnée). Aucun moyen extrait ⇒
     * aucune ligne persistée, liste vide. Exécuté dans la transaction ouverte par
     * {@link #loadOrExtract} / {@link #refresh} (replace-set atomique).
     */
    private List<AdverseMoyenWithId> extractAndPersist(UUID caseFileId, String domain, String country) {
        List<String> textesAdverses = loadAdverseTexts(caseFileId);
        if (textesAdverses.isEmpty()) {
            // Aucun document adverse exploitable → on remplace par un set vide pour
            // rester cohérent (un refresh après suppression des écritures vide le set).
            adverseMoyenRepository.deleteByCaseFileId(caseFileId);
            return List.of();
        }
        List<AdverseMoyen> extracted =
                adverseMoyensExtractor.extract(textesAdverses, domain, country, caseFileId);

        adverseMoyenRepository.deleteByCaseFileId(caseFileId);
        if (extracted.isEmpty()) {
            return List.of();
        }
        List<AdverseMoyenWithId> persisted = new ArrayList<>(extracted.size());
        int ordre = 0;
        for (AdverseMoyen moyen : extracted) {
            AdverseMoyenEntity entity = new AdverseMoyenEntity();
            entity.setCaseFileId(caseFileId);
            entity.setThese(moyen.these());
            entity.setFondements(serialize(moyen.fondements()));
            entity.setPiecesInvoquees(serialize(moyen.piecesInvoquees()));
            entity.setOrdre(ordre++);
            persisted.add(toWithId(adverseMoyenRepository.save(entity)));
        }
        return List.copyOf(persisted);
    }

    /**
     * Charge le texte extrait de chaque document marqué « écritures adverses »
     * ({@code adverse_pleadings}) du dossier. Aucun document adverse → liste vide
     * (l'appelant n'appellera pas l'extracteur).
     */
    private List<String> loadAdverseTexts(UUID caseFileId) {
        List<String> textesAdverses = new ArrayList<>();
        for (Document document : documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId)) {
            if (!document.isAdversePleadings()) {
                continue;
            }
            documentExtractionRepository.findByDocumentId(document.getId())
                    .map(DocumentExtraction::getExtractedText)
                    .filter(text -> text != null && !text.isBlank())
                    .ifPresent(textesAdverses::add);
        }
        return textesAdverses;
    }

    private AdverseMoyenWithId toWithId(AdverseMoyenEntity entity) {
        return new AdverseMoyenWithId(
                entity.getId(),
                new AdverseMoyen(
                        entity.getThese(),
                        deserialize(entity.getFondements()),
                        deserialize(entity.getPiecesInvoquees())));
    }

    /** Sérialise une liste de chaînes en tableau JSON ({@code []} si null/vide ou échec). */
    private String serialize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            log.warn("F-261 — sérialisation JSON d'un moyen adverse échouée : {}", e.getMessage());
            return "[]";
        }
    }

    /** Désérialise un tableau JSON en liste de chaînes (vide si null/blank ou illisible). */
    private List<String> deserialize(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST);
            return values == null ? List.of() : List.copyOf(values);
        } catch (Exception e) {
            log.warn("F-261 — désérialisation JSON d'un moyen adverse échouée : {}", e.getMessage());
            return List.of();
        }
    }
}
