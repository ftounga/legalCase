package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-215-01 : résultat interne business du calcul d'éligibilité / délai de renouvellement
 * du permit unique belge (Loi du 30/04/1999 ; AR du 02/09/2018 art. 23).
 *
 * <p>Outil <b>single-country BE</b> : le séjour pour activité salariée FR (titres salariés
 * L.421-1 CESEDA) est une procédure juridiquement distincte couverte par d'autres outils
 * Immigration FR (F-IM-21 changement de statut salarié notamment).
 */
public record SinglePermitBeResult(
        LocalDate dateDebutPermit,
        LocalDate dateFinPermit,
        SinglePermitBeRegionEnum regionInstruction,
        SinglePermitBeTypeActiviteEnum typeActivite,
        SinglePermitBeMotifEnum motifDemande,
        LocalDate dateLimiteDemande,
        long joursAvantExpiration,
        SinglePermitBeStatutRenouvellement statutRenouvellement,
        String regionCompetente,
        List<String> etapesProchaines,
        String baseJuridique
) {}
