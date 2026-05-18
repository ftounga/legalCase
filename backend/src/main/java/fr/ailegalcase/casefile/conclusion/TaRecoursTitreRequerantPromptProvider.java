package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-25 — cellule de matrice : requête en annulation devant le
 * <strong>tribunal administratif</strong> contre une décision préfectorale de
 * refus de titre de séjour ou de refus de regroupement familial, côté
 * <strong>requérant</strong>, droit de l'immigration, France.
 *
 * <p>Contentieux administratif français — requête en annulation. Ancrage :
 * CESEDA et code de justice administrative. Cellule distincte de SF-98-18
 * (OQTF) : l'objet du litige est le refus de titre, non l'éloignement.</p>
 */
@Component
public class TaRecoursTitreRequerantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — TA / recours refus de titre ou de regroupement familial /
     * requérant / droit de l'immigration FR. Instructions de rédaction stables
     * (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du requérant devant le tribunal administratif.
            Rédige un PROJET DE REQUÊTE EN ANNULATION dirigée contre une décision préfectorale
            de refus de titre de séjour ou de refus de regroupement familial.
            Ancre l'argumentation dans le CESEDA et le code de justice administrative.
            Structure la requête :
            - en-tête (POUR [requérant] / CONTRE le préfet [autorité auteure de la décision attaquée]),
            - EXPOSÉ DES FAITS ET DE LA PROCÉDURE,
            - RECEVABILITÉ (justifier le respect du délai de recours contre la décision attaquée),
            - DISCUSSION, en deux temps :
              * moyens de LÉGALITÉ EXTERNE — incompétence de l'auteur de l'acte, vice de procédure,
                défaut ou insuffisance de motivation,
              * moyens de LÉGALITÉ INTERNE — erreur de droit sur la catégorie de titre sollicitée,
                erreur manifeste d'appréciation, atteinte au droit au respect de la vie privée et
                familiale (article 8 de la Convention européenne des droits de l'homme),
            - PAR CES MOTIFS (annuler la décision de refus et enjoindre à l'administration de
              réexaminer la demande ou de délivrer le titre sollicité).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants et délais exacts des éléments fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_IMMIGRATION,
                ProcedureStageCatalog.FRANCE, "TA", "RECOURS_TITRE", "REQUERANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
