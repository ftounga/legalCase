# Mini-spec — F-236 / SF-236-01 Audit fin + plan de migration `static getPrefillCount`

## Identifiant

`F-236 / SF-236-01`

## Feature parente

`F-236` — Robustesse pré-fill IA outils décisionnels frontend

## Statut

`ready`

## Date de création

2026-05-10

## Branche Git

`feat/SF-236-01-audit-fin-prefill`

---

## Objectif

Produire une matrice exhaustive (58 outils × N champs IA × parité runtime/static) qui pilote l'implémentation des SF-236-02 à 05, et extraire le contrat unifié du helper `<ComponentName>PrefillRules` à appliquer sur chaque composant.

---

## Comportement attendu

### Cas nominal

1. Scanner exhaustivement tous les `*-section.component.ts` référencés dans `TOOL_REGISTRY` du panel F-IA-04
2. Pour chaque composant, extraire :
   - Présence/absence de `static getPrefillCount` (existante)
   - Logique exacte de `prefillFromAi()` runtime — branches conditionnelles, guards (`typeof`, `> 0`, country gates), champs IA consommés
   - Sources IA effectivement lues (`aiData.<champ>`, `synthesis.<section>.<champ>`, `procedureChecks`, `aiQuestions`, `piecesManquantes`, `triggerEvents`)
   - Maximum théorique de champs pré-remplissables (compteur N tel que `getPrefillCount` doit retourner N quand toutes les sources alimentent)
3. Produire le livrable `docs/features/F-236/audit-matrix.md` (tableau structuré par domaine)
4. Produire le livrable `docs/features/F-236/contract-prefill-rules.md` (signature du helper, exemple canonique sur F-IM-05, conventions de nommage `<ComponentName>PrefillRules.ts`)
5. Identifier les divergences runtime/static parmi les 4 composants déjà conformes
6. Identifier les ancrages mono-champ fragiles (au-delà de F-FA-07)
7. Identifier les composants où `synthesis.*` est exposé en input mais non consommé en fallback (F-DT-09, F-DT-20, F-DT-25 et autres)
8. Identifier les composants Immigration BE/FR sans gating pays correct

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Composant trouvé hors TOOL_REGISTRY mais avec `aiData?` | Listé dans annexe "candidats hors panel" — hors scope F-236 mais documenté |
| `prefillFromAi()` privé inaccessible à l'audit statique | Lecture du source via Read + Grep — pas de fallback dynamique |
| Champ IA consommé inexistant dans le record `*ExtractedData` | Anomalie listée dans la matrice (priorité P0) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : les 58 du TOOL_REGISTRY sont **précisément l'objet du scan**, donc cible directe
- [ ] **Autres pays** : FR / BE — certains outils n'existent que pour un pays, l'audit doit le noter
- [ ] **Autres domaines** : Travail / Immigration / Famille — l'audit segmente par domaine pour piloter les vagues SF-236-02
- [ ] **Autres UI patterns** : non applicable (cette SF est purement analytique, pas de modification UI)
- [ ] **Autres flows transversaux** : non applicable

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : oui, lecture des `@Input()` de chaque composant et entrées TOOL_REGISTRY
- [x] **Service / logique métier** : oui, lecture des branches de `prefillFromAi()`
- [ ] **Record / DTO backend** : confronter les champs IA consommés à `TravailExtractedData` / `ImmigrationExtractedData` / `FamilleExtractedData` pour détecter les références à des champs morts
- [ ] **Entité JPA + schéma DB** : non applicable (pas de persistence touchée)
- [ ] **Tests existants** : oui, repérer les composants ayant déjà un test `getPrefillCount` (les 4 conformes) pour reprendre le pattern de test

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable — cette SF n'introduit aucun outil. Audit pur.

### Cas spécifique : nouveau pattern UI ou service partagé

**Applicable** : la SF définit le contrat du helper partagé `<ComponentName>PrefillRules` qui sera consommé par 58 composants.

- [x] **Où le nouveau pattern peut-il être réutilisé ?** Le helper est conçu pour être consommé par TOUS les futurs composants décisionnels. Le contrat doit être suffisamment générique pour ne pas devoir évoluer à chaque feature.
- [x] **Patterns concurrents existants ?** Les 4 composants conformes (F-FA-05/07, F-IM-05/07) ont du code dupliqué runtime/static qui sera **remplacé** par le helper. Migration immédiate dans SF-236-02.
- [x] **Le service peut-il servir à d'autres features ?** Le pattern helper est un cas spécial d'extraction de fonctions pures — utile partout où une logique doit être appelée à la fois eagerly (pour un compteur/badge) ET lazy (pour le rendu UI complet).
- [x] **Équivalent design existant ?** Le pattern `TOOL_LABEL` / `TOOL_ICON` (constantes statiques exposées par le composant et consommées par le panel) est l'ancêtre direct. `<ComponentName>PrefillRules` étend ce pattern aux fonctions de calcul.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| 4 composants déjà conformes (F-FA-05/07, F-IM-05/07) | Oui | Refactor pour adopter le helper, dans SF-236-02 (vague de leur domaine respectif) |
| 54 composants non conformes | Oui | Implémentation complète dans SF-236-02 (3 vagues parallèles par domaine) |
| 49 composants `*-section` hors TOOL_REGISTRY | Non | Hors scope F-236 — documenté en annexe pour suivi |
| Records backend `*ExtractedData` | Audit en lecture | Détecter les champs morts (consommés mais inexistants) — P0 si trouvés |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature **(audit complet 58 outils + production du contrat helper)**
- [ ] Subfeature(s) parallèle(s) créée(s) : SF-236-02 à 05 dans la même feature
- [ ] Backlog : non applicable
- [ ] Non applicable : 49 composants hors panel — documentés mais non traités

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : cette SF est une SF d'audit/documentation (livrables = fichiers Markdown), elle ne modifie aucun composant Angular ni aucun fichier `*.component.ts`. Les 5 blocs F-IA-04 s'appliquent intégralement aux SF-236-02 et 04 qui font le code.

