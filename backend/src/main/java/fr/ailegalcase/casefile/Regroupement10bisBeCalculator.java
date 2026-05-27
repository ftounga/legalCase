package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-215-05 — calculateur d'éligibilité au regroupement familial des
 * ressortissants tiers en séjour <b>limité</b> (carte A) au sens de l'art. 10
 * et 10bis Loi du 15/12/1980 sur l'accès au territoire, le séjour,
 * l'établissement et l'éloignement des étrangers.
 *
 * <p>Sources :
 * <ul>
 *   <li>Loi du 15/12/1980 art. 10 et 10bis — conditions générales du regroupement familial
 *       du regroupant en séjour limité.</li>
 *   <li>AR du 17/05/2007 — fixation des conditions de ressources (120 % du revenu d'intégration
 *       sociale × 1,5 ≈ 1 495 € en 2024 — valeur arrondie à 1 500 €, paramétrable
 *       application.properties).
 *       <i>(à vérifier par avocat BE 2026 — révision annuelle)</i></li>
 *   <li>AR du 11/06/2018 — règles complémentaires sur la procédure et le retrait du titre.</li>
 * </ul>
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct du 40ter (regroupant Belge,
 * F-IM-14) et du 10ter (regroupant en séjour illimité, SF-215-03).
 *
 * <p>Scoring pondéré (40/20/20/10/10 = 100) — verdict 3 états :
 * ELIGIBLE ≥ 80, SOUS_RESERVE 50-79, INELIGIBLE &lt; 50.
 *
 * <p><b>Règle dure spécifique 10bis</b> : si {@code dateFinCarteA} est antérieure
 * à aujourd'hui (carte A expirée), le verdict est forcé à INELIGIBLE peu importe
 * le score pondéré — sans titre en cours de validité, le regroupement est
 * juridiquement impossible. Cette règle override le score.
 */
public final class Regroupement10bisBeCalculator {

    /**
     * Seuil de ressources mensuelles nettes (€) — alias de
     * {@link RegroupementBeConstants#SEUIL_RESSOURCES_MENSUEL} pour cohérence des tests.
     */
    public static final int SEUIL_RESSOURCES_DEFAUT = RegroupementBeConstants.SEUIL_RESSOURCES_MENSUEL;

    /** Durée minimale de séjour ininterrompu du regroupant en mois (art. 10bis — 12 mois). */
    public static final int DUREE_SEJOUR_MIN_MOIS = RegroupementBeConstants.DUREE_SEJOUR_MINIMALE_MOIS;

    /** Pondération du critère ressources (le plus discriminant). */
    static final int POIDS_RESSOURCES = 40;

    /** Pondération du critère durée de séjour. */
    static final int POIDS_DUREE = 20;

    /** Pondération du critère logement conforme. */
    static final int POIDS_LOGEMENT = 20;

    /** Pondération du critère assurance maladie. */
    static final int POIDS_ASSURANCE = 10;

    /** Pondération du critère ordre public. */
    static final int POIDS_ORDRE_PUBLIC = 10;

    /** Score seuil ELIGIBLE (inclusif). */
    static final int SEUIL_ELIGIBLE = 80;

    /** Score seuil SOUS_RESERVE (inclusif). */
    static final int SEUIL_SOUS_RESERVE = 50;

    private static final String BASE_JURIDIQUE =
            "Loi du 15/12/1980 art. 10 et 10bis (séjour limité ressortissant tiers) + "
                    + "AR du 17/05/2007 + AR du 11/06/2018";

    private Regroupement10bisBeCalculator() {
    }

    /** Surcharge utilisant le seuil de ressources par défaut (1 500 €) et l'horloge système. */
    public static Regroupement10bisBeResult compute(Regroupement10bisBeLienFamilialEnum lienFamilial,
                                                    Regroupement10bisBeTypeCarteEnum typeCarteRegroupant,
                                                    Integer revenusMensuelsNetsRegroupant,
                                                    Integer dureeSejour,
                                                    LocalDate dateFinCarteA,
                                                    Boolean logementConforme,
                                                    Boolean assuranceMaladie,
                                                    Boolean menaceOrdrePublic) {
        return compute(lienFamilial, typeCarteRegroupant, revenusMensuelsNetsRegroupant,
                dureeSejour, dateFinCarteA, logementConforme, assuranceMaladie, menaceOrdrePublic,
                SEUIL_RESSOURCES_DEFAUT, LocalDate.now());
    }

    /** Surcharge avec seuil de ressources injecté — utilise l'horloge système. */
    public static Regroupement10bisBeResult compute(Regroupement10bisBeLienFamilialEnum lienFamilial,
                                                    Regroupement10bisBeTypeCarteEnum typeCarteRegroupant,
                                                    Integer revenusMensuelsNetsRegroupant,
                                                    Integer dureeSejour,
                                                    LocalDate dateFinCarteA,
                                                    Boolean logementConforme,
                                                    Boolean assuranceMaladie,
                                                    Boolean menaceOrdrePublic,
                                                    int seuilRessources) {
        return compute(lienFamilial, typeCarteRegroupant, revenusMensuelsNetsRegroupant,
                dureeSejour, dateFinCarteA, logementConforme, assuranceMaladie, menaceOrdrePublic,
                seuilRessources, LocalDate.now());
    }

