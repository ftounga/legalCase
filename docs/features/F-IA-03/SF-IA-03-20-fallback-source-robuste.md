# Mini-spec — F-IA-03 / SF-IA-03-20 Migration Sonnet + fallback source + navigation sensée

## Identifiant

`F-IA-03 / SF-IA-03-20`

## Feature parente

`F-IA-03`

## Branche Git

`feat/SF-IA-03-20-fallback-source-robuste`

## Date de création

2026-04-16

## Statut

`draft`

---

## Objectif

Correctif post-validation staging SF-IA-03-19. Trois problèmes :

1. **Haiku rate ~30-40 % des sourcekeys** sur dossier complexe (confirmé sur Martin BTP : `convention_collective`, `salaire_brut_mensuel` non produits). Décision retenue **Option A** : migrer la génération de `source_explanations` dans le prompt Sonnet principal (qui voit les documents directement et produit déjà les faits F-93 avec source+extrait). Un seul appel LLM, cohérence maximale, coût réduit (~0.2¢ Sonnet vs 2¢ Haiku séparé + maintenance du 2e appel).
2. **Zone SOURCE vide** même après migration Sonnet possible sur des sourcekeys non détectés → conserver un fallback Java heuristique comme filet de sécurité depuis les faits F-93.
3. **Fallback frontend redirige vers `/synthesis?section=questions`** en dur, peu importe le champ survolé. Aberrant — navigate vers `/synthesis` neutre.

---

## Comportement attendu

### Backend — fallback depuis les faits F-93

Nouveau service `SourceExplanationFallback` qui, pour un sourceKey classique **non produit par Haiku**, scanne la synthèse JSON (champs `faits[]`, `points_juridiques[]`, `risques[]`, tous porteurs de `source` + `extrait` via F-93 traçabilité) :

- Recherche une occurrence contenant des **mots-clés** associés au sourceKey (map statique).
- Construit une `SourceExplanationData` avec :
  - `sourceType = DOCUMENT` si le fait a un `source` ≠ null
  - `sourceType = ANALYSIS_DETECTION` sinon
  - `label` = nom du document (ou "Synthèse du dossier")
  - `secondaryText` = `extrait` verbatim du fait trouvé, tronqué à 200 car
  - `anchorDocId` résolu via la liste des docs du dossier

Le generator **fusionne** Haiku-prioritaire + fallback-complémentaire : pour chaque sourceKey où Haiku n'a pas produit d'entrée, le fallback la complète.

Mots-clés par sourceKey (map statique) :

| sourceKey | Mots-clés |
|---|---|
| `convention_collective` | "convention", "CCN", "IDCC", "collective" |
| `salaire_brut_mensuel` | "salaire", "rémunération", "brut", "mensuel" |
| `date_entree` | "entrée", "embauche", "embauché", "recruté", "commencé" |
| `conges_contractuels` | "congés", "CP", "jours de congé" |
| `prime_anciennete_contractuelle` | "prime", "ancienneté" |
| `type_rupture` | "licenciement", "rupture conventionnelle", "démission" |
| `date_licenciement` | "licenciement", "notification", "lettre" |
| `duree_mariage` | "mariage", "époux", "conjoint" |
| `revenus_conjoints` | "revenus", "salaire conjoint" |
| `nationalite_ue` | "nationalité", "UE", "ressortissant" |
| `date_notification_decision_contestee` | "notification", "refus", "décision" |
| `type_titre_sejour` | "titre", "séjour", "carte" |
| `type_recours` | "recours", "contestation" |

Les codes critères F-96 (ex. `FR_CONVOCATION`, `RC_CONSENTEMENT`) ont un fallback différent : lookup direct dans `procedure_checks` → `{label: description, secondaryText: raison}`.

### Frontend — navigation fallback sensée

Au clic sur une card **sans explanation**, ne plus naviguer vers `OPEN_QUESTIONS`. Naviguer vers `/case-files/{id}/synthesis` **sans query param** → l'utilisateur atterrit sur la synthèse et peut choisir.

Côté UX, changer aussi le libellé du bouton de secours : "Voir la synthèse" (au lieu de "Voir dans la synthèse") pour être sémantiquement correct.

---

## Analyse de cohérence transversale

