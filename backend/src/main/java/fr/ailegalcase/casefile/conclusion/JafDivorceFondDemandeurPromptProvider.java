package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-30 — cellule de matrice : conclusions du <strong>demandeur</strong>
 * dans un divorce contentieux au fond devant le juge aux affaires familiales
 * (tribunal judiciaire), droit de la famille, France.
 *
 * <p>Le prompt système ancre la rédaction sur le code civil (art. 233 et s. —
 * cas de divorce) et le code de procédure civile, et couvre les demandes
 * accessoires usuelles du divorce.</p>
 */
@Component
public class JafDivorceFondDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — JAF / divorce au fond / demandeur / droit de la famille FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur dans une procédure de divorce contentieux,
            au fond, devant le juge aux affaires familiales (tribunal judiciaire).
            Rédige un PROJET DE CONCLUSIONS structuré :
            - en-tête (POUR [demandeur] / CONTRE [défendeur]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION :
              * le cas de divorce invoqué, ancré sur le code civil (art. 233 et
                suivants — acceptation du principe de la rupture du mariage,
                altération définitive du lien conjugal, faute) et le code de
                procédure civile,
              * puis les demandes accessoires : prestation compensatoire,
                contribution à l'entretien et à l'éducation des enfants,
                exercice de l'autorité parentale, résidence des enfants,
                jouissance du logement, usage du nom,
              un paragraphe argumenté par moyen ou demande,
            - PAR CES MOTIFS (dispositif avec demandes chiffrées et numérotées).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "JAF", "DIVORCE_FOND", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
