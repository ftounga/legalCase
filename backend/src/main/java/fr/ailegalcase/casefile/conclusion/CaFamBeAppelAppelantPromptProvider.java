package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-44 — cellule de matrice : conclusions d'<strong>appel</strong> côté
 * <strong>appelant</strong> devant la <strong>chambre de la famille de la cour
 * d'appel</strong>, droit de la famille, Belgique.
 *
 * <p>Droit belge — pas un miroir de l'appel français. L'appel des jugements du
 * tribunal de la famille se porte devant la chambre de la famille de la cour
 * d'appel et obéit au Code judiciaire (art. 1050 et s. ; délai d'un mois
 * art. 1051 ; forme des conclusions et conclusions de synthèse art. 748bis ;
 * effet dévolutif). Le droit applicable au fond est le Code civil belge
 * (divorce, autorité parentale et hébergement, contributions alimentaires). Le
 * prompt système est figé et cachable ; la consigne de style F-98-47 est
 * appliquée par-dessus par {@link CaseConclusionPromptBuilder}.</p>
 */
@Component
public class CaFamBeAppelAppelantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — chambre de la famille de la cour d'appel / appel /
     * appelant / droit de la famille BE. Instructions de rédaction stables
     * (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat de l'appelant devant la chambre de la famille de la cour d'appel (Belgique).
            Rédige un PROJET DE CONCLUSIONS D'APPEL ancré dans la procédure belge.
            La chambre de la famille de la cour d'appel connaît de l'appel des jugements
            du tribunal de la famille : l'appel est régi par le Code judiciaire
            (art. 1050 et suivants ; le délai d'appel est d'un mois, art. 1051) et
            emporte effet dévolutif — la cour rejuge le litige dans les limites de la
            saisine. Les conclusions respectent la forme de l'art. 748bis du Code
            judiciaire et prennent la forme de conclusions de synthèse.
            Le droit applicable au fond est le Code civil belge : divorce, autorité
            parentale et hébergement de l'enfant, contributions alimentaires.
            Structure le projet :
            - en-tête (POUR [appelant] / CONTRE [intimé]),
            - EXPOSÉ DES FAITS ET DE LA PROCÉDURE, incluant le jugement entrepris
              rendu par le tribunal de la famille (date, dispositif critiqué),
            - RECEVABILITÉ DE L'APPEL (appel formé dans le délai d'un mois de
              l'art. 1051 du Code judiciaire, qualité et intérêt à agir),
            - DISCUSSION : critique du jugement entrepris chef par chef — expose les
              griefs d'appel sur le divorce, l'autorité parentale, l'hébergement ou
              les contributions alimentaires, un paragraphe argumenté par grief,
            - DISPOSITIF : « PAR CES MOTIFS, plaise à la Cour de mettre à néant le
              jugement entrepris en ce qu'il… ; statuant à nouveau,… » avec les
              demandes chiffrées,
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
                ProcedureStageCatalog.BELGIQUE, "CA_FAM_BE", "APPEL", "APPELANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
