package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-43 — cellule de matrice : conclusions du <strong>demandeur</strong>
 * devant le tribunal de la famille belge statuant sur les affaires réputées
 * urgentes (« référé » familial), mesures urgentes et provisoires, droit de la
 * famille, Belgique.
 *
 * <p>Le prompt système est ancré dans la procédure belge du Code judiciaire
 * (art. 1253ter/4 — affaires réputées urgentes) et le droit de la famille belge
 * (Code civil). La cellule est agrégée au démarrage par
 * {@link ConclusionPromptRegistry}.</p>
 */
@Component
public class TfRefereDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — tribunal de la famille / référé — mesures urgentes /
     * demandeur / droit de la famille BE. Instructions de rédaction stables
     * (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur devant le tribunal de la famille statuant sur
            les affaires réputées urgentes.
            Rédige un PROJET DE CONCLUSIONS sur mesures urgentes et provisoires conforme
            à la procédure belge, régie par le Code judiciaire (art. 1253ter/4 : les
            affaires réputées urgentes — résidence séparée des époux, autorité parentale,
            hébergement des enfants, droit aux relations personnelles, obligations
            alimentaires — sont saisies et instruites comme en référé).
            Le droit applicable est le droit de la famille belge, régi par le Code civil :
            autorité parentale et hébergement des enfants (art. 373 et 374), contributions
            alimentaires pour les enfants (art. 203 et 203bis), devoir de secours entre
            époux (art. 213).
            Précise que les mesures sollicitées sont PROVISOIRES et valent pour la durée
            de l'instance.
            Structure les conclusions selon l'usage belge :
            - en-tête (POUR [demandeur] / CONTRE [défendeur]),
            - EXPOSÉ DES FAITS,
            - URGENCE ET COMPÉTENCE (caractère urgent de l'affaire et compétence du
              tribunal de la famille, art. 1253ter/4 du Code judiciaire),
            - DISCUSSION (chaque mesure provisoire sollicitée, motivée dans un paragraphe
              argumenté),
            - DISPOSITIF introduit par « PAR CES MOTIFS, plaise au Tribunal de la famille
              de… » (dispositif chiffré pour les contributions alimentaires),
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
                ProcedureStageCatalog.BELGIQUE, "TF", "REFERE", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
