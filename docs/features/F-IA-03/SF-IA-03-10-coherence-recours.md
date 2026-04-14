# Mini-spec — F-IA-03 / SF-IA-03-10 Cohérence IA sur F-IM-06 Recours

## Identifiant

`F-IA-03 / SF-IA-03-10`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IA-03-10-coherence-recours`

---

## Objectif

Étendre le moteur de cohérence à F-IM-06 Recours sur 2 champs : `recoursType` (enum 6 valeurs) et `dateNotification` (date). Sources : détection IA (`typeRecoursCode` et `dateNotificationDecisionContestee` de SF-IM-06-04), F-96, questions IA. Warning uniquement — l'avocat garde le choix final sur la stratégie de recours.

---

## Comportement attendu

### Champs surveillés

| Champ user | Code IA-03 | Source IA |
|---|---|---|
| `recoursType` (enum 6) | `IM06_RECOURS_TYPE` | `typeRecoursCode` IA |
| `dateNotification` | `IM06_DATE_NOTIFICATION` | `dateNotificationDecisionContestee` IA |

### Hiérarchie `recoursType`

| Étape | Condition | Niveau |
|---|---|---|
| A | user vide | rien |
| B | F-96 VERIFIED + `expected_value` parmi 6 codes → diverge | `warning` |
| C | Question IA "oui" + `expected_value` → diverge | `warning` |
| D | IA `typeRecoursCode` → diverge | `warning` |
| E | sinon | rien |

MULTI si ≥ 2 sources convergent.

### Hiérarchie `dateNotification`

| Étape | Condition | Niveau |
|---|---|---|
| A | user vide | rien |
| B | IA `dateNotificationDecisionContestee` → écart > 7 jours | `warning` |
| C | sinon | rien |

Pas de F-96/questions pour les dates (signal numérique pur).

### Pourquoi seuil 7 jours

Les délais de recours sont serrés (15-60 jours). Un écart > 7 jours sur la date de notification peut faire basculer un recours de "dans les délais" à "forclos". Seuil strict justifié par l'enjeu métier.

### Pourquoi warning et non blocker

F-IM-06 est un outil de génération : l'avocat peut vouloir essayer un autre type de recours ou corriger une date après lecture du dossier. Divergence = signal utile, pas un blocage.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| `aiData` null | aucune alerte |
| Résultat recours généré (non en édition) | alertes gelées |
| `expected_value` hors enum | ignoré |
| Date IA malformée | ignorée |
| Date user malformée | ignorée |

---

## Critères d'acceptation

- [ ] Prompts F-96 et questions IA étendus avec code `IM06_RECOURS_TYPE` + `expected_value` parmi 6 codes.
- [ ] Computed `coherenceAlerts: Partial<Record<IM06AlertField, alert>>` sur `ImmigrationRecoursSectionComponent`.
- [ ] 2 nouveaux `@Input` (procedureChecks, aiQuestions).
- [ ] Hiérarchie `recoursType` A-E avec MULTI, warning only.
- [ ] Hiérarchie `dateNotification` : IA seule, seuil 7 jours, warning.
- [ ] Alertes gelées quand recours chargé.
- [ ] Rétrocompat SF-IM-06-04 préservée.
- [ ] Tests frontend : matrice type + date + MULTI + freeze + fallback.

---

## Périmètre

### Hors scope (explicite)

- Surveillance des infos requérant (nom, prénom, nationalité, adresse) : non extraites par IA.
- Surveillance de `autorite`, `reference`, `exposeFaits` : choix avocat.
- Surveillance de `dateDecision` : redondant avec `dateNotification`.
- Calcul de la `dateLimite` de recours : déjà fait côté backend, pas de cohérence IA nécessaire.
- Extension à F-IM-07 → SF-IA-03-11.
- Niveau `info` + justification obligatoire.

---

## Valeurs initiales

Aucune entité créée.

---

## Contraintes de validation

| Champ | Format | Normalisation |
|-------|--------|---------------|
| `expected_value` IM06_RECOURS_TYPE | 1 des 6 codes | upper-case, filtré enum |
| Seuil date | 7 jours calendaires | `abs(ms_a - ms_b) / 86400000 > 7` |

---

## Technique

### Endpoint(s)

Aucun — prompts uniquement.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] **Non applicable**.

### Composants Angular

- `ImmigrationRecoursSectionComponent` :
  - 2 nouveaux `@Input` (`procedureChecks`, `aiQuestions`)
  - Signaux miroirs + `aiDataSignal`
  - Types `IM06CoherenceAlert`, `IM06AlertField = 'RECOURS_TYPE' | 'DATE_NOTIFICATION'`, `IM06AlertSource`
  - Helpers `buildRecoursTypeAlert` (hiérarchie A-E MULTI), `buildDateAlert` (IA seule + seuil)
  - Computed `coherenceAlerts`, `alertsSummary`
  - Alertes gelées quand `!showForm() || recours()`
  - Badges + tooltip + bandeau récap
- `CaseFileDetailComponent` : passer les 2 nouveaux inputs

### Prompts

- `CaseAnalysisService`, `EnrichedAnalysisService`, `AiQuestionService` : code `IM06_RECOURS_TYPE` ajouté avec `expected_value` obligatoire.

---

## Plan de test

### Tests unitaires frontend

RecoursType :
- [ ] F-96 VERIFIED + expected=RECOURS_CCE, user RECOURS_CGRA → warning F96.
- [ ] Question IA "oui" + expected=RECOURS_CNDA, user RECOURS_GRACIEUX_PREFET → warning QUESTION_IA.
- [ ] IA typeRecoursCode=RECOURS_CONTENTIEUX_TA, user RECOURS_GRACIEUX_PREFET → warning IA.
- [ ] F-96 + Question + IA convergents contre user → MULTI.
- [ ] Code IA match user → pas d'alerte.
- [ ] expected_value hors enum → ignoré.

DateNotification :
- [ ] IA date=2026-03-10, user 2026-03-12 (2 jours) → pas d'alerte.
- [ ] IA date=2026-03-10, user 2026-03-20 (10 jours) → warning.
- [ ] IA date=2026-03-10, user 2026-03-17 (7 jours pile) → pas d'alerte (non strict).
- [ ] IA date=2026-03-10, user 2026-03-18 (8 jours) → warning.
- [ ] Date IA malformée → pas d'alerte.
- [ ] User date vide → pas d'alerte.

Transverses :
- [ ] Compteur agrège 2 champs.
- [ ] Recours chargé → alertes gelées.
- [ ] `aiData` null → pas d'alerte.

### Isolation workspace

- [x] Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune**.

### Composants impactés

| Composant | Impact |
|---|---|
| `ImmigrationRecoursSectionComponent` | 2 Inputs + computed + UI |
| Prompts IA | rallongement modéré |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IM-06-04` (Done) — fournit `typeRecoursCode` normalisé et `dateNotificationDecisionContestee`.
- `SF-IA-03-05` (Done) — infrastructure `expected_value`.

### Questions ouvertes

- [ ] Aucune.

---

## Notes et décisions

- **Seuil date 7 jours strict** : plus serré que F-DT-07 (15 jours) car les délais de recours sont courts. Un écart > 7 jours peut faire perdre un recours.
- **Pourquoi warning et pas blocker** : F-IM-06 est un outil de génération. L'avocat peut vouloir tenter un recours différent de ce que suggère l'IA (ex: gracieux avant contentieux). Le choix reste son jugement professionnel.
- **Pas de surveillance `dateDecision`** : ce champ est sémantiquement proche de `dateNotification` (date de la décision). Double surveillance = bruit. On se limite à `dateNotification` qui est le calcul-clé des délais.
- **Pas de F-96/questions sur la date** : les documents procéduraux (F-96) ne parlent pas de dates précises. Les questions IA sur une date ("Le refus date-t-il du 10 mars ?") sont rares dans la pratique.
