package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AncienneteRequest(
        String conventionCode,
        LocalDate dateEntree,
        BigDecimal salaireBase,
        int congesContrat,
        BigDecimal primeContrat
) {}
