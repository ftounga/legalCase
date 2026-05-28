package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-215-11 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/aesm-mena-be-analysis}.
 *
 * <p>Outil composite 2 volets — tutelle DGDE (Loi 04/05/2007) + AESM (loi 15/12/1980
 * art. 9bis adapté MENA + circulaire OE 15/09/2005). Voir
 * {@link AesmMenaBeResult} pour le détail des champs.
 */
public record AesmMenaBeResponse(
        UUID caseFileId,
        int ageActuel,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateArriveeBelgique,
        boolean tuteurDesigne,
        boolean integrationScolaire,
        Integer dureeScolaire,
        boolean projetVieElabore,
        boolean perspectiveAutonomie,
        boolean menaceOrdrePublic,
        // Volet tutelle DGDE
        String etapeTutelle,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate delaiDesignationTuteur,
        // Volet AESM
        int scoreIntegration,
        int bonus,
        AesmMenaBeVerdictAesm verdictAESM,
        boolean prioriteUrgence,
        List<String> criteresNonRemplis,
        String prochainActe,
        String baseJuridique
) {}
