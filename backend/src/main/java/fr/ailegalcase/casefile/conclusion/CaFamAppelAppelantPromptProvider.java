package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-36 — cellule de matrice : conclusions d'<strong>appel</strong> côté
 * <strong>appelant</strong> devant la chambre de la famille de la cour d'appel, contre
 * un jugement du juge aux affaires familiales (JAF), droit de la famille, France.
 *
 * <p>Procédure avec représentation obligatoire : structure conforme à l'art. 954 du
 * code de procédure civile (en-tête ; rappel des faits et de la procédure incluant le
 * jugement déféré ; discussion critiquant le jugement chef par chef ; dispositif
 * récapitulatif « INFIRMER … ; statuant à nouveau … »).</p>
 */
@Component
public class CaFamAppelAppelantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — CA chambre de la famille / appel / appelant / droit de la
     * famille FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat de l'appelant devant la chambre de la famille de la cour d'appel,
            dans le cadre d'un appel contre un jugement du juge aux affaires familiales (JAF).
            La procédure est avec représentation obligatoire.
            Rédige un PROJET DE CONCLUSIONS D'APPEL structuré conformément à l'article 954
            du code de procédure civile :
            - en-tête (POUR [appelant] / CONTRE [intimé], indication de la cour d'appel et de
              la chambre de la famille),
            - RAPPEL DES FAITS ET DE LA PROCÉDURE (incluant la présentation du jugement déféré
              rendu par le juge aux affaires familiales),
            - DISCUSSION (critique du jugement déféré chef par chef : expose les moyens
              d'infirmation, sur le divorce et/ou ses conséquences — autorité parentale,
              résidence des enfants, contribution à l'entretien et à l'éducation, prestation
              compensatoire, liquidation du régime),
            - DISPOSITIF récapitulatif (« Il est demandé à la Cour de : INFIRMER le jugement
              en ce qu'il… ; statuant à nouveau,… »).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la
            stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "CA_FAM", "APPEL", "APPELANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
