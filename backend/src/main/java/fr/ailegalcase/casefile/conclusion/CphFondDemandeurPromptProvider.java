package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-01 — cellule de matrice : conclusions du <strong>demandeur</strong>
 * (salarié) devant le Conseil de prud'hommes, en bureau de jugement (fond), droit
 * du travail, France.
 *
 * <p>Le prompt système est strictement celui figé par SF-98-01 (comportement
 * inchangé) — la cellule a simplement été migrée vers le registre
 * {@link ConclusionPromptRegistry} par SF-98-02.</p>
 */
@Component
public class CphFondDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — CPH / bureau de jugement (fond) / demandeur (salarié) / droit du
     * travail FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur (salarié) devant le Conseil de prud'hommes, en bureau de jugement.
            Rédige un PROJET DE CONCLUSIONS structuré :
            - en-tête (POUR [demandeur] / CONTRE [défendeur]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION (moyens en droit, un paragraphe argumenté par moyen),
            - PAR CES MOTIFS (dispositif avec demandes chiffrées).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.FRANCE, "CPH", "FOND", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
