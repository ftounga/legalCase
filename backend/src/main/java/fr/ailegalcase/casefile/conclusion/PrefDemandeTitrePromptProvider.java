package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-24 — cellule de matrice : <strong>mémoire d'admission au séjour</strong>
 * adressé à la préfecture, côté demandeur de titre, droit de l'immigration, France.
 *
 * <p>Cellule <strong>hors contentieux</strong> : le document est un mémoire / une note
 * de soutien adressé à l'administration à l'appui d'une demande de titre de séjour
 * (l'administration n'a pas encore statué) — pas une requête juridictionnelle. Le
 * registre argumenté reste argumenté mais non procédural. Ancrage : CESEDA.</p>
 */
@Component
public class PrefDemandeTitrePromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — Préfecture / demande de titre / demandeur de titre / droit de
     * l'immigration FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur de titre de séjour, en droit de l'immigration français.
            Rédige un PROJET DE MÉMOIRE DE SOUTIEN (note de soutien) adressé à la préfecture,
            à l'appui d'une demande d'admission au séjour.
            Il s'agit d'une démarche HORS CONTENTIEUX : une demande gracieuse adressée à
            l'administration, qui n'a pas encore statué. Ce n'est PAS une requête contentieuse
            ni une requête juridictionnelle — le ton est argumenté mais non procédural.
            Structure le mémoire :
            - identification du demandeur et de la demande (titre de séjour sollicité, autorité saisie : la préfecture),
            - EXPOSÉ DE LA SITUATION (situation personnelle, familiale et professionnelle du demandeur),
            - DISCUSSION (fondement légal du titre sollicité au regard du CESEDA — vie privée et
              familiale, salarié, étudiant, admission exceptionnelle au séjour… —, démonstration
              que les conditions de délivrance sont remplies, éléments d'intégration et
              d'ancrage en France),
            - DEMANDE (sollicite la délivrance du titre de séjour demandé).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie l'argumentation sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants et dates exacts des éléments fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_IMMIGRATION,
                ProcedureStageCatalog.FRANCE, "PREF", "DEMANDE_TITRE", "DEMANDEUR_TITRE");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
