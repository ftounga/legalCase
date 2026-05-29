package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-43 : résultat interne business de l'analyse des obligations de
 * l'employeur recrutant un travailleur étranger hors UE (autorisation de travail,
 * L. 5221-1 Code du travail). Outil <b>FRANCE UNIQUEMENT</b> — côté employeur,
 * distinct de F-IM-07 (côté étranger).
 *
 * @param obligationsDemande pièces à fournir pour la demande d'autorisation
 *        (vide si autorisation non requise).
 * @param delaiInstructionOFIIMois délai d'instruction OFII en mois ({@code null}
 *        si autorisation non requise).
 * @param taxeOFII indication relative à la taxe OFII due par l'employeur après
 *        obtention ({@code null} si autorisation non requise).
 * @param delaiRecoursTa échéance du délai de recours TA contre un refus
 *        ({@code null} hors refus daté).
 */
public record AutorisationTravailEmployeurResult(
        AutorisationTravailEmployeurTypeContrat typeContrat,
        String posteProposes,
        String nationaliteCandidat,
        Integer dureeContratMois,
        boolean autorisationRequise,
        List<String> obligationsDemande,
        Integer delaiInstructionOFIIMois,
        String taxeOFII,
        boolean refusAutorisation,
        LocalDate dateRefusAutorisation,
        boolean recoursPossible,
        LocalDate delaiRecoursTa,
        AutorisationTravailEmployeurStatut statut,
        String recommandation,
        String baseJuridique
) {}
