package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-207-02 : résultat brut produit par {@link C4OnemChecklistCalculator}.
 *
 * <p>Snapshot canonique (inputs normalisés + outputs calculés) sérialisé en
 * JSON dans la colonne {@code c4_onem_checklist_analyses.result_data}.</p>
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code verdict} — {@link Verdict#CONFORME}, {@link Verdict#NON_CONFORME}
 *       ou {@link Verdict#RISQUE_EXCLUSION_FAUTE_GRAVE} (priorité décroissante
 *       inverse : faute grave domine).</li>
 *   <li>{@code mentionsManquantes} — liste précise des mentions absentes ou
 *       invalides (vide si {@code CONFORME}).</li>
 *   <li>{@code fauteGraveDetectee} — recopie le booléen input pour traçabilité.</li>
 *   <li>{@code exclusionOnemRange} — fourchette 4-52 semaines (art. 144 AR
 *       25/11/1991) si {@code RISQUE_EXCLUSION_FAUTE_GRAVE}, null sinon.</li>
 *   <li>{@code lettreRectificativeProposee} — template texte (sans en-tête ni
 *       signature) listant les mentions à corriger ; non null UNIQUEMENT pour
 *       le verdict {@code NON_CONFORME} (cas faute grave → contestation
 *       distincte SF-207-03, pas rectification).</li>
 *   <li>{@code baseJuridique} — citation des bases juridiques BE applicables.</li>
 *   <li>{@code etapeSuivante} — orientation procédurale ({@link EtapeSuivante}).</li>
 *   <li>Champs <i>input</i> normalisés persistés pour pré-fill GET.</li>
 * </ul>
 */
public record C4OnemChecklistResult(
        // Inputs normalisés (persistés pour survie au reload)
        String raisonSocialeEmployeur,
        String numeroBce,
        String nomSalarie,
        String numeroNationalRegistre,
        LocalDate dateEntreeService,
        LocalDate dateSortieService,
        String categorieOnem,
        String motifExplicite,
        boolean fauteGraveMentionnee,
        Integer preavisPresteJours,
        BigDecimal dernierSalaireMensuelBrut,
        // Outputs calculés
        Verdict verdict,
        List<C4OnemChecklistMention> mentionsManquantes,
        boolean fauteGraveDetectee,
        ExclusionOnemRange exclusionOnemRange,
        String lettreRectificativeProposee,
        String baseJuridique,
        EtapeSuivante etapeSuivante
) {

    /**
     * Verdict global de conformité du C4 ONEM.
     *
     * <p>Priorité : {@link #RISQUE_EXCLUSION_FAUTE_GRAVE} domine
     * {@link #NON_CONFORME} qui domine {@link #CONFORME}. La détection
     * de la mention "faute grave" entraîne un risque pécuniaire majeur
     * (exclusion ONEM 4-52 sem.) qui prime sur le défaut de mentions.</p>
     */
    public enum Verdict {
        CONFORME,
        NON_CONFORME,
        RISQUE_EXCLUSION_FAUTE_GRAVE
    }

    /**
     * Orientation procédurale recommandée à l'avocat selon le verdict.
     *
     * <p>{@link #CONTESTATION_C4} : faute grave détectée — orienter vers
     * l'outil de contestation C4 (SF-207-03, à venir).</p>
     * <p>{@link #RECTIFICATION_AUPRES_EMPLOYEUR} : mentions manquantes
     * sans faute grave — utiliser la {@code lettreRectificativeProposee}.</p>
     * <p>{@link #AUCUNE} : verdict {@code CONFORME}, aucune action requise.</p>
     */
    public enum EtapeSuivante {
        CONTESTATION_C4,
        RECTIFICATION_AUPRES_EMPLOYEUR,
        AUCUNE
    }

    /**
     * Fourchette d'exclusion ONEM (art. 144 AR 25/11/1991) — 4 à 52 semaines
     * de privation des allocations de chômage en cas de licenciement pour
     * faute grave reconnu par l'ONEM.
     */
    public record ExclusionOnemRange(int minSemaines, int maxSemaines) {}
}
