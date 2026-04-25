package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-IM-11-01 : requête pour l'analyse d'un changement de statut CESEDA
 * (transition entre titres de séjour, art. L.421+ et R.5221 CESEDA).
 * Outil single-country FR — l'équivalent BE relève d'une procédure distincte
 * (modification 9bis OE) traitée dans F-IM-11-BE (backlog).
 *
 * @param titreActuel             code du titre actuellement détenu (ex: "ETUDIANT", "VISITEUR",
 *                                "VPF", "SALARIE", "PASSEPORT_TALENT_SALARIE_QUALIFIE", etc.)
 * @param titreEnvisage           code du titre visé après transition
 * @param dureeRestanteSurTitreActuelMois nombre de mois restants sur le titre actuel
 * @param documentJustificatifFourni      contrat de travail / attestation école / autre fourni ?
 * @param remunerationContratEur  rémunération brute mensuelle du contrat (si transition vers SALARIE)
 * @param casierJudiciaireVierge  casier judiciaire vierge (Boolean tristate, default true côté service)
 */
public record ChangementStatutRequest(
        String titreActuel,
        String titreEnvisage,
        Integer dureeRestanteSurTitreActuelMois,
        Boolean documentJustificatifFourni,
        BigDecimal remunerationContratEur,
        Boolean casierJudiciaireVierge
) {}
