package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-29 — cellule de matrice : projet de <strong>mémoire de demande de
 * titre</strong> adressé à l'<strong>Office des étrangers</strong>, côté
 * <strong>demandeur de titre</strong>, droit de l'immigration, Belgique.
 *
 * <p>Cellule <em>hors contentieux</em> du droit belge des étrangers : l'Office des
 * étrangers n'a pas encore statué. Le document n'est pas une requête contentieuse
 * mais une <em>demande / un mémoire de soutien</em> adressé à l'administration, à
 * l'appui d'une demande d'autorisation de séjour. Procédure strictement ancrée dans
 * la loi du 15 décembre 1980 sur l'accès au territoire, le séjour, l'établissement
 * et l'éloignement des étrangers (notamment art. 9bis — autorisation de séjour pour
 * circonstances exceptionnelles ; art. 9ter — séjour pour raisons médicales ;
 * regroupement familial). Aucune référence au droit français (ni préfecture, ni
 * CESEDA) : le droit belge des étrangers n'est pas un miroir du droit français. Le
 * registre est argumenté mais non procédural — il n'y a pas de partie adverse ni de
 * dispositif de type « PAR CES MOTIFS ».</p>
 */
@Component
public class OeDemandeTitrePromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — Office des étrangers / demande de titre / demandeur de titre /
     * droit de l'immigration BE. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur d'un titre de séjour en Belgique. Tu rédiges un projet de \
            MÉMOIRE DE SOUTIEN adressé à l'OFFICE DES ÉTRANGERS, à l'appui d'une DEMANDE \
            D'AUTORISATION DE SÉJOUR.
            Il s'agit d'une DEMANDE adressée à l'administration, HORS CONTENTIEUX : l'Office des \
            étrangers n'a pas encore statué. Ce n'est PAS une requête contentieuse — il n'y a ni \
            partie adverse, ni juridiction, ni dispositif de type « PAR CES MOTIFS ». Le registre \
            est argumenté et sollicitant, non procédural.
            Procédure strictement ancrée dans la LOI DU 15 DÉCEMBRE 1980 sur l'accès au territoire, \
            le séjour, l'établissement et l'éloignement des étrangers — selon le fondement de la \
            demande : art. 9bis (autorisation de séjour pour circonstances exceptionnelles), \
            art. 9ter (séjour pour raisons médicales) ou regroupement familial.
            Mobilise EXCLUSIVEMENT le droit belge des étrangers : aucune référence à une \
            administration ou à une codification d'un autre État.
            Structure le mémoire ainsi :
            - IDENTIFICATION DU DEMANDEUR ET DE LA DEMANDE : identité du demandeur, nature de \
            l'autorisation de séjour sollicitée et fondement légal invoqué dans la loi du \
            15 décembre 1980,
            - EXPOSÉ DE LA SITUATION : situation personnelle, familiale et éléments d'ancrage \
            durable du demandeur en Belgique,
            - DISCUSSION : expose le fondement légal de l'autorisation sollicitée ; démontre, \
            selon le fondement, l'existence de circonstances exceptionnelles (art. 9bis) ou le \
            respect des conditions légales (art. 9ter, regroupement familial) ; développe les \
            éléments d'intégration du demandeur,
            - DEMANDE : sollicite de l'Office des étrangers l'OCTROI de l'autorisation de séjour \
            sollicitée.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie la demande sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            N'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_IMMIGRATION,
                ProcedureStageCatalog.BELGIQUE, "OE", "DEMANDE_TITRE", "DEMANDEUR_TITRE");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
