package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-208-01 : résultat de l'analyse du contentieux de la rétention administrative
 * devant le Juge des libertés et de la détention (JLD), CESEDA L.741+ et L.743+.
 *
 * <p>Outil <b>single-country FR</b> : la chambre du conseil belge (Loi 15/12/1980
 * art. 71+) est une procédure juridiquement distincte, traitée par F-209 si pertinent.
 */
public record JldRetentionResult(
        LocalDate dateNotificationPlacement,
        String motifPlacement,
        boolean recoursForme,
        LocalDate dateRecours,
        LocalDate dateExpirationSaisineJld,
        LocalDate dateAudienceJld,
        LocalDate dateExpirationRecoursAppel,
        long joursRestantsAvantSaisine,
        String statut,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
