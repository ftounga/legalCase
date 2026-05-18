package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-08 — cellule de matrice : conclusions d'<strong>appel</strong> côté
 * <strong>intimé</strong> devant la chambre sociale de la cour d'appel, droit du
 * travail, France.
 *
 * <p>Cellule miroir de la cellule appelant (SF-98-07) : l'intimé défend le jugement
 * déféré. La {@code DISCUSSION} réfute moyen par moyen les moyens d'infirmation de
 * l'appelant et, le cas échéant, soutient un appel incident. Conformément à
 * l'article 954 du code de procédure civile, les conclusions d'intimé comportent un
 * dispositif récapitulatif (« CONFIRMER le jugement… ; sur appel incident,
 * INFIRMER… »).</p>
 */
@Component
public class CaSocAppelIntimePromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — Cour d'appel chambre sociale / appel / intimé / droit du
     * travail FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat de l'intimé devant la chambre sociale de la cour d'appel.
            Rédige un PROJET DE CONCLUSIONS D'INTIMÉ conforme à l'article 954 du code de \
            procédure civile, structuré :
            - en-tête (POUR [intimé] / CONTRE [appelant]),
            - RAPPEL DES FAITS ET DE LA PROCÉDURE (rappel des faits, du jugement déféré — \
            juridiction, date, sens du dispositif — et de la déclaration d'appel),
            - DISCUSSION orientée CONFIRMATION DU JUGEMENT : réfute moyen par moyen les moyens \
            d'infirmation soulevés par l'appelant et démontre le bien-fondé de la décision \
            entreprise (un paragraphe argumenté par moyen) ; le cas échéant, développe un \
            APPEL INCIDENT sur les chefs du jugement défavorables à l'intimé,
            - DISPOSITIF RÉCAPITULATIF, obligatoire pour les conclusions d'intimé \
            (« Il est demandé à la Cour de : CONFIRMER le jugement en ce qu'il… ; \
            sur appel incident, INFIRMER le jugement en ce qu'il… ; statuant à nouveau… »).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.FRANCE, "CA_SOC", "APPEL", "INTIME");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
