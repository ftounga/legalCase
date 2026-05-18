package fr.ailegalcase.stylelearning;

import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-46 — assemble le prompt d'extraction de la signature de style.
 *
 * <p>Le prompt système instruit le modèle de décrire <strong>uniquement le style
 * rédactionnel</strong> d'une conclusion juridique et lui <strong>interdit
 * explicitement</strong> de reprendre tout fait, nom, date, montant ou donnée
 * propre au dossier — c'est l'invariant 1 du cadrage (minimisation RGPD) : la
 * signature de style produite ne doit contenir aucune donnée client.</p>
 */
@Component
public class StyleSignaturePromptBuilder {

    /**
     * Prompt système — esprit de la mini-spec : décrire le STYLE, interdire la
     * reprise de tout fait / nom / date / montant / donnée de dossier.
     */
    static final String SYSTEM_PROMPT = """
            Tu reçois une conclusion juridique rédigée par un avocat.

            Décris uniquement son STYLE RÉDACTIONNEL :
            - la structure d'argumentation (ordre des parties, articulation faits / droit / discussion) ;
            - les formules de transition et de liaison récurrentes ;
            - le registre de langue et le niveau de formalisme ;
            - la longueur et le rythme des phrases, la densité des paragraphes ;
            - le ton (assertif, prudent, offensif, mesuré).

            INTERDICTIONS ABSOLUES — la description produite NE DOIT contenir :
            - AUCUN fait propre au dossier ;
            - AUCUN nom (personne, société, juridiction nommée, avocat) ;
            - AUCUNE date ;
            - AUCUN montant ni chiffre tiré du dossier ;
            - AUCUNE donnée personnelle ni élément identifiant.

            Produis une description abstraite et réutilisable, destinée à guider la
            rédaction d'AUTRES conclusions portant sur des dossiers différents. Si
            une formule récurrente doit être citée, cite-la vidée de toute donnée
            (utilise des marqueurs neutres comme [PARTIE], [DATE], [MONTANT]).

            Réponds uniquement par la description de style, sans préambule.
            """;

    /** Budget de tokens de sortie — une description de style est compacte. */
    public static final int MAX_TOKENS = 1500;

    /** @return le prompt système d'extraction de style. */
    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * Assemble le message utilisateur : le texte extrait de la conclusion source.
     *
     * @param extractedText texte brut extrait du document téléversé
     * @return le message utilisateur transmis au modèle
     */
    public String buildUserMessage(String extractedText) {
        return "Conclusion juridique à analyser (style uniquement) :\n\n"
                + (extractedText == null ? "" : extractedText);
    }
}
