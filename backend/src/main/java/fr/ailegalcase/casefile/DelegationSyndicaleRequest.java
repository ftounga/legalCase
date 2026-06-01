package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-218-33 : requête POST pour l'analyse du statut et de la protection d'un
 * délégué syndical (DS) ou représentant de section syndicale (RSS)
 * (art. L.2143-1 et s., L.2142-1-1, L.2143-3, L.2411-3 CT, F-DT-69). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise (requis, &gt; 0). Un DS peut être
 *        désigné dès 50 salariés (art. L.2143-3).
 * @param typeMandat type de mandat — {@code DELEGUE_SYNDICAL} ou {@code RSS}
 *        (requis).
 * @param syndicatRepresentatif true si l'organisation désignante est
 *        représentative (≥ 10 % des suffrages au 1er tour CSE ; requis). Pour un
 *        DS, la représentativité est obligatoire ; pour un RSS, elle est attendue
 *        à false (le RSS existe précisément faute de représentativité).
 * @param pourcentageScorePersonnel score personnel du candidat aux dernières
 *        élections (0..100, optionnel, DS uniquement — condition des 10 %,
 *        L.2143-3).
 * @param dateDesignation date de désignation (optionnelle).
 * @param licenciementEnvisage true si un licenciement est envisagé / engagé
 *        (défaut false).
 * @param autorisationInspecteurTravail true si l'autorisation préalable de
 *        l'inspecteur du travail a été obtenue (défaut false).
 */
public record DelegationSyndicaleRequest(
        Integer effectif,
        MandatSyndicalType typeMandat,
        Boolean syndicatRepresentatif,
        BigDecimal pourcentageScorePersonnel,
        LocalDate dateDesignation,
        Boolean licenciementEnvisage,
        Boolean autorisationInspecteurTravail
) {}
