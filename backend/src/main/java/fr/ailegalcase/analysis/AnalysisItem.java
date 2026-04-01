package fr.ailegalcase.analysis;

public record AnalysisItem(String texte, String source, String extrait) {

    public static AnalysisItem ofText(String texte) {
        return new AnalysisItem(texte, null, null);
    }
}
