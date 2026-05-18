package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-42 — cellule de matrice : conclusions du <strong>défendeur</strong>
 * devant le tribunal de la famille, au fond, droit de la famille, Belgique.
 *
 * <p>Le prompt système est ancré dans la procédure belge (Code judiciaire) et le
 * droit de la famille belge (Code civil belge). La cellule est agrégée au
 * démarrage par {@link ConclusionPromptRegistry}.</p>
 */
@Component
public class TfFondDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — tribunal de la famille / fond / défendeur / droit de la
     * famille BE. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur devant le tribunal de la famille.
            Rédige un PROJET DE CONCLUSIONS au fond conforme à la procédure belge,
            régie par le Code judiciaire (art. 572bis : compétences du tribunal de
            la famille ; art. 1253ter et suivants : procédure devant le tribunal de
            la famille ; art. 748bis : conclusions de synthèse).
            Le droit applicable est le droit de la famille issu du Code civil belge :
            divorce pour désunion irrémédiable (art. 229), autorité
            parentale et hébergement de l'enfant (art. 373-374), contributions
            alimentaires pour les enfants (art. 203 et 203bis), pension alimentaire
            après divorce (art. 301).
            Adopte la posture du défendeur : réfute chef par chef les demandes
            formulées par le demandeur, conteste la mesure d'hébergement sollicitée
            et/ou le montant des contributions alimentaires réclamées, et formule
            le cas échéant des demandes reconventionnelles (hébergement principal
            de l'enfant, contribution alimentaire mise à charge du demandeur).
            Structure les conclusions selon l'usage belge :
            - en-tête (POUR [défendeur] / CONTRE [demandeur]),
            - EXPOSÉ DES FAITS, présenté selon la version du défendeur,
            - RECEVABILITÉ ET COMPÉTENCE (compétence du tribunal de la famille, art. 572bis du Code judiciaire),
            - DISCUSSION (réfutation chef par chef des demandes adverses, puis moyens reconventionnels),
            - DISPOSITIF introduit par « PAR CES MOTIFS, plaise au Tribunal de la famille de… »
              (débouter le demandeur de ses demandes ; statuer sur les demandes reconventionnelles chiffrées),
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
                ProcedureStageCatalog.BELGIQUE, "TF", "FOND", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