- [x] **Autres outils** : le fallback est dans `SourceExplanationFallback`, consommé par `SourceExplanationGenerator` unique → 10 outils bénéficient.
- [x] **Autres pays** : les mots-clés sont en français (FR + BE partagent le vocabulaire juridique). Aucune logique pays-spécifique dans le fallback.
- [x] **Autres domaines** : les sourcekeys couvrent Travail + Famille + Immigration (duree_mariage, nationalite_ue, type_recours, etc.).
- [x] **Nouveau pattern service partagé** : oui — `SourceExplanationFallback` est un nouveau service interne au générateur. Il est privé à la feature F-IA-03-15+. Pas de risque de divergence car il est consommé par un seul point d'entrée (`SourceExplanationGenerator.generate()`).

### Décision

- [x] Étendu à toutes les cibles via composants partagés.

---

## Critères d'acceptation

- [ ] Nouveau service backend `SourceExplanationFallback.buildFallbacks(analysisJson, documents, f96Checks, existingSourceKeys)` retourne la liste de `SourceExplanationData` de secours pour les sourcekeys classiques non déjà couverts.
- [ ] `SourceExplanationGenerator.generate()` : après parsing Haiku, appelle le fallback pour compléter. Fusion Haiku-prioritaire.
- [ ] Mots-clés par sourceKey implémentés (map statique). Matching insensible à la casse et aux accents.
- [ ] Pour un critère F-96 non couvert par Haiku, fallback sur `ProcedureCheck` correspondant.
- [ ] Frontend `CoherencePopoverTriggerDirective.onSourceClicked()` : si `!explanation`, navigate vers `/synthesis` sans query param (pas `OPEN_QUESTIONS`).
- [ ] Frontend `CoherencePopoverComponent.actionLabel()` : si pas d'explanation, retourne "Voir la synthèse".
- [ ] Tests backend : 4+ cas fallback (convention trouvée via "CCN", salaire trouvé via "€", date_entree vide si aucun match, F-96 via procedure_checks).
- [ ] Tests frontend : click sans explanation → navigator appelé avec route neutre.
- [ ] Non-régression 862 backend, 974 frontend, build prod vert.

---

## Hors scope

- Remplissage rétroactif des dossiers analysés (ré-analyse requise).
- Fallbacks pour sourcekeys F-FA / F-IM non listés.
- Sélection multi-matches (on prend le premier match).
- Matching sémantique Haiku (on reste heuristique mots-clés).

---

## Technique

### Backend

- Nouveau `SourceExplanationFallback.java` (service Spring).
- `SourceExplanationGenerator` consomme le fallback.
- Aucune migration.

### Frontend

- `CoherencePopoverTriggerDirective` : modifier `onSourceClicked` pour navigate `/case-files/{id}/synthesis` sans params.
- `CoherencePopoverComponent` : libellé du bouton "Voir la synthèse" si `!explanation`.

---

## Plan de test

### Tests backend

- [ ] `SourceExplanationFallbackTest` : match convention via mot-clé "CCN", salaire via "€", prime via "prime+ancienneté", pas de match → liste vide.
- [ ] Fusion : sourceKeys déjà présents dans Haiku output → fallback ignoré.
- [ ] F-96 : lookup `ProcedureCheck` fonctionne.

### Tests frontend

- [ ] Directive : click sur card sans explanation → navigator appelé sans actionTarget, route /synthesis.
- [ ] Composant : actionLabel retourne "Voir la synthèse" si pas d'explanation.

---

## Analyse d'impact

- [x] Aucune préoccupation transversale structurelle.

### Composants impactés

| Composant | Impact |
|---|---|
| `SourceExplanationGenerator` | Fusion avec nouveau fallback |
| `SourceExplanationFallback` (nouveau) | Service interne |
| `CoherencePopoverTriggerDirective` | Fallback navigation changée |
| `CoherencePopoverComponent` | Libellé bouton adapté |

---

## Dépendances

- `SF-IA-03-19 Done` — navigation effective + auto-width + prompt raffiné.

---

## Notes

- **Pourquoi fallback heuristique plutôt que "Haiku doit être parfait"** : Haiku peut rater un sourceKey (contrat ambigu, extraction partielle, limite de tokens). Le fallback garantit qu'au moins une info de source apparaît — c'est un filet de sécurité.
- **Pourquoi ne pas appeler Haiku 2× pour retenter** : coût token + latence supplémentaire. Le fallback heuristique est immédiat et utilise les extraits F-93 déjà vérifiés.
- **Pourquoi /synthesis sans params en fallback** : l'utilisateur atterrit sur le panneau d'ensemble, il peut scroll vers la section pertinente lui-même. C'est moins frustrant que d'être envoyé systématiquement aux questions.
