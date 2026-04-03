# Mini-spec — F-DT-03 / SF-DT-03-01 Calcul des délais de prescription emploi

---

## Identifiant

`F-DT-03 / SF-DT-03-01`

## Feature parente

`F-DT-03` — Calcul automatique des délais de prescription (droit du travail)

## Statut

`ready`

## Date de création

2026-04-04

## Branche Git

`feat/SF-DT-03-01-prescription-emploi`

---

## Objectif

Après une analyse enrichie, classifier le type de litige et calculer automatiquement le délai de prescription applicable selon le Code du travail, puis l'afficher dans le bloc délais comme "Délais légaux applicables" (lecture seule, distinct des délais extraits des pièces).

---

## Comportement attendu

### Cas nominal

1. L'analyse enrichie se termine (`EnrichedAnalysisService`).
2. Le JSON enrichi contient deux nouveaux champs : `type_litige_detecte` (string, ex : `"LICENCIEMENT_SANS_CAUSE_REELLE"`) et `date_reference_prescription` (date ISO, ex : `"2025-11-15"`).
3. `StatutoryDeadlineService.createStatutoryDeadlines(analysis, rawJson)` est appelé — parse ces champs, calcule la date limite (`date_reference + période légale`), persiste un `CaseDeadline` avec `source = "STATUTORY"`.
4. Le frontend charge les délais du dossier et affiche les délais `STATUTORY` dans une nouvelle sous-section "Délais légaux applicables" (lecture seule, avec l'article de référence).

### Périodes légales mappées

| Type de litige (`type_litige_detecte`) | Période | Article |
|---------------------------------------|---------|---------|
| `LICENCIEMENT_SANS_CAUSE_REELLE` | 1 an | Art. L1471-1 |
| `LICENCIEMENT_ECONOMIQUE` | 1 an | Art. L1471-1 |
| `PRISE_ACTE_RUPTURE` | 1 an | Art. L1471-1 |
| `HARCELEMENT_MORAL` | 5 ans | Art. L1152-1 |
| `DISCRIMINATION` | 5 ans | Art. L1132-1 |
| `HEURES_SUPPLEMENTAIRES` | 3 ans | Art. L3245-1 |
| `RAPPEL_SALAIRE` | 3 ans | Art. L3245-1 |

### Label affiché

Format : `"Prescription — [libellé humain du type] ([article])"`.
Ex : `"Prescription — Licenciement sans cause réelle (Art. L1471-1)"`.

### Date de référence

- Si `date_reference_prescription` est fournie et valide → utilisée pour calculer la date limite.
- Sinon → date de création du dossier (`caseFile.createdAt`) utilisée comme fallback.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `type_litige_detecte` absent ou non reconnu | Log debug, aucun délai créé (fail-open) |
| `date_reference_prescription` absente ou invalide | Fallback sur `caseFile.createdAt`, délai créé quand même |
| Délai STATUTORY déjà présent pour ce dossier et ce type | Le nouveau remplace l'ancien (upsert sur `caseFileId + source + label`) |
| JSON brut malformé | Log warn, fail-open — aucune exception propagée |

---

## Critères d'acceptation

- [ ] Un délai `STATUTORY` est créé après une analyse enrichie si `type_litige_detecte` est reconnu
- [ ] La date limite est correctement calculée (date_reference + période légale)
- [ ] Le fallback sur `caseFile.createdAt` fonctionne si `date_reference_prescription` est absente
- [ ] Un type non reconnu ne crée pas de délai et ne fait pas planter la pipeline
- [ ] Le frontend affiche les délais STATUTORY dans une sous-section "Délais légaux applicables" séparée des délais manuels/IA
- [ ] Les délais STATUTORY sont en lecture seule (pas de boutons éditer/supprimer/accepter/rejeter)
- [ ] L'article de référence est affiché (`Art. L1471-1`, etc.)
- [ ] Isolation workspace : un utilisateur ne voit que les délais de ses dossiers

---

## Périmètre

### Hors scope

- Calcul de délais pour d'autres domaines (immigration, famille) — V1 emploi uniquement
- Gestion manuelle du type de litige par l'avocat (classification IA uniquement)
- Notifications/alertes liées aux délais STATUTORY (couvert par F-69 existant)
- Types de litige non listés dans la table ci-dessus (extensible en V2)

---

## Technique

### Endpoint(s)

Aucun nouveau — lecture via l'endpoint existant `GET /api/v1/case-files/{id}/deadlines`.

### Nouveaux composants

| Composant | Rôle |
|---|---|
| `StatutoryDeadlineService` | Parse `type_litige_detecte` + `date_reference_prescription`, calcule et persiste le délai STATUTORY |
| `LitigationTypeMapper` | Enum ou classe utilitaire : `type_litige_detecte` → `(période, article, libellé)` |

### Composants modifiés

| Composant | Modification |
|---|---|
| `EnrichedAnalysisService` | Appel à `StatutoryDeadlineService.createStatutoryDeadlines()` après l'analyse (comme `createAiDetectedDeadlines`) |
| Prompt enrichi (système ou user) | Ajout instruction pour fournir `type_litige_detecte` et `date_reference_prescription` dans le JSON de réponse |
| `case-deadlines-section.component` | Nouvelle sous-section "Délais légaux applicables" pour `source === 'STATUTORY'` (lecture seule) |
| `case-deadline.model.ts` | Ajout de `'STATUTORY'` à l'union `source` |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_deadlines` | INSERT | Nouvelle valeur `source = 'STATUTORY'` — pas de migration DDL, colonne déjà `VARCHAR(10)` |

### Migration Liquibase

- [x] Non applicable — pas de changement de schéma

---

## Plan de test

### Tests unitaires

- [ ] `LitigationTypeMapper` — chaque type reconnu retourne la bonne période et l'article correct
- [ ] `LitigationTypeMapper` — type inconnu retourne `Optional.empty()`
- [ ] `StatutoryDeadlineService` — type reconnu + date_reference → délai STATUTORY persisté avec bonne date
- [ ] `StatutoryDeadlineService` — date_reference absente → fallback sur createdAt
- [ ] `StatutoryDeadlineService` — type inconnu → aucun délai créé, pas d'exception
- [ ] `StatutoryDeadlineService` — JSON malformé → fail-open, pas d'exception

### Tests d'intégration

- [ ] Non applicable — pas de nouvel endpoint HTTP

### Isolation workspace

- [x] Non applicable — délai STATUTORY créé dans le contexte d'un `CaseFile` déjà isolé par workspace (contrôle existant dans `CaseDeadlineService.list()`)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — ajout d'un service + valeur `source` + sous-section frontend, aucune modification auth/routing/workspace/plans.

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- F-97 (détection délais IA) — statut : **done** (architecture `CaseDeadline` en place, réutilisée)
- F-69 (alertes délais) — statut : **done** (les délais STATUTORY déclenchent les alertes existantes automatiquement)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `source = "STATUTORY"` étend la colonne existante (VARCHAR(10)) — valeur de longueur 9, compatible.
- L'upsert est implémenté comme : si un délai STATUTORY avec le même label existe déjà pour ce dossier, il est mis à jour (évite les doublons sur re-analyse).
- Le prompt enrichi est modifié pour demander explicitement `type_litige_detecte` (valeur parmi l'enum défini) et `date_reference_prescription` (ISO 8601). Si l'IA ne peut pas déterminer le type, elle retourne `null` — traité comme type inconnu.
- Pattern identique à `createAiDetectedDeadlines` dans `CaseDeadlineService` — même fail-open, même lieu d'appel.
