package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-06 — cellule de matrice : conclusions <strong>en défense</strong>
 * du défendeur (employeur) devant le Conseil de prud'hommes statuant en
 * <strong>formation de départage</strong> (juge départiteur, art. L.1454-2
 * C. trav.), au fond, droit du travail, France.
 *
 * <p>Cellule miroir de {@code CphDepartageDemandeurPromptProvider} (SF-98-05)
 * côté défense : le départage est le contexte procédural — l'audience est tenue
 * après partage de voix du bureau de jugement et présidée par un magistrat du
 * tribunal judiciaire faisant fonction de juge départiteur. Le document reste un
 * projet de conclusions au fond ; la {@code DISCUSSION} réfute moyen par moyen
 * les demandes du salarié et le dispositif vise à le débouter.</p>
 */
@Component
public class CphDepartageDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — CPH / audience de départage (fond) / défendeur (employeur) /
     * droit du travail FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur (employeur) devant le Conseil de prud'hommes statuant en \
            formation de départage.
            L'audience est tenue en formation de départage : elle se déroule après partage de voix \
            du bureau de jugement et est présidée par un magistrat du tribunal judiciaire faisant \
            fonction de juge départiteur (art. L.1454-2 du Code du travail).
            Rédige un PROJET DE CONCLUSIONS EN DÉFENSE au fond structuré :
            - en-tête (POUR [défendeur / employeur] / CONTRE [demandeur / salarié]),
            - FAITS ET PROCÉDURE (rappelant le renvoi de l'affaire en départage à la suite du \
            partage de voix du bureau de jugement),
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
                ProcedureStageCatalog.FRANCE, "CPH", "DEPARTAGE", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
