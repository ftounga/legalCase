package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

public record DivorceChecklistResponse(
        UUID caseFileId, String country,
        List<DivorceChecklistResult.EtapeStatus> etapes,
        List<DivorceChecklistResult.PieceStatus> pieces,
        int etapesCompletees, int etapesTotal,
        int piecesPresentes, int piecesTotal,
        String baseJuridique
) {}
