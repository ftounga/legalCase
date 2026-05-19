# Mini-spec — [F-246 / SF-246-15] Pré-remplissage IA — Fiches Travail : identités salarié/employeur

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Audit de référence : `docs/features/F-246/SF-246-14-audit-prefill-exhaustif.md` (§9.4 + §10, ligne SF-246-15, vague A).
> **Dette (c)+(d) pure** : helper + composant uniquement. Les champs identités sont déjà présents dans
> `TravailExtractedData` côté backend — **aucune extension backend n'est nécessaire**.

---

## Identifiant

`F-246 / SF-246-15`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-15-fiches-travail-identites`

---

## Objectif

Brancher les champs identités salarié/employeur déjà présents dans `TravailExtractedData`
(`nomSalarie`, `prenomSalarie`, `adresseSalarie`, `nomEmployeur`, `adresseEmployeur`,
`siretEmployeur`, `bceEmployeur`) sur les helpers `*-prefill-rules.ts` et la méthode
`prefillFromAi()` des fiches `prudhome-fiche` (FR) et `tribunal-travail-fiche` (BE),
qui n'en lisent actuellement que `poste`/`conventionCollective`.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier de droit du travail après analyse IA.
2. L'analyse a extrait dans `TravailExtractedData` : `nomSalarie`, `prenomSalarie`,
   `adresseSalarie`, `nomEmployeur`, `adresseEmployeur`, `siretEmployeur` (FR) ou
   `bceEmployeur` (BE).
3. À l'ouverture de la **fiche prud'homale FR** (`prudhome-fiche-section`), `prefillFromAi()`
   pré-remplit :
   - `demandeur.nom` ← `nomSalarie`
   - `demandeur.prenom` ← `prenomSalarie`
   - `demandeur.adresse` ← `adresseSalarie`
   - `defendeur.nom` ← `nomEmployeur`
   - `defendeur.adresse` ← `adresseEmployeur`
   - `defendeur.siret` ← `siretEmployeur`
   (en plus du champ existant `demandeur.profession` ← `poste`)
4. À l'ouverture de la **requête tribunal du travail BE** (`tribunal-travail-fiche-section`),
   `prefillFromAi()` pré-remplit :
   - `requerant.nom` ← `nomSalarie`
   - `requerant.prenom` ← `prenomSalarie`
   - `requerant.domicile` ← `adresseSalarie`
   - `defendeur.nom` ← `nomEmployeur`
   - `defendeur.siegeSocial` ← `adresseEmployeur`
   - `defendeur.numeroBce` ← `bceEmployeur`
   (en plus des 5 champs existants)
5. Chaque champ pré-rempli affiche un badge `auto_awesome`.
6. La modification manuelle de n'importe quel champ remet `provenance<Field>` à `null`
   et fait disparaître le badge.
7. Le badge « Pré-rempli par l'IA (N) » du panel F-IA-04 reflète le nouveau `getPrefillCount()`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `nomSalarie` absent ou vide dans `TravailExtractedData` | Champ `demandeur.nom` / `requerant.nom` reste vide — no-op gracieux | n/a |
| `siretEmployeur` présent sur un dossier BE | Champ ignoré (`tribunal-travail-fiche` consomme `bceEmployeur` uniquement) | n/a |
| `bceEmployeur` présent sur un dossier FR | Champ ignoré (`prudhome-fiche` consomme `siretEmployeur` uniquement) | n/a |
| La fiche persistée arrive après le pré-fill | `patchForm()` écrase les valeurs IA (existant — comportement inchangé) | n/a |
| `aiData` arrive après le premier rendu | `prefillFromAi()` réinvoqué dans `ngOnChanges()` si `!hasPersistedFiche()` (existant) | n/a |
| Champ déjà saisi manuellement | Condition `!ctrl.value` préserve la saisie (comportement inchangé) | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : `prudhome-fiche` (FR) et `tribunal-travail-fiche` (BE) sont les deux seuls
  outils « fiche de procédure » du domaine Travail avec des champs nom/adresse saisissables. Aucun autre outil
  Travail n'expose ces mêmes champs identités dans son formulaire.
- [x] **Autres pays** : `prudhome-fiche` = FR uniquement (`siretEmployeur`) ; `tribunal-travail-fiche` = BE
  uniquement (`bceEmployeur`). Séparation correcte.
- [x] **Autres domaines** : non applicable — les champs identités de `TravailExtractedData` sont propres au
  domaine Travail.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badges `auto_awesome`, alertes F-IA-03
  (extension pour les nouveaux champs).
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `TravailExtractedData` dans `case-analysis.model.ts` — ajout des
  7 champs identités **au DTO frontend** (ils existent en backend mais sont absents de l'interface TS).
- [x] **Record / DTO backend** : `CaseAnalysisResponse.TravailExtractedData` — champs déjà présents, **aucune
  modification backend**.
- [x] **Service / logique métier** : `extractTravailData()` — vérifier que les 7 champs sont déjà parsés
  (probable, mais à confirmer). Si le parsing est manquant, l'ajouter.
- [x] **Entité JPA + schéma DB** : non applicable — champs JSON de la synthèse.
- [x] **Tests existants** : `prudhome-fiche-section-prefill-rules.spec.ts` + `tribunal-travail-fiche-section-prefill-rules.spec.ts` — mis à jour pour couvrir les nouveaux champs.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : alertes étendues aux nouveaux champs.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà câblé.
- [x] **Pré-remplissage IA** : objet de la SF.
- [x] **Persistance des inputs** : inchangée — endpoints F-DT-04/F-DT-06 existants.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `prudhome-fiche-section` | Oui | Intégré dans cette SF |
| `tribunal-travail-fiche-section` | Oui | Intégré dans cette SF |
| Autres outils Travail FR/BE | Non | Aucun n'a de champs nom/adresse saisissables |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

### 1. Cohérence visuelle

- [x] **Palette statut** : navy/or/vert — conservé.
- [x] **Typographie** : `Inter` pour les champs identités — conservé.
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: TravailExtractedData | null` — déjà présent sur les 2 composants.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()` — déjà en place.
- [x] Signaux `provenance<Field>` : ajout de `provenanceNomSalarie`, `provenancePrenomSalarie`,
  `provenanceAdresseSalarie`, `provenanceNomEmployeur`, `provenanceAdresseEmployeur`,
  `provenanceSiretEmployeur` (FR) / `provenanceBceEmployeur` (BE).
- [x] Badge `auto_awesome` par champ pré-rempli.
- [x] Handler `onXxxChange()` par champ remettant `provenance<Field>` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` étendu pour les nouveaux champs (`NOM_SALARIE`, `NOM_EMPLOYEUR`,
  `SIRET_EMPLOYEUR` / `BCE_EMPLOYEUR`).
