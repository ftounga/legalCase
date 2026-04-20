# Mini-spec — F-132 / SF-132-03 Refonte Belgique — outil "Rupture amiable" informationnel

## Identifiant
`F-132 / SF-132-03`

## Feature parente
`F-132` — Refonte F-DT-09 en outils décisionnels dédiés

## Statut `draft`  · Date `2026-04-20`  · Branche `feat/SF-132-03-rupture-amiable-info-belgique`

---

## Objectif

Terminer la refonte F-132 côté Belgique en extrayant la situation `RUPTURE_AMIABLE` (qui n'a **aucun barème** — négociation libre entre les parties) de `IndemniteComparatifCalculator`. Après cette SF, l'ancien calc ne gère plus que les situations à barème chiffré (Macron FR + CCT 109 BE). Le cas "rupture amiable BE" devient un **composant frontend purement informationnel** (pas de calcul, pas de backend) affiché conditionnellement, qui rappelle à l'avocat le cadre juridique applicable.

**Décision d'architecture** : pas d'entity / endpoint / migration pour la rupture amiable. L'outil n'a rien à persister ni calculer — un panneau statique suffit. Cela évite la dette de "tool vide par design" et garde l'invariant "un outil = une situation métier" satisfait au sens fonctionnel.

---

## Comportement attendu

### Cas nominal

Sur un dossier `DROIT_DU_TRAVAIL` + `workspaceCountry == BELGIQUE` + `synthesis.compensationEstimate.typeRupture == RUPTURE_AMIABLE` :
- `app-indemnite-comparatif-section` est **masquée**
- `app-rupture-amiable-info-section` est **affichée** : panneau collapsible avec titre "RUPTURE AMIABLE — CADRE JURIDIQUE", 2 messages :
  1. "Aucun barème légal ne s'impose en rupture amiable belge. Le montant est librement négocié entre les parties."
  2. "Le salarié conserve le droit à l'indemnité compensatoire de préavis si la rupture n'est pas effective (cf. F-DT-05)."
- Un lien/bouton "Voir la checklist préavis (F-DT-05)" peut pointer vers la section existante si elle est présente — hors scope pour SF-132-03, juste un message texte

Sur un dossier BE `LICENCIEMENT_ORDINAIRE` : F-DT-09 reste affiché (CCT 109 avec barème chiffré) — comportement inchangé.

Card dashboard "Indemnités estimées" sur dossier BE rupture amiable : **masquée** (pas de montant estimable). Alternative : afficher un badge textuel "Négociation libre" avec un label pays/type. Décision dans cette SF : **masquer la card** — plus cohérent que de laisser un "0 — 0 €" trompeur.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Backend reçoit `RUPTURE_AMIABLE` pour BE après SF-132-03 | `IllegalArgumentException` — le type ne fait plus partie de `TYPES_RUPTURE_BE` |

---

## Analyse de cohérence transversale

### Périmètres scannés

| Cible | Applicable ? | Traitement |
|---|---|---|
| SF-132-01/02 (FR rupture conv) | Oui — même pattern | Référence directe, choix différent (pas de backend ici) — justifié par l'absence de calcul |
| `RecoursGenerator` F-IM-06 | Oui | **F-133** (feature jumelle au backlog) |
| Autres outils (F-DT-07/08/10, F-IM, F-FA) | Non applicable | Scan F-132 explicite |
| Cohérence IA (F-IA-03) | Non — pas de saisie avocat à croiser | N/A |
| Refresh dashboard (F-IA-02) | Oui — la card "Indemnités estimées" est masquée en contexte rupture amiable BE | **Intégré** : `CaseFileDashboardService.buildIndemnite` retourne `null` si dossier BE + type `RUPTURE_AMIABLE` (détection via `CaseAnalysis.compensationEstimate` ou absence d'`IndemniteComparatif` quand l'avocat n'a rien saisi) — la card n'est pas rendue si `summary == null` (pattern existant) |
| Pré-remplissage IA | Non — pas de champ à préremplir | N/A |
| Persistance inputs | Non — pas d'input à persister | N/A |
| Masquage conditionnel selon type | Oui — cœur de la SF | **Intégré** : `showRuptureAmiableInfo` computed dans `case-file-detail`, `showComparatifIndemnites` étendu pour exclure BE+RUPTURE_AMIABLE |
| Nouveau pattern UI partagé | Non — composant informationnel isolé, style cohérent avec les autres sections collapsibles | N/A |

### Décision

- [x] Étendu aux cibles applicables (masquage, refresh dashboard)
- [x] SFs parallèles : F-133 (recours generator), aucune autre
- [x] Non applicable aux autres outils (scan F-132)

---

## Critères d'acceptation

### Frontend

- [ ] Nouveau composant `rupture-amiable-info-section.component.ts` (+ html + scss + spec) dans `frontend/src/app/case-files/rupture-amiable-info-section/`
- [ ] Composant **sans input formulaire** — juste un panneau collapsible avec titre + 2 messages + lien vers F-DT-05 (texte simple, pas de navigation)
- [ ] `@Input() caseFileId` (pour cohérence de signature avec les autres sections), pas de service injecté
- [ ] Styles cohérents avec les autres sections (bordure, header cliquable, `mat-icon`, vars CSS du design system)
- [ ] `case-file-detail.ts` : nouveau computed `showRuptureAmiableInfo` = `legalDomain==DROIT_DU_TRAVAIL && country==BELGIQUE && compensationEstimate?.typeRupture==RUPTURE_AMIABLE`
- [ ] `case-file-detail.html` : `app-indemnite-comparatif-section` masquée si `showRuptureAmiableInfo()` ou `showRuptureConvIndemnite()` (condition étendue) ; `app-rupture-amiable-info-section` affichée si `showRuptureAmiableInfo()`
- [ ] 3 tests Jest : rendu nominal (messages visibles), collapsible toggle, pas d'input/service

### Backend

- [ ] Retirer `"RUPTURE_AMIABLE"` de `IndemniteComparatifCalculator.TYPES_RUPTURE_BE` (BE n'accepte plus que `LICENCIEMENT_ORDINAIRE`)
- [ ] Supprimer la branche `if ("RUPTURE_AMIABLE".equals(typeRupture))` dans `calculateBelgique()` (lignes ~95-104) et la méthode `negociationLibre` de `IndemniteComparatifResult` si elle devient inutilisée
- [ ] `CaseFileDashboardService.buildIndemnite` : si `compensationEstimate.typeRupture == RUPTURE_AMIABLE` (lu depuis la dernière `CaseAnalysis`), retourner `null` (card dashboard masquée)
- [ ] Mettre à jour `IndemniteComparatifCalculatorTest` : retirer le test `belgique_ruptureAmiable_returnsNegociationLibre`, muter `belgique_typeRupture_fr_throws` pour aussi couvrir `RUPTURE_AMIABLE` désormais rejeté
- [ ] Mettre à jour `IndemniteComparatifControllerIT` : si un test envoie `RUPTURE_AMIABLE`, l'attente doit devenir `400`
- [ ] Tous les tests restent verts

### Cohérence

- [ ] Pas de régression : un dossier BE `LICENCIEMENT_ORDINAIRE` affiche toujours F-DT-09 avec la fourchette CCT 109 (test Jest case-file-detail existant doit rester vert)

---

## Périmètre

### Hors scope

- F-IM-06 `RecoursGenerator` → **F-133**
- Ajout de champs saisis libres (notes avocat, montant négocié) sur l'outil rupture amiable → possible en follow-up si les avocats en font la demande
- Export PDF du panneau information → non pertinent (pas de données structurées)
- Lien/routing vers F-DT-05 fonctionnel → juste un message texte dans cette SF
- Migration des éventuels `IndemniteComparatif` existants en BE rupture amiable → pas pertinent en staging (données de test seulement)

---

## Contraintes de validation

### Backend

`TYPES_RUPTURE_BE` restreint à `{ "LICENCIEMENT_ORDINAIRE" }` après cette SF.

---

## Technique

### Endpoints consommés

Aucun (composant purement frontend, pas d'appel backend).

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Aucune

### Composants Angular

- `RuptureAmiableInfoSectionComponent` — panneau informationnel collapsible
- `CaseFileDetailComponent` — nouveau computed `showRuptureAmiableInfo` + condition étendue de masquage pour `app-indemnite-comparatif-section`

---

## Plan de test

### Tests Jest frontend

- `RuptureAmiableInfoSectionComponent` :
  - Rendu nominal : 2 messages visibles, titre présent
  - Toggle collapse : contenu visible/caché selon état
  - Pas d'appel HTTP (test d'absence de dépendance — via mock `HttpTestingController.expectNone`)
- `CaseFileDetailComponent` :
  - `showRuptureAmiableInfo` vrai si DROIT_DU_TRAVAIL + BELGIQUE + type=RUPTURE_AMIABLE
  - `showRuptureAmiableInfo` faux pour les autres combinaisons (FR, type licenciement, etc.)
  - Masquage d'`app-indemnite-comparatif-section` si `showRuptureAmiableInfo==true`

### Tests backend mis à jour

- `IndemniteComparatifCalculatorTest` : retrait + ajout de test "BE RUPTURE_AMIABLE → throws"
- `IndemniteComparatifControllerIT` : idem
- `CaseFileDashboardServiceTest` (si applicable) : si l'implémentation du masquage passe par le dashboard, ajouter un test qui vérifie `buildIndemnite` retourne `null` sur un dossier BE rupture amiable

### Isolation workspace

- N/A (pas de nouveaux endpoints / entités)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal : non
- [ ] Workspace context : non
- [ ] Plans / limites : non
- [x] **Navigation / routing** : `case-file-detail` rend un nouveau composant conditionnellement → smoke tests navigation doivent rester verts
- [x] **Outil décisionnel métier** : scan F-132 complet, cibles jumelles tracées (F-133 reste)
- [x] **Refresh dashboard F-IA-02** : la card "Indemnités estimées" peut maintenant disparaître (return null) selon le type de rupture BE

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `IndemniteComparatifCalculator` / `Service` | Plus de branche RUPTURE_AMIABLE BE — n'accepte que LICENCIEMENT_ORDINAIRE | Tests existants mis à jour |
| `CaseFileDashboardService.buildIndemnite` | Retourne null en contexte BE rupture amiable | Test ciblé |
| `app-indemnite-comparatif-section` | Masquée conditionnellement via template | Tests case-file-detail |
| `negociationLibre` factory dans `IndemniteComparatifResult` | Supprimée si unused après la SF | Grep de vérification |

### Smoke tests E2E

- `e2e/smoke/navigation.spec.ts` — doit rester vert (case-file-detail se charge)
- `e2e/smoke/workspace.spec.ts` — doit rester vert

---

## Dépendances

- **SF-132-01** mergée (PR #414) ✅
- **SF-132-02** mergée (PR #415) ✅

---

## Notes et décisions

- **Pourquoi pas d'entity / endpoint côté backend** : contrairement à F-DT-10 validité rupture conventionnelle (qui a des critères saisis par l'avocat) et à SF-132-01 indemnité rupture conv (qui a 2 inputs + 1 calcul), la rupture amiable belge n'a **aucun input** ni **aucun calcul**. C'est un rappel juridique statique. Créer une table vide pour respecter le pattern serait de la dette d'over-engineering.
- **Pourquoi masquer la card dashboard plutôt que la remplir avec "Négociation libre"** : cohérent avec le principe "ne pas afficher d'information sans valeur décisionnelle". Le panneau détaillé est suffisant pour informer l'avocat.
- **Pourquoi pas de lien cliquable vers F-DT-05** : complexifie le composant pour un bénéfice marginal. Un message texte suffit. Si le besoin se confirme, ajouter en follow-up.
- **Pas de support `IndemniteComparatif` existant stockant `RUPTURE_AMIABLE`** : staging uniquement, pas de prod affectée. Les avocats qui ont stocké cela recalculeront depuis l'outil pertinent (ou verront simplement le panneau informationnel).
