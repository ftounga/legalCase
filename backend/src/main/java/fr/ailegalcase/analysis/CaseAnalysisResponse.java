package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record CaseAnalysisResponse(
        UUID id,
        int version,
        String analysisType,
        String status,
        List<TimelineEntry> timeline,
        List<AnalysisItem> faits,
        List<AnalysisItem> pointsJuridiques,
        List<AnalysisItem> risques,
        List<String> questionsOuvertes,
        List<String> piecesManquantes,
        List<String> pointsProcedure,
        String riskLevel,
        Integer riskScore,
        String modelUsed,
        Instant updatedAt,
        List<AnalysisDocumentEntry> analysisDocuments,
        CompensationCalculator.CompensationEstimate compensationEstimate,
        BelgianCompensationCalculator.BelgianCompensationEstimate belgianCompensationEstimate,
        PensionAlimentaireCalculator.PensionAlimentaireEstimate pensionAlimentaireEstimate,
        PrestationCompensatoireCalculator.PrestationCompensatoireEstimate prestationCompensatoireEstimate,
        LiquidationCommunauteResult liquidationCommunaute,
        TravailExtractedData travailExtractedData,
        ImmigrationExtractedData immigrationExtractedData,
        LicenciementValidityDetection licenciementValidityDetection,
        RuptureConvValidityDetection ruptureConvValidityDetection,
        List<PieceManquanteEntry> piecesManquantesDetails
) {

    public record PieceManquanteEntry(String texte, String critereCode) {}

    public record TravailExtractedData(
            String conventionCollective, String dateEntree, Double salaireBrutMensuel,
            String typeContrat, String poste, String motifLicenciement, String dateLicenciement,
            Integer congesContractuels, Double primeAncienneteContractuelle,
            // SF-DT-04-04 : identité salarié + employeur pour pré-remplissage fiches prud'homale (FR) et
            // requête tribunal du travail (BE). siretEmployeur renseigné côté FR uniquement,
            // bceEmployeur côté BE uniquement (champs à formats distincts).
            String nomSalarie, String prenomSalarie, String adresseSalarie,
            String nomEmployeur, String adresseEmployeur,
            String siretEmployeur, String bceEmployeur,
            String representantEmployeur) {

        /** Constructeur rétrocompat 9 champs (avant SF-DT-04-04). */
        public TravailExtractedData(String conventionCollective, String dateEntree, Double salaireBrutMensuel,
                                     String typeContrat, String poste, String motifLicenciement, String dateLicenciement,
                                     Integer congesContractuels, Double primeAncienneteContractuelle) {
            this(conventionCollective, dateEntree, salaireBrutMensuel,
                    typeContrat, poste, motifLicenciement, dateLicenciement,
                    congesContractuels, primeAncienneteContractuelle,
                    null, null, null, null, null, null, null, null);
        }
    }

    public record DetectedAnswer(String reponse, String justification) {}

    public record LicenciementValidityDetection(Map<String, DetectedAnswer> detections) {
        public LicenciementValidityDetection {
            detections = detections == null ? Map.of() : Map.copyOf(detections);
        }
    }

    public record RuptureConvValidityDetection(Map<String, DetectedAnswer> detections) {
        public RuptureConvValidityDetection {
            detections = detections == null ? Map.of() : Map.copyOf(detections);
        }
    }

    static final Set<String> RUPTURE_CONV_CRITERE_CODES = Set.of(
            "RC_CONSENTEMENT", "RC_DELAI_RETRACTATION", "RC_HOMOLOGATION",
            "RC_ASSISTANCE", "RC_INDEMNITE", "RC_ENTRETIENS"
    );

    static final Set<String> LICENCIEMENT_CRITERE_CODES = Set.of(
            "FR_CONVOCATION", "FR_ENTRETIEN", "FR_DELAI_NOTIFICATION", "FR_MOTIVATION",
            "FR_MOTIF_REEL", "FR_PROCEDURE_DISCIPLINAIRE", "FR_ORDRE_LICENCIEMENT",
            "BE_NOTIFICATION", "BE_PREAVIS", "BE_MOTIVATION", "BE_AUDITION",
            "BE_NON_DISCRIMINATION", "BE_PROTECTION_SPECIALE", "BE_INDEMNITE_MANIFESTE"
    );

    static final int MAX_JUSTIFICATION_LENGTH = 500;

    public record ImmigrationExtractedData(
            String dateExpirationTitre, String typeTitreSejour,
            String typeProcedureDetectee, String dateDepotProcedure,
            String typeTitreSejourCode, Boolean nationaliteUe,
            String typeRecoursCode, String dateNotificationDecisionContestee) {
        public ImmigrationExtractedData(String dateExpirationTitre, String typeTitreSejour,
                                         String typeProcedureDetectee, String dateDepotProcedure) {
            this(dateExpirationTitre, typeTitreSejour, typeProcedureDetectee, dateDepotProcedure, null, null, null, null);
        }
        public ImmigrationExtractedData(String dateExpirationTitre, String typeTitreSejour,
                                         String typeProcedureDetectee, String dateDepotProcedure,
                                         String typeTitreSejourCode, Boolean nationaliteUe) {
            this(dateExpirationTitre, typeTitreSejour, typeProcedureDetectee, dateDepotProcedure,
                    typeTitreSejourCode, nationaliteUe, null, null);
        }
    }

    static final Set<String> IMMIGRATION_TITLE_CODES = Set.of(
            "VLS_TS_ETUDIANT", "VLS_TS_SALARIE", "CST_SALARIE", "CARTE_PLURIANNUELLE",
            "CARTE_RESIDENT", "APS", "CST_VPF", "RECEPISSE_ASILE",
            "CARTE_A_TRAVAIL", "CARTE_A_ETUDES", "CARTE_A_FAMILLE",
            "CARTE_B", "CARTE_C", "PERMIS_UNIQUE", "ANNEXE_15", "ATTESTATION_IMMATRICULATION"
    );

    static final Set<String> IMMIGRATION_RECOURS_CODES = Set.of(
            "RECOURS_GRACIEUX_PREFET", "RECOURS_CONTENTIEUX_TA", "RECOURS_CNDA",
            "RECOURS_CGRA", "RECOURS_CCE", "RECOURS_CE_BELGIQUE"
    );

    public record TimelineEntry(String date, String evenement) {}

    public record AnalysisDocumentEntry(int index, String name) {}

    public record VersionSummary(
            UUID id,
            int version,
            String analysisType,
            Instant updatedAt,
            Integer faitsCount,
            Integer pointsJuridiquesCount,
            Integer risquesCount,
            Integer questionsOuvertesCount,
            Integer timelineCount
    ) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void populateCounts(CaseAnalysis analysis, String rawResult) {
        if (rawResult == null || rawResult.isBlank()) return;
        try {
            JsonNode root = MAPPER.readTree(stripMarkdownCodeBlock(rawResult));
            analysis.setFaitsCount(sizeOf(root, "faits"));
            analysis.setPointsJuridiquesCount(sizeOf(root, "points_juridiques"));
            analysis.setRisquesCount(sizeOf(root, "risques"));
            analysis.setQuestionsOuvertesCount(sizeOf(root, "questions_ouvertes"));
            analysis.setTimelineCount(sizeOf(root, "timeline"));
        } catch (Exception ignored) {
            // JSON malformé — compteurs restent null (fail-open)
        }
    }

    public static void populateRiskScore(CaseAnalysis analysis, String rawResult) {
        if (rawResult == null || rawResult.isBlank()) return;
        try {
            JsonNode root = MAPPER.readTree(stripMarkdownCodeBlock(rawResult));
            JsonNode scoreNode = root.get("score_risque");
            if (scoreNode == null || !scoreNode.isObject()) return;
            JsonNode niveauNode = scoreNode.get("niveau");
            JsonNode valeurNode = scoreNode.get("valeur");
            if (niveauNode != null && niveauNode.isTextual()) {
                String niveau = niveauNode.asText().toUpperCase();
                if (niveau.equals("FAIBLE") || niveau.equals("MOYEN") || niveau.equals("ELEVE")) {
                    analysis.setRiskLevel(niveau);
                }
            }
            if (valeurNode != null && valeurNode.isNumber()) {
                int valeur = valeurNode.asInt();
                if (valeur >= 0 && valeur <= 100) {
                    analysis.setRiskScore(valeur);
                }
            }
        } catch (Exception ignored) {
            // JSON malformé — risk score reste null (fail-open)
        }
    }

    private static int sizeOf(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return (node != null && node.isArray()) ? node.size() : 0;
    }

    public static CaseAnalysisResponse from(CaseAnalysis analysis) {
        return from(analysis, List.of());
    }

    public static CaseAnalysisResponse from(CaseAnalysis analysis, List<AnalysisDocument> documents) {
        List<TimelineEntry> timeline = List.of();
        List<AnalysisItem> faits = List.of();
        List<AnalysisItem> pointsJuridiques = List.of();
        List<AnalysisItem> risques = List.of();
        List<String> questionsOuvertes = List.of();
        List<String> piecesManquantes = List.of();
        List<String> pointsProcedure = List.of();
        CompensationCalculator.CompensationEstimate compensationEstimate = null;
        BelgianCompensationCalculator.BelgianCompensationEstimate belgianCompensationEstimate = null;
        PensionAlimentaireCalculator.PensionAlimentaireEstimate pensionAlimentaireEstimate = null;
        PrestationCompensatoireCalculator.PrestationCompensatoireEstimate prestationCompensatoireEstimate = null;
        LiquidationCommunauteResult liquidationCommunaute = null;
        TravailExtractedData travailExtractedData = null;
        ImmigrationExtractedData immigrationExtractedData = null;
        LicenciementValidityDetection licenciementValidityDetection = null;
        RuptureConvValidityDetection ruptureConvValidityDetection = null;
        List<PieceManquanteEntry> piecesManquantesDetails = List.of();

        String raw = stripMarkdownCodeBlock(analysis.getAnalysisResult());
        if (raw != null && !raw.isBlank()) {
            try {
                JsonNode root = MAPPER.readTree(raw);
                timeline = extractTimeline(root);
                faits = extractItemList(root, "faits");
                pointsJuridiques = extractItemList(root, "points_juridiques");
                risques = extractItemList(root, "risques");
                questionsOuvertes = extractStringList(root, "questions_ouvertes");
                piecesManquantesDetails = extractPiecesManquantesDetails(root);
                piecesManquantes = piecesManquantesDetails.stream().map(PieceManquanteEntry::texte).toList();
                pointsProcedure = extractPointsProcedureTexts(root);
                compensationEstimate = extractCompensationEstimate(root);
                pensionAlimentaireEstimate = extractPensionAlimentaireEstimate(root);
                prestationCompensatoireEstimate = extractPrestationCompensatoireEstimate(root);
                liquidationCommunaute = extractLiquidationCommunaute(root);
                travailExtractedData = extractTravailData(root);
                immigrationExtractedData = extractImmigrationData(root);
                licenciementValidityDetection = extractLicenciementValidityDetection(root);
                ruptureConvValidityDetection = extractRuptureConvValidityDetection(root);
            } catch (Exception ignored) {
                // JSON malformé — on retourne les listes vides
            }
        }

        List<AnalysisDocumentEntry> analysisDocuments = buildAnalysisDocuments(documents);

        return new CaseAnalysisResponse(
                analysis.getId(),
                analysis.getVersion(),
                analysis.getAnalysisType().name(),
                analysis.getAnalysisStatus().name(),
                timeline,
                faits,
                pointsJuridiques,
                risques,
                questionsOuvertes,
                piecesManquantes,
                pointsProcedure,
                analysis.getRiskLevel(),
                analysis.getRiskScore(),
                analysis.getModelUsed(),
                analysis.getUpdatedAt(),
                analysisDocuments,
                compensationEstimate,
                belgianCompensationEstimate,
                pensionAlimentaireEstimate,
                prestationCompensatoireEstimate,
                liquidationCommunaute,
                travailExtractedData,
                immigrationExtractedData,
                licenciementValidityDetection,
                ruptureConvValidityDetection,
                piecesManquantesDetails
        );
    }

    public static CaseAnalysisResponse from(CaseAnalysis analysis, List<AnalysisDocument> documents, String country) {
        CaseAnalysisResponse base = from(analysis, documents);
        if ("BELGIQUE".equals(country) && base.compensationEstimate() != null) {
            var ce = base.compensationEstimate();
            var belgian = BelgianCompensationCalculator.calculate(
                    ce.ancienneteAnnees(), ce.ancienneteMois(), ce.salaireReference()).orElse(null);
            // Conserver compensationEstimate non-null pour que le frontend F-DT-09
            // (indemnite-comparatif-section) puisse lire typeRupture / ancienneté /
            // salaire détectés par l'IA et déclencher les alertes F-IA-03 aussi
            // côté BE. Le panneau d'affichage Macron FR est masqué côté frontend
            // via un guard sur belgianCompensationEstimate.
            return new CaseAnalysisResponse(
                    base.id(), base.version(), base.analysisType(), base.status(),
                    base.timeline(), base.faits(), base.pointsJuridiques(), base.risques(),
                    base.questionsOuvertes(), base.piecesManquantes(), base.pointsProcedure(),
                    base.riskLevel(), base.riskScore(), base.modelUsed(), base.updatedAt(),
                    base.analysisDocuments(),
                    ce, belgian,
                    base.pensionAlimentaireEstimate(), base.prestationCompensatoireEstimate(),
                    base.liquidationCommunaute(),
                    base.travailExtractedData(), base.immigrationExtractedData(),
                    base.licenciementValidityDetection(),
                    base.ruptureConvValidityDetection(),
                    base.piecesManquantesDetails());
        }
        return base;
    }

    static CompensationCalculator.CompensationEstimate extractCompensationEstimate(JsonNode root) {
        JsonNode compNode = root.get("compensation_data");
        try {
            String typeRupture = null;
            Integer annees = null;
            Integer mois = null;
            Double salaire = null;

            if (compNode != null && compNode.isObject()) {
                String rawType = compNode.has("type_rupture") && !compNode.get("type_rupture").isNull()
                        ? compNode.get("type_rupture").asText() : null;
                typeRupture = TypeRuptureFallback.normalize(rawType);
                annees  = compNode.has("anciennete_annees")  && !compNode.get("anciennete_annees").isNull()
                        ? compNode.get("anciennete_annees").intValue() : null;
                mois    = compNode.has("anciennete_mois")    && !compNode.get("anciennete_mois").isNull()
                        ? compNode.get("anciennete_mois").intValue() : null;
                salaire = compNode.has("salaire_reference_mensuel") && !compNode.get("salaire_reference_mensuel").isNull()
                        ? compNode.get("salaire_reference_mensuel").doubleValue() : null;
            }

            // Fallback : si l'IA n'a pas peuplé type_rupture (ou pas compensation_data du tout)
            // mais qu'elle a détecté un licenciement ailleurs, on dérive un type par défaut
            // et on remonte au minimum un estimate partiel porteur de ce type_rupture
            // (pour le pré-remplissage F-DT-09 et les alertes de cohérence F-IA-03).
            if (typeRupture == null) {
                typeRupture = TypeRuptureFallback.derive(root);
                if (typeRupture == null) return null;
                var calculated = CompensationCalculator.calculate(typeRupture, annees, mois, salaire);
                if (calculated.isPresent()) return calculated.get();
                int safeAnnees = annees != null ? annees : 0;
                int safeMois   = mois != null ? mois : 0;
                double safeSalaire = (salaire != null && salaire > 0) ? salaire : 0;
                return new CompensationCalculator.CompensationEstimate(
                        0.0, safeSalaire, safeAnnees, safeMois,
                        typeRupture, 0, 0.0, true);
            }

            return CompensationCalculator.calculate(typeRupture, annees, mois, salaire).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    static final Set<String> MODE_GARDE_DETAILLE_VALUES = Set.of(
            "ALTERNEE_FR", "DVH_CLASSIQUE_FR", "DVH_ELARGI_FR",
            "ALTERNEE_BE", "SECONDAIRE_BE", "SECONDAIRE_ELARGI_BE"
    );

    static PensionAlimentaireCalculator.PensionAlimentaireEstimate extractPensionAlimentaireEstimate(JsonNode root) {
        JsonNode node = root.get("pension_alimentaire_data");
        if (node == null || !node.isObject()) return null;
        try {
            Double revenus = node.has("revenus_net_mensuel_debiteur") && !node.get("revenus_net_mensuel_debiteur").isNull()
                    ? node.get("revenus_net_mensuel_debiteur").doubleValue() : null;
            Integer nbEnfants = node.has("nb_enfants") && !node.get("nb_enfants").isNull()
                    ? node.get("nb_enfants").intValue() : null;
            String modeGarde = node.has("mode_garde") && !node.get("mode_garde").isNull()
                    ? node.get("mode_garde").asText() : null;
            String pays = node.has("pays_applicable") && !node.get("pays_applicable").isNull()
                    ? node.get("pays_applicable").asText() : null;
            String modeGardeDetaille = null;
            if (node.has("mode_garde_detaille") && !node.get("mode_garde_detaille").isNull()) {
                String raw = node.get("mode_garde_detaille").asText();
                if (raw != null && !raw.isBlank()) {
                    String normalized = raw.trim().toUpperCase();
                    if (MODE_GARDE_DETAILLE_VALUES.contains(normalized)) {
                        modeGardeDetaille = normalized;
                    }
                }
            }
            var estimate = PensionAlimentaireCalculator.calculate(revenus, nbEnfants, modeGarde, pays).orElse(null);
            if (estimate == null) return null;
            if (modeGardeDetaille == null) return estimate;
            return new PensionAlimentaireCalculator.PensionAlimentaireEstimate(
                    estimate.montantMin(), estimate.montantMax(), estimate.revenus(),
                    estimate.nbEnfants(), estimate.modeGarde(), estimate.pays(),
                    estimate.donneesPartielles(), modeGardeDetaille);
        } catch (Exception ignored) {
            return null;
        }
    }

    static PrestationCompensatoireCalculator.PrestationCompensatoireEstimate extractPrestationCompensatoireEstimate(JsonNode root) {
        JsonNode node = root.get("prestation_compensatoire_data");
        if (node == null || !node.isObject()) return null;
        try {
            Double revenusA    = node.has("revenus_net_mensuel_epoux_a") && !node.get("revenus_net_mensuel_epoux_a").isNull()
                    ? node.get("revenus_net_mensuel_epoux_a").doubleValue() : null;
            Double revenusB    = node.has("revenus_net_mensuel_epoux_b") && !node.get("revenus_net_mensuel_epoux_b").isNull()
                    ? node.get("revenus_net_mensuel_epoux_b").doubleValue() : null;
            Integer duree      = node.has("duree_mariage_annees") && !node.get("duree_mariage_annees").isNull()
                    ? node.get("duree_mariage_annees").intValue() : null;
            String pays        = node.has("pays_applicable") && !node.get("pays_applicable").isNull()
                    ? node.get("pays_applicable").asText() : null;
            return PrestationCompensatoireCalculator.calculate(revenusA, revenusB, duree, pays).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    static LiquidationCommunauteResult extractLiquidationCommunaute(JsonNode root) {
        JsonNode node = root.get("liquidation_communaute_data");
        if (node == null || !node.isObject()) return null;
        try {
            String regime = node.has("regime_matrimonial") && !node.get("regime_matrimonial").isNull()
                    ? node.get("regime_matrimonial").asText() : null;
            List<LiquidationCommunauteResult.BienItem> actifCommun       = extractBienItems(node, "actif_commun", "valeur_estimee");
            List<LiquidationCommunauteResult.BienItem> biensPropresA     = extractBienItems(node, "biens_propres_epoux_a", "valeur_estimee");
            List<LiquidationCommunauteResult.BienItem> biensPropresB     = extractBienItems(node, "biens_propres_epoux_b", "valeur_estimee");
            List<LiquidationCommunauteResult.BienItem> passifCommun      = extractBienItems(node, "passif_commun", "montant");
            return new LiquidationCommunauteResult(regime, actifCommun, biensPropresA, biensPropresB, passifCommun);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<LiquidationCommunauteResult.BienItem> extractBienItems(JsonNode parent, String field, String valeurKey) {
        JsonNode array = parent.get(field);
        if (array == null || !array.isArray()) return List.of();
        java.util.List<LiquidationCommunauteResult.BienItem> result = new java.util.ArrayList<>();
        for (JsonNode item : array) {
            if (!item.isObject() || !item.has("libelle")) continue;
            String libelle = item.get("libelle").asText();
            Double valeur  = item.has(valeurKey) && !item.get(valeurKey).isNull()
                    ? item.get(valeurKey).doubleValue() : null;
            result.add(new LiquidationCommunauteResult.BienItem(libelle, valeur));
        }
        return List.copyOf(result);
    }

    private static List<AnalysisDocumentEntry> buildAnalysisDocuments(List<AnalysisDocument> documents) {
        if (documents == null || documents.isEmpty()) return List.of();
        List<AnalysisDocumentEntry> result = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            result.add(new AnalysisDocumentEntry(i, documents.get(i).getDocumentName()));
        }
        return List.copyOf(result);
    }

    /**
     * Parse un array JSON en List<AnalysisItem>. Fail-open :
     * - item string → AnalysisItem(texte, null, null)
     * - item objet {texte, source?, extrait?} → AnalysisItem complet
     * - item malformé → ignoré
     */
    static List<AnalysisItem> extractItemList(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isArray()) return List.of();
        List<AnalysisItem> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) {
                result.add(AnalysisItem.ofText(item.asText()));
            } else if (item.isObject()) {
                String texte = item.has("texte") ? item.get("texte").asText() : item.toString();
                String source = item.has("source") && !item.get("source").isNull()
                        ? item.get("source").asText() : null;
                String extrait = item.has("extrait") && !item.get("extrait").isNull()
                        ? item.get("extrait").asText() : null;
                result.add(new AnalysisItem(texte, source, extrait));
            }
        }
        return List.copyOf(result);
    }

    private static List<String> extractStringList(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) result.add(item.asText());
        }
        return List.copyOf(result);
    }

    /**
     * Extrait pieces_manquantes en tolérant les deux formats :
     * - legacy : array de strings
     * - nouveau : array d'objets {texte, critere_code?}
     */
    static List<PieceManquanteEntry> extractPiecesManquantesDetails(JsonNode root) {
        JsonNode node = root.get("pieces_manquantes");
        if (node == null || !node.isArray()) return List.of();
        List<PieceManquanteEntry> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) {
                String txt = item.asText();
                if (txt != null && !txt.isBlank()) result.add(new PieceManquanteEntry(txt, null));
            } else if (item.isObject()) {
                JsonNode texteNode = item.get("texte");
                if (texteNode == null || !texteNode.isTextual()) continue;
                String texte = texteNode.asText();
                if (texte.isBlank()) continue;
                String code = null;
                JsonNode codeNode = item.get("critere_code");
                if (codeNode != null && codeNode.isTextual()) {
                    String raw = codeNode.asText().trim();
                    if (!raw.isEmpty()) code = raw.toUpperCase();
                }
                result.add(new PieceManquanteEntry(texte, code));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Extrait les descriptions de points_procedure en tolérant les deux formats :
     * - legacy : array de strings
     * - nouveau : array d'objets {texte, critere_code?}
     */
    static List<String> extractPointsProcedureTexts(JsonNode root) {
        JsonNode node = root.get("points_procedure");
        if (node == null || !node.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) {
                String txt = item.asText();
                if (txt != null && !txt.isBlank()) result.add(txt);
            } else if (item.isObject()) {
                JsonNode texte = item.get("texte");
                if (texte != null && texte.isTextual()) {
                    String t = texte.asText();
                    if (!t.isBlank()) result.add(t);
                }
            }
        }
        return List.copyOf(result);
    }

    public static String stripMarkdownCodeBlock(String raw) {
        if (raw == null) return null;
        String s = raw.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline != -1) s = s.substring(firstNewline + 1);
            if (s.endsWith("```")) s = s.substring(0, s.lastIndexOf("```")).strip();
        }
        return s;
    }

    private static List<TimelineEntry> extractTimeline(JsonNode root) {
        JsonNode node = root.get("timeline");
        if (node == null || !node.isArray()) return List.of();
        List<TimelineEntry> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isObject()) {
                String date = item.has("date") ? item.get("date").asText() : "";
                String evenement = item.has("evenement") ? item.get("evenement").asText() : "";
                result.add(new TimelineEntry(date, evenement));
            }
        }
        return List.copyOf(result);
    }

    static TravailExtractedData extractTravailData(JsonNode root) {
        JsonNode node = root.get("travail_extracted_data");
        if (node == null || !node.isObject()) return null;
        try {
            return new TravailExtractedData(
                    // SF-129-01 : normaliser le code convention pour matcher le référentiel
                    fr.ailegalcase.casefile.ConventionCodeNormalizer.normalize(textOrNull(node, "convention_collective")),
                    textOrNull(node, "date_entree"),
                    doubleOrNull(node, "salaire_brut_mensuel"),
                    textOrNull(node, "type_contrat"),
                    textOrNull(node, "poste"),
                    textOrNull(node, "motif_licenciement"),
                    textOrNull(node, "date_licenciement"),
                    intOrNull(node, "conges_contractuels"),
                    doubleOrNull(node, "prime_anciennete_contractuelle"),
                    textOrNull(node, "nom_salarie"),
                    textOrNull(node, "prenom_salarie"),
                    textOrNull(node, "adresse_salarie"),
                    textOrNull(node, "nom_employeur"),
                    textOrNull(node, "adresse_employeur"),
                    normalizeFrIdentifier(textOrNull(node, "siret_employeur")),
                    normalizeBeBceIdentifier(textOrNull(node, "bce_employeur")),
                    textOrNull(node, "representant_employeur")
            );
        } catch (Exception ignored) { return null; }
    }

    /** Normalise un SIREN/SIRET : garde uniquement les chiffres, null si 0 chiffre, sinon renvoie la chaîne. */
    static String normalizeFrIdentifier(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    /** Normalise un BCE belge : retire le préfixe `BE`, les espaces/points, garde les chiffres. */
    static String normalizeBeBceIdentifier(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    static LicenciementValidityDetection extractLicenciementValidityDetection(JsonNode root) {
        JsonNode node = root.get("licenciement_validity_detection");
        if (node == null || !node.isObject() || node.size() == 0) return null;
        Map<String, DetectedAnswer> detections = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            String code = entry.getKey();
            if (!LICENCIEMENT_CRITERE_CODES.contains(code)) return;
            JsonNode value = entry.getValue();
            if (value == null || !value.isObject()) return;
            String reponse = normalizeReponse(textOrNull(value, "reponse"));
            String justification = textOrNull(value, "justification");
            if (justification != null && justification.length() > MAX_JUSTIFICATION_LENGTH) {
                justification = justification.substring(0, MAX_JUSTIFICATION_LENGTH);
            }
            detections.put(code, new DetectedAnswer(reponse, justification));
        });
        return detections.isEmpty() ? null : new LicenciementValidityDetection(detections);
    }

    static RuptureConvValidityDetection extractRuptureConvValidityDetection(JsonNode root) {
        JsonNode node = root.get("rupture_conv_validity_detection");
        if (node == null || !node.isObject() || node.size() == 0) return null;
        Map<String, DetectedAnswer> detections = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            String code = entry.getKey() == null ? null : entry.getKey().toUpperCase();
            if (code == null || !RUPTURE_CONV_CRITERE_CODES.contains(code)) return;
            JsonNode value = entry.getValue();
            if (value == null || !value.isObject()) return;
            String reponse = normalizeReponse(textOrNull(value, "reponse"));
            String justification = textOrNull(value, "justification");
            if (justification != null && justification.length() > MAX_JUSTIFICATION_LENGTH) {
                justification = justification.substring(0, MAX_JUSTIFICATION_LENGTH);
            }
            detections.put(code, new DetectedAnswer(reponse, justification));
        });
        return detections.isEmpty() ? null : new RuptureConvValidityDetection(detections);
    }

    private static String normalizeReponse(String raw) {
        if (raw == null) return "INCONNU";
        String up = raw.trim().toUpperCase();
        return (up.equals("OUI") || up.equals("NON")) ? up : "INCONNU";
    }

    static ImmigrationExtractedData extractImmigrationData(JsonNode root) {
        String dateExpiration = textOrNull(root, "date_expiration_titre");
        String typeTitre = textOrNull(root, "type_titre_sejour");
        String typeProcedure = textOrNull(root, "type_procedure_detectee");
        String dateDepot = textOrNull(root, "date_depot_procedure");
        String typeCode = normalizeTitleCode(textOrNull(root, "type_titre_sejour_code"));
        Boolean nationaliteUe = normalizeNationaliteUe(root.get("nationalite_ue"));
        String recoursCode = normalizeRecoursCode(textOrNull(root, "type_recours_code"));
        String dateNotif = textOrNull(root, "date_notification_decision_contestee");
        if (dateExpiration == null && typeTitre == null && typeProcedure == null
                && dateDepot == null && typeCode == null && nationaliteUe == null
                && recoursCode == null && dateNotif == null) return null;
        return new ImmigrationExtractedData(dateExpiration, typeTitre, typeProcedure, dateDepot,
                typeCode, nationaliteUe, recoursCode, dateNotif);
    }

    private static String normalizeRecoursCode(String raw) {
        if (raw == null) return null;
        String up = raw.trim().toUpperCase();
        return IMMIGRATION_RECOURS_CODES.contains(up) ? up : null;
    }

    private static String normalizeTitleCode(String raw) {
        if (raw == null) return null;
        String up = raw.trim().toUpperCase();
        return IMMIGRATION_TITLE_CODES.contains(up) ? up : null;
    }

    private static Boolean normalizeNationaliteUe(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isBoolean()) return node.booleanValue();
        if (node.isTextual()) {
            String s = node.asText().trim().toLowerCase();
            if ("true".equals(s)) return Boolean.TRUE;
            if ("false".equals(s)) return Boolean.FALSE;
        }
        return null;
    }

    private static String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private static Double doubleOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).doubleValue() : null;
    }

    private static Integer intOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).intValue() : null;
    }
}
