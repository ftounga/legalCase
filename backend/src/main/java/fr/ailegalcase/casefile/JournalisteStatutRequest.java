package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-218-15 : requête POST pour l'analyse du statut de journaliste professionnel
 * lors d'une rupture — clause de cession / de conscience, indemnité de
 * congédiement, commission arbitrale (art. L.7111-1 et s. CT). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateEntree début du contrat (LocalDate, requis).
 * @param dateRupture date de notification de la rupture (LocalDate, requis ;
 *        doit être ≥ {@code dateEntree}).
 * @param typeRupture type de rupture invoqué (requis).
 * @param salaireMensuelMoyen base de l'indemnité de congédiement (€, strictement
 *        positif, requis).
 * @param carteIdentiteProfessionnelle détention de la carte de presse (CCIJP) —
 *        présomption de la qualité de journaliste ; défaut true.
 * @param cessionTitreConstatee fait générateur de la clause de cession (cession
 *        ou cessation de publication du titre) ; défaut false.
 * @param changementOrientationConstate fait générateur de la clause de
 *        conscience (changement notable de l'orientation du journal) ;
 *        défaut false.
 */
public record JournalisteStatutRequest(
        LocalDate dateEntree,
        LocalDate dateRupture,
        JournalisteStatutTypeRupture typeRupture,
        BigDecimal salaireMensuelMoyen,
        Boolean carteIdentiteProfessionnelle,
        Boolean cessionTitreConstatee,
        Boolean changementOrientationConstate
) {}
