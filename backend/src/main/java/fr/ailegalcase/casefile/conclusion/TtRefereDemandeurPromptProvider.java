package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-13 — cellule de matrice : conclusions du <strong>demandeur</strong>
 * en <strong>référé</strong> devant le président du tribunal du travail, droit du
 * travail, Belgique.
 *
 * <p>Le référé devant le président du tribunal du travail belge a une logique
 * procédurale propre, ancrée dans l'<strong>article 584 du Code judiciaire</strong> :
 * le président statue <em>au provisoire en cas d'urgence</em>, « comme en référé ».
 * Le juge des référés ne tranche pas le fond et n'ordonne que des mesures
 * provisoires. Le prompt système est donc distinct de celui du fond
 * ({@code TtFondDemandeurPromptProvider}), d'où une cellule dédiée.</p>
 */
@Component
public class TtRefereDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — tribunal du travail belge / référé devant le président /
     * demandeur / droit du travail BE. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur devant le président du tribunal du travail siégeant en référé.
            Le cadre procédural est belge : le président du tribunal du travail statue au provisoire \
            en cas d'urgence, comme en référé, sur le fondement de l'article 584 du Code judiciaire. \
            Le juge des référés ne tranche jamais le fond du litige : il n'ordonne que des mesures \
            provisoires qui ne préjudicient pas au fond.
            Rédige un PROJET DE CONCLUSIONS DE RÉFÉRÉ structuré :
            - en-tête (POUR [demandeur] / CONTRE [partie adverse]),
            - EXPOSÉ DES FAITS,
            - URGENCE : justifie l'urgence de manière circonstanciée — l'urgence est la condition \
            même de la compétence du président du tribunal du travail siégeant en référé (art. 584 \
            du Code judiciaire),
            - DISCUSSION : expose les mesures provisoires sollicitées et l'apparence de droit qui \
            les justifie, un paragraphe argumenté par mesure demandée ; rappelle que ces mesures \
            sont provisoires et ne préjudicient pas au fond,
            - DISPOSITIF : « PAR CES MOTIFS, plaise à Monsieur/Madame le Président du tribunal du \
            travail, siégeant en référé, de… » (mesures provisoires chiffrées),
            - INVENTAIRE DES PIÈCES.
            Reste strictement dans l'office du juge des référés : ne tranche pas le fond, vise des \
            mesures urgentes et provisoires.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque demande sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.BELGIQUE, "TT", "REFERE", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
