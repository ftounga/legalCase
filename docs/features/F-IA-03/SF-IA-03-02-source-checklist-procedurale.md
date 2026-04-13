# Mini-spec — F-IA-03 / SF-IA-03-02 Ajouter la checklist procédurale F-96 comme source de cohérence

## Identifiant

`F-IA-03 / SF-IA-03-02`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-13

## Branche Git

`feat/SF-IA-03-02-source-checklist-procedurale`

---

## Objectif

Étendre le moteur de cohérence sur F-DT-08 pour qu'il prenne en compte, en plus de la détection IA, les points validés de la checklist procédurale F-96 (`VERIFIED` / `NON_COMPLIANT`) taggés avec un `critere_code`. Un point F-96 validé qui contredit la réponse avocat déclenche un `blocker` quelle que soit la criticité du critère.

---

## Comportement attendu

### Cas nominal

1. À l'analyse IA d'un dossier DROIT_DU_TRAVAIL, le prompt étend `points_procedure` : chaque item renvoyé par Claude est un objet `{ texte, critere_code? }` où `critere_code` est l'un des 14 codes F-DT-08 si le point porte clairement sur ce critère, sinon omis.
2. Le `critere_code` est persisté en colonne dédiée sur `procedure_checks` (migration 068).
3. La route `GET /api/v1/case-files/{caseFileId}/analyses/{analysisId}/procedure-checks` expose maintenant `critereCode` dans `ProcedureCheckResponse`.
4. `CaseFileDetailComponent` charge les procedure-checks (ou les reçoit déjà via la synthèse), et les transmet à `LicenciementSectionComponent` via un nouvel `@Input() procedureChecks`.
5. Le computed `coherenceAlerts` applique la priorité décrite plus bas. L'UI reste identique (badge + tooltip + compteur), mais le texte et la couleur suivent la nouvelle logique.

### Ordre de priorité des sources (par critère)

Pour chaque critère de la grille, en parcourant dans l'ordre :

| Étape | Condition | Résultat |
|---|---|---|
| A | avocat `INCONNU` | aucune alerte |
| B | ≥ 1 point F-96 taggé `critere_code=X` et statut `VERIFIED` | voir règle F-96 ci-dessous |
| C | ≥ 1 point F-96 taggé `critere_code=X` et statut `NON_COMPLIANT` | voir règle F-96 ci-dessous |
| D | (pas de F-96 validé) IA `aiData.detections[X].reponse ∈ {OUI, NON}` | logique SF-IA-03-01 (warning/blocker selon criticité) |
| E | sinon | aucune alerte |

Règle F-96 (étapes B et C) : F-96 écrase IA. Si contradiction entre le statut F-96 et la réponse avocat → `blocker` systématique (même sur critère non bloquant). Si plusieurs points F-96 sur le même critère ont des statuts opposés, on privilégie l'ordre suivant : `NON_COMPLIANT` > `VERIFIED` (on parie sur la dernière requalification stricte).

**Équivalence statut F-96 ↔ réponse attendue sur le critère** :
- `VERIFIED` signifie que l'obligation est respectée → équivaut à `OUI` sur le critère
- `NON_COMPLIANT` signifie que l'obligation n'est pas respectée → équivaut à `NON` sur le critère
- `TO_CHECK` ou statut absent → point non validé, ignoré pour la cohérence

### Badge et tooltip

- Badge `blocker` (rouge) quand la source dominante est F-96 validé contredit l'avocat.
- Badge `warning` ou `blocker` (comportement SF-IA-03-01) quand la source dominante est l'IA.
- Texte du badge :
  - F-96 gagnant → `Incohérence F-96 (OUI)` / `(NON)`
  - IA gagnant → `Incohérence IA (OUI)` / `(NON)` (inchangé)
- Tooltip :
  - F-96 : `L'avocat a validé : <statut>. <raison si présente>`
  - IA : inchangé
  - Si F-96 **et** IA sont en contradiction avec l'avocat dans le même sens : tooltip affiche les deux, mais **un seul badge**, niveau blocker, source "F-96 + IA".

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `critere_code` absent du JSON IA | Point créé sans code, pas utilisé pour la cohérence (fail-open) |
| `critere_code` invalide (hors enum) | Stocké tel quel mais ignoré côté front (fail-open) |
| Format `points_procedure` rétrocompatible (array de strings) | Continue de fonctionner, aucun `critere_code` associé |
| Échec de chargement des procedure-checks côté front | Moteur de cohérence retombe silencieusement sur la seule source IA |
| Plusieurs points F-96 même critère, statuts différents | Règle `NON_COMPLIANT` > `VERIFIED` > `TO_CHECK` |

