package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-215-09 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/naturalisation-conjoint-belge-be-analysis}.
 *
 * <p>Source : Code de la nationalité belge art. 16 §1 2°.
 */
public record NaturalisationConjointBelgeBeResponse(
        UUID caseFileId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateMarriage,
        boolean cohabitationLegale,
        int dureeCohabitationMois,
        NaturalisationBeNiveauLangueEnum niveauLangue,
        boolean preuveIntegration,
        boolean menaceOrdrePublic,
        boolean condamnationPenale,
        boolean eligible,
        NaturalisationConjointBelgeBeVerdict verdict,
        int dureeManquante,
        List<String> criteresNonRemplis,
        String delaiDeclaration,
        String baseJuridique
) {}
