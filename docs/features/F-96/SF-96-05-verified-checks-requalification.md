# Mini-spec — F-96 / SF-96-05 Préservation et requalification automatique des points VERIFIED

## Identifiant

`F-96 / SF-96-05`

## Feature parente

`F-96` — Checklist procédurale interactive

## Statut

`ready`

## Date de création

2026-04-03

## Branche Git

`feat/SF-96-05-verified-checks-requalification`

---

## Objectif

Préserver les points procéduraux `VERIFIED` d'une analyse à l'autre et laisser Claude les requalifier automatiquement (`NON_COMPLIANT` ou `TO_CHECK`) si de nouveaux éléments (Q&A, chat, documents) les contredisent, avec une raison visible dans la checklist.

---

## Comportement attendu

### Cas nominal

1. Lors du lancement d'une nouvelle synthèse enrichie, les points `VERIFIED` de la dernière analyse `DONE` du dossier sont injectés dans le prompt sous `[Points procéduraux vérifiés — à reconsidérer si nécessaire]`
2. Les points `NON_COMPLIANT` et `TO_CHECK` continuent d'être injectés — comportement inchangé (SF-96-03, SF-96-04)
3. Le system prompt est mis à jour pour demander à Claude de retourner un champ `checks_a_requalifier` dans sa sortie JSON : liste d'objets `{description, nouveau_statut, raison}` pour les points VERIFIED qu'il estime devoir requalifier
4. Après parsing de la réponse Claude :
   - Les points listés dans `checks_a_requalifier` : leur statut est mis à jour (`NON_COMPLIANT` ou `TO_CHECK`) et la `raison` est stockée
   - Les points `VERIFIED` non listés restent `VERIFIED` et sont propagés tels quels vers la nouvelle analyse
5. `createChecks()` est modifié : au lieu de supprimer tous les checks existants, il préserve les `VERIFIED` non requalifiés et les rattache à la nouvelle analyse
6. L'avocat peut toujours requalifier manuellement un point après la requalification automatique de Claude

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `listVerified` lève une exception | Fail-open : section VERIFIED absente du prompt, synthèse continue |
| `checks_a_requalifier` absent du JSON | Fail-open : aucun VERIFIED requalifié, tous préservés |
| `checks_a_requalifier` invalide / JSON malformé | Fail-open : aucun VERIFIED requalifié, tous préservés |
| `nouveau_statut` hors valeurs autorisées | Ce check est ignoré (fail-open), les autres sont traités |
| Point listé dans `checks_a_requalifier` mais introuvable parmi les VERIFIED | Ignoré silencieusement |

---

## Critères d'acceptation

- [ ] Les points `VERIFIED` de la dernière analyse `DONE` sont injectés dans le prompt sous `[Points procéduraux vérifiés — à reconsidérer si nécessaire]`
- [ ] Les points `VERIFIED` non listés dans `checks_a_requalifier` sont propagés vers la nouvelle analyse avec statut `VERIFIED`
- [ ] Les points listés dans `checks_a_requalifier` voient leur statut mis à jour et leur `raison` stockée
- [ ] `nouveau_statut` n'accepte que `NON_COMPLIANT` et `TO_CHECK`
- [ ] Les sections `NON_COMPLIANT` et `TO_CHECK` du prompt restent inchangées
- [ ] La `raison` est visible dans le composant checklist frontend
- [ ] Si `listVerified` échoue, la synthèse enrichie continue sans planter (fail-open)
- [ ] Si `checks_a_requalifier` est absent ou invalide, tous les VERIFIED sont préservés (fail-open)
- [ ] L'avocat peut requalifier manuellement après requalification automatique de Claude

---

## Périmètre

### Hors scope

- Écran de diff entre deux analyses (prévu SF-96-06)
- Modification du comportement des statuts `TO_CHECK` et `NON_COMPLIANT`
- Historisation complète des requalifications (audit trail)
- Notification à l'avocat lors d'une requalification automatique

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. Modification interne au pipeline IA et au composant checklist frontend.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `procedure_checks` | ALTER + SELECT + UPDATE | Ajout colonne `raison` (nullable, text) |

### Migration Liquibase

- [x] Oui — `ALTER TABLE procedure_checks ADD COLUMN raison TEXT`

### Composants Angular

- `ProcedureChecklistComponent` — afficher `raison` si renseignée, sous la description du check

---

## Plan de test

### Tests unitaires

- [ ] `ProcedureCheckService` — `listVerified` retourne uniquement les `VERIFIED` de la dernière analyse DONE
- [ ] `EnrichedAnalysisService` — `buildEnrichedPrompt` avec VERIFIED : section `[Points procéduraux vérifiés]` présente
- [ ] `EnrichedAnalysisService` — `buildEnrichedPrompt` sans VERIFIED : section absente
- [ ] `EnrichedAnalysisService` — `applyRequalifications` : point listé dans `checks_a_requalifier` → statut mis à jour + raison stockée
- [ ] `EnrichedAnalysisService` — `applyRequalifications` : JSON invalide → fail-open, aucun VERIFIED modifié
- [ ] `EnrichedAnalysisService` — `applyRequalifications` : `nouveau_statut` invalide → ce check ignoré, les autres traités
- [ ] `ProcedureCheckService` — `createChecks` : les VERIFIED non requalifiés sont propagés vers la nouvelle analyse

### Tests d'intégration

Pas de nouvel endpoint — les IT existants du pipeline `EnrichedAnalysisService` couvrent le flux global.

### Isolation workspace

- [x] Non applicable — modification interne du prompt IA, pas d'accès croisé entre workspaces

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification interne du pipeline IA et du prompt uniquement

---

## Dépendances

### Subfeatures bloquantes

- SF-96-03 — statut : done (injection NON_COMPLIANT)
- SF-96-04 — statut : in-review (injection TO_CHECK, PR #219)

---

## Notes et décisions

- Option A retenue : Claude décide lui-même de la requalification via `checks_a_requalifier`
- Matching VERIFIED ↔ `checks_a_requalifier` par `description` (comparaison exacte)
- `raison` nullable : absente si statut non modifié par Claude, présente si requalifié
- `createChecks()` modifié : ne supprime plus les VERIFIED (supprime TO_CHECK et NON_COMPLIANT comme avant, puis propage les VERIFIED non requalifiés)
- System prompt mis à jour pour inclure la description du champ `checks_a_requalifier`
- `nouveau_statut` : seules valeurs autorisées `NON_COMPLIANT` et `TO_CHECK`
