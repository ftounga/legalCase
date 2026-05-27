package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-215-03 — calculateur d'éligibilité au regroupement familial des
 * ressortissants tiers en séjour illimité (carte B ou C) au sens de l'art. 10
 * et 10ter Loi du 15/12/1980 sur l'accès au territoire, le séjour,
 * l'établissement et l'éloignement des étrangers.
 *
 * <p>Sources :
 * <ul>
 *   <li>Loi du 15/12/1980 art. 10 et 10ter — conditions générales du regroupement familial.</li>
 *   <li>AR du 17/05/2007 — fixation des conditions de ressources (120 % du revenu d'intégration
 *       sociale × 1,5 ≈ 1 495 € en 2024 — valeur arrondie à 1 500 €, paramétrable application.properties).
 *       <i>(à vérifier par avocat BE 2026 — révision annuelle)</i></li>
 *   <li>AR du 11/06/2018 — règles complémentaires sur la procédure et le retrait du titre.</li>
 * </ul>
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct du 40ter (regroupant Belge,
 * F-IM-14) et du 10bis (regroupant en séjour limité, SF-215-05).
 *
 * <p>Scoring pondéré (40/20/20/10/10 = 100) — verdict 3 états :
 * ELIGIBLE ≥ 80, SOUS_RESERVE 50-79, INELIGIBLE &lt; 50.
 */
public final class Regroupement10terBeCalculator {

    /**
     * Seuil de ressources mensuelles nettes (€) — AR 17/05/2007 + AR 11/06/2018.
     * Référence : 120 % × revenu d'intégration sociale (RIS) × 1,5 ≈ 1 495 €.
     * Valeur arrondie 1 500 € — paramétrable application.properties.
     * <i>(à vérifier par avocat BE 2026 — révision annuelle indexation pivot)</i>
     */
    public static final int SEUIL_RESSOURCES_DEFAUT = 1_500;

    /** Durée minimale de séjour ininterrompu du regroupant en mois (art. 10ter — 12 mois). */
    public static final int DUREE_SEJOUR_MIN_MOIS = 12;

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
            "Loi du 15/12/1980 art. 10 et 10ter (séjour des étrangers) + "
                    + "AR du 17/05/2007 (conditions ressources/logement/assurance) + "
                    + "AR du 11/06/2018";

    private Regroupement10terBeCalculator() {
    }

    /** Surcharge utilisant le seuil de ressources par défaut (1 500 €). */
    public static Regroupement10terBeResult compute(Regroupement10terBeLienFamilialEnum lienFamilial,
                                                    Regroupement10terBeTypeCarteEnum typeCarteRegroupant,
                                                    Integer revenusMensuelsNetsRegroupant,
                                                    Integer dureeSejour,
                                                    Boolean logementConforme,
                                                    Boolean assuranceMaladie,
                                                    Boolean menaceOrdrePublic) {
        return compute(lienFamilial, typeCarteRegroupant, revenusMensuelsNetsRegroupant,
                dureeSejour, logementConforme, assuranceMaladie, menaceOrdrePublic,
                SEUIL_RESSOURCES_DEFAUT);
    }

    /**
     * Calcule l'éligibilité au regroupement 10ter avec seuil de ressources injecté.
     *
     * @param seuilRessources seuil (€) — typiquement {@value #SEUIL_RESSOURCES_DEFAUT}.
     */
    public static Regroupement10terBeResult compute(Regroupement10terBeLienFamilialEnum lienFamilial,
                                                    Regroupement10terBeTypeCarteEnum typeCarteRegroupant,
                                                    Integer revenusMensuelsNetsRegroupant,
                                                    Integer dureeSejour,
                                                    Boolean logementConforme,
                                                    Boolean assuranceMaladie,
                                                    Boolean menaceOrdrePublic,
                                                    int seuilRessources) {
        validate(lienFamilial, typeCarteRegroupant, revenusMensuelsNetsRegroupant, dureeSejour,
                logementConforme, assuranceMaladie, menaceOrdrePublic, seuilRessources);

        int revenus = revenusMensuelsNetsRegroupant;
        int duree = dureeSejour;

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

        Regroupement10terBeVerdict verdict;
        if (score >= SEUIL_ELIGIBLE) {
            verdict = Regroupement10terBeVerdict.ELIGIBLE;
        } else if (score >= SEUIL_SOUS_RESERVE) {
            verdict = Regroupement10terBeVerdict.SOUS_RESERVE;
        } else {
            verdict = Regroupement10terBeVerdict.INELIGIBLE;
        }

        List<String> criteresNonRemplis = new ArrayList<>();
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

        return new Regroupement10terBeResult(
                lienFamilial,
                typeCarteRegroupant,
                revenus,
                duree,
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

    private static void validate(Regroupement10terBeLienFamilialEnum lienFamilial,
                                 Regroupement10terBeTypeCarteEnum typeCarteRegroupant,
                                 Integer revenusMensuelsNetsRegroupant,
                                 Integer dureeSejour,
                                 Boolean logementConforme,
                                 Boolean assuranceMaladie,
                                 Boolean menaceOrdrePublic,
                                 int seuilRessources) {
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
    }
}