- [x] Helper `CoherenceAlertBuilder` réutilisé.

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Entrées `prudhome-fiche` et `tribunal-travail-fiche` déjà présentes dans `TOOL_REGISTRY`.
- [x] Static `getPrefillCount(input)` recalculé pour inclure les nouveaux champs.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()`.
- [x] Tests Jest : cas (a) 0 champ, (b) partiel (seulement nom/prenom), (c) nominal (tous les champs).

---

## Champs IA à extraire (pré-remplissage)

> Les champs sources existent déjà dans le record backend `TravailExtractedData`. Seule la couche
> frontend (DTO TypeScript + helper + composant) est à mettre à jour.

| Champ du formulaire | Outil consommateur | Type | Champ source `TravailExtractedData` | Extension requise |
|---------------------|-------------------|------|--------------------------------------|-------------------|
| `demandeur.nom` | `prudhome-fiche` | string | `nomSalarie` | DTO frontend seulement |
| `demandeur.prenom` | `prudhome-fiche` | string | `prenomSalarie` | DTO frontend seulement |
| `demandeur.adresse` | `prudhome-fiche` | string | `adresseSalarie` | DTO frontend seulement |
| `defendeur.nom` | `prudhome-fiche` | string | `nomEmployeur` | DTO frontend seulement |
| `defendeur.adresse` | `prudhome-fiche` | string | `adresseEmployeur` | DTO frontend seulement |
| `defendeur.siret` | `prudhome-fiche` | string | `siretEmployeur` (FR uniquement) | DTO frontend seulement |
| `requerant.nom` | `tribunal-travail-fiche` | string | `nomSalarie` | DTO frontend seulement |
| `requerant.prenom` | `tribunal-travail-fiche` | string | `prenomSalarie` | DTO frontend seulement |
| `requerant.domicile` | `tribunal-travail-fiche` | string | `adresseSalarie` | DTO frontend seulement |
| `defendeur.nom` | `tribunal-travail-fiche` | string | `nomEmployeur` | DTO frontend seulement |
| `defendeur.siegeSocial` | `tribunal-travail-fiche` | string | `adresseEmployeur` | DTO frontend seulement |
| `defendeur.numeroBce` | `tribunal-travail-fiche` | string | `bceEmployeur` (BE uniquement) | DTO frontend seulement |

- [x] **Aucune extension backend requise** — les champs existent dans `TravailExtractedData`
  (record + extracteur), la dette est purement frontend : DTO TypeScript non déclaré + helpers
  ne lisant pas ces champs.

---

## Critères d'acceptation

- [ ] L'interface TypeScript `TravailExtractedData` dans `case-analysis.model.ts` déclare les 7
  champs identités : `nomSalarie`, `prenomSalarie`, `adresseSalarie`, `nomEmployeur`,
  `adresseEmployeur`, `siretEmployeur`, `bceEmployeur` (tous `string | null`).
- [ ] Le helper `PrudhomeFicheSectionPrefillRules` expose `computeNomSalarie`,
  `computePrenomSalarie`, `computeAdresseSalarie`, `computeNomEmployeur`,
  `computeAdresseEmployeur`, `computeSiretEmployeur`, et `computePrefillCount()` retourne le
  nombre exact de champs non nuls.
- [ ] Le helper `TribunalTravailFicheSectionPrefillRules` expose les fonctions correspondantes pour
  les champs BE (`bceEmployeur` remplace `siretEmployeur`), et `computePrefillCount()` retourne le
  nombre exact.
- [ ] `PrudhomeFicheSectionComponent.prefillFromAi()` pré-remplit les 6 champs identités + le champ
  `profession` existant (7 total) depuis `TravailExtractedData`.
- [ ] `TribunalTravailFicheSectionComponent.prefillFromAi()` pré-remplit les 6 champs identités + les 5
  champs existants (11 total).
- [ ] Chaque champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet
  `provenance<Field>` à `null`.
- [ ] `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte sur les 2 composants.
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si `nomSalarie` ou `nomEmployeur` saisi diverge de
  la détection IA.
