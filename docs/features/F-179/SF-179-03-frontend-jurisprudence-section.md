# Mini-spec — [F-179 / SF-179-03] Frontend — section « Jurisprudences citées » dans SynthesisComponent

> Mini-spec produite via `ai-skills/story-writer.md`. À valider avant dev.

---

## Identifiant

`F-179 / SF-179-03`

## Feature parente

`F-179` — Vérification de jurisprudence citée dans les documents uploadés (FR + BE)

## Statut

`ready`

## Date de création

2026-05-18

## Branche Git

`feat/SF-179-03-frontend-jurisprudence-section`

---

## Objectif

Afficher dans la page de synthèse du dossier une section « Jurisprudences citées » listant, groupées par document, les références jurisprudentielles vérifiées, chacune avec un badge de statut (`✅ Vérifiée` / `⚠️ Suspecte` / `❌ Non trouvée` / `❓ Incertaine`), une explication courte et un lien source cliquable quand disponible.

---

## Comportement attendu

### Cas nominal

1. Au chargement de `SynthesisComponent` pour un dossier, le composant appelle `GET /api/v1/case-files/{caseFileId}/jurisprudence-checks` (via un nouveau `JurisprudenceCheckService`).
2. La réponse (`{ checks: [...] }`) est stockée dans un signal.
3. Un nouveau composant standalone `app-jurisprudence-citations-section` reçoit la liste en `@Input()`.
4. Le composant **groupe les checks par `documentName`**, et pour chaque groupe affiche les références avec leur badge de statut.
5. La section est rendue comme un **nouveau `mat-expansion-panel`** dans le `mat-accordion` `synthesis-accordion` de `SynthesisComponent`, **après le panneau « Risques » et avant « Questions ouvertes »**.
6. Le panneau **n'est affiché que si au moins une référence a été détectée** (`@if (jurisprudenceChecks().length > 0)`), comme tous les panneaux conditionnels de l'accordéon.
7. Chaque ligne affiche : la `reference`, le badge de statut, l'`explication`, la `positionAlleguee` (si présente, pour les `SUSPECT`), et un lien `sourceUrl` cliquable (`target="_blank"`) quand `sourceUrl` est non null.
8. Quand **toutes** les références d'un dossier sont `UNCERTAIN`, un message d'en-tête de section explicite que la vérification automatique n'a pas pu conclure et invite à la vérification manuelle.
9. La description du panneau (`mat-panel-description`) affiche un compteur — ex. « 4 référence(s), 1 suspecte ».

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `GET .../jurisprudence-checks` échoue (réseau / 5xx) | `catchError` → liste vide, panneau non affiché ; aucune erreur bloquante pour le reste de la synthèse | — |
| `GET .../jurisprudence-checks` → 404 (dossier hors workspace) | Liste vide, panneau non affiché | 404 |
| Réponse `{ checks: [] }` | Panneau non affiché | 200 |
| `sourceUrl` absent | Aucun lien affiché — uniquement le badge + l'explication | — |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : aucun outil décisionnel — la section « Jurisprudences citées » est une zone d'affichage d'information IA, comme `synthesis-faits` / `synthesis-risques` / `divorce-consentement-scoring-section`. Pas de formulaire, pas de saisie, pas de calcul.
- [x] **Autres pays** : FR + BE — l'affichage est agnostique du pays (les badges et libellés sont identiques ; la donnée pays-spécifique vient du backend). Pas de gate `workspaceCountry`.
- [x] **Autres domaines** : transversal — la section s'affiche pour tout domaine si des références sont détectées.
- [x] **Autres UI patterns** : pattern visuel **badge de statut + explication + lien source** — identique à F-92 (pièces manquantes) et F-93 (traçabilité sources). Scan ci-dessous.
- [x] **Autres flows transversaux** : aucun (pas d'auth/workspace/plan/navigation modifié — le composant vit dans un écran existant).

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Où le pattern pourrait-il être réutilisé ?** Le pattern « liste groupée par document + badge de statut 4 valeurs » est spécifique à F-179. Les badges de statut existants (pièces manquantes F-194, risques F-195, checklist F-96) suivent déjà un pattern de boutons-statut markables — mais F-179 n'est PAS markable en V1 : ses badges sont purement informatifs (statut IA en lecture seule).
- [x] **Patterns concurrents** : `synthesis-badges` (grille F-162), badges de statut des panneaux markables. F-179 n'introduit pas de pattern concurrent : il réutilise les conventions visuelles de badge (`mat-icon` + label + classe de couleur) déjà en place dans `synthesis-faits` / `divorce-consentement-scoring-section`, sans bouton d'action.
- [x] **Nouveau service** : `JurisprudenceCheckService` (`core/services/`) — un GET unique. Pas réutilisable ailleurs (endpoint spécifique F-179). Pas de sur-conception.
- [x] **Classement** : pattern visuel **harmonisé immédiatement** avec les conventions de badge existantes (icône + couleur DESIGN_SYSTEM). Aucune dette de convergence.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `SynthesisComponent` (accordéon) | Oui | Intégré : nouveau `mat-expansion-panel` après « Risques ». |
| Composant standalone dédié | Oui | `app-jurisprudence-citations-section` créé (pattern `divorce-consentement-scoring-section`). |
| `JurisprudenceCheckService` | Oui | Créé dans cette SF (GET unique). |
| Badge grille `synthesis-grid` (F-162) | Optionnel | Reporté : non bloquant (cf. cadrage écran ajustement 4). Non intégré V1 pour limiter le périmètre — tracé comme amélioration possible. |
| Outils décisionnels | Non | F-179 n'est pas un outil décisionnel. |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (section + service).
- [x] Backlog / non prioritaire : badge dans `synthesis-grid` (F-162) — amélioration non bloquante, non intégrée V1.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : `app-jurisprudence-citations-section` n'est **pas un composant décisionnel**. Ce n'est pas une `<app-XXX-section>` consommant un endpoint POST décisionnel, il n'y a pas de formulaire de saisie avocat, pas de `result_data` persisté par saisie, pas d'entrée `TOOL_REGISTRY`, pas de pré-fill IA à ressaisir. C'est une **zone d'affichage en lecture seule** d'un résultat de post-traitement IA, au même titre que `synthesis-faits`, `synthesis-risques` ou `divorce-consentement-scoring-section` (eux aussi hors F-IA-04). La section 5 blocs F-IA-04 ne s'applique pas.

---

## Critères d'acceptation

- [ ] Quand le dossier a des `jurisprudence_checks`, un panneau « Jurisprudences citées » apparaît dans l'accordéon de synthèse, après « Risques » et avant « Questions ouvertes ».
- [ ] Quand le dossier n'a aucun check, le panneau n'est pas affiché.
- [ ] Les références sont groupées par `documentName` ; chaque groupe affiche son nom de document en en-tête.
- [ ] Chaque référence affiche un badge `✅ Vérifiée` (VERIFIED) / `⚠️ Suspecte` (SUSPECT) / `❌ Non trouvée` (NOT_FOUND) / `❓ Incertaine` (UNCERTAIN) avec la couleur DESIGN_SYSTEM correspondante (rouge réservé à `SUSPECT`, gris pour `UNCERTAIN`).
- [ ] Quand `sourceUrl` est présent, un lien cliquable `target="_blank" rel="noopener"` est affiché ; absent sinon.
- [ ] Quand toutes les références sont `UNCERTAIN`, un message d'en-tête invite à la vérification manuelle.
- [ ] Quand l'appel API échoue, le reste de la synthèse s'affiche normalement et le panneau est masqué (pas d'erreur bloquante).

---

## Périmètre

### Hors scope (explicite)

- Statut markable avocat (vu / traité / écarté) sur les références — hors V1 (les badges sont en lecture seule).
- Alerte cohérence F-IA-03 sur `SUSPECT` → SF-179-04.
- Badge dans la grille `synthesis-grid` (F-162) → non intégré V1.
- Export de la section → hors scope F-179 (V2).

---

## Valeurs initiales

Aucune entité créée côté frontend — composant d'affichage uniquement.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `statut` (affichage) | — | — | `VERIFIED` / `SUSPECT` / `NOT_FOUND` / `UNCERTAIN` | — | statut inconnu → rendu comme `UNCERTAIN` (défensif) |
| `sourceUrl` (lien) | Non | — | rendu uniquement si `http(s)://...` | — | — |

---

## Technique

### Contrat API consommé (figé par SF-179-01)

`GET /api/v1/case-files/{caseFileId}/jurisprudence-checks` → `200 { checks: JurisprudenceCheck[] }`.
`JurisprudenceCheck` : `{ id, documentName, reference, statut, explication, positionAlleguee, sourceUrl, claudeConfidence, webSearchUsed }`.

### Endpoint(s)

Aucun endpoint créé — consommation du GET de SF-179-01.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable.

### Composants Angular

- `JurisprudenceCheck` — modèle TypeScript (`core/models/jurisprudence-check.model.ts`).
- `JurisprudenceCheckService` — `core/services/jurisprudence-check.service.ts`, méthode `getChecks(caseFileId)`.
- `JurisprudenceCitationsSectionComponent` — `case-files/jurisprudence-citations-section/`, standalone, `@Input() checks`. Groupe par document, expose les badges. `ChangeDetectionStrategy.OnPush`.
- `SynthesisComponent` — modifié : injection du service, signal `jurisprudenceChecks`, appel API dans le `forkJoin` de chargement (avec `catchError(() => of({checks:[]}))`), insertion du `<app-jurisprudence-citations-section>` dans le template entre « Risques » et « Questions ouvertes ».

> **Garde-fou OnPush** : si la liste est mise à jour dans un `subscribe()`, injecter `ChangeDetectorRef` et appeler `markForCheck()` dans `next` ET `error` (cf. retour `feedback_onpush_subscribe_markforcheck`). Le composant `app-jurisprudence-citations-section` reçoit la donnée en `@Input()` → la CD du parent suffit, mais le parent `SynthesisComponent` doit gérer son propre `markForCheck()` si nécessaire.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `JurisprudenceCitationsSectionComponent` — groupe correctement 4 checks de 2 documents en 2 groupes.
- [ ] `JurisprudenceCitationsSectionComponent` — badge `SUSPECT` → classe rouge ; `UNCERTAIN` → classe grise ; `VERIFIED` → vert ; `NOT_FOUND` → badge erreur.
- [ ] `JurisprudenceCitationsSectionComponent` — `sourceUrl` présent → lien rendu ; absent → pas de lien.
- [ ] `JurisprudenceCitationsSectionComponent` — toutes les références `UNCERTAIN` → message d'en-tête « vérification manuelle ».
- [ ] `JurisprudenceCitationsSectionComponent` — `checks` vide → composant ne rend rien.
- [ ] `JurisprudenceCheckService` — `getChecks` appelle la bonne URL.
- [ ] `SynthesisComponent` — l'échec de `getChecks` n'empêche pas l'affichage de la synthèse (catchError → liste vide).

### Tests d'intégration

- [x] Non applicable (frontend) — couvert par les tests Jest + le build prod.

### Isolation workspace

- [x] Non applicable côté frontend — l'isolation est portée par le backend (404 camouflage sur le GET, SF-179-01).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — la section vit dans un écran existant (`SynthesisComponent`), aucune nouvelle route, aucun guard, aucun changement d'auth/workspace.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `SynthesisComponent` | Ajout d'un appel API + d'un panneau. Le `forkJoin` de chargement gagne une branche `catchError`. | Test Jest : synthèse s'affiche même si `getChecks` échoue. Tests Jest existants de `SynthesisComponent` doivent rester verts. |

### Smoke tests E2E concernés

- [x] Aucun smoke test `e2e/smoke/` concerné — pas de route ni de guard modifié. Le build prod (`npm run build`) + Jest couvrent la non-régression.

---

## Dépendances

### Subfeatures bloquantes

- `SF-179-01` — fournit le contrat API. **Le contrat étant figé dans la mini-spec SF-179-01, SF-179-03 peut être développée en parallèle de SF-179-01/02** sur une branche isolée (parallélisation autorisée — CLAUDE.md étape 3). Le merge de SF-179-03 attend que SF-179-01 soit mergée (l'endpoint doit exister — cf. `feedback_pre_merge_endpoint_check`).

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- Composant standalone dédié — pas de HTML inline dans `synthesis.component.html` (invariant anti-surcharge 2 du cadrage écran).
- Lecture seule en V1 : pas de bouton d'action, pas de statut markable — décision assumée du cadrage étape 0.
- `OnPush` + `@Input()` : la donnée descend du parent, pas de mutation interne dans un `subscribe()` du composant section.
