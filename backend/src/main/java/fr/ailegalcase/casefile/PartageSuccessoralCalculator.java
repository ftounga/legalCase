package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-24-09 : calculateur d'analyse de la modalité de partage successoral
 * (FR — art. 815-840 Cciv + 1364 CPC).
 *
 * <p>Trois modalités possibles :</p>
 * <ul>
 *   <li><strong>PARTAGE_AMIABLE</strong> (art. 835) — entre tous les héritiers
 *       consentants ; acte sous seing privé sauf si immeubles → notaire obligatoire.</li>
 *   <li><strong>PARTAGE_JUDICIAIRE</strong> (art. 840 + 1364 CPC) — désaccord
 *       persistant ou héritier protégé ; TJ ordonne avec expertise notariale.</li>
 *   <li><strong>PARTAGE_PARTIEL</strong> (art. 838) — sur seulement certains biens,
 *       le reste demeure en indivision.</li>
 * </ul>
 *
 * <p>Bascule possible : si {@code modePartageDemande = PARTAGE_AMIABLE} mais
 * {@code consentementsTous = false} ou {@code desaccordPersistant = true},
 * le {@code modeRecommande} bascule vers {@code PARTAGE_JUDICIAIRE}.</p>
 *
 * <p>Outil <strong>single-country FRANCE</strong> — l'équivalent BE
 * (CJ art. 1207 et s., partage devant juge de paix) suit un régime distinct
 * et fait l'objet d'une feature jumelle au backlog.</p>
 */
public final class PartageSuccessoralCalculator {

    /** Modalité de partage demandée par l'avocat. */
    public enum ModePartage {
        /** Partage amiable entre tous les héritiers consentants (art. 835). */
        PARTAGE_AMIABLE,
        /** Partage judiciaire ordonné par le TJ (art. 840 + 1364 CPC). */
        PARTAGE_JUDICIAIRE,
        /** Partage partiel sur certains biens (art. 838) — reste en indivision. */
        PARTAGE_PARTIEL
    }

    /** Verdict de recevabilité de l'analyse. */
    public enum VerdictRecevabilite {
        ELEVEE,
        MOYENNE,
        FAIBLE
    }

    /** Frais 1 % pour partage amiable simple. */
    private static final double FRAIS_AMIABLE_SIMPLE_PCT = 0.01;

    /** Frais 2 % pour partage amiable avec immeubles (notaire). */
    private static final double FRAIS_AMIABLE_NOTAIRE_PCT = 0.02;

    /** Frais 3 % pour partage judiciaire avec immeubles. */
    private static final double FRAIS_JUDICIAIRE_PCT = 0.03;

    /** Délai amiable (mois). */
    private static final int DELAI_AMIABLE_MOIS = 3;

    /** Délai partiel (mois). */
    private static final int DELAI_PARTIEL_MOIS = 4;

    /** Délai judiciaire min (mois). */
    private static final int DELAI_JUDICIAIRE_MIN_MOIS = 6;

    /** Délai judiciaire max (mois). */
    private static final int DELAI_JUDICIAIRE_MAX_MOIS = 18;

    /** Base juridique consolidée. */
    private static final String BASE_JURIDIQUE =
            "Art. 815-840 Cciv + 1364 CPC";

    private PartageSuccessoralCalculator() {}

