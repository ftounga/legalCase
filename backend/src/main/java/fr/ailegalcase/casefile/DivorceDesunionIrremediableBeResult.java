package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-11-01 : résultat de l'analyse de divorce pour <b>désunion irrémédiable</b>
 * BE (art. 229 §3 Code civil belge, Loi 27/04/2007). Outil single-country
 * BELGIQUE — équivalent belge des features FR F-FA-08 (altération) et
 * F-FA-10 (accepté). Procédure unique fondée sur la séparation de fait :
 * 6 mois si demande conjointe, 1 an si demande unilatérale.
 */
public record DivorceDesunionIrremediableBeResult(
        LocalDate dateSeparation,
        boolean separationConsentue,
        boolean preuvesSeparation,
        boolean preuvesDocumentaires,
        boolean tentativesReconciliation,
        LocalDate dateAssignation,
        int dureeSeparationMois,
        int seuilSeparationMois,
        boolean delaiObjectifOk,
        boolean conditionsReunies,
        int scoreGlobal,
        String verdictProbabilite,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
