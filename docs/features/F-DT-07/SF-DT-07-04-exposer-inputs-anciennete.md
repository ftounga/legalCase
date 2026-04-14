# Mini-spec — F-DT-07 / SF-DT-07-04 Persister et exposer les inputs d'ancienneté

## Identifiant

`F-DT-07 / SF-DT-07-04`

## Feature parente

`F-DT-07` — Barème d'ancienneté et congés conventionnels

## Statut

`draft`

## Date de création

2026-04-15

## Branche Git

`feat/SF-DT-07-04-exposer-inputs-anciennete`

---

## Objectif

Finaliser le pré-remplissage complet du formulaire F-DT-07 après reload de page. Suite au découpage de SF-118-04, les 3 champs d'entrée `salaireBase`, `congesContrat`, `primeContrat` ne sont ni stockés en base, ni renvoyés par `AncienneteResponse`. Conséquence : après reload → clic Modifier, ces champs reviennent à leurs valeurs par défaut (0, 25, 0) au lieu des valeurs saisies par l'avocat.

Cette SF étend le backend pour persister ces inputs et les exposer dans la réponse, puis applique le `prefillForm(resp)` complet côté front — aligné sur le pattern des autres outils (F-DT-09, F-IM-05, F-IM-06).

---

## Comportement attendu

### Cas nominal

1. L'avocat renseigne les 5 champs du formulaire Ancienneté : convention, dateEntrée, salaireBase, congesContrat, primeContrat.
2. Clic **Calculer** → POST `/anciennete`. Le backend persiste les 5 champs en base (table `anciennete_analyses`) et calcule les résultats dérivés.
3. La réponse contient les 5 inputs + tous les résultats dérivés.
4. Reload de page. `loadExisting()` → GET 200 → `prefillForm(resp)` restaure les 5 champs.
5. Clic **Modifier** → formulaire visible avec les 5 valeurs pré-remplies.
6. L'avocat peut modifier, recalculer, tout fonctionne.

### Schéma backend

- Table `anciennete_analyses` : ajouter 3 colonnes `salaire_base NUMERIC(12,2)`, `conges_contrat INTEGER`, `prime_contrat NUMERIC(5,2)`, toutes NULLABLE en phase 1 pour rétrocompatibilité (analyses existantes sans ces colonnes).
- Migration Liquibase 073.
- Entité `AncienneteAnalysis` : ajouter les 3 champs.
- Record `AncienneteResponse` : ajouter `salaireBase`, `congesContrat`, `primeContrat` (tous optionnels via `BigDecimal` / `Integer` nullable, ordre après `conventionCode`).
- Service `AncienneteService.calculate()` : persister les 3 valeurs reçues dans le POST.
- Service `AncienneteService.get()` : remonter les 3 valeurs.

### Rétrocompatibilité

- Dossiers historiques sans les 3 colonnes → valeurs `null` en base → réponse contient `null` pour ces champs → front garde ses valeurs par défaut. Pas de crash.
- Front : `prefillForm` n'applique `this.salaireBase.set(...)` que si `resp.salaireBase != null`.

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| Analyse legacy sans les 3 colonnes | Réponse 200 avec `null` sur les 3 champs. Front garde défauts |
| POST avec valeurs négatives ou décimales invalides | Validation existante de `AncienneteRequest` inchangée |
| GET sur dossier non analysé | 404 inchangé |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : F-DT-09 `IndemniteComparatif`, F-IM-05/06/07, F-DT-10 — tous ont déjà des inputs exposés dans leur response
- [x] **Autres pays** : non applicable — la persistance est structurelle
- [x] **Autres domaines** : non applicable
- [x] **Autres UI patterns** : le pattern "persister les inputs dans la réponse" est le standard — cet outil F-DT-07 était une exception à corriger
- [x] **Autres flows transversaux** : non applicable

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-07 Ancienneté | Oui | Intégré dans cette SF |
| Autres outils | Non | Déjà conformes au pattern |

### Décision

- [x] Étendu à la seule cible applicable (F-DT-07)
- [ ] Subfeature parallèle
- [ ] Backlog
- [x] Non applicable aux autres outils (déjà conformes)

---

## Critères d'acceptation

- [ ] Migration `073-add-inputs-to-anciennete-analyses.xml` ajoute 3 colonnes nullable et est réversible.
- [ ] Entité `AncienneteAnalysis` expose les 3 champs avec getters/setters Lombok.
- [ ] `AncienneteService.analyze` persiste les 3 valeurs depuis `AncienneteRequest`.
- [ ] `AncienneteService.get` retourne les 3 valeurs.
- [ ] `AncienneteResponse` expose les 3 champs.
- [ ] Frontend `AncienneteResponse` TypeScript model mis à jour.
- [ ] `AncienneteSectionComponent.prefillForm` restaure les 5 champs de façon conditionnelle (null-safe pour le legacy).
- [ ] TODO dans `prefillForm` retiré (désormais complet).
- [ ] Tests backend : IT POST/GET round-trip persiste les 3 inputs. GET sur dossier legacy (colonnes null) → réponse avec nulls, pas d'erreur.
- [ ] Tests frontend : `AncienneteSectionComponent` pré-remplit les 5 signals depuis GET 200 + `editForm` restaure les 5 champs.
- [ ] 940+ tests frontend verts, suite backend existante verte.

