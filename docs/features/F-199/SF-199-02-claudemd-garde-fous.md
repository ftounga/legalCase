# Mini-spec — F-199 / SF-199-02 Ajout 2 règles de blocage automatique dans CLAUDE.md

> Pure SF de gouvernance. Aucun code applicatif. Modification ciblée de la table « Blocages
> automatiques » de `CLAUDE.md`.

---

## Identifiant

`F-199 / SF-199-02`

## Feature parente

`F-199` — Gouvernance F-IA-04 (template SF + 2 garde-fous CLAUDE.md)

## Statut

`ready`

## Date de création

2026-05-09

## Branche Git

`feat/F-199-gouvernance-fia04`

---

## Objectif

Ajouter dans la section « Blocages automatiques » de `CLAUDE.md` deux nouvelles lignes de la table « Situation / Réponse » :

- **Règle (a)** — Bloque tout INSERT/UPDATE dans `decision_tool_visibility_rules` (mode `visibility` ou flag) si la mini-spec ne contient pas l'audit explicite « Impact F-166 cross-C×D ».
- **Règle (b)** — Bloque tout ajout d'entrée seed (`legal_referentials` ou `decision_tool_visibility_rules`) avec `country='FR'` ou `'BE'` si la mini-spec ne contient pas l'audit explicite « Exhaustivité du droit national X-FR/BE ».

---

## Comportement attendu

### Cas nominal

1. Une SF backend touche `decision_tool_visibility_rules` ou seed un `legal_referential` country-specific.
2. La readiness checklist détecte la situation et exige l'audit correspondant dans la mini-spec.
3. Sans audit → REFUS standard, dev ne démarre pas.

### Cas d'erreur (gouvernance)

| Situation | Comportement attendu |
|-----------|---------------------|
| SF qui INSERT dans `decision_tool_visibility_rules` sans bloc audit F-166 | REFUS — règle (a) |
| SF qui INSERT seed `legal_referentials` country=FR sans bloc exhaustivité droit FR | REFUS — règle (b) |
| SF qui INSERT seed `legal_referentials` country=BE sans bloc exhaustivité droit BE | REFUS — règle (b) |
| SF qui INSERT seed `legal_referentials` sans `country` (transversal niveau Union/EU) | Hors scope règle (b) — la règle ne s'applique qu'aux entrées country-specific |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable — règles transversales sur le mécanisme de seed.
- [x] **Autres pays** : couverts explicitement par la règle (b) (FR + BE).
- [x] **Autres domaines** : couverts indirectement — l'audit F-166 cross-C×D inclut les 3 domaines.
- [x] **Autres UI patterns** : non applicable — règles backend / migration.
- [x] **Autres flows transversaux** : la règle (a) renforce le mécanisme de visibilité conditionnelle F-IA-04 / F-166. La règle (b) renforce la parité FR/BE.

### Niveaux de vérification à couvrir

- [x] Modèle TypeScript / API exposée — non applicable.
- [x] Record / DTO backend — non applicable.
- [x] Service / logique métier — non applicable.
- [x] Entité JPA + schéma DB — la règle (a) cible `decision_tool_visibility_rules` ; la règle (b) cible `legal_referentials` et `decision_tool_visibility_rules` country-specific.
- [x] Tests existants — non applicable (les règles bloquent en amont du dev).

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — modification documentaire de gouvernance.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Toute SF backend touchant `decision_tool_visibility_rules` | Oui | Couverte par règle (a) |
| Toute SF backend seed `legal_referentials` country-specific | Oui | Couverte par règle (b) |
| Migrations transversales sans country | Non | Hors scope règle (b) |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (CLAUDE.md = source de vérité).
- [ ] Subfeature(s) parallèle(s) — non applicable.
- [ ] Backlog — non applicable.

---

## Critères d'acceptation

