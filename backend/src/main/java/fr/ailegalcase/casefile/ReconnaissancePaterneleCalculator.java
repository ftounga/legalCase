package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-18-01 : calculateur de recevabilité d'une reconnaissance paternelle
 * volontaire (FR — art. 316 + 332-335 + 372 Cciv).
 *
 * <p>3 sous-types prévus à l'art. 316 :</p>
 * <ul>
 *   <li><strong>RECONNAISSANCE_PRENATALE</strong> (al. 1) — avant la naissance,
 *       devant tout officier d'état civil. Effet à la naissance.</li>
 *   <li><strong>RECONNAISSANCE_POST_NATALE_NAISSANCE</strong> (al. 2) —
 *       à l'établissement de l'acte de naissance.</li>
 *   <li><strong>RECONNAISSANCE_POST_NATALE_ULTERIEURE</strong> (al. 3) — à tout
 *       moment après la naissance, devant officier d'état civil ou notaire.</li>
 * </ul>
 *
 * <p>Critères de validité (verdict ELEVEE) :</p>
 * <ul>
 *   <li>{@code consentementLibreDuPere = true} (vice = nullité ; 1130 Cciv)</li>
 *   <li>{@code paterniteVraisemblable = true} (présomption — pas d'ADN à ce stade)</li>
 *   <li>{@code enfantNonReconnuParAutrePere = true} (sinon contestation préalable)</li>
 *   <li>{@code procedureRespectee = true} (présence OU procuration spéciale)</li>
 * </ul>
 *
 * <p>Outil <strong>single-country FRANCE</strong>. La Belgique a un régime
 * distinct (CC art. 327 et s. — consentement maternel requis) qui sera traité
 * par une feature jumelle au backlog.</p>
 */
public final class ReconnaissancePaterneleCalculator {

    /** Sous-type de reconnaissance paternelle (art. 316 Cciv). */
    public enum SousType {
        /** Reconnaissance prénatale (art. 316 al. 1). */
        RECONNAISSANCE_PRENATALE,
        /** Reconnaissance lors de l'acte de naissance (art. 316 al. 2). */
        RECONNAISSANCE_POST_NATALE_NAISSANCE,
        /** Reconnaissance ultérieure à la naissance (art. 316 al. 3). */
        RECONNAISSANCE_POST_NATALE_ULTERIEURE
    }

    /** Verdict de recevabilité de la reconnaissance. */
    public enum VerdictRecevabilite {
        ELEVEE,
        MOYENNE,
        FAIBLE
    }

    /** Délai de contestation par les tiers (art. 333 Cciv). */
    public static final int DELAI_CONTESTATION_ANS = 10;

    /** Base juridique consolidée. */
    private static final String BASE_JURIDIQUE =
            "Art. 316 Cciv + 332-335 + 372 Cciv";

    private ReconnaissancePaterneleCalculator() {}

    /**
     * Évalue la recevabilité d'une reconnaissance paternelle.
     *
     * @param sousType                       sous-type de reconnaissance (obligatoire)
     * @param dateNaissanceEnfant            date de naissance (peut être null pour prénatale)
     * @param dateReconnaissance             date à laquelle la reconnaissance est faite
     * @param consentementLibreDuPere        consentement libre du père (1130 Cciv)
     * @param paterniteVraisemblable         paternité vraisemblable (présomption)
     * @param enfantNonReconnuParAutrePere   enfant non déjà reconnu par un autre père
     * @param procedureRespectee             présence du père OU procuration spéciale notariée
     * @param presenceParProcuration         vrai si la reconnaissance est faite par procuration spéciale
     * @param country                        pays du workspace (FRANCE attendu)
     * @return résultat structuré
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static ReconnaissancePaterneleResult compute(SousType sousType,
                                                        LocalDate dateNaissanceEnfant,
                                                        LocalDate dateReconnaissance,
                                                        Boolean consentementLibreDuPere,
                                                        Boolean paterniteVraisemblable,
                                                        Boolean enfantNonReconnuParAutrePere,
                                                        Boolean procedureRespectee,
                                                        Boolean presenceParProcuration,
                                                        String country) {
        if (sousType == null) {
            throw new IllegalArgumentException("Sous-type de reconnaissance requis");
        }
        if (consentementLibreDuPere == null) {
            throw new IllegalArgumentException("Consentement libre du père (oui/non) requis");
        }
        if (paterniteVraisemblable == null) {
            throw new IllegalArgumentException("Paternité vraisemblable (oui/non) requise");
        }
        if (enfantNonReconnuParAutrePere == null) {
            throw new IllegalArgumentException(
                    "Enfant non reconnu par un autre père (oui/non) requis");
        }
        if (procedureRespectee == null) {
            throw new IllegalArgumentException("Procédure respectée (oui/non) requise");
        }
        if (presenceParProcuration == null) {
            presenceParProcuration = false;
        }
        // Pour les sous-types post-natals, la date de naissance est obligatoire
        if (sousType != SousType.RECONNAISSANCE_PRENATALE && dateNaissanceEnfant == null) {
            throw new IllegalArgumentException(
                    "Date de naissance de l'enfant requise pour une reconnaissance post-natale");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CC art. 327 et s., consentement maternel"
                            + " requis) sera traité dans une feature jumelle dédiée.");
        }

        // Calcul du score (0 à 100)
        int score = 0;
        if (consentementLibreDuPere) {
            score += 30; // critère cardinal — vice = nullité absolue
        }
        if (paterniteVraisemblable) {
            score += 20;
        }
        if (enfantNonReconnuParAutrePere) {
            score += 25; // sinon il faut d'abord contester l'autre filiation
        }
        if (procedureRespectee) {
            score += 20;
        }
        // Bonus si sous-type prénatal (effet immédiat à la naissance — moins
        // de risque de contestation par possession d'état contraire)
        if (sousType == SousType.RECONNAISSANCE_PRENATALE) {
            score += 5;
        }

        // Verdict
        VerdictRecevabilite verdict;
        if (!consentementLibreDuPere) {
            // Vice de consentement = nullité absolue → recevabilité FAIBLE
            verdict = VerdictRecevabilite.FAIBLE;
        } else if (!enfantNonReconnuParAutrePere) {
            // Conflit avec une filiation existante = blocage
            verdict = VerdictRecevabilite.FAIBLE;
        } else if (!procedureRespectee) {
            // Sans procuration ni présence : risque de nullité de forme
            verdict = VerdictRecevabilite.FAIBLE;
        } else if (!paterniteVraisemblable) {
            // Possession d'état contraire = risque de contestation
            verdict = VerdictRecevabilite.MOYENNE;
        } else if (score >= 90) {
            verdict = VerdictRecevabilite.ELEVEE;
        } else {
            verdict = VerdictRecevabilite.MOYENNE;
        }

        // Effet de la filiation : rétroactif à la naissance dans tous les cas
        // (al. 1 = à la naissance, al. 2 = à la naissance, al. 3 = rétroactif)
        LocalDate effetFiliation;
        if (sousType == SousType.RECONNAISSANCE_PRENATALE) {
            // Effet au jour de la naissance (à venir) — on retourne la date
            // de naissance si connue, sinon la date de reconnaissance + flag
            effetFiliation = dateNaissanceEnfant; // peut être null = à la naissance
        } else {
            effetFiliation = dateNaissanceEnfant;
        }

        // Risques de contestation
        List<String> risques = buildRisques(consentementLibreDuPere, paterniteVraisemblable,
                enfantNonReconnuParAutrePere, procedureRespectee, presenceParProcuration);

        // Documents requis selon le sous-type
        List<String> documents = buildDocuments(sousType, presenceParProcuration);

        String formule = String.format(Locale.ROOT,
                "Sous-type %s + consentement=%s + paternité vraisemblable=%s + enfant non reconnu=%s "
                        + "+ procédure=%s + procuration=%s → score %d → verdict %s "
                        + "→ effet filiation=%s, %d risques de contestation, %d documents requis",
                sousType.name(), consentementLibreDuPere, paterniteVraisemblable,
                enfantNonReconnuParAutrePere, procedureRespectee, presenceParProcuration,
                score, verdict.name(),
                effetFiliation != null ? effetFiliation.toString() : "à la naissance",
                risques.size(), documents.size());

        List<String> messages = buildMessages(sousType, verdict, consentementLibreDuPere,
                paterniteVraisemblable, enfantNonReconnuParAutrePere, procedureRespectee,
                presenceParProcuration, risques, documents);

        return new ReconnaissancePaterneleResult(
                sousType,
                dateNaissanceEnfant,
                dateReconnaissance,
                consentementLibreDuPere,
                paterniteVraisemblable,
                enfantNonReconnuParAutrePere,
                procedureRespectee,
                presenceParProcuration,
                countryNormalized,
                verdict,
                score,
                effetFiliation,
                risques,
                documents,
                DELAI_CONTESTATION_ANS,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static List<String> buildRisques(boolean consentement,
                                             boolean paterniteVraisemblable,
                                             boolean enfantNonReconnu,
                                             boolean procedureRespectee,
                                             boolean parProcuration) {
        List<String> risques = new ArrayList<>();
        if (!consentement) {
            risques.add("Vice de consentement (art. 1130 Cciv) — nullité absolue de la "
                    + "reconnaissance, action ouverte aux tiers intéressés sans condition de délai.");
        }
        if (!paterniteVraisemblable) {
            risques.add("Possession d'état contraire ou paternité non vraisemblable — "
                    + "risque de contestation par tout intéressé (art. 332-335 Cciv) "
                    + "dans un délai de " + DELAI_CONTESTATION_ANS + " ans.");
        }
        if (!enfantNonReconnu) {
            risques.add("Enfant déjà reconnu par un autre père — la reconnaissance ne peut "
                    + "produire effet qu'après contestation préalable et succès de l'action en "
                    + "contestation de paternité (art. 333 Cciv).");
        }
        if (!procedureRespectee) {
            risques.add("Procédure non respectée — défaut de présence ou de procuration spéciale "
                    + "notariée. Nullité de forme possible.");
        }
        if (parProcuration && procedureRespectee) {
            risques.add("Reconnaissance par procuration — contrôler que la procuration "
                    + "spéciale notariée est versée au dossier.");
        }
        return risques;
    }

    private static List<String> buildDocuments(SousType sousType, boolean parProcuration) {
        List<String> docs = new ArrayList<>();
        docs.add("Pièce d'identité du père (CNI ou passeport en cours de validité)");
        switch (sousType) {
            case RECONNAISSANCE_PRENATALE -> {
                docs.add("Certificat médical de grossesse (recommandé non obligatoire)");
                docs.add("Justificatif de domicile du père");
            }
            case RECONNAISSANCE_POST_NATALE_NAISSANCE -> {
                docs.add("Certificat d'accouchement (établi par l'établissement de santé)");
                docs.add("Pièce d'identité de la mère (pour vérification de l'acte de naissance)");
            }
            case RECONNAISSANCE_POST_NATALE_ULTERIEURE -> {
                docs.add("Acte de naissance intégral de l'enfant (de moins de 3 mois)");
                docs.add("Justificatif de domicile du père");
            }
        }
        if (parProcuration) {
            docs.add("Procuration spéciale notariée du père (mention « reconnaissance "
                    + "d'enfant »)");
        }
        return docs;
    }

    private static List<String> buildMessages(SousType sousType,
                                              VerdictRecevabilite verdict,
                                              boolean consentement,
                                              boolean paterniteVraisemblable,
                                              boolean enfantNonReconnu,
                                              boolean procedureRespectee,
                                              boolean parProcuration,
                                              List<String> risques,
                                              List<String> documents) {
        List<String> msgs = new ArrayList<>();

        msgs.add("Sous-type : " + libelleSousType(sousType));

        if (!consentement) {
            msgs.add("Consentement NON libre — vice du consentement (art. 1130 Cciv). "
                    + "Reconnaissance susceptible de nullité absolue.");
        }
        if (!paterniteVraisemblable) {
            msgs.add("Paternité NON vraisemblable — risque de contestation par tout "
                    + "intéressé sur le fondement des art. 332-335 Cciv.");
        }
        if (!enfantNonReconnu) {
            msgs.add("Enfant DÉJÀ reconnu par un autre père — engager d'abord une action "
                    + "en contestation de paternité (art. 333 Cciv) avant toute reconnaissance "
                    + "pour ne pas créer un conflit de filiations.");
        }
        if (!procedureRespectee) {
            msgs.add("Procédure NON respectée — défaut de présence ou absence de procuration "
                    + "spéciale notariée. La reconnaissance encourt la nullité de forme.");
        }
        if (parProcuration && procedureRespectee) {
            msgs.add("Reconnaissance par procuration — vérifier la mention spéciale "
                    + "« reconnaissance d'enfant » dans l'acte notarié de procuration.");
        }

        switch (verdict) {
            case ELEVEE -> msgs.add("VERDICT ELEVEE — la reconnaissance paternelle est "
                    + "valable, l'effet est rétroactif à la naissance et la filiation est "
                    + "opposable erga omnes sous réserve d'une contestation dans les "
                    + DELAI_CONTESTATION_ANS + " ans (art. 333 Cciv).");
            case MOYENNE -> msgs.add("VERDICT MOYENNE — la reconnaissance est recevable mais "
                    + "comporte un facteur de risque (paternité vraisemblable, possession "
                    + "d'état). Préparer la défense face à une contestation potentielle.");
            case FAIBLE -> msgs.add("VERDICT FAIBLE — la reconnaissance ne devrait pas être "
                    + "effectuée en l'état (vice de consentement, conflit de filiations, "
                    + "ou nullité de forme). Régulariser les préalables avant tout dépôt.");
        }

        msgs.add("Effet de la filiation : rétroactif à la naissance (art. 316 Cciv).");
        msgs.add("Délai de contestation par les tiers intéressés : "
                + DELAI_CONTESTATION_ANS + " ans (art. 333 Cciv).");
        msgs.add("Droits-devoirs attachés : autorité parentale (art. 372 Cciv, sous "
                + "conditions de reconnaissance dans la 1ère année), nom (art. 311-21), "
                + "obligation alimentaire, droits successoraux.");

        if (!risques.isEmpty()) {
            msgs.add(risques.size() + " risque(s) de contestation identifié(s).");
        }
        msgs.add(documents.size() + " document(s) requis pour le dépôt.");
        msgs.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return msgs;
    }

    private static String libelleSousType(SousType s) {
        return switch (s) {
            case RECONNAISSANCE_PRENATALE -> "Reconnaissance prénatale (art. 316 al. 1 Cciv)";
            case RECONNAISSANCE_POST_NATALE_NAISSANCE ->
                    "Reconnaissance à l'acte de naissance (art. 316 al. 2 Cciv)";
            case RECONNAISSANCE_POST_NATALE_ULTERIEURE ->
                    "Reconnaissance ultérieure (art. 316 al. 3 Cciv)";
        };
    }
}
