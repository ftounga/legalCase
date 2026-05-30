package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-218-23 : requête POST pour l'analyse de la validité de la rupture du
 * contrat d'apprentissage — régime hybride distinguant la rupture libre durant
 * les 45 premiers jours de la rupture sur motif limité au-delà (art. L.6222-18
 * et s. CT, F-DT-110). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateDebutContrat date de début de l'exécution du contrat
 *        d'apprentissage (requise).
 * @param dateRupture date de la rupture (requise, ≥ dateDebutContrat).
 * @param auteurRupture auteur de la rupture (EMPLOYEUR | APPRENTI, requis).
 * @param motifRupture motif invoqué (ACCORD_PARTIES | FAUTE_GRAVE |
 *        FORCE_MAJEURE | INAPTITUDE | EXCLUSION_DEFINITIVE_CFA | SANS_MOTIF,
 *        requis).
 * @param apprentiMajeur true si l'apprenti est majeur (défaut true) — pertinent
 *        pour certaines formalités.
 */
public record ApprentissageRuptureRequest(
        LocalDate dateDebutContrat,
        LocalDate dateRupture,
        ApprentissageAuteurRupture auteurRupture,
        ApprentissageMotifRupture motifRupture,
        Boolean apprentiMajeur
) {}
