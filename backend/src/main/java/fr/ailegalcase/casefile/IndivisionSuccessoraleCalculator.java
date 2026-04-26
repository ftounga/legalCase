package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SF-FA-24-11 : calculateur d'analyse de la <b>gestion d'une indivision
 * successorale</b> (art. 815 à 832-2 Cciv pour l'indivision légale,
 * art. 1873-1 et s. Cciv pour l'indivision conventionnelle, art. 815-1 et s.
 * Cciv pour le maintien forcé). Calcule également l'indemnité d'occupation
 * (art. 815-9 al. 2 Cciv).
 *
 * <p>Outil <b>single-country FR DROIT_FAMILLE</b>. La BE (CC BE art. 577-2 +)
 * suit une logique distincte non couverte ici.</p>
 *
 * <p>À distinguer de {@link IndivisionCalculator} (F-FA-22 — indivision
 * post-communautaire, suite divorce). Ici l'origine est le décès et le
 * dispositif s'inscrit dans la gestion successorale.</p>
 *
 * <h2>Verdict de gestion</h2>
 * <ul>
 *   <li>tous consentements + pas de conflit + pas d'occupation exclusive →
 *       {@code HARMONIEUSE}</li>
 *   <li>occupation exclusive non consentie OU actes admin contestés OU absence
 *       de consentement → {@code CONFLICTUELLE}</li>
 *   <li>partage demandé + actes admin contestés + pas de consentement →
 *       {@code BLOCAGE}</li>
 * </ul>
 *
 * <h2>Indemnité d'occupation (art. 815-9 al. 2)</h2>
 * <pre>indemnite = valeurBienOccupe × 0.04 / 12 × dureeMois × quotePartLesee</pre>
 * où {@code quotePartLesee = 1 - 1/nbHeritiers} (V1 simplifiée — quote-parts
 * théoriques égales).
 *
 * <h2>Frais de gestion estimés</h2>
 * <pre>fraisAnnuels = valeurPatrimoineIndivis × 0.01 + 800
 * fraisTotaux = fraisAnnuels × dureeMois / 12</pre>
 */
public final class IndivisionSuccessoraleCalculator {

    public static final String BASE_JURIDIQUE =
            "Art. 815 à 832-2 + 1873-1 et s. Cciv";

    public static final String VERDICT_HARMONIEUSE = "HARMONIEUSE";
    public static final String VERDICT_CONFLICTUELLE = "CONFLICTUELLE";
    public static final String VERDICT_BLOCAGE = "BLOCAGE";

    public static final String DISPOSITIF_CONVENTION = "CONVENTION_INDIVISION_5_ANS";
    public static final String DISPOSITIF_MAINTIEN_LEGALE = "MAINTIEN_INDIVISION_LEGALE";
    public static final String DISPOSITIF_MEDIATION = "MEDIATION_FAMILIALE";
    public static final String DISPOSITIF_PARTAGE_AMIABLE = "PARTAGE_AMIABLE";
    public static final String DISPOSITIF_PARTAGE_JUDICIAIRE = "PARTAGE_JUDICIAIRE";
    public static final String DISPOSITIF_MAINTIEN_FORCE = "MAINTIEN_FORCE_PRESERVE";

    public static final BigDecimal TAUX_OCCUPATION_ANNUEL = new BigDecimal("0.04");
    public static final BigDecimal TAUX_FRAIS_ENTRETIEN = new BigDecimal("0.01");
    public static final BigDecimal FRAIS_FIXES_ANNUELS = new BigDecimal("800");

    private static final Set<String> TYPES_INDIVISION = new LinkedHashSet<>(Arrays.asList(
            "INDIVISION_LEGALE", "INDIVISION_CONVENTIONNELLE", "MAINTIEN_FORCE"));

    private IndivisionSuccessoraleCalculator() {
    }

