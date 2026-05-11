package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-211-04 : résultat de l'analyse du pacte successoral global belge (Loi 31/07/2017
 * modifiant CC art. 1100/1 et s. — entrée en vigueur 01/09/2018).
 * Outil <b>single-country BE</b>.
 */
public record PacteSuccessoralBe2018Result(
        LocalDate dateSignaturePacte,
        boolean acteAuthentique,
        boolean accordTousHeritiersReservataires,
        boolean equilibreDonationsRapportables,
        boolean presenceTousHeritiersReservataires,
        String verdict,
        List<String> motifsAnnulation,
        List<String> motifsContestabilite,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
