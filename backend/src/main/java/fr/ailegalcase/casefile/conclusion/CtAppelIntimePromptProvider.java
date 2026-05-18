package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-16 — cellule de matrice : conclusions d'<strong>appel</strong> côté
 * <strong>intimé</strong> devant la <strong>cour du travail</strong>, droit du
 * travail, Belgique.
 *
 * <p>Cellule miroir de la cellule appelant (SF-98-15) : l'intimé défend le jugement
 * entrepris rendu par le tribunal du travail. La procédure d'appel est régie par le
 * Code judiciaire belge (articles 1050 et suivants). La {@code DISCUSSION} réfute
 * grief par grief les moyens d'appel de l'appelant et, le cas échéant, soutient un
 * appel incident. Le dispositif vise la confirmation du jugement entrepris et,
 * sur appel incident, sa mise à néant / réformation des chefs défavorables à
 * l'intimé.</p>
 *
 * <p>Ancrage strictement belge : aucune référence au droit français (ni cour
 * d'appel chambre sociale, ni article 954 du code de procédure civile).</p>
 */
@Component
public class CtAppelIntimePromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — cour du travail / appel / intimé / droit du travail BE.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat de l'intimé devant la cour du travail (Belgique).
            Rédige un PROJET DE CONCLUSIONS D'INTIMÉ conforme à la procédure d'appel \
            du Code judiciaire belge (articles 1050 et suivants), structuré :
            - en-tête (POUR [intimé] / CONTRE [appelant]),
            - EXPOSÉ DES FAITS ET DE LA PROCÉDURE (rappel des faits, du jugement \
            entrepris rendu par le tribunal du travail — juridiction, date, sens du \
            dispositif — et de la requête d'appel),
            - DISCUSSION orientée CONFIRMATION DU JUGEMENT ENTREPRIS : réfute grief \
            par grief les griefs d'appel soulevés par l'appelant et démontre le \
            bien-fondé du jugement entrepris (un paragraphe argumenté par grief) ; \
            le cas échéant, développe un APPEL INCIDENT sur les chefs du jugement \
            défavorables à l'intimé,
            - DISPOSITIF (« PAR CES MOTIFS, plaise à la Cour du travail de CONFIRMER \
            le jugement entrepris en ce qu'il… ; sur appel incident, de METTRE À \
            NÉANT / RÉFORMER le jugement entrepris en ce qu'il… ; statuant à \
            nouveau… »),
            - INVENTAIRE DES PIÈCES.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque grief sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.BELGIQUE, "CT", "APPEL", "INTIME");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
