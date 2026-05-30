package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-218-11 : requête POST pour l'analyse de la rupture d'un VRP statutaire
 * (statut, préavis, indemnité de clientèle — art. L.7311-1 et s. CT). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateEntree début du contrat VRP (requis).
 * @param dateRupture date de notification de la rupture (requise, ≥ dateEntree).
 * @param causeRupture cause de la rupture (requise).
 * @param typeVrp type de VRP — défaut {@code EXCLUSIF}.
 * @param commissionsAnnuellesMoyennes moyenne annuelle des commissions des 3
 *        dernières années (assiette de l'indemnité de clientèle ; requise, ≥ 0).
 * @param salaireMensuelMoyen salaire mensuel moyen (indemnité légale comparée ;
 *        requis, ≥ 0).
 * @param clienteleDeveloppee true si le VRP a créé / développé / accru la
 *        clientèle (condition de fond L. 7313-13 CT) — défaut true.
 */
public record VrpIndemniteClienteleRequest(
        LocalDate dateEntree,
        LocalDate dateRupture,
        VrpCauseRupture causeRupture,
        VrpTypeVrp typeVrp,
        BigDecimal commissionsAnnuellesMoyennes,
        BigDecimal salaireMensuelMoyen,
        Boolean clienteleDeveloppee
) {}