---

## Critères d'acceptation

- [ ] Le prompt IA accepte et remplit `points_procedure` au format `[{ texte, critere_code? }]` (format string legacy toléré et silencieusement migré vers le nouveau format en lecture).
- [ ] Migration Liquibase `068-add-critere-code-to-procedure-checks.xml` ajoute la colonne nullable `critere_code VARCHAR(50)`.
- [ ] `ProcedureCheck` entity + `ProcedureCheckResponse` exposent `critereCode`.
- [ ] `ProcedureCheckService.createChecks()` et `createChecksWithVerifiedPropagation()` parsent et persistent `critere_code`, fail-open.
- [ ] La propagation VERIFIED/NON_COMPLIANT d'une analyse à la suivante préserve également le `critere_code`.
- [ ] `LicenciementSectionComponent` reçoit `@Input() procedureChecks: ProcedureCheck[] | null`.
- [ ] Le computed `coherenceAlerts` applique l'ordre de priorité A-E décrit plus haut.
- [ ] Contradiction F-96 VERIFIED ↔ avocat NON → blocker avec badge "Incohérence F-96 (OUI)".
- [ ] Contradiction F-96 NON_COMPLIANT ↔ avocat OUI → blocker avec badge "Incohérence F-96 (NON)".
- [ ] F-96 absent sur un critère → comportement SF-IA-03-01 strictement préservé.
- [ ] Tooltip affiche la raison F-96 si présente, "Statut confirmé par l'avocat" sinon.
- [ ] Bandeau récap agrège F-96 et IA, compteur blockers inclut les deux.
- [ ] Tests backend verts (parsing nouveau format, rétrocompat, fail-open, propagation du code, clé invalide).
- [ ] Tests frontend verts (matrice F-96 seul, F-96 + IA concordants, F-96 + IA contradictoires, fallback IA seule, procedureChecks vides).

---

## Périmètre

### Hors scope (explicite)

- Autres sources que F-96 et IA (pièces manquantes, questions IA interactives, citations documents) → SF-IA-03-03.
- Extension aux autres outils (F-DT-07, F-DT-09, F-FA-*, F-IM-*) → SF-IA-03-04 et suivantes.
- Niveau `info` et justification obligatoire → SF ultérieure.
- Rétro-tagging des points F-96 existants en base : pas de backfill, seules les nouvelles analyses auront des codes.
- Édition manuelle du `critere_code` par l'avocat (pas de UI d'association).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `procedure_checks.critere_code` | `null` | peuplé par parsing IA si présent dans JSON, sinon null |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Normalisation |
|-------|-------------|-------------|----------------------------|---------------|
| `critere_code` | Non | 50 | texte libre côté DB ; côté front, seuls les codes connus (14 codes F-DT-08) déclenchent une alerte | upper-case côté backend avant persistance |

---

## Technique

### Endpoint(s)

| Méthode | URL | Changement |
|---------|-----|------------|
| GET | `/api/v1/case-files/{caseFileId}/analyses/{analysisId}/procedure-checks` | ajoute `critereCode` au payload (nullable) |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `procedure_checks` | ALTER — ajout colonne `critere_code VARCHAR(50) NULL` | migration 068 |

### Migration Liquibase

- [x] Oui — `068-add-critere-code-to-procedure-checks.xml`
- [ ] Non applicable

Réversible : DROP COLUMN critere_code. Colonne nullable sans donnée migrée, pas de risque pour les analyses existantes.

### Composants Angular

