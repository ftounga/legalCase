package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-08-05 : résultat de l'analyse d'un ordre de quitter le territoire belge (annexe 13 de l'AR
 * 08/10/1981 d'exécution de la Loi du 15/12/1980 sur les étrangers). Calcule les délais de recours
 * devant le Conseil du contentieux des étrangers (CCE) et identifie les référés disponibles.
 * Outil single-country BE — équivalent FR (OQTF) couvert par SF-IM-08-01/03.
 */
public record Annexe13BeResult(
        LocalDate dateNotificationAnnexe13,
        int delaiDepartImposeJours,
        String motifOqt,
        boolean transfertImminent,
        boolean recoursForme,
        LocalDate dateRecours,
        String typeRecours,
        LocalDate dateExpirationDelaiDepart,
        LocalDate dateExpirationRecoursAnnulation,
        LocalDate dateExpirationRecoursExtremeUrgence,
        long joursRestantsAvantExpirationAnnulation,
        String statutRecoursAnnulation,
        LocalDate dateAudiencePrevisionnelle,
        LocalDate dateDecisionPrevisionnelle,
        List<String> referedDisponibles,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