- [ ] La table « Blocages automatiques » de `CLAUDE.md` contient une nouvelle ligne pour la règle (a) : INSERT/UPDATE `decision_tool_visibility_rules` sans audit « Impact F-166 cross-C×D » → REFUS.
- [ ] La règle (a) cite explicitement la motivation : éviter l'accumulation silencieuse d'outils ALWAYS_ON candidats CONTEXTUAL et la dérive du périmètre F-IA-04.
- [ ] La table « Blocages automatiques » de `CLAUDE.md` contient une nouvelle ligne pour la règle (b) : seed `legal_referentials` ou `decision_tool_visibility_rules` avec `country='FR'` ou `'BE'` sans audit « Exhaustivité du droit national X-FR/BE » → REFUS.
- [ ] La règle (b) cite explicitement la motivation : éviter qu'un outil soit implémenté pour 1 pays sans considérer l'équivalent dans l'autre.
- [ ] Les deux règles utilisent le format standard de la table (colonne « Situation » + colonne « Réponse »).
- [ ] Les deux règles citent le format de refus standard de CLAUDE.md (« REFUS — … »).

---

## Périmètre

### Hors scope (explicite)

- Aucun code applicatif modifié.
- Aucune modification du template (couvert par SF-199-01).
- Aucune modification de `docs/PRODUCT_SPEC.md` ni de `MEMORY.md`.
- Aucune nouvelle feature, juste 2 règles documentaires de blocage.

---

## Valeurs initiales

Non applicable.

---

## Contraintes de validation

Non applicable.

---

## Technique

### Endpoint(s)

Non applicable.

### Tables impactées

Non applicable (la règle référence `decision_tool_visibility_rules` et `legal_referentials`, mais aucune de ces tables n'est modifiée par cette SF).

### Migration Liquibase

- [ ] Oui
- [x] Non applicable

### Composants Angular (si applicable)

Non applicable.

### Fichier impacté

| Fichier | Opération | Notes |
|---------|-----------|-------|
| `CLAUDE.md` | INSERT (2 nouvelles lignes dans la table « Blocages automatiques ») | Insertion à la fin de la table existante, juste avant le bloc « Format de refus standard » |

---

## Plan de test

### Tests unitaires

Non applicable.

### Tests d'intégration

Non applicable.

### Validation manuelle (relecture)

- [ ] La table `Situation / Réponse` de `CLAUDE.md` affiche correctement les 2 nouvelles lignes.
- [ ] Le rendu Markdown est conservé (pipes alignés, pas de saut de table).
- [ ] L'orthographe et les accents sont vérifiés.
- [ ] Les règles ne contredisent aucune règle existante de la table.

### Isolation workspace

- [x] Non applicable — modification documentaire.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale runtime** — modification documentaire pure.

### Composants / endpoints existants potentiellement impactés

Aucun.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (justification : pas de chemin runtime modifié).

---

## Impact par domaine métier

Cette SF est **transversale** — les 2 règles couvrent uniformément les 3 domaines (travail, immigration, famille) et les 2 pays (FR, BE). La règle (b) cible spécifiquement la parité FR/BE.

---

## Parité des domaines métier

Non applicable — la SF ne livre pas un outil décisionnel de niveau ≥ 5. Elle livre 2 garde-fous de gouvernance dont l'effet est précisément d'imposer la parité aux SF futures.

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Les deux règles s'inscrivent dans la lignée des garde-fous existants : « Migration Liquibase qui INSERT/UPDATE dans `decision_tool_visibility_rules` un `tool_id` absent de `TOOL_REGISTRY`… » (F-164 SF-164-01) et « Migration Liquibase qui INSERT une entry `legal_referentials` avec `is_system=true` sans la colonne `description` renseignée » (F-140 SF-140-03).
- Règle (a) — l'audit « Impact F-166 cross-C×D » désigne l'analyse des cellules visibility par croisement Country × Domain (BE×Travail, FR×Immigration, etc.), formalisée par F-166. Sans cet audit, l'ajout d'un outil contextuel devient silencieusement ALWAYS_ON dans des cellules où il n'est pas pertinent (ou inversement).
- Règle (b) — l'audit « exhaustivité du droit national X-FR/BE » désigne le contrôle systématique : pour chaque seed national ajouté côté FR, vérifier l'équivalent BE et inversement. C'est le complément de la règle « SF livre un outil décisionnel de niveau ≥ 5 sans Parité des domaines métier » (F-150-153) appliqué côté seeding.
- Les deux règles utilisent le format de refus standard déjà défini : `REFUS [contexte] / Motif / Artefact manquant / Référence`.
