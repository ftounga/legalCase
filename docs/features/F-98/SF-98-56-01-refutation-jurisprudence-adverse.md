# Mini-spec — F-98 / SF-98-56 — Réfutation de la jurisprudence adverse dans les conclusions

> Base : `project-governance/templates/subfeature-template.md`. À valider avant dev.

## Identifiant

`F-98 / SF-98-56`

## Feature parente

`F-98` — Génération de courrier / conclusions

## Statut

`ready` (étape 0 GO avec ajustements + étape 0 bis GO avec ajustements validées ; décision PO Option A prise)

## Date de création

2026-06-09

## Branche Git

`feat/SF-98-56-refutation-jurisprudence-adverse`

---

## Objectif

> Permettre à l'avocat de **marquer** les jurisprudences citées suspectes/introuvables (détectées par F-179) comme **issues de l'adversaire**, pour qu'elles soient **réfutées** dans le projet de conclusions généré (F-98).

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre l'écran **Synthèse** (`/case-files/:id/synthesis`), section « Jurisprudences citées » (F-179).
2. Sur une citation au statut **Suspecte** ou **Non trouvée**, une action inline **« Marquer comme adverse à réfuter »** est disponible (toggle). Elle est **absente** sur les statuts Vérifiée / Incertaine.
3. L'avocat active le toggle → `PATCH …/adverse-marking {markedAdverse:true}` → la citation est persistée comme `marked_adverse = true` ; l'état visuel reflète le marquage (toggle actif). Re-cliquer le désactive (`false`).
4. Une **mention de continuité** apparaît sous la section F-179 dès qu'il existe ≥ 1 citation éligible : « Les citations marquées comme adverses alimenteront la réfutation dans le projet de conclusions. »
5. L'avocat va à l'onglet **Décision** (CTA existant), section **« Projet de conclusions »**, et **génère / régénère** les conclusions.
6. À la génération, le backend charge les citations `statut ∈ {SUSPECT, NOT_FOUND}` **ET** `marked_adverse = true` du dossier, les injecte dans le prompt sous une section **« JURISPRUDENCE ADVERSE À RÉFUTER »**, et le LLM produit une **réfutation** rédigée (l'arrêt invoqué par l'adversaire est inexistant / sa portée est dénaturée), traduite en droit (pas de jargon, pas de statut technique exposé — invariant SF-98-55).
7. Après génération, `ConclusionsSectionComponent` **signale factuellement** : « N citation(s) adverse(s) marquée(s) prise(s) en compte » (rien si N = 0).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `markedAdverse` absent du body | Message d'erreur explicite | 400 |
| `checkId` inexistant | Ressource introuvable | 404 |
| Check d'un autre dossier / workspace | Accès refusé | 404 (pas de fuite d'existence) |
| Marquage tenté sur statut ≠ SUSPECT/NOT_FOUND | Refus : seuls les statuts réfutables sont marquables | 422 |
| Aucune citation marquée à la génération | Génération normale, **aucune** section réfutation (pas de rubrique vide) | 202/200 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- **Autres outils métier** : non applicable — SF-98-56 ne touche aucun outil décisionnel (calculator/analyzer). Elle agit sur la chaîne F-179 (détection) → F-98 (conclusions).
- **Autres pays (FR/BE)** : la garde et l'injection sont **agnostiques pays** — la section réfutation est ajoutée au prompt système commun (mécanisme `JURISPRUDENCE_GUARD`, déjà sur les 45 cellules). F-179 fonctionne FR+BE → le marquage et la réfutation valent pour les deux pays sans code spécifique.
- **Autres domaines (Travail/Immigration/Famille)** : idem — agnostique domaine (la section s'ajoute à toutes les cellules). Aucune duplication par domaine.
- **Autres UI patterns** : le toggle de marquage est un nouveau **micro-pattern d'action inline sur une citation**. Cibles concurrentes : aucune action par citation n'existe aujourd'hui dans `JurisprudenceCitationsSectionComponent` (lecture seule). Pas de pattern maison à remplacer.
- **Flows transversaux** : isolation workspace (le PATCH doit être borné au workspace — couvert). Pas d'impact auth/plan/navigation.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Injection prompt agnostique pays/domaine | Oui | Intégré dans cette SF (section commune, 0 duplication) |
| Marquage sur F-179 FR + BE | Oui | Intégré (F-179 couvre déjà FR+BE) |
| Outils décisionnels | Non | SF-98-56 ne crée/modifie aucun outil décisionnel |
| Persistance marquage survivant à une ré-analyse | Partiel | **Hors scope MVP** : une ré-analyse recrée les `jurisprudence_checks` → marquage réinitialisé (cohérent avec « analyse fraîche »). Noté en limite. |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (FR+BE, 3 domaines, via la section de prompt commune).
- [x] Backlog : persistance du marquage à travers une ré-analyse + **Option B (tag de camp à l'upload)** = évolutions backlog (cf. `SF-98-56-00-coherence.md`).
- [x] Non applicable aux outils décisionnels (justifié : aucune création/modification d'outil).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF-98-56 ne livre pas de section décisionnelle `<app-XXX-section>` consommant un endpoint décisionnel POST/GET intégré au panel F-IA-04 via `TOOL_REGISTRY`. Elle ajoute une **action inline** dans un composant d'affichage existant (`JurisprudenceCitationsSectionComponent`, écran Synthèse) et une **mention factuelle** dans `ConclusionsSectionComponent`. Aucun `tool_id`, aucun formulaire de calcul, aucun pré-remplissage de champs saisissables.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF-98-56 ne crée ni ne modifie d'outil décisionnel à champs saisissables. Le marquage est un **booléen d'action** posé par l'avocat sur une citation déjà détectée par F-179 ; il n'est pas extrait par l'IA (au contraire — c'est un jugement humain sur l'origine adverse, raison d'être de l'Option A).

---

## Critères d'acceptation

- [ ] Sur une citation **SUSPECT** ou **NOT_FOUND**, le toggle « Marquer comme adverse à réfuter » est présent ; sur **VERIFIED**/**UNCERTAIN**, il est **absent**.
- [ ] Activer le toggle persiste `marked_adverse = true` (vérifié après reload de la Synthèse) ; le désactiver le repasse à `false`.
- [ ] La mention de continuité s'affiche sous la section F-179 **seulement** s'il existe ≥ 1 citation SUSPECT/NOT_FOUND.
- [ ] À la génération des conclusions, **seules** les citations `marked_adverse = true` ET de statut SUSPECT/NOT_FOUND alimentent la section « JURISPRUDENCE ADVERSE À RÉFUTER » du prompt.
- [ ] Le texte produit **réfute** ces citations sans exposer de jargon : aucun `document_name` brut, aucun statut technique (« SUSPECT »), aucune référence « citée avec autorité » comme si elle était valable (non-régression SF-98-55 + garde jurisprudence respectée).
- [ ] Si 0 citation marquée → aucune section réfutation dans l'acte (pas de rubrique vide / « néant »).
- [ ] `ConclusionsSectionComponent` affiche « N citation(s) adverse(s) marquée(s) prise(s) en compte » après génération (rien si N = 0).
- [ ] **Isolation workspace** : un utilisateur du workspace A ne peut pas marquer un check du workspace B (404).
- [ ] Marquage refusé (422) sur un statut non réfutable (VERIFIED/UNCERTAIN), même via appel API direct.

---

## Périmètre

### Hors scope (explicite)

- **Tag de camp à l'upload** (Option B) — évolution backlog.
- **Persistance du marquage à travers une ré-analyse** — MVP : réinitialisé (les checks sont recréés).
- **Détection automatique du camp adverse** (Option C heuristique) — rejetée.
- **Réfutation des citations VERIFIED** — un arrêt valable ne se réfute pas.
- **Posture procédurale automatique** (« en réponse ») — la réfutation reste pilotée par la présence de citations marquées (invariant 5/6 étape 0).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `marked_adverse` | `false` | Toute citation détectée par F-179 naît non marquée |

Comportements : le marquage est posé **uniquement** par action explicite de l'avocat (jamais par l'IA, jamais à la création du check).

---

## Contraintes de validation

| Champ | Obligatoire | Valeurs autorisées | Normalisation |
|-------|-------------|--------------------|---------------|
| `markedAdverse` (body PATCH) | Oui | `true` / `false` (booléen) | — |
| statut du check ciblé | — | marquage autorisé **seulement** si `statut ∈ {SUSPECT, NOT_FOUND}` | contrôle serveur |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle min. |
|---------|-----|------|-----------|
| PATCH | `/api/v1/case-files/{caseFileId}/jurisprudence-checks/{checkId}/adverse-marking` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/jurisprudence-checks` (existant) | Oui | MEMBER |

**Contrat figé (parallélisation back/front)** :
- PATCH body : `{ "markedAdverse": boolean }` → 200 `JurisprudenceCheckResponse.Check` (à jour).
- GET : chaque `Check` expose désormais `markedAdverse: boolean` (champ ajouté au DTO `JurisprudenceCheckResponse.Check`).
- Génération des conclusions : **aucun nouvel endpoint** — l'injection se fait dans le flux `POST …/conclusions/generate` existant (worker async).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `jurisprudence_checks` | ALTER (ajout colonne) + UPDATE (marquage) + SELECT (chargement génération) | Nouvelle colonne `marked_adverse BOOLEAN NOT NULL DEFAULT false` |

### Migration Liquibase

- [x] Oui — `597-add-marked-adverse-to-jurisprudence-checks.xml` (numéro **à confirmer au dev** = prochain libre ≥ 597 ; UUID changeSet pré-assigné dans le brief d'agent pour éviter toute collision inter-sessions, cf. `feedback_parallel_agents_uuid_collision`). Colonne `marked_adverse` `BOOLEAN NOT NULL DEFAULT false`.

### Backend — fichiers

- `JurisprudenceCheck.java` (entité) : + champ `markedAdverse`.
- `JurisprudenceCheckResponse.Check` : + champ `markedAdverse`.
- `JurisprudenceCheckController` : + `@PatchMapping(".../{checkId}/adverse-marking")`.
- `JurisprudenceCheckQueryService` (ou service de commande) : méthode `markAdverse(caseFileId, checkId, markedAdverse, principal)` avec isolation workspace + garde de statut.
- `JurisprudenceCheckRepository` : query `findByCaseFileIdAndStatutInAndMarkedAdverseTrue(...)` (statuts SUSPECT, NOT_FOUND).
- `CaseConclusionPromptBuilder` : + champ `adverseToRefute` dans `ConclusionPromptInput`, + méthode `appendAdverseJurisprudenceToRefute(...)`, + mise à jour de `JURISPRUDENCE_GUARD` (autoriser la 3ᵉ section, consigne de réfutation sans citer avec autorité).
- `CaseConclusionService` : loader `loadAdverseJurisprudenceChecks(caseFileId)` passé au constructeur de `ConclusionPromptInput`.

### Composants Angular

- `JurisprudenceCitationsSectionComponent` — toggle « Marquer comme adverse à réfuter » (inline, SUSPECT/NOT_FOUND uniquement), appel PATCH, mention de continuité conditionnelle. `OnPush` + `markForCheck()` dans le `subscribe` (cf. `feedback_onpush_subscribe_markforcheck`).
- Service frontend des jurisprudence-checks — méthode `markAdverse(caseFileId, checkId, markedAdverse)`.
- `ConclusionsSectionComponent` — mention factuelle « N citations adverses prises en compte » après génération (N = nombre de checks marqués éligibles).

---

## Plan de test

### Tests unitaires (backend)

- [ ] `CaseConclusionPromptBuilder` — la section « JURISPRUDENCE ADVERSE À RÉFUTER » apparaît quand `adverseToRefute` non vide ; **absente** quand vide.
- [ ] `CaseConclusionPromptBuilder` — `JURISPRUDENCE_GUARD` mis à jour cite la 3ᵉ section et la consigne « réfuter sans citer avec autorité » ; non-régression des assertions SF-98-55.
- [ ] Service marquage — refuse (422) un statut VERIFIED/UNCERTAIN ; accepte SUSPECT/NOT_FOUND.
- [ ] Repository — `findByCaseFileIdAndStatutInAndMarkedAdverseTrue` ne renvoie que les marqués éligibles.

### Tests d'intégration

- [ ] `PATCH …/adverse-marking {markedAdverse:true}` → 200, persistance vérifiée.
- [ ] `PATCH` body sans `markedAdverse` → 400.
- [ ] `PATCH` sur `checkId` d'un autre workspace → 404.
- [ ] `PATCH` sur un check VERIFIED → 422.
- [ ] `GET …/jurisprudence-checks` expose `markedAdverse`.

### Tests unitaires (frontend, Jest)

- [ ] Toggle présent sur SUSPECT/NOT_FOUND, absent sur VERIFIED/UNCERTAIN.
- [ ] Mention de continuité affichée ssi ≥ 1 citation éligible.
- [ ] `ConclusionsSectionComponent` — mention « N citations adverses » ; rien si N = 0.

### Isolation workspace

- [x] Applicable — un utilisateur du workspace A ne peut pas marquer un check du workspace B (404).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — pas de changement auth/Principal, workspace context (l'isolation réutilise le pattern existant du `JurisprudenceCheckController`), plans/limites, ni navigation/routing. Ajout d'un endpoint et d'une colonne, sans impact sur les flows transversaux.

### Smoke tests E2E concernés

- [ ] `e2e/smoke/` — aucun smoke test n'exerce la chaîne F-179 → conclusions aujourd'hui. **Aucun smoke test concerné** (justification : pas de modification auth/workspace/navigation ; la chaîne est validée par tests unitaires/intégration + validation staging manuelle façon SF-98-55).

---

## Dépendances

### Subfeatures bloquantes

- F-179 (SF-179-01→05) — `done` (détection + statuts SUSPECT/NOT_FOUND + `position_alleguee`/`explication`).
- SF-98-55 — `done` (garde rédactionnelle commune ; la réfutation s'y conforme).

### Questions ouvertes impactées

- [x] `docs/OPEN_QUESTIONS.md` — « conclusions = outils CALCULÉS seulement » : **non impactée** (cette question porte sur les verdicts d'outils alimentant les conclusions ; la réfutation F-179 est une source distincte, pilotée par marquage humain, pas par calcul d'outil).

---

## Notes et décisions

- **Découpage** : marquage (UI + persistance + API) et injection (builder) forment **un seul flux cohérent** → une SF (SF-98-56). Livrer le marquage sans l'injection produirait un toggle sans effet (réduction de scope interdite). Back/front **parallélisables** sur le contrat figé ci-dessus.
- **Origine adverse = jugement humain** (Option A) : le produit ne devine jamais le camp ; l'avocat le déclare. C'est l'invariant anti-contresens central (cf. étape 0).
- **Anti-régression SF-98-55** : la réfutation est de la matière interne traduite en droit ; la mise à jour de `JURISPRUDENCE_GUARD` doit explicitement interdire d'exposer le statut technique et de « citer avec autorité » l'arrêt réfuté.
- **Limite MVP assumée** : une ré-analyse réinitialise le marquage (checks recréés) — backlog.
