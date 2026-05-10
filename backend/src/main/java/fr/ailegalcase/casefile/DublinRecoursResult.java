package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-208-02 : résultat de l'analyse du recours contre une décision de transfert Dublin
 * (Règlement UE 604/2013 art. 27 + CESEDA L.572+). Délai 7 j calendaires suspensif.
 *
 * <p>Outil <b>single-country FR</b>.
 */
public record DublinRecoursResult(
        LocalDate dateNotificationDecisionTransfert,
        String etatMembreResponsable,
        String motifTransfert,
        boolean recoursForme,
        LocalDate dateRecours,
        LocalDate dateExpirationRecours,
        LocalDate dateLimiteTransfertEffectif,
        long joursRestants,
        String statut,
        String effetSuspensif,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
