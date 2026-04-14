# Mini-spec — F-DT-10 / SF-DT-10-03 Composant Angular de validité de la rupture conventionnelle

## Identifiant

`F-DT-10 / SF-DT-10-03`

## Feature parente

`F-DT-10` — Analyse de validité de la rupture conventionnelle

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-DT-10-03-frontend-rupture-conventionnelle`

---

## Objectif

Offrir à l'avocat une UI pour évaluer la validité d'une rupture conventionnelle : checklist des 6 critères FR, saisie `OUI` / `NON` / `INCONNU`, jauge de risque, verdict coloré, messages de base juridique. Le composant consomme `POST /api/v1/case-files/{id}/rupture-conv` (enregistrement) et `GET` (relecture). Miroir simplifié de F-DT-08, **sans la couche de cohérence IA** (hors scope, cf. SF-DT-10-01).

---

## Comportement attendu

### Cas nominal

1. Le composant `app-rupture-conv-section` est monté dans `case-file-detail` sous le bloc F-DT-09 Comparateur (pour un dossier éligible — la règle d'affichage est SF-DT-10-04, pas ici).
2. `ngOnInit` appelle `GET /rupture-conv`. Si 200 → pré-remplit les 6 radios, affiche le verdict + jauge. Si 404 → affiche la checklist vide (tous `INCONNU`).
3. L'avocat clique sur les radios `OUI` / `NON` / `INCONNU` pour chaque critère.
4. Clic sur "Analyser" → `POST /rupture-conv` avec `country: "FRANCE"` et la map des réponses → affiche le résultat (score + verdict + annotations par critère).
5. Clic sur "Modifier" après analyse → retour à la checklist éditable.
6. Sur succès POST, déclenche `CaseDashboardRefreshService.triggerRefresh()` (pattern SF-IA-02-03).
7. Section **repliable** par défaut (collapsed) avec un header qui indique le verdict actuel si disponible (miroir F-DT-08).

### Structure visuelle

- **Header de section** : icône `gavel` + titre "Validité de la rupture conventionnelle" + badge verdict coloré (si résultat disponible) + chevron.
- **Corps (si déplié)** :
  - Message légal de référence discret : "Rupture conventionnelle individuelle — art. L1237-11 à L1237-16 du Code du travail."
  - Pour chaque critère (6) : libellé, description, mat-radio-group `OUI | NON | INCONNU`, badge "bloquant" si applicable.
  - Bouton "Analyser" (avec spinner en chargement).
  - Après résultat : jauge SVG horizontale 0-100 avec seuils 15/40/70, verdict coloré, liste des critères avec leur commentaire (conforme / non-conforme / à vérifier).
  - Bouton "Modifier" pour revenir à la checklist.

### Couleurs verdict (design system)

- `VALIDE` → vert #27AE60
- `RISQUE_MODERE` → ambre #F59E0B
- `RISQUE_ELEVE` → orange foncé #E67E22
- `INVALIDE` → rouge #C0392B

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| GET → 404 | Checklist vide, pas d'erreur affichée |
| GET → 5xx ou autre 4xx | Checklist vide + console.error silencieux (pas de snackbar) |
| POST → 4xx/5xx | `MatSnackBar.open('Erreur lors de l'analyse', 'Fermer', { duration: 4000 })`, le formulaire reste accessible |
| Perte réseau pendant POST | Idem, le bouton Analyser revient actif |

### Scope frontend (cohérence avec SF-DT-10-01)

- **Pas de pré-remplissage IA** (hors scope V2 initial). L'avocat remplit chaque radio manuellement.
- **Pas de cohérence IA** (pas de badges de divergence F-IA-03). Une subfeature ultérieure pourra l'ajouter si besoin.
- **Pas d'intégration dans `CaseDashboardComponent` (F-IA-02)** — une card dédiée viendra dans SF-DT-10-04 ou plus tard.

### Pré-remplissage du type de rupture

Le composant ne dépend d'aucun champ IA spécifique — son unique source de données est l'API `/rupture-conv`. La visibilité conditionnelle selon `compensation_data.type_rupture = RUPTURE_CONVENTIONNELLE` est portée par SF-DT-10-04 (orchestration UX).

---

## Critères d'acceptation

- [ ] Service Angular `RuptureConvService` dans `core/services/rupture-conv.service.ts` — méthodes `get(caseFileId)` et `analyze(caseFileId, { country, reponses })`.
- [ ] Modèle `RuptureConvResponse` dans `core/models/rupture-conv.model.ts` (miroir du record backend).
- [ ] Catalogue statique des 6 critères FR dans le composant (libellés, descriptions, bloquant) — duplication assumée (cf. décision mini-spec SF-DT-10-02).
- [ ] Composant standalone `RuptureConvSectionComponent` avec selector `app-rupture-conv-section`.
- [ ] `@Input() caseFileId!: string`.
- [ ] Signals : `collapsed`, `loading`, `analyzing`, `showForm`, `reponses` (Record<code, "OUI"|"NON"|"INCONNU">), `result`.
- [ ] Rendu : header repliable, checklist radios, bouton Analyser, jauge SVG, verdict coloré, bouton Modifier.
- [ ] `ngOnInit` : appelle `GET /rupture-conv` ; 404 → formulaire vide ; 200 → préchargement.
- [ ] Clic Analyser → POST → affiche résultat ; erreur → snackbar.
- [ ] Sur succès POST, appelle `CaseDashboardRefreshService.triggerRefresh()` (pattern SF-IA-02-03 — `@Optional() private refresh: CaseDashboardRefreshService | null` en constructeur).
- [ ] Tests Jest unitaires (≥ 5) : rendu initial 404, préchargement 200, radio change met à jour reponses, POST succès → affichage résultat, POST erreur → snackbar.
- [ ] **Pas** d'intégration dans `case-file-detail` dans cette SF (déléguée à SF-DT-10-04 pour la logique conditionnelle de visibilité).

---

## Périmètre

### Hors scope (explicite)

- Orchestration conditionnelle dans `case-file-detail` selon `compensation_data.type_rupture` (→ SF-DT-10-04).
- Pré-remplissage IA des critères depuis les documents (hors V2 initial).
- Alertes de cohérence F-IA-03 sur ce composant.
- Export PDF du résultat.
- Intégration au tableau de bord décisionnel F-IA-02 (ajout d'une card "Rupture conventionnelle") — envisageable plus tard.
- Version belge.

---

## Valeurs initiales

- Tous les critères démarrent à `INCONNU` en l'absence d'appel backend ou de retour 404.
- Après un GET 200, les valeurs viennent de la réponse.

---

## Contraintes de validation

| Champ | Obligatoire | Valeurs autorisées |
|-------|-------------|-------------------|
| radio réponse | Oui (forcé via value par défaut `INCONNU`) | `OUI` / `NON` / `INCONNU` |

Aucune validation côté frontend au-delà de ces radios — le backend accepte tout via normalisation fail-open.

---

## Technique

### Endpoints consommés

- `GET /api/v1/case-files/{caseFileId}/rupture-conv` — préchargement.
- `POST /api/v1/case-files/{caseFileId}/rupture-conv` — enregistrement.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular

- `core/models/rupture-conv.model.ts` — `RuptureConvResponse`, `RuptureConvCritereData`.
- `core/services/rupture-conv.service.ts` — injection HttpClient, deux méthodes.
- `case-files/rupture-conv-section/rupture-conv-section.component.ts` + `.html` + `.scss`.
- `case-files/rupture-conv-section/rupture-conv-section.component.spec.ts`.

### Design system

- Couleurs verdict : palette existante.
- Fonte : Inter (header + corps), JetBrains Mono possible pour les codes de critère si besoin.
- Espacements : multiples de 4px.
- `mat-form-field` avec `appearance="outline"` si un champ texte est ajouté ultérieurement (pas dans cette SF — uniquement radios).
- `MatSnackBar` pour erreur POST.
- Pas de `window.alert/confirm/prompt`.

### Refresh dashboard

- Le composant injecte `CaseDashboardRefreshService` en `@Optional()` et appelle `triggerRefresh()` dans le `next:` du POST (pattern SF-IA-02-03 identique aux 9 autres outils).

---

## Plan de test

### Tests unitaires Jest

- [ ] `RuptureConvService` : `get` et `analyze` envoient les bonnes URL et body (HttpTestingController).
- [ ] `RuptureConvSectionComponent` : ngOnInit → GET → 404 → formulaire vide, tous critères `INCONNU`.
- [ ] `RuptureConvSectionComponent` : ngOnInit → GET → 200 → préchargement des radios + résultat affiché.
- [ ] `RuptureConvSectionComponent` : changement de radio met à jour `reponses`.
- [ ] `RuptureConvSectionComponent` : clic Analyser → POST → résultat affiché, `triggerRefresh()` appelé.
- [ ] `RuptureConvSectionComponent` : POST en erreur → snackbar affiché.
- [ ] `RuptureConvSectionComponent` : clic Modifier → `showForm()` true, reponses préservées.

### Tests d'intégration

- [x] N/A — les 8 IT backend (SF-DT-10-02) couvrent les contrats API.

### Isolation workspace

- [x] Garantie par le backend, pas de re-test nécessaire côté Angular.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — nouveau composant isolé, aucune route ajoutée, aucun guard touché.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| `CaseFileDetailComponent` | Aucun — intégration différée à SF-DT-10-04 | Aucun à ce stade |
| `CaseDashboardComponent` | Aucun | — |
| Autres sections existantes | Aucun | — |

### Smoke tests E2E concernés

- [ ] Aucun — composant non visible tant que SF-DT-10-04 n'a pas câblé l'orchestration.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-10-01` Done — entité/analyzer backend disponibles.
- `SF-DT-10-02` Done — endpoints POST/GET disponibles.
- `SF-IA-02-03` Done — `CaseDashboardRefreshService` exposé, pattern d'injection `@Optional()` suivi.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi le composant n'est pas monté dans `case-file-detail` ici** : la logique de visibilité conditionnelle dépend du type de rupture extrait par l'IA (`compensation_data.type_rupture = RUPTURE_CONVENTIONNELLE`). C'est une orchestration transverse qui couvre aussi le masquage symétrique de F-DT-08 — mieux isoler dans une SF dédiée (SF-DT-10-04) pour rester chirurgical.
- **Pourquoi pas de pré-remplissage IA** : sans calibration supplémentaire du prompt, l'IA n'a pas de détections structurées pour les 6 critères de rupture conventionnelle (contrairement à F-DT-08 où `licenciement_validity_detection` existe). Ajouter un volet IA impliquerait un nouveau champ structuré — traité plus tard si besoin avéré.
- **Pourquoi garder le catalogue statique front** : les 6 critères changent rarement, le référentiel est figé côté back, dupliquer évite un endpoint et un aller-retour. Le contrat est verrouillé par les tests unitaires.
- **Pas d'intégration F-IA-02 tout de suite** : le tableau de bord décisionnel a déjà 10 cards. En ajouter une sans avoir validé l'usage UX serait prématuré. À rediscuter après retour utilisateur.
