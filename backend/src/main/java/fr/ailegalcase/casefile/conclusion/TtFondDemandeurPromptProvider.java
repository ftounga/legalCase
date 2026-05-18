package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-11 — cellule de matrice : conclusions du <strong>demandeur</strong>
 * (travailleur) devant le tribunal du travail, au fond, droit du travail, Belgique.
 *
 * <p>Le prompt système est ancré dans la procédure belge (Code judiciaire) et le
 * droit du travail belge (loi du 3 juillet 1978, CCT n° 109). La cellule est
 * agrégée au démarrage par {@link ConclusionPromptRegistry}.</p>
 */
@Component
public class TtFondDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — tribunal du travail / fond / demandeur (travailleur) / droit du
     * travail BE. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur (travailleur) devant le tribunal du travail.
            Rédige un PROJET DE CONCLUSIONS au fond conforme à la procédure belge,
            régie par le Code judiciaire (art. 578 : compétence du tribunal du travail ;
            art. 740 et suivants : forme et communication des conclusions).
            Le droit applicable est le droit du travail belge : loi du 3 juillet 1978
            relative aux contrats de travail, CCT n° 109 relative au licenciement
            manifestement déraisonnable, et les conventions collectives de travail
            pertinentes.
            Structure les conclusions selon l'usage belge :
            - en-tête (POUR [demandeur] / CONTRE [défendeur]),
            - EXPOSÉ DES FAITS,
            - RECEVABILITÉ ET COMPÉTENCE (compétence du tribunal du travail, art. 578 du Code judiciaire),
            - DISCUSSION (moyens en droit, un paragraphe argumenté par moyen),
            - DISPOSITIF introduit par « PAR CES MOTIFS, plaise au Tribunal du travail de… »,
            - inventaire des pièces.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.BELGIQUE, "TT", "FOND", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
