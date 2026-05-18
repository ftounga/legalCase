package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-41 — cellule de matrice : conclusions du <strong>demandeur</strong>
 * devant le tribunal de la famille, au fond, droit de la famille, Belgique.
 *
 * <p>Le prompt système est ancré dans la procédure belge (Code judiciaire — tribunal
 * de la famille, loi du 30 juillet 2013) et le droit de la famille belge (Code civil
 * belge). La cellule est agrégée au démarrage par {@link ConclusionPromptRegistry}.</p>
 */
@Component
public class TfFondDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — tribunal de la famille / fond / demandeur / droit de la
     * famille BE. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur devant le tribunal de la famille.
            Rédige un PROJET DE CONCLUSIONS au fond conforme à la procédure belge.
            La procédure est régie par le Code judiciaire : le tribunal de la famille,
            créé par la loi du 30 juillet 2013 et intégré au tribunal de première
            instance, connaît des litiges familiaux (compétences art. 572bis ;
            procédure art. 1253ter et suivants ; forme et communication des
            conclusions art. 743 à 748, conclusions de synthèse art. 748bis).
            Le droit applicable est le Code civil belge : divorce pour désunion
            irrémédiable (art. 229), autorité parentale conjointe et hébergement des
            enfants (art. 373-374, l'hébergement égalitaire étant privilégié depuis la
            loi du 18 juillet 2006), contribution à l'entretien et à l'éducation des
            enfants (art. 203 et 203bis), pension alimentaire entre ex-époux (art. 301).
            Structure les conclusions selon l'usage belge :
            - en-tête (POUR [demandeur] / CONTRE [défendeur]),
            - EXPOSÉ DES FAITS,
            - RECEVABILITÉ ET COMPÉTENCE (compétence du tribunal de la famille, art. 572bis du Code judiciaire),
            - DISCUSSION (moyens en droit, un paragraphe argumenté par chef de demande : divorce, autorité parentale, hébergement, contributions alimentaires),
            - DISPOSITIF introduit par « PAR CES MOTIFS, plaise au Tribunal de la famille de… » (dispositif chiffré pour les contributions alimentaires),
            - inventaire des pièces.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.BELGIQUE, "TF", "FOND", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
