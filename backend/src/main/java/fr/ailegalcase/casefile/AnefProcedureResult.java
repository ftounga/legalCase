package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-25 : résultat interne business du guide des démarches ANEF et des recours
 * en cas de panne du dépôt dématérialisé. Outil single-country FR.
 *
 * @param joursAvantExpiration jours calendaires restants avant l'expiration du
 *        titre (négatif si déjà expiré).
 * @param etapesStandard parcours ANEF normal (toujours fourni).
 * @param etapesAlternatives procédure de contournement en cas de panne — vide si
 *        aucune panne signalée.
 * @param delaiRecoursForFauteAnnees délai du recours pour faute de l'administration
 *        (responsabilité administrative) — 2 ans.
 */
public record AnefProcedureResult(
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
        String baseJuridique
) {}
