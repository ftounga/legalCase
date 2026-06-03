package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-221-02 : résultat interne business du calcul de passage carte A → carte B
 * (séjour ILLIMITÉ d'un ressortissant tiers — art. 14 Loi 15/12/1980).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de F-IM-53 (prorogation de la
 * carte A, maintien temporaire du même motif) et de F-IM-55 (statut résident
 * longue durée UE, directive 2003/109/CE — conditions propres ressources /
 * assurance / intégration ET portabilité intra-UE). La carte B ne confère PAS
 * la mobilité intra-UE : les deux outils ne chevauchent pas. Snapshot complet
 * pour restitution UI sans recalcul (pattern F-DT-42).
 */
public record CarteBSejourIllimiteBeResult(
        LocalDate dateDebutSejourRegulier,
        boolean sejourIninterrompu,
        boolean absencesSuperieuresLimites,
        boolean motifSejourStable,
        boolean ordrePublicRisque,
        CarteBSejourIllimiteBeVerdict verdict,
        int dureeSejourMois,
        int moisRestants,
        List<String> basesJuridiques,
        List<String> messages
) {}