    /**
     * Évalue la modalité de partage successoral applicable.
     *
     * @param modePartageDemande   modalité demandée par l'avocat (obligatoire)
     * @param nombreCoheritiers    nombre de cohéritiers (≥ 2 obligatoire)
     * @param consentementsTous    tous les héritiers consentent (pour amiable)
     * @param presenceImmeubles    présence d'immeubles dans la masse
     * @param accordsValuation     accord sur les évaluations
     * @param desaccordPersistant  désaccord persistant (déclenche judiciaire)
     * @param dateDeces            date du décès (≤ aujourd'hui obligatoire)
     * @param valeurMasseEur       valeur estimée de la masse successorale (≥ 0, optionnel)
     * @param country              pays du workspace (FRANCE attendu)
     * @return résultat structuré
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static PartageSuccessoralResult compute(ModePartage modePartageDemande,
                                                    Integer nombreCoheritiers,
                                                    Boolean consentementsTous,
                                                    Boolean presenceImmeubles,
                                                    Boolean accordsValuation,
                                                    Boolean desaccordPersistant,
                                                    LocalDate dateDeces,
                                                    Double valeurMasseEur,
                                                    String country) {
        if (modePartageDemande == null) {
            throw new IllegalArgumentException("Modalité de partage demandée requise");
        }
        if (nombreCoheritiers == null) {
            throw new IllegalArgumentException("Nombre de cohéritiers requis");
        }
        if (nombreCoheritiers < 2) {
            throw new IllegalArgumentException(
                    "Le nombre de cohéritiers doit être ≥ 2 (partage sans objet sinon)");
        }
        if (consentementsTous == null) {
            throw new IllegalArgumentException("Consentement de tous les héritiers (oui/non) requis");
        }
        if (presenceImmeubles == null) {
            throw new IllegalArgumentException("Présence d'immeubles (oui/non) requise");
        }
        if (accordsValuation == null) {
            throw new IllegalArgumentException("Accord sur les évaluations (oui/non) requis");
        }
        if (desaccordPersistant == null) {
            throw new IllegalArgumentException("Désaccord persistant (oui/non) requis");
        }
        if (dateDeces == null) {
            throw new IllegalArgumentException("Date du décès requise");
        }
        if (dateDeces.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date du décès ne peut pas être dans le futur");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CJ art. 1207 et s., partage"
                            + " devant juge de paix) sera traité dans une feature"
                            + " jumelle dédiée.");
        }
        double valeurMasse = (valeurMasseEur == null) ? 0.0 : valeurMasseEur;
        if (valeurMasse < 0) {
            throw new IllegalArgumentException("Valeur de la masse doit être ≥ 0");
        }

        // ----- Bascule de mode -----
        ModePartage modeRecommande = modePartageDemande;
        boolean bascule = false;
        if (modePartageDemande == ModePartage.PARTAGE_AMIABLE
                && (!consentementsTous || desaccordPersistant)) {
            modeRecommande = ModePartage.PARTAGE_JUDICIAIRE;
            bascule = true;
        }

        // ----- Risque licitation -----
        boolean risqueLicitation = presenceImmeubles && desaccordPersistant;

        // ----- Score (0 à 100) -----
        int score = 0;
        if (consentementsTous) score += 30;
        if (accordsValuation) score += 20;
        if (!desaccordPersistant) score += 25;
        if (nombreCoheritiers >= 2) score += 10;
        if (modeRecommande == modePartageDemande) score += 15;

        // ----- Verdict -----
        // La bascule amiable→judiciaire signale un frottement procédural
        // (procédure en deux temps, durée allongée, coût supérieur), donc
        // verdict MOYENNE même si le judiciaire final + désaccord persistant
        // serait classé ELEVEE pour un partage demandé directement en
        // judiciaire. La précédence `bascule → MOYENNE` doit donc précéder
        // `JUDICIAIRE && desaccord → ELEVEE`.
        VerdictRecevabilite verdict;
        if (modeRecommande == ModePartage.PARTAGE_AMIABLE
                && consentementsTous && !desaccordPersistant) {
            verdict = VerdictRecevabilite.ELEVEE;
        } else if (bascule || modePartageDemande == ModePartage.PARTAGE_PARTIEL) {
            verdict = VerdictRecevabilite.MOYENNE;
        } else if (modeRecommande == ModePartage.PARTAGE_JUDICIAIRE
                && desaccordPersistant) {
            verdict = VerdictRecevabilite.ELEVEE;
        } else if (score >= 70) {
            verdict = VerdictRecevabilite.MOYENNE;
        } else {
            verdict = VerdictRecevabilite.FAIBLE;
        }

        // ----- Délai instruction -----
        int delaiMois;
        switch (modeRecommande) {
            case PARTAGE_AMIABLE -> delaiMois = DELAI_AMIABLE_MOIS;
            case PARTAGE_PARTIEL -> delaiMois = DELAI_PARTIEL_MOIS;
            case PARTAGE_JUDICIAIRE -> {
                if (presenceImmeubles && desaccordPersistant) {
                    delaiMois = DELAI_JUDICIAIRE_MAX_MOIS;
                } else if (presenceImmeubles || desaccordPersistant) {
                    delaiMois = (DELAI_JUDICIAIRE_MIN_MOIS + DELAI_JUDICIAIRE_MAX_MOIS) / 2;
                } else {
                    delaiMois = DELAI_JUDICIAIRE_MIN_MOIS;
                }
            }
            default -> delaiMois = DELAI_AMIABLE_MOIS;
        }

        // ----- Frais estimés (% des biens) -----
        double fraisPct;
        if (modeRecommande == ModePartage.PARTAGE_JUDICIAIRE) {
            fraisPct = FRAIS_JUDICIAIRE_PCT;
        } else if (presenceImmeubles) {
            fraisPct = FRAIS_AMIABLE_NOTAIRE_PCT;
        } else {
            fraisPct = FRAIS_AMIABLE_SIMPLE_PCT;
        }
        double fraisEur = valeurMasse * fraisPct;

        String formule = String.format(Locale.ROOT,
                "Mode demandé %s + %d cohéritiers + consentements=%s + immeubles=%s "
                        + "+ accords valuation=%s + désaccord persistant=%s "
                        + "→ mode recommandé %s%s + verdict %s + délai %d mois "
                        + "+ frais %.2f%% (%.2f €) + licitation=%s",
                modePartageDemande.name(), nombreCoheritiers,
                consentementsTous, presenceImmeubles, accordsValuation, desaccordPersistant,
                modeRecommande.name(), bascule ? " (bascule)" : "",
                verdict.name(), delaiMois, fraisPct * 100.0, fraisEur, risqueLicitation);

        List<String> messages = buildMessages(modePartageDemande, modeRecommande, bascule,
                consentementsTous, presenceImmeubles, accordsValuation,
                desaccordPersistant, verdict, risqueLicitation, delaiMois, fraisPct, fraisEur);

        return new PartageSuccessoralResult(
                modePartageDemande,
                nombreCoheritiers,
                consentementsTous,
                presenceImmeubles,
                accordsValuation,
                desaccordPersistant,
                dateDeces,
                valeurMasse,
                countryNormalized,
                verdict,
                modeRecommande,
                bascule,
                score,
                delaiMois,
                fraisPct,
                fraisEur,
                risqueLicitation,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static List<String> buildMessages(ModePartage demande,
                                              ModePartage recommande,
                                              boolean bascule,
                                              boolean consentementsTous,
                                              boolean presenceImmeubles,
                                              boolean accordsValuation,
                                              boolean desaccordPersistant,
                                              VerdictRecevabilite verdict,
                                              boolean risqueLicitation,
                                              int delaiMois,
                                              double fraisPct,
                                              double fraisEur) {
        List<String> msgs = new ArrayList<>();

        msgs.add("Modalité demandée : " + libelleMode(demande) + ".");

        if (bascule) {
            msgs.add("BASCULE : la modalité amiable n'est pas viable "
                    + "(consentements partiels ou désaccord persistant). "
                    + "Le mode recommandé bascule vers PARTAGE_JUDICIAIRE "
                    + "(art. 840 Cciv + 1364 CPC).");
        } else {
            msgs.add("Mode recommandé : " + libelleMode(recommande) + ".");
        }

        if (recommande == ModePartage.PARTAGE_AMIABLE) {
            msgs.add("Partage amiable (art. 835 Cciv) — accord de tous les héritiers requis.");
            if (presenceImmeubles) {
                msgs.add("Présence d'immeubles — l'acte de partage doit être passé en la forme "
                        + "AUTHENTIQUE devant notaire (art. 835 al. 2 Cciv).");
            } else {
                msgs.add("Pas d'immeubles — l'acte sous seing privé est suffisant.");
            }
        } else if (recommande == ModePartage.PARTAGE_PARTIEL) {
            msgs.add("Partage partiel (art. 838 Cciv) — n'opère que sur les biens visés. "
                    + "Le reste demeure en INDIVISION et pourra faire l'objet d'un partage "
                    + "ultérieur sans nouvelle ouverture de succession.");
        } else { // PARTAGE_JUDICIAIRE
            msgs.add("Partage judiciaire (art. 840 Cciv + 1364 CPC) — saisine du tribunal "
                    + "judiciaire du dernier domicile du défunt avec expertise notariale.");
        }

        if (!consentementsTous && demande == ModePartage.PARTAGE_AMIABLE) {
            msgs.add("Tous les cohéritiers ne consentent PAS — la voie amiable (art. 835) "
                    + "est exclue, voie judiciaire imposée (art. 840).");
        }

        if (!accordsValuation) {
            msgs.add("Accord sur les évaluations NON acquis — risque d'expertise judiciaire "
                    + "des biens et allongement de la procédure.");
        }

        if (desaccordPersistant) {
            msgs.add("Désaccord persistant entre cohéritiers — voie judiciaire imposée.");
        }

        if (risqueLicitation) {
            msgs.add("⚠ Risque de LICITATION (vente aux enchères judiciaire) — bien immobilier "
                    + "indivisible et désaccord persistant. Examiner une demande "
                    + "d'attribution préférentielle (art. 831, 832-1 Cciv) si applicable.");
        }

        switch (verdict) {
            case ELEVEE -> msgs.add("VERDICT ELEVEE — la modalité recommandée est cohérente "
                    + "avec les critères saisis. Démarrage de la procédure conseillé.");
            case MOYENNE -> msgs.add("VERDICT MOYENNE — la procédure est viable mais comporte "
                    + "des facteurs d'allongement (bascule de mode, partage partiel, "
                    + "ou critères partiels).");
            case FAIBLE -> msgs.add("VERDICT FAIBLE — préalables non remplis. Documenter "
                    + "les consentements et les accords d'évaluation avant toute saisine.");
        }

        msgs.add(String.format(Locale.ROOT,
                "Délai d'instruction estimé : %d mois.", delaiMois));

        msgs.add(String.format(Locale.ROOT,
                "Frais estimés : %.2f%% des biens (%.2f €) — provision notariale + frais "
                        + "expertise selon mode.", fraisPct * 100.0, fraisEur));

        msgs.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return msgs;
    }

    private static String libelleMode(ModePartage m) {
        return switch (m) {
            case PARTAGE_AMIABLE -> "Partage amiable (art. 835 Cciv)";
            case PARTAGE_JUDICIAIRE -> "Partage judiciaire (art. 840 Cciv + 1364 CPC)";
            case PARTAGE_PARTIEL -> "Partage partiel (art. 838 Cciv)";
        };
    }
}
