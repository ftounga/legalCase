package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MotifGraveBeRequest(
        LocalDate dateConnaissanceFait,
        LocalDate dateNotificationRupture,
        LocalDate dateNotificationMotifs,
        Integer anciennetteAnnees,
        BigDecimal salaireMensuelReference
) {}
