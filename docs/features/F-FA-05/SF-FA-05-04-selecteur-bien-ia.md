# Mini-spec — F-FA-05 / SF-FA-05-04 Sélecteur IA pour importer un bien immobilier

## Identifiant

`F-FA-05 / SF-FA-05-04`

## Feature parente

`F-FA-05` — Simulateur partage des biens immobiliers

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-FA-05-04-selecteur-bien-ia`

---

## Objectif

Ajouter à F-FA-05 un sélecteur "Choisir un bien détecté par l'IA" qui liste les biens immobiliers extraits de `liquidationCommunaute.actifCommun`. Quand l'avocat sélectionne un bien, la `valeurVenale` est pré-remplie automatiquement. Un second sélecteur optionnel pré-remplit `capitalRestantDu` depuis `passifCommun`. Préalable technique à SF-IA-03-08.

---

## Comportement attendu

### Cas nominal

1. Le composant reçoit `@Input() liquidationCommunaute: LiquidationCommunaute | null`.
2. Si la liste `actifCommun` contient ≥ 1 bien avec une valeur numérique, un bouton "Importer depuis l'analyse IA" apparaît en haut du formulaire.
3. Clic → ouvre un panneau avec :
   - Liste déroulante "Bien immobilier" avec chaque `actifCommun` item (libellé + valeur)
   - Liste déroulante "Prêt associé" avec chaque `passifCommun` item + option "Aucun prêt"
4. Bouton "Appliquer" remplit `valeurVenale` et `capitalRestantDu` depuis les sélections.
5. Une note de provenance s'affiche sous les champs importés : "Valeur importée depuis l'analyse IA".
6. L'avocat peut ensuite modifier librement les valeurs. Toute modification efface la note.

### Filtrage "bien immobilier"

Pour réduire le bruit, on ne propose dans `actifCommun` que les items dont le libellé contient un mot-clé immobilier : `immobilier`, `maison`, `appartement`, `résidence`, `villa`, `studio`, `terrain`, `logement`, `bien immobilier` (insensible à la casse, racine). Les autres biens (comptes, véhicules, mobilier) sont filtrés côté front.

Si après filtrage la liste est vide, le bouton "Importer depuis l'analyse IA" est désactivé avec tooltip "Aucun bien immobilier détecté dans l'analyse".

### Filtrage "prêt"

Symétrique pour `passifCommun` : mots-clés `prêt`, `emprunt`, `crédit`, `hypothèque`, `hypothécaire`. Si vide → option "Aucun prêt" seule disponible.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| `liquidationCommunaute` null | bouton "Importer" non visible |
| `actifCommun` vide ou sans item immobilier | bouton désactivé, tooltip explicatif |
| `valeur` du bien IA = null | bien listé avec libellé "(valeur indéterminée)", non cliquable |
| Résultat F-FA-05 déjà sauvegardé | bouton non visible (on modifie depuis edit) |
| L'avocat modifie valeurVenale après import | note de provenance effacée |

---

## Critères d'acceptation

- [ ] Nouveau `@Input() liquidationCommunaute: LiquidationCommunaute | null` sur `PartageImmobilierSectionComponent`.
- [ ] Bouton "Importer depuis l'analyse IA" visible quand au moins un bien immobilier est détecté.
- [ ] Liste déroulante bien + liste déroulante prêt (+ option "Aucun prêt"), application par clic sur "Appliquer".
- [ ] `valeurVenale` et `capitalRestantDu` mis à jour depuis la sélection.
- [ ] Note de provenance "Valeur importée depuis l'analyse IA" sous chaque champ importé.
- [ ] Note effacée quand l'avocat modifie manuellement le champ.
- [ ] Filtrage par mots-clés côté front (9+ mots-clés immo, 5+ mots-clés prêt).
- [ ] Items IA sans valeur numérique listés mais non sélectionnables.
- [ ] Rétrocompat : si `liquidationCommunaute` null, comportement actuel strictement préservé.
- [ ] Tests unitaires frontend (filtrage, sélection, modification manuelle, cas vides).

---

## Périmètre

### Hors scope (explicite)

- Extraction IA des quote-parts (peu de signal dans les documents typiques).
- Sélection multi-biens (un seul bien par F-FA-05).
- Enrichissement prompt IA pour taguer explicitement les biens immobiliers : on utilise le filtrage par mot-clé côté front comme first pass. Peut être amélioré plus tard si faux négatifs.
- Backend modifié : aucune. `liquidationCommunaute` est déjà exposé par F-IA-01.
- Cohérence IA (alerte si user diverge après import) → SF-IA-03-08 suivante.

---

## Valeurs initiales

Aucune entité créée. Valeurs de provenance stockées dans un signal transient `provenanceNotes`.

---

## Contraintes de validation

| Champ | Obligatoire | Format | Normalisation |
|-------|-------------|--------|---------------|
| Libellé bien pour filtrage | Non | texte libre IA | lower-case pour comparaison mot-clé |
| Valeur bien pour sélection | Oui (sinon non cliquable) | nombre > 0 | — |

---

## Technique

### Endpoint(s)

Aucun. Les données `liquidationCommunaute` sont déjà exposées par `GET /case-analysis` (F-IA-01).

### Tables impactées

Aucune.

### Migration Liquibase

- [x] **Non applicable**.

### Composants Angular

- `PartageImmobilierSectionComponent` :
  - `@Input() liquidationCommunaute: LiquidationCommunaute | null`
  - Signal miroir `liquidationSignal`
  - Computed `biensImmobiliersFiltres: BienItem[]` (filtrage mots-clés)
  - Computed `pretsFiltres: BienItem[]`
  - Signal `showImportPanel: boolean`, `selectedBien?: BienItem`, `selectedPret?: BienItem | 'NONE'`
  - Signal `provenanceValeur: 'IA' | null`, `provenancePret: 'IA' | null`
  - Méthodes `openImportPanel()`, `applyImport()`, `onValeurChange()`, `onCapitalChange()`
  - Template : bouton importer + panneau replié + note de provenance
- Aucun changement backend.

---

## Plan de test

### Tests unitaires frontend

Filtrage :
- [ ] `actifCommun` avec 3 items ("Maison principale", "Compte épargne", "Véhicule") → seul "Maison principale" proposé.
- [ ] `actifCommun` avec "appartement Paris" et "APPARTEMENT Lyon" → les 2 proposés (insensible à la casse).
- [ ] `actifCommun` vide → bouton désactivé.
- [ ] `passifCommun` "Prêt BNP" et "Carte bleue" → seul "Prêt BNP" proposé.

Import :
- [ ] Sélection bien + prêt + Appliquer → `valeurVenale` et `capitalRestantDu` mis à jour, notes de provenance visibles.
- [ ] Sélection bien + "Aucun prêt" + Appliquer → `capitalRestantDu` inchangé, pas de note sur ce champ.
- [ ] Sélection bien avec valeur null → non cliquable (pas d'import possible).

État :
- [ ] Modification manuelle de `valeurVenale` après import → note effacée.
- [ ] `liquidationCommunaute` null → bouton non visible.
- [ ] Résultat F-FA-05 déjà chargé → bouton masqué.

### Isolation workspace

- [x] Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune**.

### Composants impactés

| Composant | Impact |
|---|---|
| `PartageImmobilierSectionComponent` | Extension flux pre-fill, tests existants conservés |
| `CaseFileDetailComponent` | passe le nouveau input |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `F-IA-01` (Done) — expose `liquidationCommunaute.actifCommun` et `.passifCommun`.

### Questions ouvertes

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi filtrage par mot-clé côté front plutôt qu'un tag IA dédié** : l'IA extrait déjà la liste sans typage. Ajouter un tag "type_bien" au prompt alourdirait pour un gain marginal (les libellés contiennent presque toujours le mot "appartement", "maison", etc.). Si retour terrain montre trop de faux négatifs, on ajoutera un tag plus tard.
- **Pourquoi ne pas extraire les quote-parts** : les documents de divorce ne renseignent presque jamais les quote-parts précises (présumées 50/50 sauf clause expresse). L'avocat saisit quasi toujours 50 %. Surveiller ce champ ajouterait du bruit.
- **Pattern cohérent avec SF-DT-09-04 et SF-FA-06-04** : on enrichit l'outil avant d'y greffer la cohérence. Deux subfeatures enchaînées.