    /**
     * Calcule l'éligibilité au regroupement 10bis avec seuil de ressources et date du jour
     * injectés (testabilité).
     *
     * @param seuilRessources seuil (€) — typiquement {@value #SEUIL_RESSOURCES_DEFAUT}.
     * @param today date à utiliser pour le test de validité de la carte A (injectable).
     */
    public static Regroupement10bisBeResult compute(Regroupement10bisBeLienFamilialEnum lienFamilial,
                                                    Regroupement10bisBeTypeCarteEnum typeCarteRegroupant,
                                                    Integer revenusMensuelsNetsRegroupant,
                                                    Integer dureeSejour,
                                                    LocalDate dateFinCarteA,
                                                    Boolean logementConforme,
                                                    Boolean assuranceMaladie,
                                                    Boolean menaceOrdrePublic,
                                                    int seuilRessources,
                                                    LocalDate today) {
        validate(lienFamilial, typeCarteRegroupant, revenusMensuelsNetsRegroupant, dureeSejour,
                dateFinCarteA, logementConforme, assuranceMaladie, menaceOrdrePublic, seuilRessources,
                today);

        int revenus = revenusMensuelsNetsRegroupant;
        int duree = dureeSejour;

        boolean conditionTitreEnCours = !dateFinCarteA.isBefore(today);
        boolean conditionRessources = revenus >= seuilRessources;
        boolean conditionDuree = duree >= DUREE_SEJOUR_MIN_MOIS;
        boolean conditionLogement = logementConforme;
        boolean conditionAssurance = assuranceMaladie;
        boolean conditionOrdrePublic = !menaceOrdrePublic;

        int score = 0;
        if (conditionRessources) score += POIDS_RESSOURCES;
        if (conditionDuree) score += POIDS_DUREE;
        if (conditionLogement) score += POIDS_LOGEMENT;
        if (conditionAssurance) score += POIDS_ASSURANCE;
        if (conditionOrdrePublic) score += POIDS_ORDRE_PUBLIC;

        Regroupement10bisBeVerdict verdict;
        if (!conditionTitreEnCours) {
            // Règle dure 10bis : carte A expirée → impossible juridiquement
            // même si toutes les autres conditions sont remplies.
            verdict = Regroupement10bisBeVerdict.INELIGIBLE;
        } else if (score >= SEUIL_ELIGIBLE) {
            verdict = Regroupement10bisBeVerdict.ELIGIBLE;
        } else if (score >= SEUIL_SOUS_RESERVE) {
            verdict = Regroupement10bisBeVerdict.SOUS_RESERVE;
        } else {
            verdict = Regroupement10bisBeVerdict.INELIGIBLE;
        }

        List<String> criteresNonRemplis = new ArrayList<>();
        if (!conditionTitreEnCours) {
            criteresNonRemplis.add("Carte A expirée le " + dateFinCarteA
                    + " — regroupement impossible sans titre A en cours de validité");
        }
        if (!conditionRessources) {
            criteresNonRemplis.add("Revenus mensuels nets < " + seuilRessources + " €/mois");
        }
        if (!conditionDuree) {
            criteresNonRemplis.add("Durée de séjour < " + DUREE_SEJOUR_MIN_MOIS + " mois ininterrompus");
        }
        if (!conditionLogement) {
            criteresNonRemplis.add("Logement non conforme aux normes de salubrité/superficie");
        }
        if (!conditionAssurance) {
            criteresNonRemplis.add("Assurance maladie non couverte");
        }
        if (!conditionOrdrePublic) {
            criteresNonRemplis.add("Menace pour l'ordre public");
        }

        int differentielRevenus = revenus - seuilRessources;

        return new Regroupement10bisBeResult(
                lienFamilial,
                typeCarteRegroupant,
                revenus,
                duree,
                dateFinCarteA,
                conditionTitreEnCours,
                logementConforme,
                assuranceMaladie,
                menaceOrdrePublic,
                seuilRessources,
                differentielRevenus,
                score,
                verdict,
                Collections.unmodifiableList(criteresNonRemplis),
                BASE_JURIDIQUE
        );
    }

    private static void validate(Regroupement10bisBeLienFamilialEnum lienFamilial,
                                 Regroupement10bisBeTypeCarteEnum typeCarteRegroupant,
                                 Integer revenusMensuelsNetsRegroupant,
                                 Integer dureeSejour,
                                 LocalDate dateFinCarteA,
                                 Boolean logementConforme,
                                 Boolean assuranceMaladie,
                                 Boolean menaceOrdrePublic,
                                 int seuilRessources,
                                 LocalDate today) {
        if (lienFamilial == null) {
            throw new IllegalArgumentException("lienFamilial est requis");
        }
        if (typeCarteRegroupant == null) {
            throw new IllegalArgumentException("typeCarteRegroupant est requis");
        }
        if (revenusMensuelsNetsRegroupant == null) {
            throw new IllegalArgumentException("revenusMensuelsNetsRegroupant est requis");
        }
        if (revenusMensuelsNetsRegroupant < 0 || revenusMensuelsNetsRegroupant > 100_000) {
            throw new IllegalArgumentException(
                    "revenusMensuelsNetsRegroupant doit être entre 0 et 100 000");
        }
        if (dureeSejour == null) {
            throw new IllegalArgumentException("dureeSejour est requis");
        }
        if (dureeSejour < 0 || dureeSejour > 600) {
            throw new IllegalArgumentException("dureeSejour doit être entre 0 et 600 mois");
        }
        if (dateFinCarteA == null) {
            throw new IllegalArgumentException("dateFinCarteA est requise");
        }
        if (logementConforme == null) {
            throw new IllegalArgumentException("logementConforme est requis");
        }
        if (assuranceMaladie == null) {
            throw new IllegalArgumentException("assuranceMaladie est requis");
        }
        if (menaceOrdrePublic == null) {
            throw new IllegalArgumentException("menaceOrdrePublic est requis");
        }
        if (seuilRessources < 0) {
            throw new IllegalArgumentException("seuilRessources doit être positif ou nul");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
    }
}
