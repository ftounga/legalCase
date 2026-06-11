package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-62 — cellule de matrice : acte du <strong>demandeur</strong> (salarié)
 * au stade <strong>Bureau de Conciliation et d'Orientation (BCO)</strong> du Conseil de
 * prud'hommes, droit du travail, France.
 *
 * <p>Le BCO est la porte d'entrée de la saisine du CPH (art. R.1452-1 s., L.1454-1
 * C. trav.). À ce stade, l'acte du demandeur n'est pas une « conclusion au fond » de
 * bureau de jugement : c'est une <strong>requête de saisine valant conclusions</strong>
 * qui expose les faits, articule les moyens, porte les demandes au fond et, le cas
 * échéant, un volet de <strong>provisions devant la formation de référé</strong>
 * (salaires impayés, remise des documents de fin de contrat sous astreinte). Le prompt
 * est donc distinct du fond ({@link CphFondDemandeurPromptProvider}) et du référé
 * ({@link CphRefereDemandeurPromptProvider}), d'où une cellule dédiée.</p>
 *
 * <p>Cette cellule génère l'<strong>acte écrit</strong> ; elle ne reproduit pas
 * l'analyse stratégique d'opportunité de conciliation, qui relève de l'outil décisionnel
 * F-DT-84 (conciliation CPH/BCA).</p>
 */
@Component
public class CphBcoDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — CPH / BCO / demandeur (salarié) / droit du travail FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur (salarié) qui saisit le Conseil de prud'hommes ; \
            l'affaire est introduite devant le bureau de conciliation et d'orientation (BCO).
            Rédige une REQUÊTE AUX FINS DE SAISINE DU CONSEIL DE PRUD'HOMMES VALANT CONCLUSIONS, structurée :
            - en-tête (À Mesdames et Messieurs les Président et Conseillers du Conseil de prud'hommes ; \
            POUR [demandeur / salarié] / CONTRE [défendeur / employeur]),
            - RAPPEL DES FAITS ET DE LA PROCÉDURE,
            - DISCUSSION (moyens en droit, un paragraphe argumenté par moyen, sur les demandes au fond),
            - le cas échéant, une rubrique SUR LES DEMANDES DE PROVISION (formation de référé) lorsque \
            l'obligation n'est pas sérieusement contestable (rappels de salaire, indemnités) et la remise \
            des documents de fin de contrat (bulletins de paie, certificat de travail, attestation France \
            Travail) au besoin sous astreinte,
            - PAR CES MOTIFS (dispositif : demandes au fond chiffrées et, s'il y a lieu, demandes de provision).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Ne te prononce pas sur l'opportunité de la conciliation (qui relève du seul avocat) : produis l'acte écrit.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.FRANCE, "CPH", "BCO", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
