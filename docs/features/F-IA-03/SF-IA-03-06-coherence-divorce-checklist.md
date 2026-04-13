# Mini-spec — F-IA-03 / SF-IA-03-06 Cohérence IA sur F-FA-07 Checklist divorce

## Identifiant

`F-IA-03 / SF-IA-03-06`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IA-03-06-coherence-divorce-checklist`

---

## Objectif

Étendre le moteur de cohérence à F-FA-07 Checklist divorce : détecter les incohérences entre les cases cochées par l'avocat (étapes FAIT/A_FAIRE, pièces PRESENTE/MANQUANTE) et les preuves disponibles (F-96, questions IA, pièces manquantes IA). Pattern proche de F-DT-08 avec des codes binaires par étape et par pièce.

---

## Comportement attendu

### Codes surveillés

**Étapes** (binaires — statut `FAIT` = positive, `A_FAIRE` = attente) :
- FR : `FR_CHOIX_AVOCATS`, `FR_REDACTION_CONVENTION`, `FR_ENVOI_LRAR`, `FR_DELAI_REFLEXION`, `FR_SIGNATURE_CONVENTION`, `FR_DEPOT_NOTAIRE`, `FR_ENREGISTREMENT`
- BE : `BE_CHOIX_AVOCAT`, `BE_REDACTION_CONVENTION`, `BE_REQUETE_CONJOINTE`, `BE_COMPARUTION`, `BE_JUGEMENT`, `BE_TRANSCRIPTION`

**Pièces** (binaires — statut `PRESENTE` = positive, `MANQUANTE` = absente) :
- FR : `FR_ACTE_MARIAGE`, `FR_ACTE_NAISSANCE_EPOUX`, `FR_ACTE_NAISSANCE_ENFANTS`, `FR_LIVRET_FAMILLE`, `FR_JUSTIF_DOMICILE`, `FR_CONTRAT_MARIAGE`, `FR_ETAT_PATRIMOINE`, `FR_JUSTIF_REVENUS`, `FR_PIECE_IDENTITE`
- BE : `BE_ACTE_MARIAGE`, `BE_ACTE_NAISSANCE_EPOUX`, `BE_ACTE_NAISSANCE_ENFANTS`, `BE_COMPOSITION_MENAGE`, `BE_CONTRAT_MARIAGE`, `BE_CONVENTION_PREALABLE`, `BE_JUSTIF_REVENUS`, `BE_PIECE_IDENTITE`

Ces codes sont identiques aux codes internes déjà utilisés par `DivorceChecklistReferentiel`. Ils sont dispatchés par **domaine du dossier** : un dossier `DROIT_FAMILLE` n'utilise jamais les codes F-DT-08, et inversement. Pas de collision pratique.

### Hiérarchie des sources par étape

Pour chaque étape cochée par l'avocat (avec statut `FAIT` ou `A_FAIRE`), en parcourant :

| Étape | Condition | Niveau |
|---|---|---|
| A | statut `A_FAIRE` et aucune source positive | rien (pas encore fait, normal) |
| B | F-96 VERIFIED sur le code + avocat `A_FAIRE` | `warning` (IA dit étape faite, avocat dit non) |
| C | F-96 NON_COMPLIANT sur le code + avocat `FAIT` | `blocker` (IA dit étape non faite alors que avocat dit faite) |
| D | Question IA "oui" + avocat `A_FAIRE` | `warning` |
| E | Question IA "non" + avocat `FAIT` | `blocker` |
| F | Concordance | rien |

Si plusieurs sources convergent → `MULTI`.

### Hiérarchie des sources par pièce

Pour chaque pièce avec statut `PRESENTE` ou `MANQUANTE` :

| Étape | Condition | Niveau |
|---|---|---|
| A | statut `MANQUANTE` et pièce listée dans `pieces_manquantes` IA | rien (concordance) |
| B | statut `PRESENTE` et pièce taggée dans `pieces_manquantes` IA | `warning` (IA la dit absente, avocat la dit présente) |
| C | F-96 NON_COMPLIANT sur le code pièce + avocat `PRESENTE` | `blocker` |
| D | F-96 VERIFIED sur le code pièce + avocat `MANQUANTE` | `warning` |
| E | Concordance ou absence de source | rien |

### Cas d'erreur

| Situation | Comportement |
|-----------|---------------------|
| `critere_code` inconnu (hors enum F-FA-07) | ignoré (fail-open) |
| Pièce IA sans `critere_code` | non utilisée pour la cohérence (affichage checklist inchangé) |
| Dossier sans procedureChecks/questions/pièces | pas d'alerte, comportement checklist actuel |
| Avocat change d'état → computed recalculé | alerte met à jour en temps réel |

---

## Critères d'acceptation

- [ ] Prompts étendus (`CaseAnalysisService`, `EnrichedAnalysisService`, `AiQuestionService`) : pour les dossiers DROIT_FAMILLE, les codes F-FA-07 sont documentés et acceptés sur `points_procedure` et questions. Convention "oui = état positif" préservée (FAIT / PRESENTE).
- [ ] `DivorceChecklistSectionComponent` reçoit 4 `@Input` (procedureChecks, aiQuestions, piecesManquantes, déjà ou pas aiData).
- [ ] Signaux miroirs + `ngOnChanges` pour réactivité computed.
- [ ] Types `DivorceCoherenceAlert` et `DivorceAlertField = 'STEP' | 'PIECE'` (ou indexé par code).
- [ ] Computed `coherenceAlerts: Record<code, DivorceCoherenceAlert>` appliquant les hiérarchies décrites.
- [ ] Computed `alertsSummary: {total, blockers}`.
- [ ] Badge `warning` ou `blocker` à côté de chaque étape / pièce en alerte, tooltip citant source.
- [ ] Bandeau récap conditionnel en haut de la checklist.
- [ ] Rétrocompat totale : les 40+ tests existants F-IA-03 sur DT08/DT07/DT09 et F-FA-07 passent sans modification.
- [ ] Tests unitaires frontend (matrice étapes × hiérarchie + matrice pièces × hiérarchie + MULTI + fallback).
- [ ] Tests backend si prompt touché (parsing déjà couvert par SF-IA-03-02/03/05).

---

## Périmètre

### Hors scope (explicite)

- Extraction IA nouvelle spécifique F-FA-07 (pas de `divorce_checklist_detection` dans le prompt, on exploite ce qui existe : points_procedure + pieces_manquantes + questions).
- Extension aux autres outils famille (F-FA-05, F-FA-06) — subfeatures suivantes.
- Extension immigration (F-IM-*) — subfeatures ultérieures.
- Niveau `info` + justification obligatoire.
- Modification de la logique de calcul de progression F-FA-07 (inchangée).

---

## Valeurs initiales

Aucune entité créée. Pur calcul dérivé côté frontend.

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Normalisation |
|-------|-------------|------------------|---------------|
| critere_code reconnu | Non | doit être dans l'enum F-FA-07 (19 étapes + 17 pièces) | upper-case, validé côté front |

---

## Technique

### Endpoint(s)

Aucun. Subfeature purement frontend + extension prompts backend.

### Tables impactées

Aucune. Le modèle `expected_value` de SF-IA-03-05 existe mais n'est pas utilisé ici (codes binaires).

### Migration Liquibase

- [ ] Oui
- [x] **Non applicable**.

### Composants Angular

- `DivorceChecklistSectionComponent` :
  - 4 nouveaux `@Input` (procedureChecks, aiQuestions, piecesManquantes, aiData)
  - Signaux miroirs
  - Types `DivorceCoherenceAlert`, `DivorceAlertSource`
  - Computed `coherenceAlerts: Record<code, alert>`
  - Computed `alertsSummary`
  - Helpers : `buildStepAlert(code, statut)`, `buildPieceAlert(code, statut)`
  - Badge + tooltip par ligne de la checklist

### Prompts

- `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` et `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` : pour la section `points_procedure`, ajouter les codes F-FA-07 à la liste des codes autorisés. Même chose pour `pieces_manquantes`. Convention : "oui" / VERIFIED / point présent = étape faite ou pièce présente.
- `AiQuestionService.SYSTEM_PROMPT_TEMPLATE` : ajouter les codes F-FA-07 aux codes autorisés pour `critere_code` dans les questions.

### Dispatch par domaine

Les prompts listent l'ensemble des codes (DT08 + DT09 + FA07). Claude choisit les codes adaptés au domaine du dossier (droit du travail vs droit de la famille). Pas de filtrage explicite dans le prompt — c'est à Claude de ne pas mélanger les domaines.

---

## Plan de test

### Tests unitaires frontend

Étapes (matrice par statut × source) :
- [ ] Step FAIT + F-96 VERIFIED → pas d'alerte
- [ ] Step FAIT + F-96 NON_COMPLIANT → `blocker`
- [ ] Step A_FAIRE + F-96 VERIFIED → `warning`
- [ ] Step A_FAIRE + F-96 NON_COMPLIANT → pas d'alerte
- [ ] Step FAIT + Question IA "non" → `blocker`
- [ ] Step A_FAIRE + Question IA "oui" → `warning`
- [ ] Step + aucune source → pas d'alerte
- [ ] 2 sources convergent → `MULTI`

Pièces (matrice par statut × source) :
- [ ] Pièce PRESENTE + pièce listée dans piecesManquantes (avec code pièce) → `warning`
- [ ] Pièce MANQUANTE + pièce listée → pas d'alerte (concordance)
- [ ] Pièce PRESENTE + F-96 NON_COMPLIANT → `blocker`
- [ ] Pièce MANQUANTE + F-96 VERIFIED → `warning`
- [ ] Code inconnu ignoré
- [ ] piecesManquantes sans critere_code ignoré pour la cohérence

Transverses :
- [ ] Compteur agrège étapes + pièces
- [ ] Résultat sauvegardé → alertes se mettent à jour à chaque toggle
- [ ] Rétrocompat : les tests DT08/DT07/DT09 restent verts

### Tests backend

Pas de logique backend nouvelle (les prompts sont testés par observation manuelle sur staging). Les parsers `ProcedureCheckService.parsePointsProcedure` et `AiQuestionService.parseQuestions` acceptent déjà les nouveaux codes via le parsing générique upper-case.

### Isolation workspace

- [x] Non applicable — aucun accès données nouveau.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune**.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `DivorceChecklistSectionComponent` | 4 @Input ajoutés, computed, affichage enrichi | 4 tests existants conservés |
| Prompts IA | rallongement (liste de codes ~36 codes sur 2 domaines) | vérifier longueur prompts |

### Smoke tests E2E concernés

- [ ] Aucun concerné.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IA-03-02` (Done) — pattern F-96 avec `critere_code`.
- `SF-IA-03-03` (Done) — pattern questions IA + pièces.
- `F-FA-07` (Done) — composant existant et codes stables.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi pas de nouveau code `FA07_*`** : les codes `FR_CHOIX_AVOCATS`, etc. sont déjà uniques et utilisés en interne par `DivorceChecklistReferentiel`. Les préfixer créerait de la redondance. Le dispatch se fait naturellement par domaine du dossier (un dossier DROIT_FAMILLE n'exposera jamais de codes DT08).
- **Pourquoi F-96 NON_COMPLIANT sur étape = blocker (sévère)** : l'avocat a explicitement coché "étape faite" alors que l'IA (après avoir examiné les documents) affirme l'inverse. Contradiction frontale → blocker justifié.
- **Pourquoi F-96 VERIFIED + A_FAIRE = warning (moins sévère)** : l'IA dit "c'est fait" alors que l'avocat ne l'a pas encore coché. Signal utile (peut-être oubli) mais moins grave — peut-être que l'avocat n'a pas encore eu le temps de cocher.
- **Pièces pour BE** : le nombre de pièces belges (8) est légèrement inférieur à celui de la France (9). Le prompt peut omettre certains codes selon le pays — pas grave, la liste côté front filtre déjà.
- **Pas d'extraction dédiée** : on exploite uniquement les points de procédure, pièces manquantes et questions déjà générés par l'IA. Pas de nouveau bloc JSON comme `licenciement_validity_detection`. La subfeature reste légère.
