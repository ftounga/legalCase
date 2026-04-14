# Mini-spec — F-IM-06 / SF-IM-06-04 Extraction IA du type de recours + date de notification

## Identifiant

`F-IM-06 / SF-IM-06-04`

## Feature parente

`F-IM-06` — Générateur de recours préfectoral / CGRA

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IM-06-04-extraction-ia-recours`

---

## Objectif

Enrichir l'extraction IA immigration pour F-IM-06 : ajouter un champ `type_recours_code` (6 valeurs exactes du référentiel F-IM-06) et `date_notification_decision_contestee` (date de la décision que l'on attaque). Pré-remplir `recoursType` et `dateNotification` du formulaire depuis ces nouvelles valeurs. Préalable à SF-IA-03-10.

---

## Comportement attendu

### Cas nominal

1. Le prompt IMMIGRATION demande deux nouveaux champs :
   - `type_recours_code` : l'un des 6 codes (null si non déterminable)
     - France : `RECOURS_GRACIEUX_PREFET`, `RECOURS_CONTENTIEUX_TA`, `RECOURS_CNDA`
     - Belgique : `RECOURS_CGRA`, `RECOURS_CCE`, `RECOURS_CE_BELGIQUE`
   - `date_notification_decision_contestee` : date de la décision contestée au format YYYY-MM-DD (null si non déterminable)
2. Les champs existants `type_procedure_detectee` (3 valeurs) et `date_depot_procedure` sont **conservés** — ils restent utilisés ailleurs (timelines, pipeline procédural F-69).
3. `ImmigrationExtractedData` expose `typeRecoursCode` et `dateNotificationDecisionContestee`.
4. `ImmigrationRecoursSectionComponent` reçoit `@Input() aiData?: ImmigrationExtractedData | null` et pré-remplit quand le formulaire est vide (premier rendu, avant qu'un recours soit généré) :
   - `recoursType` ← `typeRecoursCode` IA si valeur compatible
   - `dateNotification` ← `dateNotificationDecisionContestee` IA si valide
5. Une note de provenance "Valeur pré-remplie depuis l'analyse IA" s'affiche sous chaque champ pré-rempli. Effacée dès modification manuelle.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Code IA hors enum | `typeRecoursCode` = null (fail-open) |
| Date malformée | `dateNotificationDecisionContestee` = null |
| Casse variable (`recours_cnda`) | upper-case, filtré contre l'enum |
| Recours déjà généré (GET renvoie un résultat) | pas de pré-remplissage IA (les valeurs persistées ont priorité) |
| IA null ou champs absents | comportement actuel préservé (formulaire vide avec defaults) |

---

## Critères d'acceptation

- [ ] `IMMIGRATION_INSTRUCTION` étendu avec `type_recours_code` (6 valeurs) et `date_notification_decision_contestee`.
- [ ] `ImmigrationExtractedData` record : 2 nouveaux champs + constructeur rétrocompat (le constructeur 6-args de SF-IM-05-04 reste fonctionnel).
- [ ] `IMMIGRATION_RECOURS_CODES` set pour validation fail-open.
- [ ] Parsing upper-case pour le code, filtre enum, parsing date standard.
- [ ] `ImmigrationExtractedData` (frontend) : 2 champs optionnels.
- [ ] `ImmigrationRecoursSectionComponent` : `@Input aiData` + logique de pré-remplissage au premier rendu quand `!loading() && showForm() && !recours()`.
- [ ] Signaux `provenanceRecoursType`, `provenanceDateNotification` gérant les notes.
- [ ] Modification manuelle → note effacée.
- [ ] Rétrocompat : tests existants F-IM-06 intacts, formulaire fonctionne sans IA.
- [ ] Tests backend (parsing code/date, fail-open, rétrocompat).
- [ ] Tests frontend (prefill nominal, fallback default, note effacée, skip si recours existant).

---

## Périmètre

### Hors scope (explicite)

- Extraction des infos requérant (nom, prénom, nationalité, adresse) : hors scope pour l'instant (données personnelles, signal fragile sur documents typiques).
- Extraction de `autorite`, `reference` de la décision : hors scope.
- Extraction de `exposeFaits` : hors scope (doit être rédigé par l'avocat).
- Cohérence IA → SF-IA-03-10 suivante.
- Modification du générateur de document : inchangé.
- Backfill des analyses existantes.

---

## Valeurs initiales

Aucune entité créée. Données dans le JSON `analysis_result`.

---

## Contraintes de validation

| Champ | Obligatoire | Format | Normalisation |
|-------|-------------|--------|---------------|
| `type_recours_code` | Non | 1 des 6 codes | upper-case, filtré contre enum |
| `date_notification_decision_contestee` | Non | YYYY-MM-DD ou null | validation format |

---

## Technique

### Endpoint(s)

| Méthode | URL | Changement |
|---|---|---|
| GET | `/api/v1/case-files/{id}/case-analysis` | `immigrationExtractedData.typeRecoursCode` et `dateNotificationDecisionContestee` ajoutés |

### Tables impactées

Aucune.

### Migration Liquibase

- [x] **Non applicable**.

### Composants Angular

- `ImmigrationExtractedData` (frontend) : 2 nouveaux champs
- `ImmigrationRecoursSectionComponent` :
  - nouvel `@Input() aiData`
  - signal miroir `aiDataSignal`
  - méthode `prefillFromAi()`
  - signaux `provenanceRecoursType`, `provenanceDateNotification`
  - gestionnaires `onRecoursTypeChange`, `onDateNotificationChange` pour effacer les notes
- `CaseFileDetailComponent` : passe `[aiData]="synthesis()?.immigrationExtractedData"`

---

## Plan de test

### Tests unitaires backend

- [ ] Code IA `recours_cnda` (casse mixte) → `RECOURS_CNDA`.
- [ ] Code IA hors enum → null.
- [ ] `date_notification_decision_contestee` ISO parsée, format invalide → null.
- [ ] Les champs existants (`type_procedure_detectee`, `date_depot_procedure`) restent fonctionnels.
- [ ] Prompt IMMIGRATION_INSTRUCTION contient les 6 codes recours.

### Tests unitaires frontend

- [ ] Prefill `recoursType` depuis IA quand pays FR workspace.
- [ ] Prefill `dateNotification` depuis IA.
- [ ] Note provenance visible si IA fournit.
- [ ] Modification manuelle efface la note.
- [ ] Recours déjà chargé → pas de prefill IA.
- [ ] aiData null → formulaire avec defaults, pas de note.
- [ ] Code IA inconnu (null côté model) → fallback default.

### Isolation workspace

- [x] Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune**.

### Composants impactés

| Composant | Impact |
|---|---|
| `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION` | prompt rallongé |
| `ImmigrationExtractedData` | record rallongé à 8 champs, constructeur rétrocompat |
| `ImmigrationRecoursSectionComponent` | flux prefill ajouté |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `F-IA-01` (Done) — pipeline extraction.
- `SF-IM-05-04` (Done) — pattern d'enrichissement `ImmigrationExtractedData`.

### Questions ouvertes

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi garder `type_procedure_detectee` existant** : il est utilisé par le pipeline procédural F-69 et les timelines. Le remplacer casserait ces dépendances. Pattern identique à SF-IM-05-04 (code normalisé à côté du texte libre).
- **Pourquoi 2 dates distinctes** : `date_notification_decision_contestee` (date de la décision attaquée, sert à calculer le délai de recours) vs `date_depot_procedure` (date de dépôt effectif). Sémantiquement différentes. F-IM-06 utilise la première, le pipeline procédural la seconde.
- **Pourquoi pas d'extraction du requérant** : données personnelles sensibles, parfois masquées dans les documents. L'avocat a déjà ces infos dans son CRM. ROI faible.
- **Cohérence avec SF-IM-05-04** : même approche (code normalisé + champ texte libre conservé), même pattern rétrocompat via constructeur overloadé de record.
