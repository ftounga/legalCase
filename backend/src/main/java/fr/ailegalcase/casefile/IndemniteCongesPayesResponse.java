package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record IndemniteCongesPayesResponse(
        UUID caseFileId,
        BigDecimal totalRemunerationPeriodeEur,
        int joursAcquisAnnee,
        int joursPris,
        BigDecimal salaireMensuelBrutEur,
        LocalDate dateRupture,
        IndemniteCongesPayesMethode methodeForcee,
        int joursDus,
        BigDecimal montantMethodeDixPourcentEur,
        BigDecimal montantMethodeMaintienEur,
        IndemniteCongesPayesMethode methodeRetenue,
        BigDecimal montantIndemniteEur,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
