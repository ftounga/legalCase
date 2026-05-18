package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-15 — cellule de matrice : conclusions d'<strong>appel</strong> côté
 * <strong>appelant</strong> devant la cour du travail, droit du travail, Belgique.
 *
 * <p>Droit belge — pas un miroir de l'appel français. L'appel des jugements du
 * tribunal du travail se porte devant la cour du travail et obéit au Code
 * judiciaire (art. 1050 et s. ; effet dévolutif). Le prompt système est figé et
 * cachable ; la consigne de style F-98-47 est appliquée par-dessus par
 * {@link CaseConclusionPromptBuilder}.</p>
 */
@Component
public class CtAppelAppelantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — cour du travail / appel / appelant / droit du travail BE.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat de l'appelant devant la cour du travail (Belgique).
            Rédige un PROJET DE CONCLUSIONS D'APPEL ancré dans la procédure belge.
            La cour du travail connaît de l'appel des jugements du tribunal du travail :
            l'appel est régi par le Code judiciaire (art. 1050 et suivants) et emporte
            effet dévolutif — la cour rejuge le litige dans les limites de la saisine.
            Structure le projet :
            - en-tête (POUR [appelant] / CONTRE [intimé]),
            - EXPOSÉ DES FAITS ET DE LA PROCÉDURE, incluant le jugement entrepris
              rendu par le tribunal du travail (date, dispositif critiqué),
            - DISCUSSION : critique du jugement entrepris — expose les griefs d'appel
              et les moyens de réformation, un paragraphe argumenté par grief,
            - DISPOSITIF : « PAR CES MOTIFS, plaise à la Cour du travail de mettre à
              néant / réformer le jugement entrepris en ce qu'il… ; statuant à
              nouveau,… » avec les demandes chiffrées.
            Termine par l'inventaire des pièces.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque grief sur les verdicts des outils décisionnels fournis et
            sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.BELGIQUE, "CT", "APPEL", "APPELANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
