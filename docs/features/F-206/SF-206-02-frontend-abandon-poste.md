# SF-206-02 — Frontend : section « abandon de poste / présomption de démission »

> Feature F-206. Outil : `F-DT-42-abandon-poste-presomption-demission`. Contrat API : `SF-206-01-backend-abandon-poste.md` (figé).

## Objectif

Afficher dans le panneau d'outils décisionnels (onglet Décision) la section permettant à l'avocat de saisir et de visualiser l'analyse de la contestation d'une présomption de démission.

## Comportement nominal

Composant standalone `AbandonPostePresomptionDemissionSectionComponent` (`frontend/src/app/case-files/abandon-poste-presomption-demission-section/`), enregistré dans le `TOOL_REGISTRY` de `decisional-tools-panel.component.ts` sous `F-DT-42-abandon-poste-presomption-demission`. Affiché en `CONTEXTUAL` (visibilité pilotée par le backend via le flag `abandon_poste_detecte`).

Formulaire (12 champs du contrat API) : date de mise en demeure, mode de notification, délai accordé, mentions de la MED (toggles), motif d'absence invoqué, reprise/justification dans le délai. Bouton « Analyser » → `POST` → affichage du verdict (légende 3 niveaux : `CONTESTATION_SOLIDE` vert / `CONTESTATION_INCERTAINE` or / `CONTESTATION_DIFFICILE` rouge), score, liste des motifs de contestation (libellé + fondement + explication), date d'expiration du délai.

**Pré-remplissage IA** : au chargement, les champs sont pré-remplis depuis `aiData` (`travailExtractedData.abandonPosteDetail`) via le helper `abandon-poste-presomption-demission-section-prefill-rules.ts` ; chaque champ pré-rempli porte le badge de provenance `auto_awesome`. `getPrefillCount()` alimente le badge du tab.

**Cohérence F-IA-03** : les champs portant un `critereCode` (`DT42_DATE_MISE_EN_DEMEURE`, `DT42_DELAI_ACCORDE`, `DT42_MENTIONS_MED`, `DT42_MOTIF_LEGITIME`) sont équipés de `CoherencePopoverTriggerDirective` (cross-check `procedureChecks` / `aiQuestions`).

**Échéance inter-onglets** (invariant SF-206-00b) : la `dateExpirationDelai` est affichée avec une mention explicite renvoyant vers l'onglet **Suivi** comme lieu de suivi de l'échéance.

## Cas d'erreur

- Backend 422 (hors domaine / hors pays) → message « outil réservé au droit du travail français », formulaire masqué.
- Backend 4xx/5xx → message d'erreur non bloquant, saisie conservée.
- Pays workspace ≠ FRANCE → section non rendue (gate `isFrance`).

## Critères d'acceptation

1. La section apparaît dans l'onglet Décision **uniquement** quand le backend la déclare visible (flag `abandon_poste_detecte`).
2. Les 12 champs sont pré-remplis depuis `aiData` quand l'information existe ; badge `auto_awesome` présent sur les champs pré-remplis.
3. Le verdict s'affiche avec la légende couleur correcte et la liste des motifs.
4. La date d'expiration du délai est affichée et renvoie vers l'onglet Suivi.
5. `getPrefillCount()` reflète le nombre de champs pré-remplis (badge tab F-244).
6. Self-check grep pré-commit : `tool_id` identique côté `TOOL_REGISTRY` et seed backend.

## Plan de test

- **Jest** : rendu du formulaire, pré-remplissage depuis `aiData`, badges `auto_awesome`, appel service POST, affichage verdict 3 niveaux, gate `isFrance`, `getPrefillCount()`.
- Self-check grep : `grep` du `tool_id` dans `TOOL_REGISTRY` ↔ migration de seed.

## Tables / endpoints / composants impactés

- **Nouveaux fichiers** : `abandon-poste-presomption-demission-section.component.{ts,html,scss}`, `abandon-poste-presomption-demission-section-prefill-rules.ts`, service `abandon-poste-presomption-demission.service.ts` (`core/services/`).
- **Modifié** : `decisional-tools-panel.component.ts` (entrée `TOOL_REGISTRY`).
- Modèle TS du `Response` (interface dans le service ou un fichier `*.model.ts`).

## Préoccupations transversales

**Outil décisionnel métier** + **Navigation** : aucune route nouvelle, insertion dans un panneau existant. Self-check grep `tool_id` obligatoire (mémoire `feedback_self_check_grep_pre_commit`). Vérifier que l'endpoint backend SF-206-01 est mergé avant le merge de cette SF (mémoire `feedback_pre_merge_endpoint_check`).

## Hors périmètre

- Backend (→ SF-206-01).
- Création de l'échéance dans l'onglet Suivi (l'outil l'affiche et y renvoie ; le branchement F-69 est hors périmètre).
