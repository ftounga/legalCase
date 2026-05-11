package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record DivorceDdiBeRequest(
        LocalDate dateSeparation,
        String natureDemande,
        Boolean preuvesDesunionDisponibles
) {}
