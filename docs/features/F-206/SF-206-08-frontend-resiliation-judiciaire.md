# SF-206-08 — Frontend : section « résiliation judiciaire du contrat »

> Feature F-206. Outil : `F-DT-40-resiliation-judiciaire-cph`. Contrat API : `SF-206-07-backend-resiliation-judiciaire.md` (figé).

## Objectif

Afficher dans le panneau d'outils décisionnels (onglet Décision) la section permettant à l'avocat d'évaluer l'opportunité d'une demande de résiliation judiciaire du contrat aux torts de l'employeur.

## Comportement nominal

Composant standalone `ResiliationJudiciaireCphSectionComponent` (`frontend/src/app/case-files/resiliation-judiciaire-cph-section/`), enregistré dans le `TOOL_REGISTRY` sous `F-DT-40-resiliation-judiciaire-cph`. Affiché en `CONTEXTUAL` (flag `resiliation_judiciaire_envisagee`).

Formulaire (13 champs du contrat API) : 8 manquements (booléens), montant des impayés, persistance au jour de la demande, salarié toujours en poste, licenciement intervenu en cours d'instance, commentaire libre. Bouton « Analyser » → `POST` → affichage : verdict 3 niveaux, score 0-100, liste des manquements retenus, date d'effet probable (`DATE_DECISION` / `DATE_LICENCIEMENT`).

Message persistant : « Voie **moins risquée** que la prise d'acte — un rejet ne rompt pas le contrat. À comparer avec l'outil **prise d'acte**. »

**Pré-remplissage IA** : champs pré-remplis depuis `aiData` (`travailExtractedData.resiliationJudiciaireDetail`) via `resiliation-judiciaire-cph-section-prefill-rules.ts` ; badge `auto_awesome`.

**Cohérence F-IA-03** : champs `DT40_DEFAUT_PAIEMENT`, `DT40_HARCELEMENT`, `DT40_MANQUEMENT_SECURITE`, `DT40_MODIFICATION_CONTRAT`, `DT40_MANQUEMENTS_PERSISTANTS` équipés de `CoherencePopoverTriggerDirective`.

## Cas d'erreur

- Backend 422 → message « outil réservé au droit du travail français », formulaire masqué.
- Backend 4xx/5xx → message non bloquant, saisie conservée.
- Pays ≠ FRANCE → section non rendue.

## Critères d'acceptation

1. La section apparaît uniquement sur visibilité backend (flag `resiliation_judiciaire_envisagee`).
2. Les 13 champs sont pré-remplis depuis `aiData` ; badges `auto_awesome`.
3. Verdict 3 niveaux avec légende couleur ; manquements retenus listés ; date d'effet probable affichée.
4. Message « voie moins risquée / comparer avec prise d'acte » visible.
5. `getPrefillCount()` correct.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu formulaire, pré-remplissage, badges, appel POST, affichage des 3 verdicts + 2 dates d'effet probable, gate `isFrance`.
- Self-check grep `tool_id`.

## Tables / endpoints / composants impactés

- **Nouveaux fichiers** : `resiliation-judiciaire-cph-section.component.{ts,html,scss}`, `resiliation-judiciaire-cph-section-prefill-rules.ts`, service `resiliation-judiciaire-cph.service.ts`.
- **Modifié** : `decisional-tools-panel.component.ts` (`TOOL_REGISTRY`).

## Préoccupations transversales

**Outil décisionnel métier** : groupe thématique F-169 « Rupture — initiative salarié / torts employeur ». Self-check grep `tool_id`. Vérifier merge backend SF-206-07 avant merge frontend.

## Hors périmètre

- Backend (→ SF-206-07).
- Génération des conclusions de résiliation judiciaire (F-98).
- Chiffrage des indemnités consécutives (F-DT-09).
