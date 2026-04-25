package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-22-01 : requête de calcul de l'éligibilité au partage judiciaire d'une
 * indivision post-communautaire (art. 815 Cciv + 1364 CPC). Le champ
 * {@code indivisionDureeAnnees} accepte également l'alias accentué
 * {@code indivisionDuréeAnnees} pour préserver le contrat figé V8.
 */
public record IndivisionRequest(
        LocalDate dateOrigineIndivision,
        List<String> natureBiens,
        BigDecimal valeurEstimeeTotaleEur,
        Integer nbIndivisaires,
        List<BigDecimal> quotesPart,
        List<String> tentativesPartageAmiable,
        Boolean consentementPartageGlobal,
        Boolean occupationBienParUnIndivisaire,
        @JsonAlias({"indivisionDuréeAnnees", "indivisionDureeAnnees"})
        Integer indivisionDureeAnnees,
        Boolean demandeMesuresConservatoires,
        Boolean conflitOuvertEntreIndivisaires
) {}
