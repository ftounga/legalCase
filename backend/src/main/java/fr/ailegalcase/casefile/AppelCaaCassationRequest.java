package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-33 : requête POST de l'analyse des délais d'appel CAA / cassation CE en
 * contentieux des étrangers (art. L. 811-1 / R. 811-1 et L. 821-1 / R. 821-1 CJA).
 * Outil single-country FR.
 *
 * @param dateJugementTA date de notification du jugement du TA (LocalDate, requise).
 * @param typeDecisionTA sens de la décision du TA (REJET | ANNULATION).
 * @param typeContentieux nature du contentieux (OQTF | REFUS_TITRE | EXPULSION | AUTRE).
 * @param delaiSpecialOQTF true si le contentieux relève du délai spécial d'appel de
 *        15 jours (OQTF sans délai de départ volontaire) ; sinon délai de droit commun (1 mois).
 */
public record AppelCaaCassationRequest(
        LocalDate dateJugementTA,
        AppelCaaCassationTypeDecisionEnum typeDecisionTA,
        AppelCaaCassationTypeContentieuxEnum typeContentieux,
        Boolean delaiSpecialOQTF
) {}
