# Mini-spec — F-DT-09 / SF-DT-09-04 Type de rupture avec calcul différencié

## Identifiant

`F-DT-09 / SF-DT-09-04`

## Feature parente

`F-DT-09` — Comparateur d'indemnités

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-DT-09-04-type-rupture`

---

## Objectif

Ajouter un sélecteur "Type de rupture" à F-DT-09, pré-rempli depuis l'extraction IA (`compensation_data.type_rupture`), et adapter le calcul et l'affichage selon 4 cas : licenciement ordinaire (Macron), licenciement économique (Macron + contexte), rupture conventionnelle (indemnité légale de licenciement à la place du Macron), rupture amiable belge (pas de fourchette, négociation libre).

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre F-DT-09 sur un dossier analysé. Le select "Type de rupture" est pré-sélectionné avec la valeur extraite par l'IA (`compensationEstimate.typeRupture`). S'il n'y a pas d'extraction, valeur par défaut : `LICENCIEMENT` (FR) ou `LICENCIEMENT_ORDINAIRE` (BE).
2. L'avocat saisit (ou laisse les valeurs pré-remplies) : ancienneté, âge, salaire. Clique Calculer.
3. Le backend applique la logique de calcul adaptée au type de rupture et renvoie une réponse enrichie avec `displayMode` et `contextualMessages`.
4. Le frontend affiche le résultat selon `displayMode`.

### Matrice de traitement

| Pays | Type de rupture | `displayMode` | Calcul | Message contextuel |
|---|---|---|---|---|
| FR | `LICENCIEMENT` | `MACRON` | Fourchette Macron (comportement actuel) | "Fourchette représentant les dommages-intérêts potentiels en cas de licenciement abusif (barème Macron, art. L1235-3)." |
| FR | `LICENCIEMENT_ECONOMIQUE` | `MACRON` | Fourchette Macron (idem) | "Vérifier indemnité conventionnelle (souvent plus favorable), obligations PSE et priorité de réembauche." |
| FR | `RUPTURE_CONVENTIONNELLE` | `INDEMNITE_SPECIFIQUE` | Indemnité légale de licenciement = `(¼ × min(10, ancienneté) + ⅓ × max(0, ancienneté - 10)) × salaireRef` | "L'indemnité spécifique de rupture conventionnelle doit être au moins égale à l'indemnité légale de licenciement (art. L1237-13). Vérifier l'indemnité conventionnelle si plus favorable." |
| BE | `LICENCIEMENT_ORDINAIRE` | `CCT_109` | Fourchette CCT 109 (comportement actuel belge) | "Fourchette représentant l'indemnité pour licenciement manifestement déraisonnable (CCT 109, 3 à 17 semaines)." |
| BE | `RUPTURE_AMIABLE` | `NEGOCIATION_LIBRE` | Pas de calcul — valeurs à zéro | "Négociation libre entre les parties, aucun barème ne s'impose. L'employé conserve le droit à l'indemnité compensatoire de préavis (cf. F-DT-05)." |

### Formule de l'indemnité légale de licenciement (FR)

```
baseMensuelle = salaireRef
anneeComplete = floor(anciennete_annees + anciennete_mois/12)
partPremieres10ans = min(10, anneeComplete) × 0.25 × baseMensuelle
partAudelas10ans   = max(0, anneeComplete - 10) × (1/3) × baseMensuelle
indemniteLegale = partPremieres10ans + partAudelas10ans
```

Pas d'indemnité si `anneeComplete < 1` (l'ancienneté minimale d'ouverture du droit est de 8 mois continus ; pour simplifier et rester prudent, on renvoie 0 sous 1 an).

### Pré-remplissage

- Si `compensationEstimate.typeRupture` ∈ enum attendu du pays workspace → sélectionné automatiquement.
- Si workspace FR et valeur IA = `DEMISSION`, `PRISE_ACTE`, etc. → pas de mapping, valeur par défaut `LICENCIEMENT`, note discrète "IA a détecté un autre type : <X>. Vérifier que cet outil est adapté."
- Si workspace BE et valeur IA = `RUPTURE_CONVENTIONNELLE` (inapplicable) → valeur par défaut `LICENCIEMENT_ORDINAIRE`, note.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `typeRupture` absent du POST | 400 "typeRupture required" |
| `typeRupture` hors enum du pays | 400 "typeRupture not allowed for country" |
| Ancienneté, âge, salaire négatifs ou nuls | 400 existants inchangés |
| Résultat sauvegardé sans `type_rupture` (legacy) | GET renvoie `typeRupture = null`, le frontend applique la valeur par défaut et affiche le résultat comme avant |

---

## Critères d'acceptation

- [ ] Migration `070-add-type-rupture-to-indemnite-comparatifs.xml` ajoute `type_rupture VARCHAR(50) NULL`. L'`indemniteLegaleMontant` reste dans `result_data` JSON (pas de colonne dédiée — simple cohérence avec le stockage actuel).
- [ ] `IndemniteComparatifRequest` accepte `typeRupture` (obligatoire à partir de cette version).
- [ ] Service branche correctement selon les 5 combinaisons pays × type.
- [ ] Formule indemnité légale implémentée et testée (cas < 1 an = 0, 10 ans pile, 15 ans, arrondi).
- [ ] `IndemniteComparatifResponse` renvoie `typeRupture`, `displayMode ∈ {MACRON, CCT_109, INDEMNITE_SPECIFIQUE, NEGOCIATION_LIBRE}`, `indemniteLegaleMontant?` (si RUPTURE_CONVENTIONNELLE), `contextualMessages: List<String>`.
- [ ] Frontend : select pays-sensible, pré-remplissage depuis `compensationEstimate.typeRupture`, note visible si mismatch IA-enum.
- [ ] Frontend : affichage conditionnel selon `displayMode` (graphique Macron, panel indemnité spécifique, panel négociation libre).
- [ ] Rétrocompat : un result legacy sans `type_rupture` s'affiche encore.
- [ ] Tests backend (service + formule + endpoint 4×2 cas + validation).
- [ ] Tests frontend (select, prefill, affichage conditionnel, types d'erreur).
- [ ] Isolation workspace préservée.

---

## Périmètre

### Hors scope (explicite)

- Calcul de l'indemnité conventionnelle collective (dépend de la convention, couvert par un message contextuel seulement).
- Calcul PSE, priorité de réembauche, plan de reclassement (procédure, pas chiffrage).
- Calcul d'une indemnité « amiable » belge (négociation libre, pas de barème).
- Vice du consentement / contestation de la rupture conventionnelle (hors outil).
- La cohérence IA sur ce champ (→ SF-IA-03-05 suivante).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `indemnite_comparatifs.type_rupture` | `null` | rempli par POST ; si absent à la lecture, frontend applique défaut selon pays |
| `indemnite_comparatifs.indemnite_legale_montant` | `null` | rempli par le service quand RUPTURE_CONVENTIONNELLE, sinon null |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Normalisation |
|-------|-------------|-------------|----------------------------|---------------|
| `typeRupture` (POST) | Oui | 50 | FR : `LICENCIEMENT`, `LICENCIEMENT_ECONOMIQUE`, `RUPTURE_CONVENTIONNELLE`. BE : `LICENCIEMENT_ORDINAIRE`, `RUPTURE_AMIABLE`. | upper-case |
| `indemnite_legale_montant` | Non | — | décimal ≥ 0 | arrondi 2 décimales |

---

## Technique

### Endpoint(s)

| Méthode | URL | Changement |
|---------|-----|------------|
| POST | `/api/v1/case-files/{id}/indemnite-comparatif` | ajoute `typeRupture` obligatoire + nouveaux champs en réponse |
| GET | idem | expose les nouveaux champs (null si legacy) |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `indemnite_comparatifs` | ALTER — 2 colonnes nullable | migration 070 |

### Migration Liquibase

- [x] Oui — `070-add-type-rupture-to-indemnite-comparatifs.xml`

Réversible : DROP COLUMN sur colonnes nullables non backfillées.

### Composants Angular

- `IndemniteComparatifSectionComponent` :
  - signaux `typeRupture`, computed `typeRuptureOptions` pays-sensible
  - `ngOnChanges` sur `aiData`/synthesis : pré-remplit `typeRupture` si disponible
  - nouveau rendu conditionnel selon `result.displayMode`
- `IndemniteComparatifResponse` model : ajout `typeRupture`, `displayMode`, `indemniteLegaleMontant?`, `contextualMessages`

---

## Plan de test

### Tests unitaires backend

- [ ] Formule indemnité légale : 0 an → 0, 1 an → 0,25 × salaire, 10 ans → 2,5 × salaire, 15 ans → 2,5 + (5/3) × salaire.
- [ ] POST FR + LICENCIEMENT → `displayMode = MACRON`, `indemniteLegaleMontant = null`.
- [ ] POST FR + LICENCIEMENT_ECONOMIQUE → `displayMode = MACRON`, message contextuel conventionnelle.
- [ ] POST FR + RUPTURE_CONVENTIONNELLE → `displayMode = INDEMNITE_SPECIFIQUE`, `indemniteLegaleMontant` calculé.
- [ ] POST BE + LICENCIEMENT_ORDINAIRE → `displayMode = CCT_109`.
- [ ] POST BE + RUPTURE_AMIABLE → `displayMode = NEGOCIATION_LIBRE`, aucun montant.
- [ ] POST FR + type belge (`LICENCIEMENT_ORDINAIRE`) → 400.
- [ ] POST BE + type FR (`RUPTURE_CONVENTIONNELLE`) → 400.
- [ ] POST sans `typeRupture` → 400.
- [ ] GET legacy (type_rupture null en base) → renvoie null, pas d'erreur.

### Tests d'intégration

- [ ] E2E POST → GET round-trip persiste `type_rupture` et `indemnite_legale_montant`.
- [ ] Isolation workspace : un workspace ne voit pas le résultat d'un autre.

### Tests unitaires frontend

- [ ] Select affiche 3 options si pays FR, 2 si BE.
- [ ] Pré-remplissage `typeRupture` depuis `compensationEstimate.typeRupture`.
- [ ] `compensationEstimate.typeRupture = DEMISSION` → default `LICENCIEMENT` + note.
- [ ] `displayMode = MACRON` affiche la fourchette + barres.
- [ ] `displayMode = INDEMNITE_SPECIFIQUE` affiche `indemniteLegaleMontant` et masque la fourchette.
- [ ] `displayMode = NEGOCIATION_LIBRE` affiche uniquement le message, aucun chiffre.
- [ ] Legacy result (typeRupture = null) : affichage comportement actuel, pas de crash.

### Isolation workspace

- [x] Applicable — vérifiée par filtre existant sur `indemnite_comparatifs`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — extension localisée d'un seul outil métier.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression |
|----------------------|-----------------|------------------------|
| `IndemniteComparatifService.calculate()` | refactor en switch sur `typeRupture` — doit rester compatible sur cas LICENCIEMENT/LICENCIEMENT_ORDINAIRE | tests existants conservés |
| `IndemniteComparatifResponse` | constructeur record rallongé, 4 nouveaux champs | vérifier toutes les invocations et tests |
| `IndemniteComparatifSectionComponent` | branches display mode — nouvelle complexité mais rétrocompat sur result legacy | test legacy |

### Smoke tests E2E concernés

- [ ] Aucun smoke test critique — l'outil F-DT-09 n'est pas dans les chemins E2E actuels.

---

## Dépendances

### Subfeatures bloquantes

- `F-IA-01` (Done) — expose `compensationEstimate.typeRupture` pour le pré-remplissage.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi intégrer l'indemnité légale de licenciement** : en rupture conventionnelle, c'est le vrai chiffre utile pour l'avocat (l'indemnité spécifique doit être au moins égale). Sans ce calcul, l'outil est muet dans ce cas et oblige l'avocat à faire le calcul à la main.
- **Pourquoi ne pas calculer l'indemnité conventionnelle** : elle dépend de la convention collective et des accords d'entreprise. Hors scope de ce comparateur simple — juste un message contextuel qui renvoie vers l'étude manuelle.
- **Pourquoi enum distinct FR/BE** : la rupture conventionnelle française et la rupture amiable belge ne sont pas interchangeables (cadre juridique différent). Valider côté backend évite de confondre les deux.
- **Pourquoi garder un `typeRupture` dans le POST même si déjà dans `compensationEstimate`** : l'avocat peut corriger la valeur IA. La source de vérité du calcul doit être ce que l'avocat a coché, pas ce que l'IA a extrait.
- **Rétrocompat** : anciens results sans `type_rupture` renvoient null — le frontend a un fallback affichant le rendu historique. Pas de backfill.
