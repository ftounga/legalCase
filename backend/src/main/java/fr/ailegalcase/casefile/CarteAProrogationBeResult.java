package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-221-01 : résultat interne business du calcul de prorogation de la carte A
 * (séjour temporaire / limité — art. 13 Loi 15/12/1980 + art. 33 AR 08/10/1981).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de F-IM-48 (passage carte A →
 * séjour illimité après 5 ans), de la délivrance initiale du titre et du
 * renouvellement single permit (F-IM-25, travail). Snapshot complet pour
 * restitution UI sans recalcul (pattern F-DT-42).
 */
public record CarteAProrogationBeResult(
        LocalDate dateExpirationCarteA,
        boolean motifSejourPersiste,
        boolean conditionsInitialesToujoursReunies,
        boolean demandeDeposee,
        LocalDate dateDemande,
        CarteAProrogationBeVerdict verdict,
        long joursAvantExpiration,
        LocalDate dateOuvertureFenetre,
        LocalDate dateLimiteRecommandee,
        List<String> basesJuridiques,
        List<String> messages
) {}
