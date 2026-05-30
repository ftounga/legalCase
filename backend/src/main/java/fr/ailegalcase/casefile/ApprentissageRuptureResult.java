package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-23 : résultat interne business de l'analyse de la rupture du contrat
 * d'apprentissage (art. L.6222-18 et s. CT, F-DT-110). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateDebutContrat date de début du contrat.
 * @param dateRupture date de la rupture.
 * @param auteurRupture auteur de la rupture.
 * @param motifRupture motif invoqué.
 * @param apprentiMajeur true si l'apprenti est majeur.
 * @param joursDepuisDebut nombre de jours entre le début du contrat et la
 *        rupture (bornes incluses).
 * @param periode période de la rupture par rapport au seuil des 45 jours.
 * @param dansPeriodeLibre true si la rupture intervient dans les 45 premiers
 *        jours (rupture libre possible).
 * @param validite validité du motif de rupture.
 * @param verdictGlobal verdict global de l'analyse.
 * @param consequences conséquences juridiques identifiées.
 * @param motif libellé synthétique du motif retenu.
 * @param baseJuridique fondements juridiques applicables.
 */
public record ApprentissageRuptureResult(
        LocalDate dateDebutContrat,
        LocalDate dateRupture,
        ApprentissageAuteurRupture auteurRupture,
        ApprentissageMotifRupture motifRupture,
        boolean apprentiMajeur,
        long joursDepuisDebut,
        ApprentissagePeriodeRupture periode,
        boolean dansPeriodeLibre,
        ApprentissageRuptureValidite validite,
        ApprentissageRuptureVerdict verdictGlobal,
        List<String> consequences,
        String motif,
        String baseJuridique
) {}
