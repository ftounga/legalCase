package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-07 — cellule de matrice : conclusions d'<strong>appel</strong> côté
 * <strong>appelant</strong> devant la chambre sociale de la cour d'appel, droit du
 * travail, France.
 *
 * <p>Les conclusions d'appel diffèrent structurellement des conclusions de
 * 1ʳᵉ instance : la {@code DISCUSSION} critique le jugement déféré chef par chef
 * (moyens d'infirmation) et le dispositif est un dispositif récapitulatif au sens
 * de l'art. 954 CPC, énonçant expressément les chefs critiqués et les prétentions
 * (« INFIRMER le jugement en ce qu'il… ; statuant à nouveau… »).</p>
 */
@Component
public class CaSocAppelAppelantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — cour d'appel chambre sociale / appel / appelant / droit du
     * travail FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat de l'appelant devant la chambre sociale de la cour d'appel.
            Rédige un PROJET DE CONCLUSIONS D'APPEL conforme à l'article 954 du Code de procédure civile, structuré :
            - en-tête (POUR [appelant] / CONTRE [intimé]),
            - RAPPEL DES FAITS ET DE LA PROCÉDURE (incluant l'identification précise du jugement déféré \
            rendu par le conseil de prud'hommes : juridiction, date, chefs du dispositif),
            - DISCUSSION : critique du jugement chef par chef — un moyen d'infirmation argumenté par chef \
            de jugement critiqué, énonçant expressément le chef visé et la raison pour laquelle il doit \
            être infirmé,
            - DISPOSITIF RÉCAPITULATIF : formule expressément les prétentions soumises à la Cour \
            (« Il est demandé à la Cour de : INFIRMER le jugement en ce qu'il… ; statuant à nouveau… »). \
            Le dispositif récapitulatif doit énoncer expressément chacun des chefs du jugement critiqués \
            et reprendre toutes les prétentions chiffrées.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen d'infirmation sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.FRANCE, "CA_SOC", "APPEL", "APPELANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
