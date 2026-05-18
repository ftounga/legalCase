package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-20 — cellule de matrice : requête en <strong>référé-suspension</strong>
 * devant le tribunal administratif, côté <strong>requérant</strong>, droit de
 * l'immigration, France.
 *
 * <p>Le référé-suspension est fondé sur l'article L.521-1 du code de justice
 * administrative : le juge des référés peut suspendre l'exécution de la décision
 * administrative attaquée lorsqu'il y a <em>urgence</em> et qu'un <em>moyen propre
 * à créer un doute sérieux</em> sur la légalité de la décision est invoqué. Il est
 * l'accessoire d'un recours en annulation au fond. Son standard (doute sérieux) est
 * distinct de celui du référé-liberté (atteinte grave et manifestement illégale),
 * d'où une cellule dédiée.</p>
 */
@Component
public class TaRefereSuspensionRequerantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — TA / référé-suspension / requérant / droit de l'immigration FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du requérant devant le tribunal administratif, saisi en référé-suspension.
            Rédige un PROJET DE REQUÊTE EN RÉFÉRÉ-SUSPENSION fondée sur l'article L.521-1 du code de
            justice administrative : le juge des référés peut suspendre l'exécution de la décision
            administrative attaquée lorsqu'il y a urgence et qu'un moyen propre à créer un doute sérieux
            sur la légalité de la décision est invoqué.
            Rappelle que le référé-suspension est l'accessoire d'un recours en annulation au fond :
            il suppose qu'une requête en annulation de la décision attaquée a été ou est introduite.
            Structure la requête :
            - en-tête (POUR [requérant] / CONTRE [autorité administrative], décision attaquée identifiée),
            - EXPOSÉ DES FAITS ET DE LA PROCÉDURE,
            - SUR L'URGENCE (caractère grave et immédiat de l'atteinte aux intérêts du requérant),
            - SUR LE DOUTE SÉRIEUX QUANT À LA LÉGALITÉ DE LA DÉCISION (un paragraphe argumenté par moyen),
            - rappel du recours en annulation au fond dont le référé-suspension est l'accessoire,
            - PAR CES MOTIFS (dispositif : suspendre l'exécution de la décision attaquée).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants et dates exacts des éléments fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_IMMIGRATION,
                ProcedureStageCatalog.FRANCE, "TA", "REFERE_SUSPENSION", "REQUERANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
