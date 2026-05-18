package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-44 — cellule de matrice : conclusions d'<strong>appel</strong> côté
 * <strong>intimé</strong> devant la <strong>chambre de la famille de la cour
 * d'appel</strong>, droit de la famille, Belgique.
 *
 * <p>Cellule miroir de la cellule appelant : l'intimé défend le jugement entrepris
 * rendu par le tribunal de la famille. La procédure d'appel est régie par le Code
 * judiciaire belge (art. 1050 et suivants ; délai d'un mois art. 1051 ; forme des
 * conclusions et conclusions de synthèse art. 748bis). La {@code DISCUSSION} réfute
 * grief par grief les moyens d'appel de l'appelant et, le cas échéant, soutient un
 * appel incident. Le dispositif vise la confirmation du jugement entrepris et, sur
 * appel incident, sa mise à néant des chefs défavorables à l'intimé. Droit
 * applicable au fond : Code civil belge (divorce, autorité parentale et
 * hébergement, contributions alimentaires).</p>
 *
 * <p>Ancrage strictement belge : aucune référence au droit français (ni juge aux
 * affaires familiales, ni article 954 du code de procédure civile).</p>
 */
@Component
public class CaFamBeAppelIntimePromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — chambre de la famille de la cour d'appel / appel / intimé /
     * droit de la famille BE. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat de l'intimé devant la chambre de la famille de la cour d'appel (Belgique).
            Rédige un PROJET DE CONCLUSIONS D'INTIMÉ ancré dans la procédure belge.
            L'appel des jugements du tribunal de la famille est porté devant la chambre
            de la famille de la cour d'appel et régi par le Code judiciaire
            (art. 1050 et suivants ; délai d'appel d'un mois, art. 1051). Les
            conclusions respectent la forme de l'art. 748bis du Code judiciaire et
            prennent la forme de conclusions de synthèse.
            Le droit applicable au fond est le Code civil belge : divorce, autorité
            parentale et hébergement de l'enfant, contributions alimentaires.
            Structure le projet :
            - en-tête (POUR [intimé] / CONTRE [appelant]),
            - EXPOSÉ DES FAITS ET DE LA PROCÉDURE, incluant le jugement entrepris
              rendu par le tribunal de la famille (juridiction, date, sens du
              dispositif) et la requête d'appel,
            - DISCUSSION orientée CONFIRMATION DU JUGEMENT ENTREPRIS : réfute grief
              par grief les griefs d'appel soulevés par l'appelant sur le divorce,
              l'autorité parentale, l'hébergement ou les contributions alimentaires,
              et démontre le bien-fondé du jugement entrepris (un paragraphe
              argumenté par grief) ; le cas échéant, développe un APPEL INCIDENT sur
              les chefs du jugement défavorables à l'intimé,
            - DISPOSITIF : « PAR CES MOTIFS, plaise à la Cour de confirmer le jugement
              entrepris en ce qu'il… ; sur appel incident, de mettre à néant le
              jugement entrepris en ce qu'il… ; statuant à nouveau,… »,
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
                ProcedureStageCatalog.BELGIQUE, "CA_FAM_BE", "APPEL", "INTIME");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
