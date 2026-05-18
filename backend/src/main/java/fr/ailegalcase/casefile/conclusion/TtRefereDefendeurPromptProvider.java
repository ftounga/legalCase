package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-14 — cellule de matrice : conclusions <strong>en défense</strong>
 * du défendeur devant le président du tribunal du travail siégeant en référé,
 * droit du travail, Belgique.
 *
 * <p>Ancrage procédural belge : compétence du juge des référés fondée sur
 * l'article 584 du Code judiciaire. L'axe central de la défense est la
 * <strong>contestation de l'urgence</strong> — condition de la compétence du
 * juge des référés — et le renvoi au juge du fond du caractère sérieusement
 * contestable de la demande. Aucune référence au droit français.</p>
 */
@Component
public class TtRefereDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — tribunal du travail / référé (président) / défendeur /
     * droit du travail BE. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur devant le président du tribunal du travail siégeant en référé, \
            en Belgique.
            Rédige un PROJET DE CONCLUSIONS EN DÉFENSE structuré selon la procédure belge :
            - en-tête (POUR [défendeur] / CONTRE [demandeur]),
            - EXPOSÉ DES FAITS,
            - DISCUSSION orientée défense, fondée sur l'article 584 du Code judiciaire qui régit \
            la compétence du président du tribunal du travail siégeant en référé :
              * contestation de l'urgence — l'urgence est une condition de la compétence du juge \
              des référés ; à défaut d'urgence réelle et actuelle, le président siégeant en référé \
              est sans pouvoir pour connaître de la demande,
              * absence d'apparence de droit dans le chef du demandeur,
              * le caractère sérieusement contestable de la demande, qui relève de l'appréciation \
              du juge du fond et non du juge des référés statuant au provisoire,
              un paragraphe argumenté par moyen,
            - DISPOSITIF : « PAR CES MOTIFS, plaise à Monsieur/Madame le Président du tribunal du \
            travail, siégeant en référé, de déclarer la demande non fondée / d'en débouter le \
            demandeur »,
            - INVENTAIRE DES PIÈCES.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.BELGIQUE, "TT", "REFERE", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
