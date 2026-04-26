package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-DT-33-01 : requête pour l'analyse de recevabilité d'une procédure AT/MP française
 * (Code de la sécurité sociale L.411-1 / L.461-1 / L.434-2).
 *
 * <p>Outil <b>single-country FR</b>. 3 dispositifs supportés :
 * RECONNAISSANCE_AT / RECONNAISSANCE_MP / CONTESTATION_TAUX_IPP.
 * L'équivalent belge (FEDRIS — Agence fédérale des risques professionnels)
 * sera traité par F-DT-33-BE (backlog).
 *
 * @param dispositif                       enum dispositif visé (3 valeurs)
 * @param dateAccident                     date accident (RECONNAISSANCE_AT)
 * @param lieuTravail                      accident sur le lieu de travail (RECONNAISSANCE_AT)
 * @param declarationEmployeurDansLes48h   déclaration employeur dans le délai L.441-1
 * @param certificatMedicalInitial         CMI L.441-6 / L.461-5 produit
 * @param numeroTableau                    numéro du tableau MP, ou "HORS_TABLEAU"
 * @param delaiPriseEnChargeRespecte       délai du tableau MP respecté
 * @param dateExposition                   date d'exposition à l'agent (RECONNAISSANCE_MP)
 * @param tauxFixeParCpam                  taux IPP fixé par la CPAM (0-100)
 * @param tauxRevendique                   taux IPP revendiqué par le salarié (0-100)
 * @param expertiseMedicaleProduite        rapport médical produit à l'appui (CONTESTATION_TAUX_IPP)
 * @param datePremierAvisCpam              date du premier avis CPAM (point de départ délai)
 */
public record AtMpRequest(
        String dispositif,
        LocalDate dateAccident,
        Boolean lieuTravail,
        Boolean declarationEmployeurDansLes48h,
        Boolean certificatMedicalInitial,
        String numeroTableau,
        Boolean delaiPriseEnChargeRespecte,
        LocalDate dateExposition,
        Integer tauxFixeParCpam,
        Integer tauxRevendique,
        Boolean expertiseMedicaleProduite,
        LocalDate datePremierAvisCpam
) {}
