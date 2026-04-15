# Mini-spec — F-FA-05 / SF-FA-05-05 Enrichissement F-IA-03 complet

## Identifiant

`F-FA-05 / SF-FA-05-05`

## Feature parente

`F-FA-05` — Partage immobilier (divorce)

## Statut

`draft`

## Date de création

2026-04-15

## Branche Git

`feat/SF-FA-05-05-enrichissement-coherence-ia`

---

## Objectif

Compléter l'alignement F-IA-03 de F-FA-05 Partage immobilier identifié par l'audit 2026-04-15. Actuellement F-FA-05 n'utilise que la source "IA best-match" (sur `liquidationCommunaute.actifCommun/passifCommun`). Cette SF ajoute les 3 autres sources standard (F96 checklist procédurale, Question IA, Pièce manquante) pour ses 2 champs numériques `valeurVenale` et `capitalRestantDu`, avec détection MULTI.

Après merge : **10/10 outils conformes au pattern F-IA-03 complet**.

---

## Comportement attendu

### Sources de cohérence (ordre hiérarchique)

1. **F96 checklist procédurale** : `procedure_checks` avec `critere_code ∈ {FA05_VALEUR_VENALE, FA05_CAPITAL_RESTANT}` et `statut = VERIFIED` → `expectedValue` (décimal en chaîne) comparé à la saisie user, seuil 10%.
2. **Question IA répondue "oui"** : `ai_questions` avec `critere_code ∈ {FA05_VALEUR_VENALE, FA05_CAPITAL_RESTANT}` et `expected_value` décimal → idem.
3. **IA best-match (existant)** : `liquidationCommunaute.actifCommun/passifCommun` avec matching heuristique sur mots-clés.
4. **Pièce manquante** : `pieces_manquantes` avec `critere_code ∈ FA05_*` → contributor supplémentaire uniquement (pas de `expected_value`).

### Niveau d'alerte

- `warning` (seul niveau appliqué — pas de critère juridiquement bloquant sur une valeur immo).
- Source `MULTI` si au moins 2 contributeurs convergent vers la même valeur attendue (avec tolérance 10%).

### Backend — prompt enrichment

Extension de l'enum `critere_code` dans `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` :

- `FA05_VALEUR_VENALE` : code numérique, `expected_value` obligatoire = valeur vénale en euros (ex. "350000").
- `FA05_CAPITAL_RESTANT` : code numérique, `expected_value` obligatoire = capital restant dû en euros (ex. "120000").

L'IA peut donc désormais émettre :
```json
{
  "points_procedure": [
    {"texte": "Valeur vénale attestée par expertise", "critere_code": "FA05_VALEUR_VENALE", "expected_value": "350000"}
  ]
}
```
Et aussi taguer une pièce manquante correspondante via `pieces_manquantes`.

### Frontend

