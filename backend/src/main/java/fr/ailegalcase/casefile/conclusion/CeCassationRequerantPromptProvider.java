package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-22 — cellule de matrice : pourvoi en <strong>cassation</strong>
 * devant le <strong>Conseil d'État</strong>, côté requérant, droit de
 * l'immigration, France.
 *
 * <p>Contentieux administratif français. Le Conseil d'État, juge de cassation,
 * contrôle le droit et n'apprécie pas souverainement les faits : le projet
 * généré est un document de pur droit, sans demandes chiffrées. Ancrage : code
 * de justice administrative.</p>
 */
@Component
public class CeCassationRequerantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — Conseil d'État / cassation / requérant / droit de
     * l'immigration FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du requérant devant le Conseil d'État, juge de cassation.
            Rédige un PROJET DE REQUÊTE EN CASSATION dirigée contre un arrêt de cour
            administrative d'appel (ou un jugement rendu en dernier ressort), dans le
            cadre du contentieux administratif français — ancrage : code de justice
            administrative.
            Structure le projet :
            - en-tête (POUR [requérant] / désignation de la décision attaquée),
            - EXPOSÉ DES FAITS ET DE LA PROCÉDURE (rappelle la décision attaquée :
              juridiction, date, sens du dispositif),
            - MOYENS DE CASSATION : un moyen par paragraphe argumenté, chaque moyen
              visant un cas d'ouverture à cassation — erreur de droit, dénaturation
              des pièces du dossier, insuffisance ou contradiction de motifs,
              inexacte qualification juridique des faits,
            - PAR CES MOTIFS (conclure à l'annulation de la décision attaquée).
            Le Conseil d'État, juge de cassation, contrôle le droit : il n'apprécie
            pas souverainement les faits. N'inclus AUCUNE demande chiffrée — le juge
            de cassation ne liquide pas le litige.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et
            sur la stratégie retenue.
            N'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_IMMIGRATION,
                ProcedureStageCatalog.FRANCE, "CE", "CASSATION", "REQUERANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
