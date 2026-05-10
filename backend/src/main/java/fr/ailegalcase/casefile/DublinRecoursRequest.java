package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record DublinRecoursRequest(
        LocalDate dateNotificationDecisionTransfert,
        String etatMembreResponsable,
        String motifTransfert,
        Boolean recoursForme,
        LocalDate dateRecours
) {}
