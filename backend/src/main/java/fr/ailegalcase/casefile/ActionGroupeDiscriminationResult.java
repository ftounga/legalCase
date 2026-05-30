package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-09 : résultat interne business de l'analyse de recevabilité d'une
 * action de groupe en discrimination au travail (art. L. 1134-7 à L. 1134-10
 * Code travail). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param typeOrganisation organisation exerçant l'action (qualité à agir).
 * @param motifDiscrimination critère de discrimination invoqué (L. 1132-1 CT).
 * @param nombrePersonnesConcernees nombre de personnes placées dans une
 *        situation similaire.
 * @param objetAction objet de l'action (cessation / réparation / les deux).
 * @param dateMiseEnDemeure date de la mise en demeure de l'employeur (peut être
 *        null si absente).
 * @param qualiteAAgir true si l'organisation est habilitée (L. 1134-7 CT).
 * @param pluraliteEtablie true si au moins 2 personnes sont concernées.
 * @param dateRecevabiliteSaisine date à partir de laquelle la saisine est
 *        possible (mise en demeure + 6 mois) ; null si mise en demeure absente.
 * @param delaiCarenceRespecte true si le délai de carence de 6 mois est écoulé
 *        (L. 1134-9 CT) ; false si non écoulé ou mise en demeure absente.
 * @param verdict verdict de recevabilité.
 * @param checklist checklist des conditions de recevabilité.
 * @param baseJuridique fondements juridiques applicables.
 */
public record ActionGroupeDiscriminationResult(
        ActionGroupeDiscriminationTypeOrganisation typeOrganisation,
        ActionGroupeDiscriminationMotif motifDiscrimination,
        int nombrePersonnesConcernees,
        ActionGroupeDiscriminationObjet objetAction,
        LocalDate dateMiseEnDemeure,
        boolean qualiteAAgir,
        boolean pluraliteEtablie,
        LocalDate dateRecevabiliteSaisine,
        boolean delaiCarenceRespecte,
        ActionGroupeDiscriminationVerdict verdict,
        List<ActionGroupeDiscriminationChecklistItem> checklist,
        String baseJuridique
) {}
