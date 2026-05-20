# SF-212-02 — Frontend : section « licenciement pour faute grave / faute lourde »

> Feature F-212. Outil : `F-DT-36-licenciement-faute-grave-lourde`. Contrat API : `SF-212-01-backend-faute-grave-lourde.md` (figé).

## Objectif

Afficher dans le panneau d'outils décisionnels (onglet Décision) la section permettant à l'avocat de qualifier la faute disciplinaire et de visualiser l'impact financier sur les indemnités de rupture.

## Comportement nominal

Composant standalone `LicenciementFauteGraveLourdeSectionComponent` (`frontend/src/app/case-files/licenciement-faute-grave-lourde-section/`), enregistré dans le `TOOL_REGISTRY` sous `F-DT-36-licenciement-faute-grave-lourde`. Affiché en `CONTEXTUAL` (flag `motif_faute_grave_pressenti`).

Formulaire (11 champs du contrat API) : faits reprochés (texte), dates des faits, qualification employeur (select), intention de nuire (toggle), preuve, ancienneté, salaire mensuel brut, délais procédure. Bouton « Analyser » → `POST` → affichage :
- badge de qualification : `FAUTE_GRAVE` (or) / `FAUTE_LOURDE` (rouge) / `FAUTE_SIMPLE` (vert) ;
- tableau indemnités calculées (préavis / IL / CP / total) ;
- alerte si prescription de la faute dépassée ;
- liste des facteurs de qualification (libellé + fondement + explication).

**Pré-remplissage IA** : champs pré-remplis depuis `aiData.travailExtractedData.fauteGraveDetail` via helper `licenciement-faute-grave-lourde-section-prefill-rules.ts` ; badge `auto_awesome` sur chaque champ pré-rempli. `getPrefillCount()` alimente le badge du tab.

**Cohérence F-IA-03** : champs `DT36_QUALIFICATION_FAUTE`, `DT36_PRESCRIPTION_FAUTE`, `DT36_INTENTION_NUIRE`, `DT36_PROCEDURE_DISCIPLINAIRE` équipés de `CoherencePopoverTriggerDirective`.

**Gate `isFrance`** : si `workspaceCountry ≠ FRANCE`, bannière info « Outil réservé au droit du travail français ».

## Cas d'erreur

- Backend 422 (hors domaine / hors pays) → message « Outil réservé au droit du travail français », formulaire masqué.
- Backend 4xx/5xx → `MatSnackBar`, saisie conservée.
- `workspaceCountry ≠ FRANCE` → bannière info, pas de masquage silencieux.

## Critères d'acceptation

1. Section visible **uniquement** quand flag `motif_faute_grave_pressenti = true` (F-IA-04).
2. Champs pré-remplis depuis `aiData` ; badge `auto_awesome` présent sur champs pré-remplis.
3. Badge qualification couleur correcte (FAUTE_GRAVE : or ; FAUTE_LOURDE : rouge ; FAUTE_SIMPLE : vert).
4. Tableau indemnités affiché avec valeurs calculées.
5. Alerte prescription visible si `alertePrescriptionFaute = true`.
6. `getPrefillCount()` correct (badge tab F-244).
7. Self-check grep : `tool_id` identique dans `TOOL_REGISTRY` et seed backend.

## Plan de test

- **Jest** : rendu formulaire, pré-remplissage depuis `aiData`, badge qualification 3 niveaux, tableau indemnités, alerte prescription, gate `isFrance`, `getPrefillCount()`.
- Self-check grep : `grep F-DT-36-licenciement-faute-grave-lourde` dans `TOOL_REGISTRY` ↔ migration seed.

## Tables / endpoints / composants impactés

- **Nouveaux fichiers** : `licenciement-faute-grave-lourde-section.component.{ts,html,scss}`, `licenciement-faute-grave-lourde-section-prefill-rules.ts`, `licenciement-faute-grave-lourde.service.ts`.
- **Modifié** : `decisional-tools-panel.component.ts` (entrée `TOOL_REGISTRY`).

## Préoccupations transversales

**Outil décisionnel métier** + self-check grep `tool_id` obligatoire (mémoire `feedback_self_check_grep_pre_commit`). Vérifier endpoint SF-212-01 mergé avant merge de cette SF (mémoire `feedback_pre_merge_endpoint_check`).

## Hors périmètre

- Backend (→ SF-212-01).
- Génération de la lettre de licenciement (F-98).