- `ProcedureCheck` interface (frontend model) : ajout de `critereCode?: string | null`.
- `LicenciementSectionComponent` :
  - `@Input() procedureChecks?: ProcedureCheck[] | null`
  - Signal miroir `procedureChecksSignal` pour la réactivité
  - Computed `coherenceAlerts` étendu (priorité F-96 > IA)
  - Le type `CoherenceAlert` gagne un champ `source: 'F96' | 'IA' | 'F96_IA'` et un champ `statut` (pour l'affichage du tooltip F-96)
- `CaseFileDetailComponent` : charger les procedure-checks de l'analyse courante (déjà exposée dans `synthesis()`) ; ou re-utiliser les appels existants si la donnée est déjà disponible. Pas de nouvel appel si la synthèse les fournit.

### Référentiel de codes accepté côté frontend

Le frontend accepte uniquement les 14 codes F-DT-08 existants. Un `critereCode` inconnu est ignoré silencieusement (pas de crash, pas d'alerte).

---

## Plan de test

### Tests unitaires backend

- [ ] `ProcedureCheckService.createChecks()` — parse nouveau format `{texte, critere_code}` : code persisté upper-case.
- [ ] Rétrocompat : format string (legacy) → `critere_code` null, autres champs OK.
- [ ] Format objet sans `critere_code` → `critere_code` null, `texte` extrait.
- [ ] Clé `critere_code` invalide (non string / vide après trim) → null.
- [ ] `createChecksWithVerifiedPropagation()` : propagation VERIFIED/NON_COMPLIANT conserve le nouveau `critere_code`.
- [ ] Suite complète backend verte.

### Tests unitaires frontend

- [ ] F-96 VERIFIED seul sur FR_MOTIVATION + avocat NON → blocker source F-96.
- [ ] F-96 NON_COMPLIANT seul sur FR_CONVOCATION + avocat OUI → blocker source F-96.
- [ ] F-96 VERIFIED + avocat OUI → aucune alerte.
- [ ] F-96 TO_CHECK + avocat NON → ignoré, retombe sur règle IA si présente.
- [ ] F-96 absent, IA dit OUI, avocat NON critère bloquant → blocker source IA (SF-IA-03-01 inchangé).
- [ ] F-96 VERIFIED + IA NON + avocat NON → aucune alerte (F-96 gagne, concordance avec avocat).
- [ ] F-96 NON_COMPLIANT + IA OUI + avocat OUI → blocker source F-96 (IA écrasée).
- [ ] F-96 VERIFIED + IA OUI + avocat NON → blocker avec tooltip "F-96 + IA".
- [ ] Plusieurs points F-96 sur le même critère (VERIFIED + NON_COMPLIANT) → le dernier requalifié (NON_COMPLIANT) prime.
- [ ] `procedureChecks` vide ou null → comportement SF-IA-03-01 strictement préservé.
- [ ] `critereCode` inconnu dans la liste → ignoré, aucun crash.
- [ ] Compteur agrégé : 1 alerte F-96 + 1 alerte IA → `{total: 2, blockers: 2}` (F-96 toujours blocker).

### Tests d'intégration

- [ ] `GET /procedure-checks` retourne `critereCode` pour une analyse ayant des codes persistés.
- [ ] `PATCH /procedure-checks/{id}` inchangé (le code n'est pas modifiable via l'API).

### Isolation workspace

- [x] Applicable — déjà garantie par les endpoints existants (filtre `workspace_id` sur `procedure_checks`).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — extension localisée du pipeline IA, d'une entité et d'un composant.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `ProcedureCheckService.createChecks()` | parsing étendu au format objet — doit rester rétrocompat string | test legacy string dans la suite |
| `SynthesisComponent` | lit `ProcedureCheck[]`, ne doit pas casser si `critereCode` est présent | tests existants déjà verts conservés |
| `EnrichedAnalysisService` / `CaseAnalysisService` | prompt rallongé, vigilance tokens | vérifier longueur prompt |

### Smoke tests E2E concernés

- [ ] Aucun smoke test critique concerné — la fonctionnalité est ajoutée sur un flux déjà actif mais sans chemin d'auth/workspace.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IA-03-01` (Done, mergée 2026-04-13) — fournit l'infrastructure d'alerte côté composant.
- `F-96` (Done) — fournit la checklist procédurale.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi F-96 > IA dans la priorité** : un point F-96 au statut `VERIFIED` ou `NON_COMPLIANT` a été posé ou requalifié par l'avocat lui-même. Si sa réponse F-DT-08 contredit sa propre validation, c'est qu'il a oublié un élément — la preuve est forte par nature. L'IA reste une suggestion statistique, donc de moins haute confiance.
- **Pourquoi le format `points_procedure` passe de `[string]` à `[{texte, critere_code?}]`** : on veut une structure extensible (demain, on pourra y ajouter `source_doc`, `piece_liee`, etc.). Le format string reste accepté en lecture pour ne casser aucun dossier existant.
- **Pourquoi `blocker` même sur critère non bloquant quand F-96 contredit** : c'est une contradiction humaine directe (l'avocat s'auto-contredit). Gravité indépendante de la pondération backend du critère.
- **Pourquoi ne pas rétro-tagger les points existants en base** : coût élevé (re-générer une analyse IA), bénéfice limité (les dossiers actifs seront ré-analysés naturellement). Backfill reporté si le besoin émerge.
- **Pourquoi ne pas proposer d'édition manuelle du `critere_code`** : UX supplémentaire, ROI incertain. Si l'IA se trompe de tag, l'avocat peut toujours ignorer l'alerte — elle est non bloquante.