- Nouveaux `@Input()` : `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- Signaux miroirs.
- `ngOnInit` / `ngOnChanges` synchronisation.
- `coherenceAlerts` computed étendu : 4 sources hiérarchisées.
- Helpers privés `buildF96Index`, `buildQuestionsIndex`, `buildPiecesIndex`, `parseNumericExpected`.
- Détection `MULTI` via `collectSupportingSources` + `multiOrSingle`.
- Badges dans le template avec tooltip agrégeant les contributeurs.

### Intégration

`case-file-detail.component.html` : brancher les 3 nouveaux inputs sur `<app-partage-immobilier-section>`.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| `expected_value` non parseable en nombre | Source ignorée (fail-open) |
| Plusieurs procedure_checks pour même critère | Premier VERIFIED pris, conflits ignorés |
| Question IA avec réponse "non" | Question ignorée (convention "oui = expected respecté") |
| `liquidationCommunaute` null | Source IA best-match désactivée, autres sources actives |
| Aucune source disponible | Aucune alerte — comportement par défaut (inchangé) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils** : audit 2026-04-15 validé — 9/10 outils déjà conformes. F-FA-05 est le dernier.
- [x] **Autres pays** : non applicable (pattern structurel frontend + prompt).
- [x] **Autres domaines** : non applicable.
- [x] **Autres UI patterns** : alignement sur pattern F-DT-07 (alertes numériques avec seuil %).
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript** : interfaces existantes `ProcedureCheck`, `AiQuestion`, `PieceManquanteEntry` réutilisées.
- [x] **Record / DTO backend** : aucun changement (codes acceptés via enum enrichi).
- [x] **Service / logique** : prompt mis à jour — les codes `FA05_*` sont injectables dans `procedure_checks.critere_code` et `pieces_manquantes.critere_code`.
- [x] **Entité JPA + schéma DB** : aucun changement — `critere_code` est un `VARCHAR` existant.
- [x] **Tests existants** : spec F-FA-05 enrichi avec 4 tests nouveaux.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| F-FA-05 | Oui | Intégré dans cette SF |
| Autres outils | Non | Déjà conformes après SF-IA-03-14 |

### Décision

- [x] Étendu à la cible applicable (F-FA-05)
- [ ] Subfeature parallèle
- [ ] Backlog
- [x] Non applicable aux 9 autres outils

---

## Critères d'acceptation

- [ ] Prompt `EnrichedAnalysisService` étendu avec codes `FA05_VALEUR_VENALE` + `FA05_CAPITAL_RESTANT` (numériques, `expected_value` obligatoire) dans les listes `points_procedure.critere_code` et `pieces_manquantes.critere_code`.
- [ ] `PartageImmobilierSectionComponent` : 3 inputs `procedureChecks`, `aiQuestions`, `piecesManquantes` + signals miroirs + ngOnChanges.
- [ ] Interface `PartageCoherenceAlert` étendue avec `source: PartageAlertSource`, `contributors`, `f96Raison`, `questionText`, `questionAnswer`, `pieceTexte`.
- [ ] Type `PartageAlertSource = 'F96' | 'QUESTION_IA' | 'IA' | 'PIECE_MANQUANTE' | 'MULTI'`.
- [ ] `coherenceAlerts` computed : 4 sources hiérarchisées, seuil 10%, détection MULTI.
- [ ] Helpers `buildF96Index`, `buildQuestionsIndex`, `buildPiecesIndex`, `parseNumericExpected`.
- [ ] `alertTooltip(alert)` et `alertBadgeLabel(alert)` exposées.
- [ ] Template : badges enrichis avec tooltip + source.
- [ ] `case-file-detail` : 3 nouveaux bindings.
- [ ] Tests Jest ≥ 4 : alerte F96 numérique, alerte Question IA numérique, alerte PIECE_MANQUANTE contributor, convergence MULTI (F96 + IA best-match).
- [ ] Non-régression sur tests F-FA-05 existants.
- [ ] 953+ tests frontend verts, build OK.

---

## Périmètre

### Hors scope

- Ajout de codes critères additionnels (seulement les 2 champs existants).
- Modification du backend calculateur (aucun impact sur le calcul).
- Refactorisation du pattern en classe abstraite partagée.
- Modification de la logique "best-match IA" existante.
- Détection F-IA-03 sur d'autres champs de F-FA-05 (quotePartAttributaire, isDivorce) — non critiques.

---

## Valeurs initiales

Sans objet.

---

## Contraintes de validation

| Champ | Obligatoire | Valeurs | Normalisation |
|---|---|---|---|
| `critere_code` | Non | FA05_VALEUR_VENALE / FA05_CAPITAL_RESTANT | upper-case |
| `expected_value` (numérique) | Oui si `critere_code` rempli | décimal en chaîne (ex. "350000.50") | parseFloat |

---

## Technique

### Endpoints

Aucun.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants backend

- `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` : enum étendu.

### Composants Angular

- `partage-immobilier-section.component.ts` : enrichi.
- `partage-immobilier-section.component.html` : template avec badges + tooltip.
- `partage-immobilier-section.component.scss` : pas de nouvelle classe (réutilise les badges existants).
- `case-file-detail.component.html` : 3 bindings.
- Spec enrichi.

---

## Plan de test

### Tests unitaires Jest

- [ ] Alerte F96 : procedure_check VERIFIED avec `critere_code=FA05_VALEUR_VENALE`, `expected_value="400000"`, user saisit 300000 → alert warning source F96, écart ~25% > 10%.
- [ ] Alerte Question IA : question répondue "oui" avec `critere_code=FA05_CAPITAL_RESTANT`, `expected_value="120000"`, user saisit 150000 → alert warning source QUESTION_IA.
- [ ] Alerte PIECE_MANQUANTE contributor : pièce taggée FA05_VALEUR_VENALE + F96 VERIFIED sur même code → alert source MULTI avec PIECE_MANQUANTE dans contributors.
- [ ] MULTI : F96 + liquidationCommunaute convergent vers même valeur + user diverge → source MULTI.
- [ ] Gate `!showForm()` : return {} (déjà couvert par test existant).
- [ ] Non-régression : les 3 tests existants sur best-match IA seul continuent de passer.

### Tests backend

- [ ] Non applicable — le prompt est une chaîne de caractères, testée indirectement par l'intégration staging.

### Validation manuelle

- [ ] Staging : dossier divorce avec liquidation → F-FA-05 montre badges sur valeurs divergentes.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune** — enrichissement local du composant + extension prompt.

### Composants / endpoints impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `PartageImmobilierSectionComponent` | Enrichi | Specs existants + nouveaux |
| `EnrichedAnalysisService` | 1 ligne ajoutée dans prompt | Tests existants |
| `CaseFileDetailComponent` | 3 bindings ajoutés | Non-régression navigation |

### Smoke tests E2E

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IA-03-14 Done` (pattern de référence aligné).
- Audit 2026-04-15.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi seul niveau `warning`** : aucun des 2 champs n'est juridiquement bloquant. Une valeur immo différente n'annule pas l'outil, elle doit juste alerter l'avocat.
- **Pourquoi garder l'IA best-match existant** : déjà en place et fonctionnel. Les nouvelles sources F96 / Question IA sont complémentaires, pas remplacements.
- **Pourquoi pas de détection F-IA-03 sur `quotePartAttributaire`** : valeur par défaut de 50% suffit pour l'usage courant, pas de signal IA fiable.
- **Pourquoi codes numériques avec `expected_value` décimal en chaîne** : cohérent avec la convention existante (DT09_TYPE_RUPTURE, IM05_MOTIF utilisent déjà `expected_value` en chaîne).
- **Pourquoi alignement `SF-FA-05-05` et pas `SF-IA-03-XX`** : clarifie que c'est une extension de F-FA-05 (pas un ajout à F-IA-03 qui est maintenant Terminée).
