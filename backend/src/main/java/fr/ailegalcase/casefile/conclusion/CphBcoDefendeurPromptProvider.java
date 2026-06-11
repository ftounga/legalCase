package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-63 — cellule de matrice : acte du <strong>défendeur</strong> (employeur)
 * au stade <strong>Bureau de Conciliation et d'Orientation (BCO)</strong> du Conseil de
 * prud'hommes, droit du travail, France.
 *
 * <p>À réception de la convocation devant le BCO (art. L.1454-1 C. trav.), l'avocat de
 * l'employeur prépare ses <strong>observations / conclusions en défense</strong> : il
 * conteste les demandes du salarié et prend position, à titre subsidiaire, en vue d'un
 * éventuel renvoi au bureau de jugement en cas d'échec de la conciliation. Le prompt est
 * distinct du fond ({@link CphFondDefendeurPromptProvider}) et du référé
 * ({@link CphRefereDefendeurPromptProvider}), d'où une cellule dédiée.</p>
 *
 * <p>Cette cellule génère l'<strong>acte écrit</strong> ; elle ne reproduit pas
 * l'analyse stratégique d'opportunité de conciliation (outil décisionnel F-DT-84).</p>
 */
@Component
public class CphBcoDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — CPH / BCO / défendeur (employeur) / droit du travail FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur (employeur) convoqué devant le bureau de conciliation et \
            d'orientation (BCO) du Conseil de prud'hommes.
            Rédige un PROJET DE CONCLUSIONS EN DÉFENSE structuré :
            - en-tête (POUR [défendeur / employeur] / CONTRE [demandeur / salarié]),
            - RAPPEL DES FAITS ET DE LA PROCÉDURE,
            - DISCUSSION (réponse aux moyens du demandeur, un paragraphe argumenté par chef de demande \
            contesté : irrecevabilité, mal-fondé, contestation des montants),
            - le cas échéant, observations sur les demandes de provision (contestation sérieuse de \
            l'obligation devant la formation de référé),
            - PAR CES MOTIFS (dispositif : débouter le demandeur de ses demandes, subsidiairement réduction \
            des montants ; demande au titre de l'article 700 du CPC).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque contestation sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Ne te prononce pas sur l'opportunité de la conciliation (qui relève du seul avocat) : produis l'acte écrit.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.FRANCE, "CPH", "BCO", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
