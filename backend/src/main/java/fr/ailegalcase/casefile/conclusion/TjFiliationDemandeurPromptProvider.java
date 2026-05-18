package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-39 — cellule de matrice : conclusions du <strong>demandeur</strong>
 * en matière de <strong>filiation</strong> devant le tribunal judiciaire, droit de
 * la famille, France.
 *
 * <p>Couvre les actions en établissement ou en contestation de filiation (titre VII
 * du livre I<sup>er</sup> du code civil). Le prompt système oriente la rédaction vers
 * le fondement de l'action, les modes de preuve de la filiation et la demande
 * d'expertise génétique. La consigne de style F-98-47 est appliquée par-dessus par
 * {@link CaseConclusionPromptBuilder}.</p>
 */
@Component
public class TjFiliationDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — TJ / filiation / demandeur / droit de la famille FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur en matière de filiation devant le tribunal judiciaire.
            L'action vise l'établissement ou la contestation d'un lien de filiation : action en \
            recherche de paternité ou de maternité, ou action en contestation de paternité ou de \
            maternité.
            Rédige un PROJET DE CONCLUSIONS structuré :
            - en-tête (POUR [demandeur] / CONTRE [défendeur]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION : fondement de l'action (titre VII du livre Iᵉʳ du code civil — de la \
            filiation), modes de preuve de la filiation (présomptions, possession d'état, indices \
            de fait), et demande d'expertise biologique / génétique le cas échéant ; un paragraphe \
            argumenté par moyen,
            - PAR CES MOTIFS (dispositif : voir établi ou contesté le lien de filiation, ordonner \
            l'expertise génétique sollicitée).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "TJ", "FILIATION", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
