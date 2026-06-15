package fr.ailegalcase.referential;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.*;
import fr.ailegalcase.casefile.ImmigrationProcedureReferentiel.ProcedureJalon;
import fr.ailegalcase.casefile.LitigationTypeMapper.LitigationPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fournit les données référentiels métier depuis la base de données.
 * Chaque méthode applique un fallback sur les constantes Java hardcodées
 * si la DB ne retourne aucun résultat (fail-open).
 */
@Service
@Transactional(readOnly = true)
public class LegalReferentialService {

    private static final Logger log = LoggerFactory.getLogger(LegalReferentialService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LegalReferentialRepository repository;

    public LegalReferentialService(LegalReferentialRepository repository) {
        this.repository = repository;
    }

    // -----------------------------------------------------------------------
    // PUT /api/v1/referentials/{id} — modification OWNER/ADMIN
    // -----------------------------------------------------------------------

    @Transactional
    public ReferentialUpdateResponse updateReferential(UUID entryId, UUID workspaceId, UUID userId,
                                                       String newLabel, String newValueJson,
                                                       boolean force,
                                                       ReferentialValidationService validationService) {
        return updateReferential(entryId, workspaceId, userId, newLabel, newValueJson, force, null, validationService);
    }

    /**
     * SF-140-03 : surcharge avec description. Rétrocompat préservée via la méthode
     * ci-dessus qui passe null.
     */
    @Transactional
    public ReferentialUpdateResponse updateReferential(UUID entryId, UUID workspaceId, UUID userId,
                                                       String newLabel, String newValueJson,
                                                       boolean force, String newDescription,
                                                       ReferentialValidationService validationService) {
        LegalReferential source = repository.findById(entryId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Référentiel introuvable"));

        // Workspace isolation: if workspace entry, must belong to caller's workspace
        if (source.getWorkspaceId() != null && !workspaceId.equals(source.getWorkspaceId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Accès refusé");
        }

        // Validate JSON
        try {
            MAPPER.readTree(newValueJson);
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "valueJson invalide : JSON mal formé");
        }

        // IA validation (skip if force=true)
        if (!force) {
            var validation = validationService.validate(
                    source.getLegalDomain(), source.getReferentialType(),
                    source.getEntryKey(), source.getLabel(),
                    source.getValueJson(), newValueJson);
            if (!validation.valid()) {
                return new ReferentialUpdateResponse(false, null, validation.warning());
            }
        }

        // Upsert: system entry → create/update workspace override; workspace entry → update in-place
        LegalReferential target;
        if (source.getWorkspaceId() == null) {
            List<LegalReferential> overrides = repository.findWorkspaceEntry(
                    workspaceId, source.getLegalDomain(), source.getReferentialType(),
                    source.getEntryKey(), source.getCountry());
            if (!overrides.isEmpty()) {
                target = overrides.get(0);
            } else {
                target = new LegalReferential();
                target.setWorkspaceId(workspaceId);
                target.setLegalDomain(source.getLegalDomain());
                target.setReferentialType(source.getReferentialType());
                target.setEntryKey(source.getEntryKey());
                target.setCountry(source.getCountry());
                target.setSystem(false);
                target.setActive(true);
                target.setSourceRef(source.getSourceRef());
            }
        } else {
            target = source;
        }

        // System entries: label is locked — always use source label
        target.setLabel(source.getWorkspaceId() == null ? source.getLabel() : newLabel);
        target.setValueJson(newValueJson);
        // SF-140-03 : description optionnelle — seulement mise à jour si fournie.
        if (newDescription != null && !newDescription.isBlank()) {
            target.setDescription(newDescription);
        }
        target.setUpdatedAt(Instant.now());
        target.setUpdatedBy(userId);
        LegalReferential saved = repository.save(target);

        return new ReferentialUpdateResponse(true,
                new ReferentialResponse.Entry(saved.getId(), saved.getEntryKey(), saved.getLabel(),
                        saved.getCountry(), saved.getValueJson(), saved.isSystem(), saved.getSourceRef(),
                        saved.getDescription()),
                null);
    }

    // -----------------------------------------------------------------------
    // DROIT_DU_TRAVAIL — délai de prescription par type de litige
    // -----------------------------------------------------------------------

    public Optional<LitigationPeriod> getLitigationPeriod(String type) {
        return getLitigationPeriod(type, null);
    }

    public Optional<LitigationPeriod> getLitigationPeriod(String type, String country) {
        if (type == null) return Optional.empty();
        try {
            List<LegalReferential> entries;
            if (country != null) {
                entries = repository.findSystemEntryByCountry(
                        "DROIT_DU_TRAVAIL", "LITIGATION_TYPE", type.trim().toUpperCase(), country);
            } else {
                entries = repository.findSystemEntry(
                        "DROIT_DU_TRAVAIL", "LITIGATION_TYPE", type.trim().toUpperCase());
            }
            if (!entries.isEmpty()) {
                JsonNode node = MAPPER.readTree(entries.get(0).getValueJson());
                int years = node.get("years").asInt();
                String article = node.get("article").asText();
                String label = entries.get(0).getLabel() != null ? entries.get(0).getLabel() : type;
                return Optional.of(new LitigationPeriod(years, article, label));
            }
        } catch (Exception e) {
            log.warn("LegalReferentialService: fallback getLitigationPeriod({}, {}) — {}", type, country, e.getMessage());
        }
        return LitigationTypeMapper.resolve(type);
    }

    /**
     * Returns the litigation type keys available for a given country.
     * Used to dynamically build the IA prompt with the right types.
     */
    public List<String> getLitigationTypeKeys(String country) {
        try {
            return repository.findSystemEntriesByTypeAndCountry(
                    "DROIT_DU_TRAVAIL", "LITIGATION_TYPE", country)
                    .stream()
                    .map(LegalReferential::getEntryKey)
                    .toList();
        } catch (Exception e) {
            log.warn("LegalReferentialService: getLitigationTypeKeys({}) — {}", country, e.getMessage());
            return List.of();
        }
    }

    // -----------------------------------------------------------------------
    // DROIT_IMMIGRATION — jalons procéduraux par type de procédure
    // -----------------------------------------------------------------------

    public List<ProcedureJalon> getImmigrationJalons(String typeProcedure) {
        if (typeProcedure == null) return List.of();
        try {
            List<LegalReferential> entries = repository.findSystemEntry(
                    "DROIT_IMMIGRATION", "IMMIGRATION_JALONS", typeProcedure);
            if (!entries.isEmpty()) {
                JsonNode array = MAPPER.readTree(entries.get(0).getValueJson());
                List<ProcedureJalon> jalons = new ArrayList<>();
                for (JsonNode item : array) {
                    jalons.add(new ProcedureJalon(
                            item.get("label").asText(),
                            item.get("offsetDays").asInt()));
                }
                return Collections.unmodifiableList(jalons);
            }
        } catch (Exception e) {
            log.warn("LegalReferentialService: fallback getImmigrationJalons({}) — {}", typeProcedure, e.getMessage());
        }
        return ImmigrationProcedureReferentiel.resolve(typeProcedure);
    }

    // -----------------------------------------------------------------------
    // DROIT_DU_TRAVAIL — jalons procéduraux par type de procédure et pays (F-136 SF-136-01)
    // -----------------------------------------------------------------------

    /**
     * Résout les jalons procéduraux pour le droit du travail (prud'hommes / appel / cassation FR
     * + tribunal du travail / cour du travail / cassation BE). DB-first sur le type
     * {@code TRAVAIL_PROCEDURE_JALONS}, fallback statique
     * {@link TravailProcedureReferentiel#resolve(String, String)}.
     *
     * @param typeProcedure code (ex. {@code PRUDHOMMES_FR}, {@code TRIBUNAL_TRAVAIL_BE})
     * @param country pays {@code FR} ou {@code BE} — obligatoire (chaque procédure est country-scoped)
     * @return liste immuable de jalons (label + offsetDays + articleRef) ; vide si introuvable
     */
    public List<TravailProcedureReferentiel.ProcedureJalon> getTravailProcedureJalons(String typeProcedure, String country) {
        if (typeProcedure == null || country == null) return List.of();
        try {
            List<LegalReferential> entries = repository.findSystemEntryByCountry(
                    "DROIT_DU_TRAVAIL", "TRAVAIL_PROCEDURE_JALONS", typeProcedure, country);
            if (!entries.isEmpty()) {
                JsonNode array = MAPPER.readTree(entries.get(0).getValueJson());
                List<TravailProcedureReferentiel.ProcedureJalon> jalons = new ArrayList<>();
                for (JsonNode item : array) {
                    jalons.add(new TravailProcedureReferentiel.ProcedureJalon(
                            item.get("label").asText(),
                            item.get("offsetDays").asInt(),
                            item.has("articleRef") ? item.get("articleRef").asText() : ""));
                }
                return Collections.unmodifiableList(jalons);
            }
        } catch (Exception e) {
            log.warn("LegalReferentialService: fallback getTravailProcedureJalons({},{}) — {}",
                    typeProcedure, country, e.getMessage());
        }
        return TravailProcedureReferentiel.resolve(typeProcedure, country);
    }

    // -----------------------------------------------------------------------
    // DROIT_FAMILLE — jalons procéduraux par type de procédure et pays (F-137 SF-F-137-01)
    // -----------------------------------------------------------------------

    /**
     * Résout les jalons procéduraux pour le droit de la famille (tribunal de la famille BE,
     * cour d'appel famille BE, cassation BE — périmètre actuel ; FR à venir en SF-F-137-02).
     * DB-first sur le type {@code FAMILLE_PROCEDURE_JALONS}, fallback statique
     * {@link FamilleProcedureReferentiel#resolve(String, String)}.
     *
     * @param typeProcedure code (ex. {@code TRIBUNAL_FAMILLE_BE})
     * @param country pays {@code BE} (FR à venir) — obligatoire (chaque procédure est country-scoped)
     * @return liste immuable de jalons (label + offsetDays + articleRef) ; vide si introuvable
     */
    public List<FamilleProcedureReferentiel.ProcedureJalon> getFamilleProcedureJalons(String typeProcedure, String country) {
        if (typeProcedure == null || country == null) return List.of();
        try {
            List<LegalReferential> entries = repository.findSystemEntryByCountry(
                    "DROIT_FAMILLE", "FAMILLE_PROCEDURE_JALONS", typeProcedure, country);
            if (!entries.isEmpty()) {
                JsonNode array = MAPPER.readTree(entries.get(0).getValueJson());
                List<FamilleProcedureReferentiel.ProcedureJalon> jalons = new ArrayList<>();
                for (JsonNode item : array) {
                    jalons.add(new FamilleProcedureReferentiel.ProcedureJalon(
                            item.get("label").asText(),
                            item.get("offsetDays").asInt(),
                            item.has("articleRef") ? item.get("articleRef").asText() : ""));
                }
                return Collections.unmodifiableList(jalons);
            }
        } catch (Exception e) {
            log.warn("LegalReferentialService: fallback getFamilleProcedureJalons({},{}) — {}",
                    typeProcedure, country, e.getMessage());
        }
        return FamilleProcedureReferentiel.resolve(typeProcedure, country);
    }

    // -----------------------------------------------------------------------
    // DROIT_IMMIGRATION — pièces requises par type de titre et pays
    // -----------------------------------------------------------------------

    public List<String> getImmigrationPieces(String titreType, String country) {
        if (titreType == null || country == null) return List.of();
        try {
            List<LegalReferential> entries = repository.findActiveByDomainAndType(
                    "DROIT_IMMIGRATION", "IMMIGRATION_PIECES", null);
            Optional<LegalReferential> match = entries.stream()
                    .filter(e -> titreType.equalsIgnoreCase(e.getEntryKey())
                            && country.equalsIgnoreCase(e.getCountry())
                            && e.getWorkspaceId() == null)
                    .findFirst();
            if (match.isPresent()) {
                return MAPPER.readValue(match.get().getValueJson(),
                        new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.warn("LegalReferentialService: fallback getImmigrationPieces({},{}) — {}", titreType, country, e.getMessage());
        }
        return ImmigrationPieceReferentiel.getPieces(titreType, country);
    }

    // -----------------------------------------------------------------------
    // Endpoint GET /api/v1/referentials — données groupées par type
    // -----------------------------------------------------------------------

    public ReferentialResponse getReferentials(String domain, UUID workspaceId) {
        return getReferentials(domain, workspaceId, null);
    }

    /**
     * SF-137-01 : filtrage pays workspace.
     *
     * <p>Quand {@code workspaceCountry} est fourni (FR ou BE), les entries
     * ciblées sur l'autre pays sont masquées. Les entries neutres
     * ({@code country == null}) restent visibles — elles sont communes
     * (ex. BAREME_MACRON, LITIGATION_TYPE).
     *
     * <p>Quand {@code workspaceCountry} est null, aucun filtrage (retour
     * intégral, utile pour l'API super-admin).
     */
    public ReferentialResponse getReferentials(String domain, UUID workspaceId, String workspaceCountry) {
        UUID safeWorkspaceId = workspaceId != null ? workspaceId : UUID.fromString("00000000-0000-0000-0000-000000000000");
        List<LegalReferential> all = repository.findActiveByDomain(domain, safeWorkspaceId);

        // Deduplicate: workspace override wins over system entry for same (type, key, country).
        Map<String, LegalReferential> deduped = new LinkedHashMap<>();
        all.stream().filter(LegalReferential::isSystem)
                .forEach(e -> deduped.put(dedupeKey(e), e));
        all.stream().filter(e -> !e.isSystem())
                .forEach(e -> deduped.put(dedupeKey(e), e));

        Map<String, List<ReferentialResponse.Entry>> sections = deduped.values().stream()
                .filter(e -> isEntryVisibleForCountry(e, workspaceCountry))
                .sorted(Comparator.comparing(LegalReferential::getReferentialType)
                        .thenComparing(LegalReferential::getEntryKey))
                .collect(Collectors.groupingBy(
                        LegalReferential::getReferentialType,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                e -> new ReferentialResponse.Entry(
                                        e.getId(),
                                        e.getEntryKey(),
                                        e.getLabel(),
                                        e.getCountry(),
                                        e.getValueJson(),
                                        e.isSystem(),
                                        e.getSourceRef(),
                                        e.getDescription()),
                                Collectors.toList())));

        return new ReferentialResponse(domain, sections);
    }

    /**
     * SF-137-01 : une entry est visible si
     * - elle est globale (country == null), ou
     * - le workspaceCountry n'est pas fourni (pas de filtrage), ou
     * - elle cible exactement le workspaceCountry.
     */
    private static boolean isEntryVisibleForCountry(LegalReferential e, String workspaceCountry) {
        if (workspaceCountry == null || workspaceCountry.isBlank()) return true;
        if (e.getCountry() == null || e.getCountry().isBlank()) return true;
        return workspaceCountry.equalsIgnoreCase(e.getCountry());
    }

    private static String dedupeKey(LegalReferential e) {
        return e.getReferentialType() + "|" + e.getEntryKey() + "|" + (e.getCountry() != null ? e.getCountry() : "");
    }

    // ── Nouveaux référentiels métier ────────────────────────────────

    /** Titres de séjour immigration FR/BE. DB first → fallback ImmigrationTitleReferentiel. */
    public TitleRecommendation getImmigrationTitle(String titreType, String country) {
        try {
            List<LegalReferential> entries = repository.findSystemEntryByCountry("DROIT_IMMIGRATION", "IMMIGRATION_TITLES", titreType, country);
            if (!entries.isEmpty()) {
                JsonNode node = MAPPER.readTree(entries.get(0).getValueJson());
                return new TitleRecommendation(
                        node.path("code").asText(), entries.get(0).getLabel(), country,
                        node.path("motif").asText(), node.path("conditions").asText(),
                        MAPPER.readValue(node.path("pieces").toString(), new TypeReference<List<String>>() {}),
                        node.path("delaiMoyenJours").asInt());
            }
        } catch (Exception e) { log.warn("Fallback statique pour IMMIGRATION_TITLES/{}/{}", titreType, country, e); }
        return ImmigrationTitleReferentiel.getByCode(titreType);
    }

    /** Types de recours immigration FR/BE. DB first → fallback ImmigrationRecoursReferentiel. */
    public RecoursType getRecoursType(String code) {
        try {
            List<LegalReferential> entries = repository.findSystemEntry("DROIT_IMMIGRATION", "IMMIGRATION_RECOURS", code);
            if (!entries.isEmpty()) {
                LegalReferential e = entries.get(0);
                JsonNode node = MAPPER.readTree(e.getValueJson());
                return new RecoursType(code, e.getLabel(), e.getCountry(),
                        node.path("delaiJours").asInt(), node.path("juridiction").asText(),
                        MAPPER.readValue(node.path("textesApplicables").toString(), new TypeReference<>() {}),
                        List.of("EN_TETE", "OBJET", "VISA_TEXTES", "EXPOSE_FAITS", "MOYENS_DROIT", "CONCLUSIONS", "PIECES"),
                        MAPPER.readValue(node.path("piecesStandard").toString(), new TypeReference<>() {}));
            }
        } catch (Exception e) { log.warn("Fallback statique pour IMMIGRATION_RECOURS/{}", code, e); }
        return ImmigrationRecoursReferentiel.getByCode(code);
    }

    /** Droit au travail par titre. DB first → fallback ImmigrationWorkRightReferentiel. */
    public WorkRightResult getWorkRight(String titreType, String country) {
        try {
            List<LegalReferential> entries = repository.findSystemEntryByCountry("DROIT_IMMIGRATION", "IMMIGRATION_WORK_RIGHTS", titreType, country);
            if (!entries.isEmpty()) {
                LegalReferential e = entries.get(0);
                JsonNode node = MAPPER.readTree(e.getValueJson());
                return new WorkRightResult(titreType, e.getLabel(), country,
                        node.path("droitTravail").asText(), node.path("conditions").asText(),
                        MAPPER.readValue(node.path("obligationsEmployeur").toString(), new TypeReference<>() {}),
                        e.getSourceRef());
            }
        } catch (Exception ex) { log.warn("Fallback statique pour IMMIGRATION_WORK_RIGHTS/{}/{}", titreType, country, ex); }
        return ImmigrationWorkRightReferentiel.getByTitreType(titreType, country);
    }

    /**
     * SF-129-03 : Barème convention collective, DB uniquement (plus de fallback statique).
     * Normalise automatiquement les codes legacy (METALLURGIE → IDCC_3248 etc.) via
     * ConventionCodeNormalizer pour les dossiers anciens qui ont stocké ces codes.
     * Retourne null si code inconnu.
     */
    public ConventionBareme getConventionBareme(String code) {
        String normalized = fr.ailegalcase.casefile.ConventionCodeNormalizer.normalize(code);
        if (normalized == null) return null;
        try {
            List<LegalReferential> entries = repository.findSystemEntry("DROIT_DU_TRAVAIL", "CONVENTION_BAREMES", normalized);
            if (entries.isEmpty()) return null;
            LegalReferential e = entries.get(0);
            JsonNode node = MAPPER.readTree(e.getValueJson());
            int congesLegaux = node.path("congesLegauxJours").asInt();
            List<ConventionBareme.CongesSupplementaire> congesSupp = new ArrayList<>();
            for (JsonNode cs : node.path("congesSupp")) {
                congesSupp.add(new ConventionBareme.CongesSupplementaire(cs.path("min").asInt(), cs.path("jours").asInt(), ""));
            }
            List<ConventionBareme.PrimeAnciennete> primes = new ArrayList<>();
            for (JsonNode p : node.path("primes")) {
                primes.add(new ConventionBareme.PrimeAnciennete(p.path("min").asInt(), p.path("pct").decimalValue(), ""));
            }
            return new ConventionBareme(normalized, e.getLabel(), e.getCountry(), congesLegaux, congesSupp, primes, e.getSourceRef());
        } catch (Exception ex) {
            log.warn("Erreur lecture référentiel CONVENTION_BAREMES/{}: {}", normalized, ex.getMessage());
            return null;
        }
    }

    /**
     * SF-DT-25-01 : durée de préavis prévue par une convention collective française pour
     * la combinaison ({@code fonction}, {@code ancienneteMois}). DB only — retourne null
     * si la CCN est inconnue, n'a pas d'entrée pour la fonction demandée, ou n'a pas de
     * tranche couvrant l'ancienneté donnée.
     *
     * <p>Format attendu de {@code value_json} :</p>
     * <pre>{
     *   "fonctions": {
     *     "OUVRIER": [{"min": 0, "max": 6, "mois": 0}, {"min": 6, "max": 24, "mois": 1}, {"min": 24, "max": null, "mois": 2}],
     *     "EMPLOYE": [...],
     *     "AGENT_MAITRISE": [...],
     *     "CADRE": [...]
     *   },
     *   "article": "CCN art. 27"
     * }</pre>
     *
     * @param code           code CCN (sera normalisé via {@link fr.ailegalcase.casefile.ConventionCodeNormalizer})
     * @param fonction       catégorie professionnelle
     * @param ancienneteMois ancienneté en mois (≥ 0)
     * @return durée + référence article ; ou {@code null} si CCN/fonction/tranche inconnue
     */
    public ConventionPreavis getConventionPreavis(String code,
                                                  fr.ailegalcase.casefile.IndemnitePreavisFonction fonction,
                                                  int ancienneteMois) {
        if (code == null || fonction == null || ancienneteMois < 0) return null;
        String normalized = fr.ailegalcase.casefile.ConventionCodeNormalizer.normalize(code);
        if (normalized == null) return null;
        try {
            List<LegalReferential> entries = repository.findSystemEntry("DROIT_DU_TRAVAIL", "CONVENTION_PREAVIS", normalized);
            if (entries.isEmpty()) return null;
            LegalReferential e = entries.get(0);
            JsonNode root = MAPPER.readTree(e.getValueJson());
            JsonNode fonctions = root.path("fonctions");
            if (fonctions.isMissingNode() || !fonctions.isObject()) return null;
            JsonNode tranches = fonctions.path(fonction.name());
            if (tranches.isMissingNode() || !tranches.isArray() || tranches.isEmpty()) return null;
            for (JsonNode t : tranches) {
                int min = t.path("min").asInt(0);
                JsonNode maxNode = t.path("max");
                Integer max = (maxNode.isMissingNode() || maxNode.isNull()) ? null : maxNode.asInt();
                if (ancienneteMois >= min && (max == null || ancienneteMois < max)) {
                    int mois = t.path("mois").asInt(0);
                    String article = root.path("article").asText("");
                    return new ConventionPreavis(normalized, fonction, mois, article);
                }
            }
            return null;
        } catch (Exception ex) {
            log.warn("Erreur lecture référentiel CONVENTION_PREAVIS/{}: {}", normalized, ex.getMessage());
            return null;
        }
    }

    /**
     * SF-DT-25-01 : résolution d'une durée de préavis CCN (FR).
     */
    public record ConventionPreavis(String code,
                                    fr.ailegalcase.casefile.IndemnitePreavisFonction fonction,
                                    int dureeMois,
                                    String article) {}

    /**
     * SF-139-01 : critère de validité licenciement. DB only (plus de fallback Java
     * depuis la suppression de {@code LicenciementCritereReferentiel}).
     */
    public LicenciementCritere getLicenciementCritere(String code) {
        try {
            List<LegalReferential> entries = repository.findSystemEntry("DROIT_DU_TRAVAIL", "LICENCIEMENT_CRITERES", code);
            if (entries.isEmpty()) return null;
            LegalReferential e = entries.get(0);
            JsonNode node = MAPPER.readTree(e.getValueJson());
            return new LicenciementCritere(code, e.getLabel(), e.getCountry(),
                    node.path("description").asText(), node.path("poids").asInt(),
                    node.path("bloquant").asBoolean(), e.getSourceRef());
        } catch (Exception ex) {
            log.warn("Erreur lecture LICENCIEMENT_CRITERES/{}: {}", code, ex.getMessage());
            return null;
        }
    }

    /** SF-139-01 : liste des critères licenciement par pays. DB only. */
    public List<LicenciementCritere> getLicenciementCriteres(String country) {
        try {
            List<LegalReferential> entries = repository.findSystemEntriesByTypeAndCountry("DROIT_DU_TRAVAIL", "LICENCIEMENT_CRITERES", country);
            List<LicenciementCritere> result = new ArrayList<>();
            for (LegalReferential e : entries) {
                JsonNode node = MAPPER.readTree(e.getValueJson());
                result.add(new LicenciementCritere(e.getEntryKey(), e.getLabel(), country,
                        node.path("description").asText(), node.path("poids").asInt(),
                        node.path("bloquant").asBoolean(), e.getSourceRef()));
            }
            return result;
        } catch (Exception ex) {
            log.warn("Erreur lecture LICENCIEMENT_CRITERES country={}: {}", country, ex.getMessage());
            return List.of();
        }
    }

    /** SF-139-01 : critères de validité rupture conventionnelle par pays. DB only. */
    public List<RuptureConvCritere> getRuptureConvCriteres(String country) {
        try {
            List<LegalReferential> entries = repository.findSystemEntriesByTypeAndCountry("DROIT_DU_TRAVAIL", "RUPTURE_CONV_CRITERES", country);
            List<RuptureConvCritere> result = new ArrayList<>();
            for (LegalReferential e : entries) {
                JsonNode node = MAPPER.readTree(e.getValueJson());
                result.add(new RuptureConvCritere(e.getEntryKey(), e.getLabel(), country,
                        node.path("description").asText(), node.path("poids").asInt(),
                        node.path("bloquant").asBoolean(), e.getSourceRef()));
            }
            return result;
        } catch (Exception ex) {
            log.warn("Erreur lecture RUPTURE_CONV_CRITERES country={}: {}", country, ex.getMessage());
            return List.of();
        }
    }

    /**
     * SF-139-01 : barème Macron depuis DB (INDEMNITE_BAREMES/MACRON).
     * Retourne la ligne correspondant à l'ancienneté (plafonnée à 29 ans).
     */
    public IndemniteBareme getBaremeMacron(int ancienneteAnnees) {
        int capped = Math.min(Math.max(ancienneteAnnees, 0), 29);
        try {
            List<LegalReferential> entries = repository.findSystemEntry("DROIT_DU_TRAVAIL", "INDEMNITE_BAREMES", "MACRON");
            if (entries.isEmpty()) return null;
            JsonNode root = MAPPER.readTree(entries.get(0).getValueJson());
            for (JsonNode e : root.path("entries")) {
                if (e.path("an").asInt() == capped) {
                    return new IndemniteBareme(
                            capped,
                            e.path("min").decimalValue(),
                            e.path("max").decimalValue());
                }
            }
            return null;
        } catch (Exception ex) {
            log.warn("Erreur lecture INDEMNITE_BAREMES/MACRON ancienneté={}: {}", capped, ex.getMessage());
            return null;
        }
    }

    /**
     * SF-139-01 : CCT 109 (Belgique) — min/max en semaines, depuis DB
     * (INDEMNITE_BAREMES/CCT109).
     */
    public Cct109Range getCct109Range() {
        try {
            List<LegalReferential> entries = repository.findSystemEntry("DROIT_DU_TRAVAIL", "INDEMNITE_BAREMES", "CCT109");
            if (entries.isEmpty()) return null;
            JsonNode node = MAPPER.readTree(entries.get(0).getValueJson());
            return new Cct109Range(
                    node.path("minSemaines").decimalValue(),
                    node.path("maxSemaines").decimalValue());
        } catch (Exception ex) {
            log.warn("Erreur lecture INDEMNITE_BAREMES/CCT109: {}", ex.getMessage());
            return null;
        }
    }

    public record Cct109Range(java.math.BigDecimal minSemaines, java.math.BigDecimal maxSemaines) {}

    /** Mode de garde famille. DB first → fallback GardeModeReferentiel. */
    public GardeMode getGardeMode(String code) {
        try {
            List<LegalReferential> entries = repository.findSystemEntry("DROIT_FAMILLE", "GARDE_MODES", code);
            if (!entries.isEmpty()) {
                LegalReferential e = entries.get(0);
                JsonNode node = MAPPER.readTree(e.getValueJson());
                return new GardeMode(code, e.getLabel(), e.getCountry(), "",
                        node.path("repartitionType").asText(),
                        MAPPER.readValue(node.path("periodesA").toString(), new TypeReference<>() {}),
                        MAPPER.readValue(node.path("periodesB").toString(), new TypeReference<>() {}),
                        node.path("vacances").asText(), e.getSourceRef());
            }
        } catch (Exception ex) { log.warn("Fallback statique pour GARDE_MODES/{}", code, ex); }
        return GardeModeReferentiel.getByCode(code);
    }

    /** Étapes divorce par pays. DB first → fallback DivorceChecklistReferentiel. */
    public List<DivorceEtape> getDivorceEtapes(String country) {
        try {
            List<LegalReferential> entries = repository.findSystemEntriesByTypeAndCountry("DROIT_FAMILLE", "DIVORCE_ETAPES", country);
            if (!entries.isEmpty()) {
                List<DivorceEtape> result = new ArrayList<>();
                for (LegalReferential e : entries) {
                    JsonNode node = MAPPER.readTree(e.getValueJson());
                    result.add(new DivorceEtape(e.getEntryKey(), e.getLabel(), country,
                            node.path("ordre").asInt(), node.path("description").asText(),
                            node.path("delai").asText(), node.path("obligatoire").asBoolean()));
                }
                result.sort(Comparator.comparingInt(DivorceEtape::ordre));
                return result;
            }
        } catch (Exception ex) { log.warn("Fallback statique pour DIVORCE_ETAPES country={}", country, ex); }
        return DivorceChecklistReferentiel.getEtapes(country);
    }

    // -----------------------------------------------------------------------
    // F-294 SF-294-01 — Pièces attendues (« socle ») par (domaine × pays × stade)
    // -----------------------------------------------------------------------

    /**
     * F-294 — Résout les pièces attendues (socle) pour une situation procédurale.
     *
     * <p>DB-first sur le type {@code EXPECTED_PIECES} (système + override
     * workspace), fallback Java {@link TravailPieceReferentiel} si la DB est
     * vide — pattern identique à {@link #getDivorcePieces(String)} (CA11).
     *
     * <p>Filtrage par stade :
     * <ul>
     *   <li>{@code stages} renseigné → pièce incluse si
     *       {@code procedureStage ∈ stages} ;</li>
     *   <li>{@code stages} absent / vide → pièce <b>générique</b>, toujours
     *       incluse (même si {@code procedureStage} est nul) ;</li>
     *   <li>{@code procedureStage} nul → seules les pièces génériques sont
     *       incluses.</li>
     * </ul>
     *
     * <p>Override workspace : une entrée {@code is_system=false} prime sur
     * l'entrée système de même {@code (entry_key, country)} (CA11).
     *
     * <p>Fail-open (CA7) : toute exception est avalée → fallback Java puis, à
     * défaut, liste vide. Jamais d'exception propagée.
     *
     * @param legalDomain    domaine (ex. {@code DROIT_DU_TRAVAIL})
     * @param country        pays (convention table : {@code FRANCE} / {@code BELGIQUE})
     * @param procedureStage code de stade F-243 (ex. {@code CPH_LICENCIEMENT}) ; nullable
     * @return liste immuable triée par {@code ordre} ; vide si non couvert
     */
    public List<ExpectedPiece> getExpectedPieces(String legalDomain, String country, String procedureStage) {
        if (legalDomain == null || country == null) return List.of();

        // F-294 SF-294-03 — Famille : réutilisation pure de DIVORCE_PIECES (DB-first
        // + fallback DivorceChecklistReferentiel via getDivorcePieces), sans seed
        // EXPECTED_PIECES dédié (anti-doublon, invariant #5). Les pièces divorce sont
        // génériques à la procédure → stages = List.of() ⇒ incluses quel que soit le
        // procedureStage. Fail-open (CA7) : exception → liste vide.
        if ("DROIT_FAMILLE".equals(legalDomain)) {
            try {
                List<DivorcePiece> divorcePieces = getDivorcePieces(country);
                List<ExpectedPiece> mapped = new ArrayList<>();
                int ordre = 1;
                for (DivorcePiece dp : divorcePieces) {
                    mapped.add(new ExpectedPiece(dp.code(), dp.label(), dp.country(),
                            List.of(), dp.obligatoire(), ordre++));
                }
                return List.copyOf(mapped);
            } catch (Exception e) {
                log.warn("LegalReferentialService: fail-open getExpectedPieces(DROIT_FAMILLE,{},{}) — {}",
                        country, procedureStage, e.getMessage());
                return List.of();
            }
        }

        List<ExpectedPiece> resolved;
        try {
            resolved = resolveExpectedPiecesFromDb(legalDomain, country);
            if (resolved.isEmpty()) {
                resolved = fallbackExpectedPieces(legalDomain, country);
            }
        } catch (Exception e) {
            log.warn("LegalReferentialService: fallback getExpectedPieces({},{},{}) — {}",
                    legalDomain, country, procedureStage, e.getMessage());
            try {
                resolved = fallbackExpectedPieces(legalDomain, country);
            } catch (Exception ignored) {
                return List.of();
            }
        }

        return resolved.stream()
                .filter(p -> matchesStage(p, procedureStage))
                .sorted(Comparator.comparingInt(ExpectedPiece::ordre))
                .toList();
    }

    /**
     * Charge les entrées {@code EXPECTED_PIECES} (système + override workspace)
     * du domaine, filtre sur le pays, applique l'override workspace (CA11) puis
     * parse {@code value_json}. Une entrée {@code value_json} mal formée est
     * ignorée (log warn) sans interrompre les autres.
     */
    private List<ExpectedPiece> resolveExpectedPiecesFromDb(String legalDomain, String country) {
        List<LegalReferential> entries = repository.findActiveByDomainAndType(
                legalDomain, "EXPECTED_PIECES", null);
        if (entries == null || entries.isEmpty()) return List.of();

        // Override workspace : (entry_key, country) ; workspace (is_system=false) prime.
        Map<String, LegalReferential> deduped = new LinkedHashMap<>();
        entries.stream()
                .filter(e -> country.equalsIgnoreCase(e.getCountry()))
                .filter(LegalReferential::isSystem)
                .forEach(e -> deduped.put(expectedPieceKey(e), e));
        entries.stream()
                .filter(e -> country.equalsIgnoreCase(e.getCountry()))
                .filter(e -> !e.isSystem())
                .forEach(e -> deduped.put(expectedPieceKey(e), e));

        List<ExpectedPiece> result = new ArrayList<>();
        for (LegalReferential e : deduped.values()) {
            try {
                JsonNode node = MAPPER.readTree(e.getValueJson());
                List<String> stages = new ArrayList<>();
                JsonNode stagesNode = node.get("stages");
                if (stagesNode != null && stagesNode.isArray()) {
                    for (JsonNode s : stagesNode) {
                        if (s.isTextual() && !s.asText().isBlank()) stages.add(s.asText());
                    }
                }
                boolean obligatoire = node.path("obligatoire").asBoolean(true);
                int ordre = node.path("ordre").asInt(0);
                result.add(new ExpectedPiece(e.getEntryKey(), e.getLabel(), e.getCountry(),
                        stages, obligatoire, ordre));
            } catch (Exception ex) {
                log.warn("LegalReferentialService: EXPECTED_PIECES value_json mal formé pour {}/{} — ignorée: {}",
                        e.getEntryKey(), e.getCountry(), ex.getMessage());
            }
        }
        return result;
    }

    private static String expectedPieceKey(LegalReferential e) {
        return e.getEntryKey() + "|" + (e.getCountry() != null ? e.getCountry() : "");
    }

    /** Fallback Java par domaine. Seul DROIT_DU_TRAVAIL est couvert en SF-294-01. */
    private List<ExpectedPiece> fallbackExpectedPieces(String legalDomain, String country) {
        if ("DROIT_DU_TRAVAIL".equals(legalDomain)) {
            return TravailPieceReferentiel.getPieces(country);
        }
        return List.of();
    }

    /**
     * Une pièce est incluse si elle est générique ({@code stages} vide) ou si le
     * stade demandé figure dans ses {@code stages}. Si {@code procedureStage}
     * est nul, seules les pièces génériques sont incluses.
     */
    private static boolean matchesStage(ExpectedPiece piece, String procedureStage) {
        if (piece.isGenerique()) return true;
        if (procedureStage == null || procedureStage.isBlank()) return false;
        return piece.stages().stream().anyMatch(procedureStage::equalsIgnoreCase);
    }

    /** Pièces divorce par pays. DB first → fallback DivorceChecklistReferentiel. */
    public List<DivorcePiece> getDivorcePieces(String country) {
        try {
            List<LegalReferential> entries = repository.findSystemEntriesByTypeAndCountry("DROIT_FAMILLE", "DIVORCE_PIECES", country);
            if (!entries.isEmpty()) {
                List<DivorcePiece> result = new ArrayList<>();
                for (LegalReferential e : entries) {
                    JsonNode node = MAPPER.readTree(e.getValueJson());
                    result.add(new DivorcePiece(e.getEntryKey(), e.getLabel(), country,
                            node.path("description").asText(), node.path("obligatoire").asBoolean()));
                }
                return result;
            }
        } catch (Exception ex) { log.warn("Fallback statique pour DIVORCE_PIECES country={}", country, ex); }
        return DivorceChecklistReferentiel.getPieces(country);
    }
}
