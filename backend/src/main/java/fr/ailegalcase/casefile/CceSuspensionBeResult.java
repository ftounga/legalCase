package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-221-05 : résultat interne business du recours CCE en SUSPENSION ORDINAIRE
 * (référé administratif : art. 39/82 Loi 15/12/1980 ; loi 15/09/2006).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — 3e recours CCE distinct de l'annulation 30j
 * (F-IM-31) et de l'extrême urgence 5j ouvrables (F-IM-32). Snapshot complet (inputs +
 * outputs) pour restitution UI sans recalcul (pattern F-DT-42).
 *
 * <p>{@code renvoiOutil} pointe vers {@code F-IM-32-cce-extreme-urgence-5j-be} lorsque
 * le verdict est {@link CceSuspensionBeVerdict#REORIENTER_EXTREME_URGENCE} (éloignement
 * imminent), null sinon.
 */
public record CceSuspensionBeResult(
        LocalDate dateNotificationDecision,
        boolean recoursAnnulationIntroduit,
        boolean urgenceInvocable,
        boolean risquePrejudiceGraveDifficilementReparable,
        boolean mesureEloignementImminente,
        CceSuspensionBeVerdict verdict,
        int joursDepuisNotification,
        String renvoiOutil,
        List<String> basesJuridiques,
        List<String> messages
) {}
