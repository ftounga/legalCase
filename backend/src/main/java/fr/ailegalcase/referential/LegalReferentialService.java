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
