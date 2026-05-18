package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-26 — cellule de matrice : projet de <strong>requête en recours de
 * plein contentieux</strong> devant le Conseil du contentieux des étrangers
 * (CCE), côté <strong>requérant</strong>, droit de l'immigration, Belgique.
 *
 * <p>Procédure ancrée dans le droit belge des étrangers : recours dirigé contre
 * une décision du Commissariat général aux réfugiés et aux apatrides (CGRA) en
 * matière de protection internationale. Le CCE statue en plein contentieux dans
 * cette matière (art. 39/2 § 1ᵉʳ de la loi du 15 décembre 1980 sur l'accès au
 * territoire, le séjour, l'établissement et l'éloignement des étrangers) : il
 * peut confirmer, réformer ou annuler la décision attaquée et reconnaître
 * lui-même la protection internationale. Le fond du droit repose sur la
 * convention de Genève du 28 juillet 1951 relative au statut des réfugiés. Aucun
 * miroir du droit français : ni CNDA, ni CESEDA, ni tribunal administratif.</p>
 */
@Component
public class CceRecoursRequerantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — Conseil du contentieux des étrangers / recours de plein
     * contentieux / requérant / droit de l'immigration BE. Instructions de
     * rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du requérant devant le Conseil du contentieux des étrangers (CCE), \
            juridiction administrative belge.
            Rédige un PROJET DE REQUÊTE EN RECOURS DE PLEIN CONTENTIEUX dirigé contre une \
            décision du Commissariat général aux réfugiés et aux apatrides (CGRA) en matière de \
            protection internationale.
            Ancrage juridique : loi du 15 décembre 1980 sur l'accès au territoire, le séjour, \
            l'établissement et l'éloignement des étrangers — l'article 39/2 § 1ᵉʳ donne au CCE \
            une compétence de pleine juridiction en matière d'asile ; convention de Genève du \
            28 juillet 1951 relative au statut des réfugiés.
            Statuant en PLEIN CONTENTIEUX, le CCE peut confirmer, réformer ou annuler la décision \
            attaquée et reconnaître lui-même la protection internationale.
            Structure la requête ainsi :
            - IDENTIFICATION DE LA DÉCISION ATTAQUÉE : décision du CGRA visée, sa date et sa portée,
            - EXPOSÉ DES FAITS ET DU PARCOURS DU REQUÉRANT : faits utiles, parcours migratoire et \
            antécédents de la procédure devant le CGRA,
            - RECEVABILITÉ : qualité du requérant, délai et forme du recours,
            - MOYENS : un titre argumenté par moyen — d'abord l'éligibilité au STATUT DE RÉFUGIÉ \
            au sens de la convention de Genève du 28 juillet 1951 et, SUBSIDIAIREMENT, \
            l'éligibilité à la PROTECTION SUBSIDIAIRE ; discute la crédibilité du récit et \
            l'actualité des craintes invoquées,
            - PAR CES MOTIFS : demande au Conseil du contentieux des étrangers de RÉFORMER la \
            décision du CGRA et de RECONNAÎTRE LA PROTECTION INTERNATIONALE au requérant.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            N'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_IMMIGRATION,
                ProcedureStageCatalog.BELGIQUE, "CCE", "RECOURS_PLEIN_CONTENTIEUX", "REQUERANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
