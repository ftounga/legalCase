package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-222-02 : analyseur statique d'éligibilité au Téléphone Grave Danger (TGD),
 * art. 41-3-1 CPP, Famille FRANCE uniquement.
 *
 * <p>Critères d'éligibilité (tous requis) :</p>
 * <ol>
 *   <li>danger grave caractérisé ;</li>
 *   <li>violences avérées ou vraisemblables ;</li>
 *   <li>interdiction de contact issue d'une procédure (ordonnance de protection,
 *       contrôle judiciaire, SME, condamnation) ;</li>
 *   <li>non-cohabitation auteur / victime ;</li>
 *   <li>consentement exprès de la victime.</li>
 * </ol>
 *
 * <p>Verdict (3 niveaux) :</p>
 * <ul>
 *   <li><b>ELIGIBLE_TGD</b> : tous les critères réunis — solliciter le TGD auprès
 *       du parquet.</li>
 *   <li><b>ELIGIBLE_SOUS_RESERVE</b> : seul le critère « interdiction de contact »
 *       manque, tous les autres critères durs étant remplis — conseiller de faire
 *       prononcer d'abord l'interdiction (ex. ordonnance de protection F-FA-14).</li>
 *   <li><b>NON_ELIGIBLE</b> : au moins un critère dur autre que l'interdiction de
 *       contact manque.</li>
 * </ul>
 *
 * <p>L'outil <b>conseille</b> l'avocat ; l'<b>attribution</b> du TGD relève du
 * procureur de la République (art. 41-3-1 CPP).</p>
 *
 * <p>Anti-doublon F-FA-14 (ordonnance de protection) : cet analyseur ne refait
 * pas la recommandation de mesures de protection ; le TGD comme <i>mesure</i>
 * reste porté par F-FA-14 ({@code OrdonnanceProtectionMesureType.TGD}).</p>
 */
public final class TgdCalculator {

    static final String BASE_41_3_1 = "art. 41-3-1 CPP (dispositif de téléprotection grave danger — TGD)";
    static final String BASE_515_11 = "art. 515-11 Cciv (ordonnance de protection — interdiction de contact)";

    static final String CRIT_DANGER_GRAVE = "Danger grave caractérisé pour la victime";
    static final String CRIT_VIOLENCES = "Violences avérées ou vraisemblables";
    static final String CRIT_INTERDICTION = "Interdiction de contact issue d'une procédure (ordonnance de protection, contrôle judiciaire, SME ou condamnation)";
    static final String CRIT_NON_COHABITATION = "Absence de cohabitation entre l'auteur et la victime";
    static final String CRIT_CONSENTEMENT = "Consentement exprès de la victime au dispositif";

    static final String MSG_DECISION_PARQUET =
            "Décision d'attribution : le TGD est attribué par le procureur de la République (art. 41-3-1 CPP). "
                    + "Cet outil évalue l'éligibilité et conseille l'avocat ; il ne décide pas de l'attribution.";

    private TgdCalculator() {}

    /**
     * Analyse l'éligibilité au TGD.
     *
     * @param req     requête validée (gate pays vérifié par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat de l'analyse
     * @throws IllegalArgumentException si les pré-requis pays ne sont pas respectés
     */
    public static TgdResult compute(TgdRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-TGD applicable uniquement en France (art. 41-3-1 CPP).");
        }
        if (req == null) {
            throw new IllegalArgumentException("Requête TGD manquante.");
        }

        boolean dangerGrave = Boolean.TRUE.equals(req.dangerGrave());
        boolean violences = Boolean.TRUE.equals(req.violencesAvereesOuVraisemblables());
        boolean interdiction = Boolean.TRUE.equals(req.interdictionContactProcedure());
        boolean nonCohabitation = Boolean.TRUE.equals(req.nonCohabitation());
        boolean consentement = Boolean.TRUE.equals(req.consentementVictime());

        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        List<String> manquants = new ArrayList<>();
        bases.add(BASE_41_3_1);

        if (!dangerGrave) manquants.add(CRIT_DANGER_GRAVE);
        if (!violences) manquants.add(CRIT_VIOLENCES);
        if (!interdiction) manquants.add(CRIT_INTERDICTION);
        if (!nonCohabitation) manquants.add(CRIT_NON_COHABITATION);
        if (!consentement) manquants.add(CRIT_CONSENTEMENT);

        // Cas 1 : tous les critères réunis → éligible.
        if (manquants.isEmpty()) {
            messages.add("Tous les critères d'éligibilité au TGD sont réunis : danger grave, violences "
                    + "avérées ou vraisemblables, interdiction de contact, non-cohabitation et consentement "
                    + "de la victime. Solliciter l'attribution du TGD auprès du procureur de la République.");
            messages.add(MSG_DECISION_PARQUET);
            return new TgdResult(VerdictTgdEnum.ELIGIBLE_TGD, manquants, bases, messages);
        }

        // Cas 2 : seul l'interdiction de contact manque (tous les autres critères durs OK)
        //         → éligible sous réserve, renvoi vers F-FA-14.
        boolean seuleInterdictionManque = !interdiction
                && dangerGrave && violences && nonCohabitation && consentement;
        if (seuleInterdictionManque) {
            bases.add(BASE_515_11);
            messages.add("Tous les critères sont réunis hormis l'interdiction de contact, qui n'est pas encore "
                    + "prononcée. Faire d'abord prononcer une interdiction de contact (ex. ordonnance de "
                    + "protection — outil F-FA-14, art. 515-11 Cciv), puis solliciter le TGD.");
            messages.add(MSG_DECISION_PARQUET);
            return new TgdResult(VerdictTgdEnum.ELIGIBLE_SOUS_RESERVE, manquants, bases, messages);
        }

        // Cas 3 : un critère dur (autre que l'interdiction de contact) manque → non éligible.
        messages.add("L'éligibilité au TGD n'est pas réunie en l'état : "
                + manquants.size() + " critère(s) manquant(s). Voir le détail des critères manquants.");
        messages.add(MSG_DECISION_PARQUET);
        return new TgdResult(VerdictTgdEnum.NON_ELIGIBLE, manquants, bases, messages);
    }
}