    public static IndivisionSuccessoraleResult compute(
            LocalDate dateOuvertureSuccession,
            String typeIndivision,
            int nbHeritiers,
            BigDecimal valeurPatrimoineIndivisEur,
            BigDecimal valeurBienOccupeEur,
            boolean consentementsTous,
            boolean occupationExclusive,
            boolean actesAdministrationContestes,
            boolean demandePartage) {

        validate(dateOuvertureSuccession, typeIndivision, nbHeritiers,
                valeurPatrimoineIndivisEur, valeurBienOccupeEur);

        // 1. Durée
        int dureeMois = computeDureeMois(dateOuvertureSuccession, LocalDate.now());

        // 2. Score de conflictualité (0-100)
        int score = 0;
        if (!consentementsTous) score += 30;
        if (occupationExclusive) score += 25;
        if (actesAdministrationContestes) score += 30;
        if (demandePartage && actesAdministrationContestes) score += 15;
        score = Math.max(0, Math.min(100, score));

        // 3. Verdict
        String verdict = computeVerdict(consentementsTous, occupationExclusive,
                actesAdministrationContestes, demandePartage);

        // 4. Dispositif recommandé
        String dispositif = computeDispositif(verdict, typeIndivision, demandePartage);

        // 5. Indemnité d'occupation (art. 815-9 al. 2)
        BigDecimal indemnite = computeIndemniteOccupation(
                occupationExclusive, valeurBienOccupeEur, dureeMois, nbHeritiers);
        boolean indemniteDue = occupationExclusive && indemnite.signum() > 0;

        // 6. Frais de gestion estimés
        BigDecimal frais = computeFraisGestion(valeurPatrimoineIndivisEur, dureeMois);

        // 7. Messages
        List<String> messages = buildMessages(verdict, dispositif, typeIndivision,
                occupationExclusive, actesAdministrationContestes, demandePartage,
                consentementsTous, indemniteDue);

        // 8. Formule
        String formule = buildFormule(score, verdict, dispositif, dureeMois,
                indemniteDue, indemnite, frais);

        return new IndivisionSuccessoraleResult(
                dateOuvertureSuccession,
                typeIndivision,
                nbHeritiers,
                money(valeurPatrimoineIndivisEur),
                money(valeurBienOccupeEur),
                consentementsTous,
                occupationExclusive,
                actesAdministrationContestes,
                demandePartage,
                dureeMois,
                verdict,
                dispositif,
                indemniteDue,
                indemnite,
                frais,
                score,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    static int computeDureeMois(LocalDate ouverture, LocalDate now) {
        long mois = ChronoUnit.MONTHS.between(ouverture, now);
        return (int) Math.max(0, mois);
    }

    static String computeVerdict(boolean consentementsTous,
                                 boolean occupationExclusive,
                                 boolean actesAdministrationContestes,
                                 boolean demandePartage) {
        boolean blocage = demandePartage
                && actesAdministrationContestes
                && !consentementsTous;
        if (blocage) return VERDICT_BLOCAGE;
        boolean conflit = !consentementsTous
                || occupationExclusive
                || actesAdministrationContestes;
        if (conflit) return VERDICT_CONFLICTUELLE;
        return VERDICT_HARMONIEUSE;
    }

    static String computeDispositif(String verdict, String typeIndivision,
                                    boolean demandePartage) {
        if (VERDICT_BLOCAGE.equals(verdict)) {
            return demandePartage ? DISPOSITIF_PARTAGE_JUDICIAIRE : DISPOSITIF_MEDIATION;
        }
        if (VERDICT_CONFLICTUELLE.equals(verdict)) {
            return demandePartage ? DISPOSITIF_PARTAGE_AMIABLE : DISPOSITIF_MEDIATION;
        }
        // HARMONIEUSE
        if (demandePartage) return DISPOSITIF_PARTAGE_AMIABLE;
        if ("MAINTIEN_FORCE".equals(typeIndivision)) return DISPOSITIF_MAINTIEN_FORCE;
        if ("INDIVISION_CONVENTIONNELLE".equals(typeIndivision)) {
            return DISPOSITIF_MAINTIEN_LEGALE;
        }
        return DISPOSITIF_CONVENTION;
    }

    static BigDecimal computeIndemniteOccupation(boolean occupation,
                                                 BigDecimal valeurBienOccupe,
                                                 int dureeMois,
                                                 int nbHeritiers) {
        if (!occupation) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (dureeMois <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (valeurBienOccupe == null || valeurBienOccupe.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        // quote-part lésée = (n - 1) / n
        BigDecimal nb = BigDecimal.valueOf(nbHeritiers);
        BigDecimal quotePartLesee = nb.subtract(BigDecimal.ONE)
                .divide(nb, 10, RoundingMode.HALF_UP);
        BigDecimal indemnite = valeurBienOccupe
                .multiply(TAUX_OCCUPATION_ANNUEL)
                .multiply(BigDecimal.valueOf(dureeMois))
                .multiply(quotePartLesee)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        return indemnite.setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal computeFraisGestion(BigDecimal valeurPatrimoine, int dureeMois) {
        if (valeurPatrimoine == null || dureeMois <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal fraisAnnuels = valeurPatrimoine
                .multiply(TAUX_FRAIS_ENTRETIEN)
                .add(FRAIS_FIXES_ANNUELS);
        BigDecimal frais = fraisAnnuels
                .multiply(BigDecimal.valueOf(dureeMois))
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        return frais.setScale(2, RoundingMode.HALF_UP);
    }

    private static List<String> buildMessages(String verdict,
                                              String dispositif,
                                              String typeIndivision,
                                              boolean occupationExclusive,
                                              boolean actesContestes,
                                              boolean demandePartage,
                                              boolean consentementsTous,
                                              boolean indemniteDue) {
        List<String> msg = new ArrayList<>();
        msg.add("Indivision successorale : analyse fondée sur les art. 815 à "
                + "832-2 Cciv (régime légal de l'indivision successorale), "
                + "art. 1873-1 et s. Cciv (indivision conventionnelle) et "
                + "art. 815-1 et s. Cciv (maintien forcé en justice).");
        msg.add("Principe fondamental : « Nul ne peut être contraint à demeurer "
                + "dans l'indivision et le partage peut toujours être provoqué » "
                + "(art. 815 al. 1 Cciv).");

        switch (typeIndivision) {
            case "INDIVISION_LEGALE" -> msg.add("Régime actuel : indivision "
                    + "légale (art. 815). Décisions à la majorité 2/3 pour "
                    + "actes d'administration courante (art. 815-3), "
                    + "unanimité pour actes de disposition (vente, donation).");
            case "INDIVISION_CONVENTIONNELLE" -> msg.add("Régime actuel : "
                    + "indivision conventionnelle (art. 1873-1 et s.) — "
                    + "convention écrite, durée maximale 5 ans renouvelables, "
                    + "gérant désigné. Sécurise la gestion sur la durée.");
            case "MAINTIEN_FORCE" -> msg.add("Régime actuel : maintien forcé "
                    + "(art. 815-1) — décision judiciaire pour 2 à 5 ans, "
                    + "destiné à préserver une exploitation familiale ou la "
                    + "résidence du conjoint survivant et des enfants.");
            default -> { /* no-op */ }
        }

        switch (verdict) {
            case VERDICT_HARMONIEUSE -> msg.add("Gestion harmonieuse : profitez "
                    + "de la période pour formaliser la stratégie. Une convention "
                    + "d'indivision (5 ans renouvelables) sécurise la gestion "
                    + "future et limite les risques de blocage.");
            case VERDICT_CONFLICTUELLE -> msg.add("Gestion conflictuelle : "
                    + "engager une médiation familiale (art. 1108 CPC, "
                    + "obligatoirement mentionnée dans toute future "
                    + "assignation) avant d'envisager une saisine du juge.");
            case VERDICT_BLOCAGE -> msg.add("Blocage caractérisé : la voie "
                    + "amiable est manifestement épuisée. Saisir le tribunal "
                    + "judiciaire en partage (art. 1364 CPC). Le juge "
                    + "désignera un notaire commis pour les opérations.");
            default -> { /* no-op */ }
        }

        switch (dispositif) {
            case DISPOSITIF_CONVENTION -> msg.add("Recommandation : rédiger une "
                    + "convention d'indivision (art. 1873-1 et s.) — durée "
                    + "≤ 5 ans renouvelables, désignation d'un gérant, règles "
                    + "de majorité, frais à la charge de l'indivision.");
            case DISPOSITIF_MAINTIEN_LEGALE -> msg.add("Recommandation : "
                    + "maintenir l'indivision conventionnelle telle qu'elle, "
                    + "vérifier la date d'expiration et programmer la "
                    + "renégociation si renouvellement souhaité.");
            case DISPOSITIF_MAINTIEN_FORCE -> msg.add("Recommandation : "
                    + "respecter la décision de maintien forcé, surveiller "
                    + "l'échéance fixée par le juge et préparer le partage à "
                    + "l'expiration.");
            case DISPOSITIF_MEDIATION -> msg.add("Recommandation : médiation "
                    + "familiale obligatoirement mentionnée dans toute "
                    + "future assignation (art. 1108 CPC). Préparer un "
                    + "rendez-vous notarial commun pour clarifier les "
                    + "comptes d'indivision.");
            case DISPOSITIF_PARTAGE_AMIABLE -> msg.add("Recommandation : "
                    + "partage amiable (art. 835 Cciv). Acte notarié "
                    + "obligatoire si bien immobilier indivis. Frais d'acte "
                    + "réduits 1,1 % de la valeur (et non 4 %). Accord "
                    + "unanime requis.");
            case DISPOSITIF_PARTAGE_JUDICIAIRE -> msg.add("Recommandation : "
                    + "saisir le tribunal judiciaire en partage (art. 1364 "
                    + "CPC). Voir l'outil partage judiciaire (F-FA-22) "
                    + "pour évaluer l'éligibilité fine et la stratégie de "
                    + "licitation éventuelle.");
            default -> { /* no-op */ }
        }

        if (indemniteDue) {
            msg.add("Indemnité d'occupation due par l'héritier occupant "
                    + "(art. 815-9 al. 2 Cciv) — taux conventionnel 4 % "
                    + "annuel sur la valeur du bien occupé, à proportion "
                    + "des quote-parts des autres héritiers. Le notaire ou "
                    + "le juge peut retenir un taux différent (3 à 5 %) "
                    + "selon expertise locative.");
        }
        if (occupationExclusive && consentementsTous) {
            msg.add("Occupation exclusive consentie par tous les héritiers : "
                    + "la perception de l'indemnité peut être renoncée par "
                    + "convention écrite — sécuriser cet accord par acte sous "
                    + "seing privé ou acte notarié.");
        }
        if (actesContestes) {
            msg.add("Actes d'administration contestés : examiner si une "
                    + "autorisation judiciaire (art. 815-5 Cciv — passe outre "
                    + "le refus d'un indivisaire en cas de péril) est "
                    + "opportune. Documenter la chronologie des contestations.");
        }
        if (!demandePartage) {
            msg.add("Aucun héritier ne demande le partage : la situation "
                    + "peut perdurer mais reste fragile — chaque héritier "
                    + "peut à tout moment provoquer le partage (art. 815). "
                    + "Anticiper en formalisant la gestion.");
        }

        msg.add("Outil indicatif : la qualification définitive (régime, "
                + "indemnité d'occupation chiffrée, partage en nature, "
                + "attribution préférentielle art. 831-832 Cciv) relève du "
                + "notaire chargé du règlement de la succession ou du juge.");
        return msg;
    }

    private static String buildFormule(int score,
                                       String verdict,
                                       String dispositif,
                                       int dureeMois,
                                       boolean indemniteDue,
                                       BigDecimal indemnite,
                                       BigDecimal frais) {
        StringBuilder sb = new StringBuilder();
        sb.append("Durée indivision ").append(dureeMois).append(" mois. ");
        sb.append("Score conflictualité ").append(score).append("/100. ");
        sb.append("Verdict ").append(verdict).append(" → ").append(dispositif).append(".");
        if (indemniteDue) {
            sb.append(" Indemnité d'occupation (art. 815-9 al. 2) = ")
                    .append(indemnite.toPlainString()).append(" €.");
        }
        sb.append(" Frais de gestion estimés ")
                .append(frais.toPlainString()).append(" €.");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    private static void validate(LocalDate dateOuvertureSuccession,
                                 String typeIndivision,
                                 int nbHeritiers,
                                 BigDecimal valeurPatrimoineIndivisEur,
                                 BigDecimal valeurBienOccupeEur) {
        if (dateOuvertureSuccession == null) {
            throw new IllegalArgumentException("dateOuvertureSuccession est requise");
        }
        if (dateOuvertureSuccession.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "dateOuvertureSuccession ne peut être dans le futur");
        }
        if (typeIndivision == null || !TYPES_INDIVISION.contains(typeIndivision)) {
            throw new IllegalArgumentException(
                    "typeIndivision : valeur invalide : " + typeIndivision
                            + ". Valeurs : " + TYPES_INDIVISION);
        }
        if (nbHeritiers < 2 || nbHeritiers > 50) {
            throw new IllegalArgumentException(
                    "nbHeritiers doit être entre 2 et 50");
        }
        if (valeurPatrimoineIndivisEur == null
                || valeurPatrimoineIndivisEur.signum() < 0) {
            throw new IllegalArgumentException(
                    "valeurPatrimoineIndivisEur ne peut être négative");
        }
        if (valeurBienOccupeEur == null || valeurBienOccupeEur.signum() < 0) {
            throw new IllegalArgumentException(
                    "valeurBienOccupeEur ne peut être négative");
        }
        if (valeurBienOccupeEur.compareTo(valeurPatrimoineIndivisEur) > 0) {
            throw new IllegalArgumentException(
                    "valeurBienOccupeEur ne peut excéder valeurPatrimoineIndivisEur");
        }
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }
}
