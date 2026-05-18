package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-19 — cellule de matrice : <strong>requête en référé-liberté</strong>
 * (art. L.521-2 du code de justice administrative) devant le tribunal administratif,
 * côté <strong>requérant</strong>, droit de l'immigration, France.
 *
 * <p>Le référé-liberté a un standard probatoire propre : l'administration doit porter
 * une atteinte <strong>grave et manifestement illégale</strong> à une <strong>liberté
 * fondamentale</strong>, en cas d'<strong>urgence</strong>. Ce prompt est distinct du
 * référé-suspension (SF-98-20).</p>
 */
@Component
public class TaRefereLiberteRequerantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — TA / référé-liberté / requérant / droit de l'immigration FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du requérant devant le tribunal administratif, juge des référés.
            Rédige un PROJET DE REQUÊTE EN RÉFÉRÉ-LIBERTÉ fondée sur l'article L.521-2 du
            code de justice administrative : saisi d'une demande en ce sens justifiée par
            l'urgence, le juge des référés peut ordonner toute mesure nécessaire à la
            sauvegarde d'une liberté fondamentale à laquelle une personne morale de droit
            public ou un organisme de droit privé chargé de la gestion d'un service public
            aurait porté, dans l'exercice d'un de ses pouvoirs, une atteinte grave et
            manifestement illégale.
            Structure la requête :
            - en-tête (POUR [requérant] / CONTRE [administration mise en cause]),
            - EXPOSÉ DES FAITS ET DE LA PROCÉDURE,
            - URGENCE : caractérise l'urgence particulière propre au référé-liberté
              (situation appelant une mesure de sauvegarde à très bref délai),
            - LIBERTÉ FONDAMENTALE EN CAUSE ET ATTEINTE GRAVE ET MANIFESTEMENT ILLÉGALE :
              identifie la liberté fondamentale invoquée puis démontre l'atteinte grave
              et manifestement illégale portée par l'administration,
            - MESURES SOLLICITÉES (injonction(s) demandée(s) au juge des référés),
            - PAR CES MOTIFS (dispositif).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la
            stratégie retenue.
            Reprends les montants et dates exacts des éléments fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_IMMIGRATION,
                ProcedureStageCatalog.FRANCE, "TA", "REFERE_LIBERTE", "REQUERANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
