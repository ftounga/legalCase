package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-28 — cellule de matrice : recours en <strong>cassation
 * administrative</strong> devant le <strong>Conseil d'État de Belgique</strong>,
 * côté requérant, droit de l'immigration, Belgique.
 *
 * <p>Contentieux administratif belge. Le recours en cassation administrative est
 * dirigé contre un arrêt du Conseil du contentieux des étrangers (CCE). Ancrage :
 * lois coordonnées du 12 janvier 1973 sur le Conseil d'État. Le Conseil d'État,
 * juge de cassation administrative, contrôle la légalité et n'apprécie pas les
 * faits : le projet généré est un document de pur droit, sans demandes chiffrées.
 * Le prompt neutralise donc la consigne transverse « montants » des cellules de
 * fond — seul l'interdit « n'invente aucun chiffre » est conservé.</p>
 */
@Component
public class CeBeCassationRequerantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — Conseil d'État de Belgique / cassation administrative /
     * requérant / droit de l'immigration BE. Instructions de rédaction stables
     * (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du requérant devant le Conseil d'État de Belgique, juge de cassation \
            administrative.
            Rédige un PROJET DE REQUÊTE EN CASSATION ADMINISTRATIVE dirigée contre un arrêt du \
            Conseil du contentieux des étrangers (CCE), en matière de droit de l'immigration — \
            ancrage : lois coordonnées du 12 janvier 1973 sur le Conseil d'État.
            Structure le projet :
            - IDENTIFICATION DE L'ARRÊT ATTAQUÉ : désigne l'arrêt du Conseil du contentieux des \
            étrangers (date, numéro, sens du dispositif),
            - ANTÉCÉDENTS DE LA PROCÉDURE : rappelle le déroulement de la procédure devant le CCE \
            et les éléments utiles du litige,
            - MOYENS DE CASSATION : un titre par moyen ; chaque moyen vise la violation de la loi \
            ou des formes substantielles ou prescrites à peine de nullité, l'excès ou le \
            détournement de pouvoir ; identifie la partie critiquée de l'arrêt attaqué et expose \
            en quoi celle-ci est illégale,
            - PAR CES MOTIFS : demande au Conseil d'État de CASSER l'arrêt attaqué et de RENVOYER \
            l'affaire devant le Conseil du contentieux des étrangers.
            Le Conseil d'État, juge de cassation administrative, CONTRÔLE LA LÉGALITÉ : il \
            n'apprécie pas les faits. N'articule AUCUNE demande chiffrée — le juge de cassation \
            ne liquide pas le litige — et ne procède à aucune réappréciation des faits.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            N'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_IMMIGRATION,
                ProcedureStageCatalog.BELGIQUE, "CE_BE", "CASSATION", "REQUERANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
