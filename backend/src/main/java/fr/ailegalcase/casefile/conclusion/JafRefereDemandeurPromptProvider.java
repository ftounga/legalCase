package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-34 — cellule de matrice : conclusions du <strong>demandeur</strong>
 * devant le juge aux affaires familiales statuant en <strong>référé</strong>,
 * droit de la famille, France.
 *
 * <p>Le référé devant le JAF a une logique procédurale propre — urgence, mesures
 * provisoires en attente du jugement au fond (autorité parentale, résidence des
 * enfants, contribution à leur entretien). L'ancrage est le code civil pour les
 * mesures sollicitées et le code de procédure civile pour la procédure de référé.
 * Le prompt système est donc distinct de celui du fond, d'où une cellule
 * dédiée.</p>
 */
@Component
public class JafRefereDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — JAF / référé / demandeur / droit de la famille FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur devant le juge aux affaires familiales statuant en référé.
            Rédige un PROJET DE CONCLUSIONS DE RÉFÉRÉ structuré :
            - en-tête (POUR [demandeur] / CONTRE [défendeur]),
            - FAITS ET PROCÉDURE,
            - URGENCE (caractérisation de l'urgence justifiant la saisine du juge des référés \
            au regard du code de procédure civile : situation appelant des mesures immédiates \
            dans l'intérêt des enfants),
            - DISCUSSION (mesures provisoires sollicitées, motivées une à une sur le fondement \
            du code civil : exercice de l'autorité parentale, fixation de la résidence des \
            enfants, droit de visite et d'hébergement, contribution à l'entretien et à \
            l'éducation des enfants — un paragraphe argumenté par mesure),
            - PAR CES MOTIFS (dispositif : mesures urgentes et provisoires demandées, chiffrées \
            lorsqu'il s'agit d'une contribution à l'entretien des enfants).
            Reste dans l'office du juge des référés : ne tranche pas le fond, vise des mesures \
            urgentes et provisoires.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "JAF", "REFERE", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
