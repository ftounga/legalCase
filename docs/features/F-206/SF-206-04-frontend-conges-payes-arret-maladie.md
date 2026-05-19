# SF-206-04 — Frontend : section « congés payés acquis pendant arrêt maladie »

> Feature F-206. Outil : `F-DT-75-conges-payes-arret-maladie`. Contrat API : `SF-206-03-backend-conges-payes-arret-maladie.md` (figé).

## Objectif

Afficher dans le panneau d'outils décisionnels (onglet Décision) la section permettant à l'avocat de chiffrer le rappel de congés payés acquis pendant un arrêt maladie.

## Comportement nominal

Composant standalone `CongesPayesArretMaladieSectionComponent` (`frontend/src/app/case-files/conges-payes-arret-maladie-section/`), enregistré dans le `TOOL_REGISTRY` sous `F-DT-75-conges-payes-arret-maladie`. Affiché en `CONTEXTUAL` (flag `arret_maladie_long_detecte`).

Formulaire (6 champs du contrat API) : type d'arrêt, nombre de mois d'arrêt, salarié encore en poste (toggle), date de rupture du contrat (conditionnel), jours de CP déjà accordés, salaire brut mensuel. Bouton « Calculer » → `POST` → affichage : jours acquis, jours de rappel, valorisation indicative en euros, verdict (`RAPPEL_SIGNIFICATIF` / `RAPPEL_LIMITE` / `PAS_DE_RAPPEL` / `ACTION_FORCLOSE`), date limite d'action.

**Pré-remplissage IA** : champs pré-remplis depuis `aiData` (`travailExtractedData.congesPayesArretMaladieDetail` + `salaireBrutMensuel` existant) via `conges-payes-arret-maladie-section-prefill-rules.ts` ; badge `auto_awesome`.

**Cohérence F-IA-03** : champs `DT75_TYPE_ARRET`, `DT75_DUREE_ARRET`, `DT75_SALARIE_EN_POSTE` équipés de `CoherencePopoverTriggerDirective`.

**Échéance inter-onglets** : `dateLimiteAction` affichée avec mention renvoyant vers l'onglet **Suivi**. Si `actionEncoreOuverte=false`, alerte visuelle (verdict `ACTION_FORCLOSE`).

## Cas d'erreur

- Backend 422 → message « outil réservé au droit du travail français », formulaire masqué.
- Backend 4xx/5xx → message non bloquant, saisie conservée.
- Pays ≠ FRANCE → section non rendue.

## Critères d'acceptation

1. La section apparaît dans l'onglet Décision uniquement sur visibilité backend (flag `arret_maladie_long_detecte`).
2. Le champ « date de rupture » n'est requis et visible que si `salarieEncoreEnPoste=false`.
3. Pré-remplissage depuis `aiData` + badges `auto_awesome`.
4. Le verdict `ACTION_FORCLOSE` déclenche une alerte visuelle ; la date limite renvoie vers l'onglet Suivi.
5. `getPrefillCount()` correct.
6. Self-check grep `tool_id` (`TOOL_REGISTRY` ↔ seed).

## Plan de test

- **Jest** : rendu formulaire, champ conditionnel date de rupture, pré-remplissage, badges, appel POST, affichage chiffrage + 4 verdicts, alerte `ACTION_FORCLOSE`, gate `isFrance`.
- Self-check grep `tool_id`.

## Tables / endpoints / composants impactés

- **Nouveaux fichiers** : `conges-payes-arret-maladie-section.component.{ts,html,scss}`, `conges-payes-arret-maladie-section-prefill-rules.ts`, service `conges-payes-arret-maladie.service.ts`.
- **Modifié** : `decisional-tools-panel.component.ts` (`TOOL_REGISTRY`).

## Préoccupations transversales

**Outil décisionnel métier** : groupe thématique F-169 « Rappels et indemnités salariales » (ce n'est **pas** une rupture). Self-check grep `tool_id`. Vérifier merge backend SF-206-03 avant merge frontend.

## Hors périmètre

- Backend (→ SF-206-03).
- Branchement de la date limite dans l'onglet Suivi (affichage + renvoi seulement).
