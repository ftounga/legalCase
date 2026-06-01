package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-33 : résultat interne business de l'analyse du statut et de la
 * protection d'un délégué syndical / RSS (art. L.2143-1 et s., L.2142-1-1,
 * L.2143-3, L.2411-3 CT, F-DT-69). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise.
 * @param typeMandat type de mandat (DS / RSS).
 * @param syndicatRepresentatif true si l'organisation désignante est
 *        représentative.
 * @param pourcentageScorePersonnel score personnel renseigné (DS, nullable).
 * @param dateDesignation date de désignation (nullable).
 * @param checklist items de régularité de la désignation.
 * @param statutDesignation verdict de régularité de la désignation.
 * @param statutProtege statut de salarié protégé (toujours OUI).
 * @param licenciementEnvisage true si un licenciement est envisagé.
 * @param autorisationInspecteurTravail true si l'autorisation de l'inspecteur
 *        du travail a été obtenue.
 * @param risqueNulliteLicenciement niveau de risque de nullité du licenciement.
 * @param consequences conséquences / points de vigilance identifiés.
 * @param baseJuridique fondements juridiques applicables.
 */
public record DelegationSyndicaleResult(
        int effectif,
        MandatSyndicalType typeMandat,
        boolean syndicatRepresentatif,
        BigDecimal pourcentageScorePersonnel,
        LocalDate dateDesignation,
        List<DelegationSyndicaleChecklistItem> checklist,
        DelegationSyndicaleStatutDesignation statutDesignation,
        DelegationSyndicaleStatutProtege statutProtege,
        boolean licenciementEnvisage,
        boolean autorisationInspecteurTravail,
        DelegationSyndicaleRisqueNullite risqueNulliteLicenciement,
        List<String> consequences,
        String baseJuridique
) {}
