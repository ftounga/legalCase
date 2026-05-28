package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-215-11 — calculateur composite AESM + tutelle DGDE (MENA) BE — outil
 * F-IM-30-aesm-mena-be.
 *
 * <p>Sources :
 * <ul>
 *   <li><b>Volet tutelle DGDE</b> — Loi du 24/12/2002 programme art. 479+ (titre XIII
 *       ch. 6) refondue par la Loi du 04/05/2007 relative à la tutelle des mineurs
 *       étrangers non accompagnés (Service des Tutelles, SPF Justice) — délai
 *       théorique 30 jours dès signalement (<i>à vérifier par avocat BE</i>).</li>
 *   <li><b>Volet AESM</b> — Loi 15/12/1980 sur l'accès au territoire art. 9bis
 *       (adapté MENA) + Circulaire OE du 15/09/2005 sur l'Admission Exceptionnelle
 *       de Séjour pour Mineur (AESM) — scoring sur intégration scolaire + projet
 *       de vie + perspective d'autonomie + ordre public.</li>
 * </ul>
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT — mineurs</b> ({@code ageActuel < 18}). Distinct
 * de :
 * <ul>
 *   <li>F-IM-19 mineurs FR (MNA — ordonnance JE + L.435-3 CESEDA) ;</li>
 *   <li>F-IM-14-9bis-humanitaire-be (9bis adulte — faisceau présence/enracinement,
 *       sans condition de minorité ni de tutelle).</li>
 * </ul>
 *
 * <p><b>Volet AESM — scoring 0-100</b> :
 * <ul>
 *   <li>{@code integrationScolaire} : +40 pts ;</li>
 *   <li>{@code projetVieElabore} : +30 pts ;</li>
 *   <li>{@code perspectiveAutonomie} : +20 pts ;</li>
 *   <li>{@code !menaceOrdrePublic} : +10 pts ;</li>
 *   <li><b>bonus</b> {@code dureeScolaire ≥ 24 mois && integrationScolaire} : +5 pts
 *       (score capé à 100).</li>
 * </ul>
 *
 * <p><b>Verdict AESM</b> :
 * <ul>
 *   <li>score ≥ 70 → {@link AesmMenaBeVerdictAesm#FAVORABLE}</li>
 *   <li>50 ≤ score ≤ 69 → {@link AesmMenaBeVerdictAesm#SOUS_RESERVE}</li>
 *   <li>score &lt; 50 → {@link AesmMenaBeVerdictAesm#DEFAVORABLE}</li>
 * </ul>
 *
 * <p><b>Volet tutelle</b> : si {@code tuteurDesigne = false}, l'outil renseigne
 * {@code etapeTutelle} (action à réaliser auprès du Service des Tutelles SPF
 * Justice) et {@code delaiDesignationTuteur} = {@code dateArriveeBelgique + 30 jours}.
 * Sinon, {@code etapeTutelle = null} (le volet tutelle est satisfait).
 *
 * <p><b>Priorité urgence</b> : {@code ageActuel ≥ 17} — approche imminente de la
 * majorité, qui ferme la procédure MENA. Signale l'urgence procédurale au frontend.
 */
public final class AesmMenaBeCalculator {

    // ---------- Poids volet AESM ----------
    static final int POIDS_INTEGRATION_SCOLAIRE = 40;
    static final int POIDS_PROJET_VIE = 30;
    static final int POIDS_PERSPECTIVE_AUTONOMIE = 20;
    static final int POIDS_PAS_MENACE_ORDRE_PUBLIC = 10;
    /** Bonus accordé si {@code dureeScolaire ≥ 24 mois} ET {@code integrationScolaire}. */
    static final int BONUS_DUREE_SCOLAIRE = 5;
    static final int SCORE_MIN = 0;
    static final int SCORE_MAX = 100;
    static final int SEUIL_FAVORABLE = 70;
    static final int SEUIL_SOUS_RESERVE = 50;
    static final int DUREE_SCOLAIRE_BONUS_MOIS = 24;

    // ---------- Volet tutelle ----------
    /** Délai théorique signalement → désignation tuteur, art. 7 Loi 04/05/2007 (à vérifier). */
    static final int DELAI_DESIGNATION_TUTEUR_JOURS = 30;
    /** Seuil d'urgence procédurale (approche majorité) — pré-majorité 12 mois. */
    static final int AGE_URGENCE_MAJORITE = 17;
    /** Age maximum admis — mineurs uniquement. */
    static final int AGE_MAX_MINEUR_INCLUSIF = 17;

    private static final String PROCHAIN_ACTE =
            "Désignation tuteur DGDE → Entretien OE → Décision AESM (3-12 mois)";

    private static final String BASE_JURIDIQUE =
            "Loi 04/05/2007 (tutelle MENA) ; loi 15/12/1980 art. 9bis (adaptation MENA) ; "
                    + "circulaire OE 15/09/2005 (AESM)";

    private static final String ETAPE_TUTELLE_SIGNALEMENT =
            "Signaler au Service des Tutelles SPF Justice — art. 7 loi 04/05/2007 "
                    + "(délai 30 jours dès arrivée)";

    private AesmMenaBeCalculator() {
    }

    /**
     * Calcule le résultat composite (tutelle + AESM) pour un dossier MENA BE.
     *
     * @throws IllegalArgumentException si un paramètre est null, hors borne,
     *     {@code ageActuel ≥ 18}, ou {@code integrationScolaire = true} sans
     *     {@code dureeScolaire} renseignée.
     */
    public static AesmMenaBeResult compute(Integer ageActuel,
                                           LocalDate dateArriveeBelgique,
                                           Boolean tuteurDesigne,
                                           Boolean integrationScolaire,
                                           Integer dureeScolaire,
                                           Boolean projetVieElabore,
                                           Boolean perspectiveAutonomie,
                                           Boolean menaceOrdrePublic) {
        validate(ageActuel, dateArriveeBelgique, tuteurDesigne, integrationScolaire,
                dureeScolaire, projetVieElabore, perspectiveAutonomie, menaceOrdrePublic);

        // ---------- Volet tutelle ----------
        LocalDate delaiDesignationTuteur =
                dateArriveeBelgique.plusDays(DELAI_DESIGNATION_TUTEUR_JOURS);
        String etapeTutelle = tuteurDesigne ? null : ETAPE_TUTELLE_SIGNALEMENT;

        // ---------- Volet AESM — scoring ----------
        int scoreBase = 0;
        if (integrationScolaire) scoreBase += POIDS_INTEGRATION_SCOLAIRE;
        if (projetVieElabore) scoreBase += POIDS_PROJET_VIE;
        if (perspectiveAutonomie) scoreBase += POIDS_PERSPECTIVE_AUTONOMIE;
        if (!menaceOrdrePublic) scoreBase += POIDS_PAS_MENACE_ORDRE_PUBLIC;

        int bonus = 0;
        if (integrationScolaire
                && dureeScolaire != null
                && dureeScolaire >= DUREE_SCOLAIRE_BONUS_MOIS) {
            bonus = BONUS_DUREE_SCOLAIRE;
        }
        int score = Math.min(SCORE_MAX, scoreBase + bonus);

        AesmMenaBeVerdictAesm verdictAESM;
        if (score >= SEUIL_FAVORABLE) {
            verdictAESM = AesmMenaBeVerdictAesm.FAVORABLE;
        } else if (score >= SEUIL_SOUS_RESERVE) {
            verdictAESM = AesmMenaBeVerdictAesm.SOUS_RESERVE;
        } else {
            verdictAESM = AesmMenaBeVerdictAesm.DEFAVORABLE;
        }

        boolean prioriteUrgence = ageActuel >= AGE_URGENCE_MAJORITE;

        // ---------- Critères non remplis (humanisés pour avocat) ----------
        List<String> criteresNonRemplis = new ArrayList<>();
        if (!integrationScolaire) {
            criteresNonRemplis.add(
                    "Intégration scolaire absente (40 pts perdus)");
        }
        if (!projetVieElabore) {
            criteresNonRemplis.add(
                    "Projet de vie OE non formalisé (30 pts perdus)");
        }
        if (!perspectiveAutonomie) {
            criteresNonRemplis.add(
                    "Perspective d'autonomie non démontrée — emploi/formation "
                            + "(20 pts perdus)");
        }
        if (menaceOrdrePublic) {
            criteresNonRemplis.add(
                    "Menace pour l'ordre public — AESM refusée");
        }
        if (!tuteurDesigne) {
            criteresNonRemplis.add(
                    "Tuteur DGDE non désigné — bloque la procédure AESM");
        }

        return new AesmMenaBeResult(
                ageActuel,
                dateArriveeBelgique,
                tuteurDesigne,
                integrationScolaire,
                dureeScolaire,
                projetVieElabore,
                perspectiveAutonomie,
                menaceOrdrePublic,
                etapeTutelle,
                delaiDesignationTuteur,
                score,
                bonus,
                verdictAESM,
                prioriteUrgence,
                Collections.unmodifiableList(criteresNonRemplis),
                PROCHAIN_ACTE,
                BASE_JURIDIQUE
        );
    }

    private static void validate(Integer ageActuel,
                                 LocalDate dateArriveeBelgique,
                                 Boolean tuteurDesigne,
                                 Boolean integrationScolaire,
                                 Integer dureeScolaire,
                                 Boolean projetVieElabore,
                                 Boolean perspectiveAutonomie,
                                 Boolean menaceOrdrePublic) {
        if (ageActuel == null) {
            throw new IllegalArgumentException("ageActuel est requis");
        }
        if (ageActuel < 0) {
            throw new IllegalArgumentException("ageActuel doit être ≥ 0");
        }
        if (ageActuel > AGE_MAX_MINEUR_INCLUSIF) {
            throw new IllegalArgumentException(
                    "Outil réservé aux mineurs (< 18 ans)");
        }
        if (dateArriveeBelgique == null) {
            throw new IllegalArgumentException("dateArriveeBelgique est requis");
        }
        if (dateArriveeBelgique.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "dateArriveeBelgique ne peut être future");
        }
        if (tuteurDesigne == null) {
            throw new IllegalArgumentException("tuteurDesigne est requis");
        }
        if (integrationScolaire == null) {
            throw new IllegalArgumentException("integrationScolaire est requis");
        }
        if (integrationScolaire && dureeScolaire == null) {
            throw new IllegalArgumentException(
                    "Durée scolaire requise si intégration scolaire");
        }
        if (dureeScolaire != null && (dureeScolaire < 0 || dureeScolaire > 120)) {
            throw new IllegalArgumentException(
                    "dureeScolaire doit être entre 0 et 120 mois");
        }
        if (projetVieElabore == null) {
            throw new IllegalArgumentException("projetVieElabore est requis");
        }
        if (perspectiveAutonomie == null) {
            throw new IllegalArgumentException("perspectiveAutonomie est requis");
        }
        if (menaceOrdrePublic == null) {
            throw new IllegalArgumentException("menaceOrdrePublic est requis");
        }
    }
}
