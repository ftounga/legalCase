package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-207-03 : résultat brut produit par {@link ContestationC4OnemCalculator}.
 *
 * <p>Snapshot canonique (inputs normalisés + outputs calculés) sérialisé en
 * JSON dans la colonne {@code contestation_c4_onem_analyses.result_data}.</p>
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code verdict} — {@link Verdict} reflétant l'état global de la voie
 *       de contestation (palier ADMIN ou TRIBUNAL selon le cas).</li>
 *   <li>{@code paliers} — liste ordonnée des paliers ADMIN puis TRIBUNAL avec
 *       date limite, jours restants et base juridique. Les paliers
 *       indéterminés (ex. TRIBUNAL avant décision Directeur) ont leurs
 *       champs date / jours à {@code null}.</li>
 *   <li>{@code etapeSuivante} — orientation procédurale recommandée selon
 *       le verdict.</li>
 *   <li>{@code baseJuridique} — texte unique citant les bases applicables
 *       (AR 25/11/1991 art. 144 ; CJ art. 580, 2° ; Loi 03/07/1978).</li>
 *   <li>{@code formuleCalcul} — explication textuelle du calcul pour la
 *       transparence vis-à-vis de l'avocat.</li>
 *   <li>Champs <i>input</i> normalisés persistés pour pré-fill GET.</li>
 * </ul>
 */
public record ContestationC4OnemResult(
        // Inputs normalisés (persistés pour survie au reload)
        LocalDate dateNotificationDecisionOnem,
        LocalDate dateActionEnvisagee,
        boolean recoursAdminDejaForme,
        LocalDate dateDecisionDirecteur,
        // Outputs calculés
        Verdict verdict,
        List<Palier> paliers,
        EtapeSuivante etapeSuivante,
        String baseJuridique,
        String formuleCalcul
) {

    /**
     * Verdict global de la contestation, six états possibles selon le palier
     * pertinent (ADMIN pour cas A — recours admin non encore formé ;
     * TRIBUNAL pour cas B — recours admin déjà formé).
     */
    public enum Verdict {
        /** Cas A — recours admin Directeur encore ouvert (joursRestants > seuil 7). */
        RECOURS_ADMIN_OUVERT,
        /** Cas A — recours admin Directeur imminent (0 &lt; joursRestants ≤ 7). */
        RECOURS_ADMIN_IMMINENT,
        /** Cas A — recours admin Directeur prescrit (joursRestants ≤ 0). */
        RECOURS_ADMIN_PRESCRIT,
        /** Cas B — recours tribunal du travail encore ouvert (joursRestants > seuil 14). */
        RECOURS_TRIBUNAL_OUVERT,
        /** Cas B — recours tribunal du travail imminent (0 &lt; joursRestants ≤ 14). */
        RECOURS_TRIBUNAL_IMMINENT,
        /** Cas B — recours tribunal du travail prescrit (joursRestants ≤ 0). */
        RECOURS_TRIBUNAL_PRESCRIT
    }

    /**
     * Orientation procédurale recommandée à l'avocat selon le verdict.
     *
     * <p>{@link #RECOURS_ADMIN_DIRECTEUR} : palier ADMIN encore ouvert ou
     * imminent — orienter vers la rédaction et l'envoi du recours
     * administratif au Directeur du Bureau du chômage.</p>
     * <p>{@link #RECOURS_TRIBUNAL_TRAVAIL} : palier TRIBUNAL applicable
     * (cas B en cours, ou cas A avec admin prescrit — saisine directe
     * possible selon CJ art. 580 sous réserve d'appréciation par l'avocat).</p>
     * <p>{@link #FORCLUSION_TOTALE} : palier TRIBUNAL prescrit en cas B —
     * plus de voie de recours interne, étudier les exceptions.</p>
     * <p>{@link #AUCUNE} : aucune action recommandée par défaut.</p>
     */
    public enum EtapeSuivante {
        RECOURS_ADMIN_DIRECTEUR,
        RECOURS_TRIBUNAL_TRAVAIL,
        FORCLUSION_TOTALE,
        AUCUNE
    }

    /**
     * Type de palier — ADMIN (recours administratif Directeur) ou TRIBUNAL
     * (recours juridictionnel tribunal du travail).
     */
    public enum PalierType {
        ADMIN,
        TRIBUNAL
    }

    /**
     * Représentation d'un palier de recours.
     *
     * @param type            ADMIN ou TRIBUNAL.
     * @param dateLimite      date butoir du palier ({@code null} si indéterminé,
     *                        par ex. TRIBUNAL avant que la décision Directeur
     *                        n'ait été notifiée).
     * @param joursRestants   jours restants jusqu'à la date butoir ({@code null}
     *                        si indéterminé ; peut être négatif si déjà prescrit).
     * @param baseJuridique   référence légale (AR 25/11/1991 art. 144 pour ADMIN,
     *                        CJ art. 580, 2° pour TRIBUNAL).
     */
    public record Palier(
            PalierType type,
            LocalDate dateLimite,
            Integer joursRestants,
            String baseJuridique
    ) {}
}
