package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-25 : réponse de l'analyse des démarches ANEF / recours panne du dépôt
 * dématérialisé. Outil single-country FR.
 */
public record AnefProcedureResponse(
        UUID caseFileId,
        String typeTitreConcerne,
        LocalDate dateExpirationTitre,
        boolean panneeANEFSignalee,
        LocalDate dateTentativeDepot,
        boolean demandeAdresseePrefecture,
        long joursAvantExpiration,
        AnefProcedureStatut statut,
        List<String> etapesStandard,
        List<String> etapesAlternatives,
        int delaiRecoursForFauteAnnees,
        String recommandation,
        String country,
        String baseJuridique
) {}
