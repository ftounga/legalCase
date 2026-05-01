# Mini-spec — F-IM-11 / SF-IM-11-04 Transitions vers VPF (conjoint / parent / PACS de ressortissant FR)

> Constat terrain (dossier "Immigration Chen - 4", staging, 2026-05-01) : le
> calculateur backend rejette `ETUDIANT → VPF` avec
> "Transition non supportée", alors que c'est le scénario stratégique central
> pour un étudiant marié à une ressortissante française (cas Chen Wei /
> BERNARD Camille — `triggerEvent = MARIAGE_RESSORTISSANT_FR`).

---

## Identifiant

`F-IM-11 / SF-IM-11-04`

## Feature parente

`F-IM-11` — Changement de statut (CESEDA)

## Statut

`draft`

## Date de création

2026-05-01

## Branche Git

`feat/SF-IM-11-04-transitions-vers-vpf`

---

## Objectif

Étendre la matrice de transitions de `ChangementStatutCalculator` (FR) avec les
transitions **vers VPF** (Vie Privée et Familiale, CESEDA L.423-x) depuis les
titres source les plus fréquents.

---

## Comportement attendu

### Cas nominal

Le calculateur accepte 5 nouvelles paires `src → dst` :

| `src` | `dst` | Base juridique |
|---|---|---|
| `ETUDIANT` | `VPF` | CESEDA L.423-1 (conjoint FR) / L.423-7 (parent enfant FR) / L.423-8 (PACS) |
| `VISITEUR` | `VPF` | idem |
| `SALARIE` | `VPF` | idem |
| `PASSEPORT_TALENT_*` | `VPF` | idem |
| `APS` | `VPF` | idem |

Pour chaque transition, l'enchaînement est :

1. `resolveTransition(src, "VPF")` retourne une `Transition` avec `kind = X_VPF`
   et la `baseJuridique` correspondante.
