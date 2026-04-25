package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-19-05 : requête de calcul d'éligibilité au recours JAF en cas de
 * désaccord parental (art. 373-2-10 Cciv).
 */
public record DesaccordsParentauxRequest(
        String domaineDesaccord,
        String intensiteDesaccord,
        List<String> tentativesMediation,
        List<Integer> ageEnfantsConcernes,
        Boolean interetSuperieurInvoque,
        Boolean expertiseDejaRealisee,
        Boolean urgence,
        LocalDate dateRequete
) {}
