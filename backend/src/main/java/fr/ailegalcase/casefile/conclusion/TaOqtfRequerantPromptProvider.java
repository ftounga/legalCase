package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-18 — cellule de matrice : <strong>requête en annulation</strong> d'une
 * obligation de quitter le territoire français (OQTF) devant le tribunal administratif,
 * côté <strong>requérant</strong>, droit de l'immigration, France.
 *
 * <p>Contentieux <em>administratif</em> : le document produit est une <strong>requête</strong>
 * devant le tribunal administratif — et non des « conclusions » au sens judiciaire. Ancrage :
 * CESEDA (L.611-1 et s.) et code de justice administrative.</p>
 */
@Component
public class TaOqtfRequerantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — TA / recours OQTF / requérant / droit de l'immigration FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du requérant devant le tribunal administratif.
            Rédige un PROJET DE REQUÊTE EN ANNULATION dirigée contre une obligation de quitter le
            territoire français (OQTF) et ses décisions accessoires (refus de séjour, décision
            fixant le délai de départ volontaire, interdiction de retour sur le territoire français,
            décision fixant le pays de destination).
            Il s'agit d'un contentieux ADMINISTRATIF : le document est une REQUÊTE devant le tribunal
            administratif, et non des « conclusions » au sens judiciaire.
            Ancrage juridique : CESEDA (L.611-1 et suivants) et code de justice administrative.
            Structure la requête ainsi :
            - en-tête (POUR [requérant] / CONTRE la décision du préfet),
            - EXPOSÉ DES FAITS ET DE LA PROCÉDURE,
            - RECEVABILITÉ (justifier le respect du délai de recours contre l'OQTF),
            - DISCUSSION, en deux blocs :
              * moyens de LÉGALITÉ EXTERNE (incompétence de l'auteur de l'acte, vice de procédure,
                défaut ou insuffisance de motivation),
              * moyens de LÉGALITÉ INTERNE (erreur de droit, erreur manifeste d'appréciation,
                atteinte disproportionnée au droit au respect de la vie privée et familiale —
                article 8 de la Convention européenne des droits de l'homme),
              un paragraphe argumenté par moyen,
            - PAR CES MOTIFS (dispositif : annuler l'OQTF et les décisions accessoires).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les éléments exacts des données fournies — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_IMMIGRATION,
                ProcedureStageCatalog.FRANCE, "TA", "RECOURS_OQTF", "REQUERANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
