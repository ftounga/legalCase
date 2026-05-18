package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-04 — cellule de matrice : conclusions <strong>en défense au référé</strong>
 * du défendeur (employeur) devant la formation de référé du Conseil de prud'hommes,
 * droit du travail, France.
 *
 * <p>Cellule miroir, en défense, du référé prud'homal : le moyen dominant est
 * l'existence d'une <strong>contestation sérieuse</strong>, qui fait échec au
 * pouvoir du juge des référés et conduit à renvoyer les parties à se pourvoir au
 * fond. La {@code DISCUSSION} conteste également le trouble manifestement illicite,
 * l'urgence et le quantum de la provision sollicitée.</p>
 */
@Component
public class CphRefereDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — CPH / référé prud'homal / défendeur (employeur) / droit du
     * travail FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur (employeur) devant la formation de référé du Conseil de prud'hommes.
            Rédige un PROJET DE CONCLUSIONS EN DÉFENSE AU RÉFÉRÉ structuré :
            - en-tête (POUR [défendeur / employeur] / CONTRE [demandeur / salarié]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION orientée défense, un paragraphe argumenté par moyen :
              * l'existence d'une contestation sérieuse, qui fait échec au pouvoir du juge des référés \
            et impose de renvoyer les parties à se pourvoir au fond,
              * l'absence de trouble manifestement illicite,
              * la contestation de l'urgence et du quantum de la provision sollicitée,
            - PAR CES MOTIFS (dispositif : dire n'y avoir lieu à référé et renvoyer les parties à se \
            pourvoir au fond ; subsidiairement, réduire la provision à de plus justes proportions).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.FRANCE, "CPH", "REFERE", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