---

## Périmètre

### Hors scope (explicite)

- Modifier la logique de calcul / les résultats dérivés.
- Ajouter des champs d'input nouveaux (seulement persister ceux existants).
- Rétro-peupler les analyses existantes avec les valeurs manquantes.
- Toucher les outils déjà conformes.
- Modifier le référentiel de conventions collectives.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `anciennete_analyses.salaire_base` | `null` (legacy), valeur POST sinon | |
| `anciennete_analyses.conges_contrat` | `null` (legacy), valeur POST sinon | |
| `anciennete_analyses.prime_contrat` | `null` (legacy), valeur POST sinon | |

---

## Contraintes de validation

| Champ | Obligatoire | Format | Normalisation |
|-------|-------------|--------|---------------|
| `salaireBase` | Oui dans POST (validation existante) | `BigDecimal > 0` | — |
| `congesContrat` | Oui dans POST | `int >= 0` | — |
| `primeContrat` | Oui dans POST | `BigDecimal >= 0` | — |

Aucune nouvelle règle — on expose simplement ce qui est déjà saisi.

---

## Technique

### Endpoints

Pas de nouveau endpoint. Contrat étendu :
- POST `/api/v1/case-files/{id}/anciennete` — réponse enrichie.
- GET `/api/v1/case-files/{id}/anciennete` — réponse enrichie.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `anciennete_analyses` | ALTER — 3 colonnes nullable | migration 073 |

### Migration Liquibase

- [x] Oui — `073-add-inputs-to-anciennete-analyses.xml`, réversible (DROP COLUMN).

### Composants backend

- `AncienneteAnalysis` (entité JPA) — 3 champs ajoutés.
- `AncienneteService.analyze` — set des 3 champs depuis request.
- `AncienneteResponse` (record) — 3 champs ajoutés.
- `AncienneteService.toResponse` — propagation des 3 champs.

### Composants Angular

- `core/models/anciennete.model.ts` — interface `AncienneteResponse` étendue (3 champs optionnels).
- `AncienneteSectionComponent.prefillForm` — appels `this.*.set()` conditionnels.

---

## Plan de test

### Tests unitaires backend

- [ ] `AncienneteServiceTest` : POST persiste les 3 inputs en base.
- [ ] `AncienneteControllerIT` : POST → GET round-trip retourne les 3 inputs.
- [ ] `AncienneteControllerIT` : GET sur analyse legacy (colonnes NULL en DB, via insertion directe) → 200 avec `null` sur les 3 champs.

### Tests unitaires frontend

- [ ] `AncienneteSectionComponent` : GET 200 avec `salaireBase=3500` → `component.salaireBase()` = 3500.
- [ ] `AncienneteSectionComponent` : GET 200 avec `salaireBase=null` (legacy) → `component.salaireBase()` garde la valeur par défaut 0.
- [ ] `AncienneteSectionComponent` : editForm après GET 200 restaure les 5 champs.

### Tests d'intégration

- [x] Couverts par `AncienneteControllerIT` existants + nouveaux cas.

### Validation manuelle

- [ ] Staging : dossier Martin (ou nouveau dossier travail), saisir F-DT-07 Calculer, hard refresh, cliquer Modifier → les 5 champs pré-remplis.
- [ ] Dossier legacy (analyse pré-migration) : Calculer n'échoue pas, Modifier restaure ce qui est disponible (convention + dateEntrée).

### Isolation workspace

- [x] Préservée par les règles existantes.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — extension structurée d'un outil existant.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| `AncienneteService.analyze` | Set supplémentaire, logique inchangée | Tests existants |
| `AncienneteResponse` | 3 champs ajoutés (optionnels) | Tests existants + nouveaux |
| Frontend `AncienneteSectionComponent` | prefillForm étendu | Tests existants + nouveaux |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `SF-118-04` mergée (PR #337) — TODO frontend déjà en place à lever.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi colonnes nullable** : rétro-compatibilité pour les analyses déjà en base. Pas de backfill, pas de migration lourde.
- **Pourquoi ne pas stocker en JSON `request_data`** : cohérence avec les autres outils (indemnite_comparatifs, etc.) qui utilisent des colonnes nommées. Le JSON aurait fait gagner peu et perdu en lisibilité SQL.
- **Pourquoi `BigDecimal(12,2)` et pas `DOUBLE`** : cohérence avec les autres colonnes monétaires du projet (primeAncienneteMontant est déjà BigDecimal). Évite les arrondis flottants.
- **Pourquoi `conges_contrat INTEGER`** : jours entiers, pas de décimale légitime.
