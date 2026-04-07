package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Résultat de la checklist divorce.
 */
public record DivorceChecklistResult(
        String country,
        List<EtapeStatus> etapes,
        List<PieceStatus> pieces,
        int etapesCompletees,
        int etapesTotal,
        int piecesPresentes,
        int piecesTotal,
        String baseJuridique
) {
    public record EtapeStatus(String code, String label, int ordre, String description, String delai, boolean obligatoire, String statut) {}
    public record PieceStatus(String code, String label, String description, boolean obligatoire, String statut) {}
}
