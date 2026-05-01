# Mini-spec — F-IM-11 / SF-IM-11-03 Pré-remplissage durée restante depuis l'analyse IA

> Constat terrain (dossier "Immigration Chen - 4", staging, 2026-05-01) : l'outil
> "CHANGEMENT DE STATUT (FR)" ne pré-remplit que le titre actuel. La durée
> restante reste vide alors que l'IA expose `dateExpirationTitre`.

---

## Identifiant

`F-IM-11 / SF-IM-11-03`

## Feature parente

`F-IM-11` — Changement de statut (CESEDA)

## Statut

`draft`

## Date de création

2026-05-01

## Branche Git

`feat/SF-IM-11-03-prefill-duree-restante`

---

## Objectif

Pré-remplir `dureeRestanteSurTitreActuelMois` dans l'outil "CHANGEMENT DE STATUT (FR)"
depuis `aiData.dateExpirationTitre` (mois entiers arrondis vers le bas, depuis
aujourd'hui), avec badge IA et reset au changement manuel — pattern canonique
F-155.

---

## Comportement attendu

### Cas nominal

Au montage du composant `changement-statut-section` (et au `ngOnChanges` quand
`aiData` arrive après le mount) :

1. Si `aiData.dateExpirationTitre` est présent et au format `YYYY-MM-DD` valide.
2. Calcul `mois = floor((dateExpirationTitre - aujourd'hui).jours / 30.44)` —
   plancher à `0` si la date est passée.
3. Si `dureeRestanteSurTitreActuelMois()` est `null` OU si la provenance courante
   est `'IA'`, poser la valeur calculée et marquer `provenanceDureeRestante = 'IA'`.
4. Badge "Pré-rempli depuis l'analyse" (icône `auto_awesome`) affiché à côté
   du champ.
5. Au changement manuel par l'avocat → `provenanceDureeRestante` repassé à `null`,
   badge masqué, valeur préservée.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| `aiData` absent ou null | Pas de pré-fill (silencieux, pas d'erreur console) |
| `dateExpirationTitre` absent / chaîne vide | Pas de pré-fill |
| `dateExpirationTitre` malformée (parsing JS échoue) | Pas de pré-fill, log `console.warn` une fois |
| Date d'expiration dans le passé | Valeur posée à `0` (déclenchera l'alerte « durée < 2 mois » du calculateur backend) |
| Avocat a déjà saisi une valeur (provenance ≠ IA) | Pas d'écrasement |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils décisionnels exposant un champ "durée restante"** : aucun aujourd'hui — la notion de durée restante sur un titre est propre à `changement-statut-section`. Les outils OQTF/AES utilisent d'autres dates (notification, dépôt). **Non applicable**.
- [x] **Pré-fill de date depuis l'analyse IA dans d'autres composants** : `belgian-9bis-section` (`dateDepotProcedure`), `aes-humanitaire-section` (`dateDepotProcedure`), `immigration-recours-section` (`dateNotificationDecisionContestee`). Ces composants pré-remplissent une **date** brute, pas un **calcul date→durée**. Le pattern de calcul introduit ici est nouveau. **Non applicable directement**.
- [x] **Service partagé "calcul mois entre 2 dates"** : aucun util commun aujourd'hui. Plusieurs composants Angular re-implémentent localement leurs calculs de durée. À noter dans le **backlog convergence** mais hors scope (SF cible une seule réutilisation).

### Niveaux de vérification

- [x] **Modèle TypeScript** : `ImmigrationExtractedData.dateExpirationTitre` (string ISO).
- [x] **Service / logique métier** : pure logique frontend, pas d'appel HTTP.
- [x] **Persistance DB** : `changement_statut_analyses.duree_restante_sur_titre_actuel_mois` existe déjà (SF-IM-11-01) — le pré-fill alimente le formulaire avant submit, persistance déjà couverte.
- [x] **Tests existants** : `changement-statut-section.component.spec.ts` couvre `prefillFromAi()` pour `titreActuel` — étendre aux nouveaux cas durée.

### Cas spécifique : modification d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : la durée restante est computée à partir d'une date IA — pas d'opportunité de divergence (la valeur saisie diverge de la valeur IA dès que l'avocat la touche, mais c'est le comportement attendu, pas une alerte). **Non applicable**.
- [x] **Refresh dashboard (F-IA-02)** : pas de submit dans cette SF, pas de cards impactées.
- [x] **Pré-remplissage IA** : c'est précisément l'objet de la SF.
- [x] **Persistance des inputs** : champ déjà persisté en colonne dédiée (SF-IM-11-01). Vérifié.
- [x] **Masquage conditionnel** : champ toujours visible (déjà géré). Pas de changement.
- [x] **Alertes actives après calcul** : le gate du `coherenceAlerts` reste inchangé.

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (1 seule cible : `changement-statut-section.prefillFromAi`)
- [x] Util "calcul mois entre 2 dates" laissé en local (pas de réutilisation immédiate identifiée)
- [x] Backlog convergence : suivre l'apparition de patterns similaires sur 2-3 composants → réévaluer un util `shared/utils/date-duration.ts`

---

## Critères d'acceptation

- [ ] Quand `aiData.dateExpirationTitre = "2026-08-31"` et que la SF est ouverte le 2026-05-01, le champ "Durée restante (mois)" affiche `4` au montage du composant.
- [ ] Le badge "Pré-rempli depuis l'analyse" (icône `auto_awesome`) est visible à côté du champ tant que la valeur n'a pas été modifiée manuellement.
- [ ] Quand l'avocat modifie le champ (ex. saisit `5`), le badge disparaît et la valeur saisie est conservée.
- [ ] Quand `aiData` est `null` ou que `dateExpirationTitre` est absent, le champ reste vide et aucun badge n'est affiché.
- [ ] Quand `dateExpirationTitre` est dans le passé, la valeur posée est `0` (et le calculateur backend remontera l'alerte FAIBLE durée < 2 mois).
- [ ] `aiData` qui arrive après le mount (via `ngOnChanges`) déclenche le pré-fill si `dureeRestanteSurTitreActuelMois()` est encore `null` ou IA.
- [ ] La provenance IA `dureeRestanteSurTitreActuelMois` est strictement effacée au moindre changement manuel (test unitaire).

---

## Périmètre

### Hors scope

- Pas de modification du calculateur backend (la durée saisie reste un input avocat).
- Pas de pré-fill des autres champs du formulaire (titre envisagé, rémunération, casier, justificatif) — ces champs n'ont pas d'origine IA fiable.
- Pas de création d'un util `shared/utils/date-duration.ts` (logique reste locale au composant tant qu'un seul appelant).
- Pas d'ouverture de l'équivalent backlog jumeau BE (F-IM-11 BE est déjà au backlog, indépendant).

---

## Contraintes de validation

| Champ | Obligatoire | Format | Normalisation |
|---|---|---|---|
| `dureeRestanteSurTitreActuelMois` (output) | Oui (déjà imposé par `formValid()`) | Entier ≥ 0 | `Math.max(0, Math.floor(diffDays / 30.44))` |

Notes :
- Le diviseur `30.44` (= 365.25 / 12) est volontaire pour absorber l'irrégularité des mois calendaires. Précision suffisante pour un champ "mois entiers".

---

## Technique

### Endpoints

Aucun appel HTTP introduit par cette SF. Le submit `POST /api/v1/case-files/{id}/changement-statut-analyses` (déjà existant) reçoit la valeur pré-remplie sans changement de contrat.

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Non applicable.

### Composants Angular

- `frontend/src/app/case-files/changement-statut-section/changement-statut-section.component.ts`
  - Ajout `provenanceDureeRestante = signal<'IA' | null>(null)`.
  - Extension `prefillFromAi()` (lignes 277-291) : nouvelle règle date → mois.
  - Modification `onDureeRestanteChange()` : reset `provenanceDureeRestante` à `null`.
- `frontend/src/app/case-files/changement-statut-section/changement-statut-section.component.html`
  - Badge "Pré-rempli depuis l'analyse" à côté du label "Durée restante (mois)".
- `frontend/src/app/case-files/changement-statut-section/changement-statut-section.component.spec.ts`
  - 4 tests unitaires (cf. plan de test).

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `prefillFromAi()` calcule 4 mois quand `dateExpirationTitre = '2026-08-31'` et "today" mocké à `2026-05-01`.
- [ ] `prefillFromAi()` ne touche pas le champ quand `aiData = null`.
- [ ] `prefillFromAi()` ne touche pas le champ quand `aiData.dateExpirationTitre` est `null` / chaîne vide.
- [ ] `prefillFromAi()` pose `0` quand `dateExpirationTitre` est dans le passé.
- [ ] `prefillFromAi()` n'écrase pas une valeur saisie manuellement (provenance `null`).
- [ ] `onDureeRestanteChange()` remet `provenanceDureeRestante` à `null`.
- [ ] `prefillFromAi()` re-pré-remplit quand `aiData` arrive après le mount via `ngOnChanges` (test du flux signal-based).

### Tests d'intégration

Aucun. Pas d'endpoint nouveau ni de logique backend modifiée.

### Isolation workspace

- [x] Non applicable — code frontend pur, l'isolation reste assurée par le contrôleur `POST /api/v1/case-files/{id}/changement-statut-analyses` non modifié.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification locale au composant `changement-statut-section`.

### Composants impactés

Aucun en dehors du composant cible.

### Smoke tests E2E

- [x] Aucun smoke test concerné — l'outil décisionnel n'est pas couvert par `e2e/smoke/`. La couverture reste les tests unitaires Jest.

---

## Impact par domaine métier

Cette SF est **sensible au domaine** : strictement DROIT_IMMIGRATION (le composant est gaté `isFrance()` + visible uniquement sur dossiers Immigration via F-IA-04).

- **Droit du travail** : non applicable (composant non visible).
- **Droit immigration** : c'est la cible.
- **Droit famille** : non applicable.
- **France / Belgique** : F-IM-11 est `single-country FR` (gaté `workspaceCountry === 'FRANCE'`). L'équivalent BE relève d'une feature jumelle au backlog (F-IM-11-BE), hors scope.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IM-11-02` — `done` (frontend changement-statut existe).

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Choix du diviseur 30.44 vs `differenceInCalendarMonths`** : 30.44 = 365.25/12, suffisant pour un champ entier. Pas d'introduction de dépendance `date-fns` dans ce composant (le projet n'en utilise pas systématiquement).
- **Pourquoi pas plancher à 0 silencieux quand date passée ?** : poser `0` permet au calculateur backend de remonter l'alerte FAIBLE explicite — meilleure expérience UX que "champ vide, pourquoi ?".
- **Pourquoi pas pré-remplir d'autres champs ?** : `titreEnvisage` est une décision avocat (intention métier, pas extractible) ; `casierJudiciaireVierge` est un fait à vérifier auprès du client ; `remunerationContratEur` n'est pas exposé par l'IA actuellement.
