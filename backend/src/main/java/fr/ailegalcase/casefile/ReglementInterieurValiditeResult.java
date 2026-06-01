package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-218-35 : résultat interne business de l'analyse de validité d'un règlement
 * intérieur (art. L.1311-1 à L.1322-4, L.1321-1 et s. CT, F-DT-100). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise.
 * @param reglementExiste true si un règlement intérieur existe.
 * @param checklist items de validité (contenu obligatoire, clauses interdites,
 *        procédure de mise en place).
 * @param itemsObligatoiresManquants nombre de contenus obligatoires manquants.
 * @param clausesInterditesPresentes nombre de clauses interdites présentes.
 * @param statut verdict global de validité.
 * @param opposabilite opposabilité du règlement intérieur aux salariés.
 * @param consequences conséquences / points de vigilance identifiés.
 * @param baseJuridique fondements juridiques applicables.
 */
public record ReglementInterieurValiditeResult(
        int effectif,
        boolean reglementExiste,
        List<ReglementInterieurValiditeChecklistItem> checklist,
        int itemsObligatoiresManquants,
        int clausesInterditesPresentes,
        ReglementInterieurValiditeStatut statut,
        ReglementInterieurOpposabilite opposabilite,
        List<String> consequences,
        String baseJuridique
) {}
