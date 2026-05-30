package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-218-13 : requête POST pour l'analyse de la rupture du contrat d'un salarié
 * du particulier employeur (CESU) ou d'un assistant maternel — préavis et
 * indemnité de licenciement / de rupture. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateEntree début du contrat (LocalDate, requis).
 * @param dateRupture date de notification du licenciement / de la rupture
 *        (LocalDate, requis ; doit être ≥ {@code dateEntree}).
 * @param categorieEmploye catégorie pilotant la CCN applicable
 *        (SALARIE_PARTICULIER_EMPLOYEUR | ASSISTANT_MATERNEL, requis).
 * @param causeRupture cause de la rupture (requis) ; {@code RETRAIT_ENFANT}
 *        réservé à la catégorie ASSISTANT_MATERNEL.
 * @param salaireMensuelMoyen moyenne des salaires servant d'assiette à
 *        l'indemnité (€, strictement positif, requis).
 */
public record ParticulierEmployeurCesuRequest(
        LocalDate dateEntree,
        LocalDate dateRupture,
        ParticulierEmployeurCesuCategorie categorieEmploye,
        ParticulierEmployeurCesuCause causeRupture,
        BigDecimal salaireMensuelMoyen
) {}
