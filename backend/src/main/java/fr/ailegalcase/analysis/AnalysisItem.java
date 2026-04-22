package fr.ailegalcase.analysis;

/**
 * Item d'analyse : un fait, un point juridique, un risque, une question…
 *
 * <p>F-146 SF-146-01 : ajout du champ optionnel {@link #sourceRef} qui pointe
 * précisément à la pièce + page dans le document source (vs le simple nom de
 * document dans {@link #source}). Rétrocompat : constructeurs 3-args et
 * {@link #ofText} préservés.
 */
public record AnalysisItem(String texte, String source, String extrait, SourceRef sourceRef) {

    /** Constructeur rétrocompat 3-args (pré-F-146). */
    public AnalysisItem(String texte, String source, String extrait) {
        this(texte, source, extrait, null);
    }

    public static AnalysisItem ofText(String texte) {
        return new AnalysisItem(texte, null, null, null);
    }
}
