package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-01 : résultat du calcul de la prestation compensatoire (art. 270-281 Cciv).
 */
public record PrestationCompensatoireResult(
        int montantCapitalIndicatifEur,
        int renteIndicativeMensuelleEur,
        FormePrestationEnum formeRecommandee,
        String dispAriteNiveauxVie,
        String baseJuridique,
        List<String> messages,
        List<String> alertes
) {}