---

## Critères d'acceptation

- [ ] `docs/features/F-236/audit-matrix.md` produit avec 58 lignes (1 par outil du TOOL_REGISTRY) et 8 colonnes :
  - tool_id
  - composant
  - domaine (Travail/Immigration/Famille)
  - pays cibles (FR/BE/Both)
  - `static getPrefillCount` présent ? (OUI/NON)
  - Champs IA consommés par `prefillFromAi()` (liste)
  - Maximum théorique N
  - Anomalies détectées (codes A/B/C/D/E selon audit initial)
- [ ] `docs/features/F-236/contract-prefill-rules.md` produit avec :
  - Signature exacte du helper `<ComponentName>PrefillRules`
  - Exemple canonique sur F-IM-05 (refactor avant/après)
  - Convention de nommage des fichiers (`<component>-prefill-rules.ts` à côté du composant)
  - Convention de structure (constantes en haut, fonctions pures en bas, pas d'effet de bord)
  - Pattern de test Jest (3 cas : 0/M/N champs)
- [ ] Le rapport identifie tous les composants Travail (~25), Immigration (~14), Famille (~15) à traiter en SF-236-02 — listes finalisées par domaine
- [ ] Les divergences runtime/static parmi les 4 composants conformes sont listées avec les corrections précises à appliquer dans SF-236-03
- [ ] Les ancrages mono-champ fragiles sont listés avec les fallbacks proposés pour SF-236-04
- [ ] Les fallbacks `synthesis.*` manquants sont listés avec la donnée disponible et la branche suggérée pour SF-236-04
- [ ] Les composants Immigration sans gating pays correct sont listés pour SF-236-04
- [ ] Le contrat du test d'intégrité est défini (ce que le test va checker exactement) pour SF-236-05

---

## Périmètre

### Hors scope (explicite)

- Toute modification de code TypeScript ou de composant Angular (couvert par SF-236-02 à 05)
- Création du helper réel (couvert par SF-236-02 par domaine)
- Mise en œuvre du test d'intégrité (couvert par SF-236-05)
- Mise à jour de CLAUDE.md avec la règle de blocage automatique (couvert par SF-236-05)

---

## Plan de test

### Tests unitaires

Non applicable — SF documentation pure, livrables sont des fichiers Markdown.

### Tests d'intégration

Non applicable.

### Isolation workspace

Non applicable.

### Vérification du livrable

- [ ] Relecture humaine de `audit-matrix.md` — au moins 5 lignes échantillonnées et confrontées au code source du composant correspondant (doit correspondre)
- [ ] Relecture humaine de `contract-prefill-rules.md` — l'exemple canonique F-IM-05 doit être directement copiable comme code
- [ ] La somme des composants par domaine (Travail+Immigration+Famille) doit faire 58

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — SF documentaire pure, aucun code modifié, aucun déploiement, aucune base de données touchée

### Composants / endpoints existants potentiellement impactés

Aucun.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — justification : aucun code applicatif modifié.

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

### Méthodologie de l'audit

L'audit s'appuie sur :
1. Lecture directe des fichiers `*-section.component.ts` via Read + Grep
2. Lecture directe de `decisional-tools-panel.component.ts` (TOOL_REGISTRY) pour la liste autoritative des 58 outils intégrés
3. Lecture directe des records backend `TravailExtractedData.java`, `ImmigrationExtractedData.java`, `FamilleExtractedData.java` pour confronter les champs consommés
4. Pas de tooling automatisé spécifique — l'audit est manuel et structuré, exécuté par un agent Explore avec brief précis

Le rapport déjà produit le 2026-05-10 (joint à la conversation) sert de **base** mais doit être affiné : la matrice détaillée doit lister chaque champ IA consommé par chaque composant (et pas seulement le décompte agrégé).

### Découpage des vagues SF-236-02

Les vagues parallèles seront calibrées en équilibrant le volume :
- Travail FR+BE : ~25 composants — 1 j (vague la plus chargée)
- Immigration FR+BE : ~14 composants — 0,75 j
- Famille FR+BE : ~15 composants — 0,75 j

Ajustement final selon comptage exact issu de SF-236-01.

### Worktrees pour les vagues parallèles

Chaque vague de SF-236-02 sera exécutée dans un worktree séparé pour respecter la règle "deux SF parallèles ne partagent pas la même branche". Branches : `feat/SF-236-02-travail`, `feat/SF-236-02-immigration`, `feat/SF-236-02-famille`.
