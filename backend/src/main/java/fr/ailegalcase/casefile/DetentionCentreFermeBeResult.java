package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-221-04 : résultat interne business du calcul de détention en centre fermé +
 * requête de mise en liberté (Loi 15/12/1980 art. 7 al. 3 / 27 / 29 / 74/5 ;
 * AR 02/08/2002 ; requête chambre du conseil art. 71 et s.).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — une situation fusionnée : la détention ET son
 * recours devant la chambre du conseil (juridiction JUDICIAIRE), DISTINCT des recours
 * CCE (F-IM-31 annulation 30j, F-IM-32 extrême urgence 5j, F-IM-57 suspension).
 * Snapshot complet pour restitution UI sans recalcul (pattern F-DT-42).
 */
public record DetentionCentreFermeBeResult(
        LocalDate dateDebutDetention,
        DetentionBaseLegale baseLegaleDetention,
        boolean prolongationNotifiee,
        LocalDate dateProlongation,
        boolean requeteMiseEnLiberteDeposee,
        LocalDate dateNotificationDecisionDetention,
        DetentionCentreFermeBeVerdict verdict,
        int dureeDetentionJours,
        LocalDate dateLimiteRequete,
        Integer joursRestantsRequete,
        List<String> basesJuridiques,
        List<String> messages
) {}
