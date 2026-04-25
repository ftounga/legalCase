package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-DT-25-01 : requête de calcul d'indemnité compensatrice de préavis FR.
 *
 * @param ancienneteAnnees          ancienneté en années (cosmétique — non utilisée dans le calcul)
 * @param ancienneteMois            ancienneté du salarié exprimée en mois (≥ 0, source de vérité)
 * @param salaireMensuelBrutEur     salaire mensuel brut (&gt; 0)
 * @param conventionCollectiveCode  code CCN (nullable — utilise durée légale si absent)
 * @param fonction                  catégorie professionnelle (obligatoire)
 * @param exemptionEmployeur        true si l'employeur a dispensé du préavis
 * @param dateRupture               date de rupture (obligatoire)
 */
public record IndemnitePreavisRequest(
        Integer ancienneteAnnees,
        Integer ancienneteMois,
        BigDecimal salaireMensuelBrutEur,
        String conventionCollectiveCode,
        IndemnitePreavisFonction fonction,
        Boolean exemptionEmployeur,
        LocalDate dateRupture
) {}
