package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-02 — cellule de matrice : conclusions <strong>en défense</strong>
 * du défendeur (employeur) devant le Conseil de prud'hommes, en bureau de jugement
 * (fond), droit du travail, France.
 *
 * <p>Cellule miroir de {@link CphFondDemandeurPromptProvider} : même structure de
 * conclusions, rôle inversé. La {@code DISCUSSION} réfute moyen par moyen les
 * demandes du salarié et le dispositif vise à le débouter.</p>
 */
@Component
public class CphFondDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — CPH / bureau de jugement (fond) / défendeur (employeur) / droit du
     * travail FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur (employeur) devant le Conseil de prud'hommes, en bureau de jugement.
            Rédige un PROJET DE CONCLUSIONS EN DÉFENSE structuré :
            - en-tête (POUR [défendeur / employeur] / CONTRE [demandeur / salarié]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION (réfutation moyen par moyen des demandes du salarié : régularité de la \
            procédure de licenciement, cause réelle et sérieuse du licenciement, contestation du \
            quantum des sommes réclamées — un paragraphe argumenté par moyen),
            - PAR CES MOTIFS (dispositif : débouter le demandeur de l'ensemble de ses demandes ; \
            subsidiairement, réduire les sommes réclamées à de plus justes proportions).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.FRANCE, "CPH", "FOND", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
