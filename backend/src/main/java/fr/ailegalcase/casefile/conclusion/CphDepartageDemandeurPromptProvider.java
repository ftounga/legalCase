package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-05 — cellule de matrice : conclusions au fond du
 * <strong>demandeur</strong> (salarié) devant le Conseil de prud'hommes statuant
 * en <strong>formation de départage</strong> (juge départiteur), droit du travail,
 * France.
 *
 * <p>Cellule distincte de {@link CphFondDemandeurPromptProvider} : même type de
 * document (conclusions au fond, même structure), mais l'audience est tenue en
 * formation de départage (art. L.1454-2 C. trav.) — après partage de voix du
 * bureau de jugement — et présidée par un magistrat du tribunal judiciaire
 * faisant fonction de juge départiteur. Le prompt système mentionne explicitement
 * ce contexte procédural particulier.</p>
 */
@Component
public class CphDepartageDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — CPH / audience de départage / demandeur (salarié) / droit du
     * travail FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur (salarié) devant le Conseil de prud'hommes statuant en formation \
            de départage, devant le juge départiteur.
            L'audience est tenue en formation de départage (art. L.1454-2 C. trav.) : elle fait suite au \
            partage de voix du bureau de jugement et est présidée par un magistrat du tribunal judiciaire \
            faisant fonction de juge départiteur.
            Rédige un PROJET DE CONCLUSIONS AU FOND structuré :
            - en-tête (POUR [demandeur] / CONTRE [défendeur]),
            - FAITS ET PROCÉDURE (mentionnant le renvoi en départage à la suite du partage de voix du \
            bureau de jugement),
            - DISCUSSION (moyens en droit, un paragraphe argumenté par moyen),
            - PAR CES MOTIFS (dispositif avec demandes chiffrées).
            Précise expressément que l'audience est tenue en formation de départage, devant le juge \
            départiteur.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.FRANCE, "CPH", "DEPARTAGE", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