2. `applyTransition(t, ...)` :
   - **Documents requis** : copie de l'acte civil justifiant le lien (mariage / naissance enfant FR / PACS), justificatif de domicile commun, attestation de communauté de vie pour mariage > 6 mois, passeport en cours de validité.
   - **Verdict de base** :
     - `documentJustificatifFourni = false` → `FAIBLE` + risque "acte civil de référence (mariage/naissance/PACS) non produit — pièce essentielle".
     - `documentJustificatifFourni = true` → `ELEVEE`.
   - **Risques additionnels (n'altèrent pas le verdict de base)** :
     - Message "communauté de vie effective requise — vérifier 6 mois minimum (L.423-1) ou 1 an (L.423-2 carte de résident)".
     - Message "rupture de la vie commune dans les 3 ans entraîne retrait du titre (L.432-4)".
3. Les bloqueurs transversaux existants (durée restante < 2 mois / casier non vierge) continuent de rétrograder le verdict à `FAIBLE`.
4. Délai d'instruction : valeur par défaut existante (`DELAI_INSTRUCTION_DEFAULT_MOIS = 3`).
5. La rémunération n'est pas requise pour ces transitions (ne pas exiger SMIC × 1,5).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|---|---|---|
| `titreActuel = VPF` et `titreEnvisage = VPF` | `IllegalArgumentException` "titres identiques" (déjà géré) | 400 |
| Combinaison non couverte (ex. `ASILE → VPF`) | `IllegalArgumentException` "Transition non supportée" (comportement existant préservé) | 400 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres calculateurs métier exposant des matrices de transitions** : aucun équivalent identifié dans `backend/src/main/java/fr/ailegalcase/casefile/`. Les autres calculateurs (Licenciement, Rupture conv., AES, OQTF) opèrent sur un seul type d'événement, pas sur une matrice src→dst.
- [x] **Outil jumeau Belgique** : F-IM-11-BE est déjà au backlog comme feature jumelle (cf. PRODUCT_SPEC ligne F-IM-11). La procédure 9bis OE belge est structurée différemment (modification du titre, pas matrice src→dst). **Non applicable à cette SF**.
- [x] **Frontend** : `TITRE_SEJOUR_LABELS` du modèle frontend liste déjà `VPF` comme option valide du `mat-radio` "Titre envisagé" — la dropdown autorise déjà ces choix sans changement.
- [x] **Tests existants** : `ChangementStatutCalculatorTest` (25 UT) et `ChangementStatutControllerIT` (13 IT) couvrent les 7 transitions actuelles. Étendre.
- [x] **Référentiels juridiques** : `legal_referentials` table, type `IMMIGRATION_TITLES`. Les codes `VPF` sont déjà présents (utilisés pour le mapping IA `mapTitreSejourFromIa`). Pas de migration nécessaire.

### Niveaux de vérification

- [x] **Modèle TypeScript** : `TitreSejourCode` inclut déjà `VPF`. Pas de changement.
- [x] **DTO backend** : `ChangementStatutRequest` / `ChangementStatutResult` agnostiques de la matrice — pas de changement.
- [x] **Service / logique métier** : modification de `ChangementStatutCalculator.resolveTransition` et `applyTransition` (ajout 1 cas de switch `kind`).
- [x] **Entité JPA + schéma DB** : `changement_statut_analyses` stocke les codes string + le verdict — pas de changement de schéma.
- [x] **Tests existants** : étendre `ChangementStatutCalculatorTest` avec ≥ 5 nouvelles UT (1 par transition) + couverture verdict ELEVEE/FAIBLE selon `documentJustificatifFourni`.

### Cas spécifique : modification d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : aucune nouvelle alerte introduite — l'alerte existante `TITRE_ACTUEL` continue de fonctionner.
- [x] **Refresh dashboard (F-IA-02)** : pas de changement (`triggerRefresh()` déjà câblé).
- [x] **Pré-remplissage IA** : pas de changement direct ; le `triggerEvent = MARIAGE_RESSORTISSANT_FR` détecté par l'IA pour Chen Wei n'alimente pas (encore) le composant. Suggérer dans une SF future un pré-fill de `titreEnvisage = VPF` quand `triggerEvents.MARIAGE_RESSORTISSANT_FR` est détecté — **hors scope**, à backlog.
- [x] **Persistance des inputs** : champs déjà persistés (SF-IM-11-01).
- [x] **Masquage conditionnel** : pas de changement.
- [x] **Alertes actives après calcul** : pas de changement.

### Décision

- [x] Étendu à toutes les cibles applicables dans cette SF (5 transitions vers VPF dans le calculateur unique).
- [x] Backlog : pré-fill de `titreEnvisage = VPF` quand `triggerEvents` contient `MARIAGE_RESSORTISSANT_FR` — note ajoutée au PRODUCT_SPEC backlog F-IM-11.
- [x] Backlog : autres transitions non couvertes (ex. `ASILE → SALARIE`, `RESIDENT → SALARIE`) — non urgent.

---

## Critères d'acceptation

- [ ] `compute("ETUDIANT", "VPF", 4, true, null, true)` retourne `verdict = ELEVEE`, `baseJuridique` mentionne L.423-1, `documentsRequis` contient acte de mariage et justificatif de domicile commun.
- [ ] `compute("ETUDIANT", "VPF", 4, false, null, true)` retourne `verdict = FAIBLE` avec `risqueRefus` mentionnant "acte civil de référence non produit".
- [ ] `compute("VISITEUR", "VPF", 6, true, null, true)` retourne `verdict = ELEVEE`.
- [ ] `compute("SALARIE", "VPF", 6, true, null, true)` retourne `verdict = ELEVEE`.
- [ ] `compute("PASSEPORT_TALENT_SALARIE_QUALIFIE", "VPF", 8, true, null, true)` retourne `verdict = ELEVEE`.
- [ ] `compute("APS", "VPF", 6, true, null, true)` retourne `verdict = ELEVEE` ; `delaiInstructionMois = 2` (APS spécifique préservé).
- [ ] `compute("ETUDIANT", "VPF", 1, true, null, true)` retourne `verdict = FAIBLE` (durée < 2 mois rétrograde).
- [ ] `compute("ETUDIANT", "VPF", 4, true, null, false)` retourne `verdict = FAIBLE` (casier non vierge rétrograde).
- [ ] La rémunération n'est pas exigée : `compute("ETUDIANT", "VPF", 4, true, null, true)` réussit sans `remunerationContratEur`.
- [ ] Les transitions non-VPF existantes (ex. `ETUDIANT → SALARIE`) restent vertes dans les 25 UT actuelles.
- [ ] Test d'intégration `POST /api/v1/case-files/{id}/changement-statut-analyses` avec body `{titreActuel: "ETUDIANT", titreEnvisage: "VPF", ...}` retourne `200` avec verdict `ELEVEE` et persiste l'analyse.

---

## Périmètre

### Hors scope

- Pas de pré-fill `titreEnvisage = VPF` depuis `triggerEvents.MARIAGE_RESSORTISSANT_FR` (à backlog).
- Pas de transitions `RESIDENT → ...` ni `ASILE → ...` (seuils métier différents, à scoper séparément).
- Pas de transitions sortantes depuis VPF déjà couvertes (`VPF → SALARIE`, `VPF → ETUDIANT`).
- Pas de modification frontend — les labels VPF sont déjà dans la dropdown.
- Pas d'équivalent Belgique — relève de F-IM-11-BE backlog jumeau.

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|---|---|---|---|
| `titreActuel` | Oui | enum `TitreSejourCode` (incl. `VPF`) | déjà validé |
| `titreEnvisage` | Oui | enum `TitreSejourCode` (incl. `VPF`) | déjà validé |
| `dureeRestanteSurTitreActuelMois` | Oui | int ≥ 0 | déjà validé |
| `documentJustificatifFourni` | Oui | boolean | déjà validé |
| `remunerationContratEur` | Non pour transitions vers VPF | BigDecimal ≥ 0 | non requis (différencier de `ETUDIANT_SALARIE` où ≥ 1.5 SMIC est exigé) |
| `casierJudiciaireVierge` | Oui | boolean | déjà validé |

---

## Technique

### Endpoints

Pas de changement de surface API.

| Méthode | URL | Auth | Rôle minimum |
|---|---|---|---|
| POST | `/api/v1/case-files/{id}/changement-statut-analyses` | Oui | LAWYER |

Le contrat reste identique. Les nouvelles paires `(src, dst)` sont acceptées sans changement de DTO.

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `changement_statut_analyses` | INSERT | inchangé, persiste les nouveaux verdicts comme avant |

### Migration Liquibase

- [ ] Non applicable.

### Composants Angular

Aucun changement frontend dans cette SF.

### Backend Java

- `backend/src/main/java/fr/ailegalcase/casefile/ChangementStatutCalculator.java`
  - `TransitionKind` enum : ajouter `ETUDIANT_VPF`, `VISITEUR_VPF`, `SALARIE_VPF`, `TALENT_VPF`, `APS_VPF`.
  - `resolveTransition` : 5 nouvelles branches.
  - `applyTransition` : 5 nouveaux cas (ou 1 helper `applyTransitionVersVpf` factorisé).
  - `buildFormule` : préserver le cas existant + ajouter le libellé pour les transitions VPF.

---

## Plan de test

### Tests unitaires (JUnit)

- [ ] `ChangementStatutCalculatorTest` :
  - [ ] `etudiantVersVpf_avecJustificatif_verdictEleve()`
  - [ ] `etudiantVersVpf_sansJustificatif_verdictFaible()`
  - [ ] `visiteurVersVpf_verdictEleve()`
  - [ ] `salarieVersVpf_verdictEleve()`
  - [ ] `passeportTalentVersVpf_verdictEleve()`
  - [ ] `apsVersVpf_delai2Mois()`
  - [ ] `vpfDureeBloque_dureeRestante1Mois_rétrogradeFaible()`
  - [ ] `vpfCasierNonVierge_rétrogradeFaible()`
  - [ ] `transitionsExistantesNonRégressées()` — sanity check sur les 7 paires actuelles.

### Tests d'intégration

- [ ] `ChangementStatutControllerIT` — `POST /api/v1/case-files/{id}/changement-statut-analyses` avec body `{titreActuel: "ETUDIANT", titreEnvisage: "VPF", documentJustificatifFourni: true, dureeRestanteSurTitreActuelMois: 4, casierJudiciaireVierge: true}` → `200` avec verdict `ELEVEE`, analyse persistée.
- [ ] `POST` avec `titreEnvisage = "VPF"` mais `documentJustificatifFourni = false` → verdict `FAIBLE`.

### Isolation workspace

- [x] Couverte par les IT existants (le contrôleur applique le filtre `workspace_id` via `@PreAuthorize`/le service standard).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — extension stricte de la matrice du calculateur, sans toucher l'auth, le workspace context, les plans ou la navigation.

### Composants impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| `ChangementStatutCalculator` (existant) | Risque de régression sur les 7 transitions actuelles si refactoring du switch | Test `transitionsExistantesNonRégressées` qui réplique les 25 UT existantes |
| Frontend `changement-statut-section` | Aucun changement de contrat — la dropdown VPF était déjà active mais provoquait l'erreur | Test manuel post-merge sur dossier Chen 4 (cas reproducteur) |

### Smoke tests E2E

- [x] Aucun smoke test concerné — l'outil n'est pas couvert par `e2e/smoke/`.

---

## Impact par domaine métier

- **Droit du travail** : non applicable.
- **Droit immigration** : c'est la cible exclusive.
- **Droit famille** : non applicable.
- **France / Belgique** : FR uniquement (CESEDA). L'équivalent BE 9bis OE est dans le backlog jumeau F-IM-11-BE.

---

## Parité des domaines métier

L'outil "Changement de statut" est de **niveau 4 (arbre décisionnel)** dans la grille de profondeur. Les 3 domaines :

- **DROIT_DU_TRAVAIL** : pas d'équivalent — la notion de "changement de statut" entre titres ne s'applique qu'à l'immigration.
- **DROIT_FAMILLE** : pas d'équivalent direct — le passage divorce → autre régime familial relève d'autres outils (F-FA-x).
- **DROIT_IMMIGRATION** : présent (cette SF).

→ **Parité non applicable** (concept exclusif au domaine immigration).

---

## Dépendances

### Subfeatures bloquantes

- `SF-IM-11-01` — `done` (calculateur en place).
- `SF-IM-11-02` — `done` (frontend permet de saisir VPF).

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Pourquoi 5 transitions et pas plus ?** : `ASILE`, `RESIDENT` et autres sont rares dans la pratique sur cette transition et impliquent des seuils métier différents (durée mariage > 4 ans pour conjoint d'asilé, par ex.). À scoper séparément.
- **Pourquoi pas de pré-fill `titreEnvisage` depuis `triggerEvents.MARIAGE_RESSORTISSANT_FR` ?** : c'est un saut UX intéressant (l'IA "sait" que VPF est la voie probable), mais c'est un cas distinct du pré-fill de fait extrait — c'est de l'inférence stratégique. Backlog dédié.
- **Pourquoi pas exiger SMIC × 1,5 sur les transitions vers VPF ?** : le titre VPF n'est pas conditionné à un seuil de rémunération (L.423-1 art.) — c'est la spécificité par rapport à `ETUDIANT_SALARIE` (R.5221-3) qui exige le seuil.
- **Communauté de vie 6 mois vs 1 an** : 6 mois pour la délivrance du VPF (L.423-1) ; 1 an pour la délivrance directe de la carte de résident (L.423-2). Mention dans `messages` mais pas de blocage automatique — décision avocat.
