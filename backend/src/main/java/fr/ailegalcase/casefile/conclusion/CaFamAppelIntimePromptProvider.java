package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-37 — cellule de matrice : conclusions d'<strong>intimé</strong>
 * devant la <strong>chambre de la famille de la cour d'appel</strong>, en
 * appel, droit de la famille, France.
 *
 * <p>Cellule miroir de {@code CaFamAppelAppelantPromptProvider} (SF-98-36) côté
 * intimé : même structure de conclusions d'appel conforme à l'article 954 du
 * code de procédure civile, rôle inversé. La {@code DISCUSSION} réfute les
 * moyens d'infirmation de l'appelant et le dispositif vise la confirmation du
 * jugement déféré du juge aux affaires familiales, sous réserve d'un éventuel
 * appel incident.</p>
 */
@Component
public class CaFamAppelIntimePromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — cour d'appel chambre de la famille / appel / intimé /
     * droit de la famille FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat de l'intimé devant la chambre de la famille de la cour d'appel.
            Rédige un PROJET DE CONCLUSIONS D'INTIMÉ structuré conformément à l'article 954 \
            du code de procédure civile :
            - en-tête (POUR [intimé] / CONTRE [appelant], avec mention de la cour d'appel et \
            de sa chambre de la famille),
            - RAPPEL DES FAITS ET DE LA PROCÉDURE (rappel des faits, puis exposé de la \
            procédure incluant le jugement déféré du juge aux affaires familiales et la \
            déclaration d'appel),
            - DISCUSSION orientée vers la CONFIRMATION du jugement : réfute moyen par moyen \
            les moyens d'infirmation soulevés par l'appelant — un paragraphe argumenté par \
            moyen — et, le cas échéant, développe l'APPEL INCIDENT sur les chefs du jugement \
            que l'intimé critique,
            - DISPOSITIF récapitulatif sous la forme « Il est demandé à la Cour de : \
            CONFIRMER le jugement déféré en l'ensemble de ses dispositions [...] ; sur appel \
            incident, INFIRMER le jugement en ce qu'il [...] et statuant à nouveau [...] », \
            chaque prétention reprise expressément comme l'exige l'article 954 du code de \
            procédure civile.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "CA_FAM", "APPEL", "INTIME");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