- [ ] Tests Jest : cas (a) `aiData` vide → 0 ; (b) seulement nom/prenom → 2 ; (c) tous les champs →
  comptage attendu.
- [ ] `extractTravailData()` parse bien les 7 champs identités (vérification — si absent, ajout).

---

## Périmètre

### Hors scope (explicite)

- Toute modification du backend (record `TravailExtractedData`, prompt, extracteur) — les champs
  existent déjà.
- Tout autre outil Travail FR/BE.
- Modification du formulaire HTML (ajout de nouveaux champs `<input>`) — les champs existent déjà
  dans les `FormGroup` des deux composants (ils sont juste non pré-remplis).
- Migrations Liquibase.

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées | Normalisation |
|-------|-------------|----------------------------|---------------|
| `nomSalarie`, `prenomSalarie`, `nomEmployeur` | Non | string non vide | `nonEmpty()` existant |
| `adresseSalarie`, `adresseEmployeur` | Non | string non vide | `nonEmpty()` existant |
| `siretEmployeur` | Non | string 14 chiffres (FR) — pas de validation de format côté prefill | `nonEmpty()` |
| `bceEmployeur` | Non | string (BE) | `nonEmpty()` |

---

## Technique

### Endpoint(s)

Aucun endpoint nouveau ou modifié. Fiches persistées via endpoints existants F-DT-04 / F-DT-06.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable.

### Composants Angular

- `PrudhomeFicheSectionComponent` — `prefillFromAi()` étendu + signaux + alertes.
- `TribunalTravailFicheSectionComponent` — idem.
- `prudhome-fiche-section-prefill-rules.ts` — lecture des 7 champs sources.
- `tribunal-travail-fiche-section-prefill-rules.ts` — idem (avec `bceEmployeur`).
- `case-analysis.model.ts` — ajout des 7 champs à `TravailExtractedData`.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `PrudhomeFichePrefillRules` :
  - `computePrefillCount()` cas 0 champ (aiData vide) → 0.
  - `computePrefillCount()` cas partiel (nomSalarie seulement) → 2 (profession + nom).
  - `computePrefillCount()` cas nominal (tous les champs) → 7.
  - `computeNomSalarie()` / `computeSiretEmployeur()` : string non vide → valeur ; vide → null.
- [ ] `TribunalTravailFichePrefillRules` :
  - idem avec `bceEmployeur`.
  - `computePrefillCount()` nominal → 11 champs.
- [ ] `PrudhomeFicheSectionComponent` :
  - `prefillFromAi()` : 7 champs renseignés, 7 badges IA.
  - `onNomSalarieChange()` : `provenanceNomSalarie` → null.
  - `coherenceAlerts` : alerte levée si `demandeur.nom` diverge de `nomSalarie`.
- [ ] `TribunalTravailFicheSectionComponent` : idem.

### Tests d'intégration backend

Aucun nouveau test — les champs existent déjà dans `extractTravailData()`. Vérification
que `nom_salarie` est bien parsé dans les tests existants de `CaseAnalysisResponseTest`.

### Isolation workspace

- [x] Non-régression sur les endpoints existants F-DT-04 / F-DT-06 (403 si workspace différent).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Outil décisionnel métier** — composants impactés :
  - `PrudhomeFicheSectionComponent`
  - `TribunalTravailFicheSectionComponent`
  - leurs helpers `*-prefill-rules.ts`

### Smoke tests E2E

- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).
- [x] ~27 échecs E2E préexistants connus — ne corriger que les régressions de cette SF.

---

## Dépendances

### Subfeatures bloquantes

Aucune. SF-246-15 ne touche pas les fichiers partagés des SF Famille (record `FamilleExtractedData`,
prompt `FAMILLE_INSTRUCTION`). Elle est indépendante et peut démarrer sur master à jour.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Vérification préalable de `extractTravailData()`

L'audit §9.4 indique que les champs identités sont présents dans le record Java mais pas dans le
DTO TypeScript frontend — cela suggère que `extractTravailData()` les parse déjà. La SF doit
confirmer ce point avant de démarrer la partie backend (lecture de `CaseAnalysisResponse.java`).
Si le parsing est absent, l'ajouter est dans le périmètre.

### `siretEmployeur` vs `bceEmployeur`

Le record backend contient les deux ; la fiche FR consomme `siretEmployeur`, la fiche BE consomme
`bceEmployeur`. Aucune logique de validation de format n'est ajoutée — le pré-fill est passif
(propose, ne valide pas).
