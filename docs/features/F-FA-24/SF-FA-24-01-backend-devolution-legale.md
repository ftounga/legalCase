# Mini-spec — F-FA-24 / SF-FA-24-01 Backend dévolution légale successorale

## Identifiant

`F-FA-24 / SF-FA-24-01`

## Feature parente

`F-FA-24` — Droit des successions (chantier ~8-10 SF — futures : testament 967+, donation 893+, réserve 913+, action en réduction, partage successoral, indivision successorale, rapport à succession)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-24-01-backend-devolution-legale`

---

## Objectif

Premier morceau backend de F-FA-24 — calculator + endpoint d'analyse de la **dévolution légale successorale** (FR — art. 731 et s. Cciv) qui détermine les héritiers et leurs quotes-parts à partir de la composition familiale du défunt en l'absence de testament.

---

## Comportement attendu

### Cas nominal

L'avocat saisit la composition familiale du défunt (conjoint survivant, descendants, ascendants, fratrie...) → l'outil applique les règles des **4 ordres d'héritiers** + spécial conjoint survivant + représentation + fente successorale → renvoie la liste des héritiers désignés avec quotes-parts en pourcentage, alertes contentieux, base juridique et formule détaillée.

#### Règles métier (FR — art. 731 et s. Cciv)

**4 ordres d'héritiers** (chacun exclut le suivant sauf représentation) :
1. **Descendants** (art. 734)
2. **Ascendants privilégiés (parents) + collatéraux privilégiés (frères/sœurs)** (art. 738)
3. **Ascendants ordinaires** (grands-parents, art. 739)
4. **Collatéraux ordinaires** jusqu'au 6ème degré (art. 740)

**Spécial conjoint survivant** (art. 757 et s. Cciv) :
- Avec descendants **tous communs** : option **¼ pleine propriété** OU **usufruit total** (art. 757)
- Avec descendants **non communs** (recomposée) : **¼ pleine propriété obligatoire** (pas d'option usufruit)
- Sans descendants, parents survivants : conjoint **½** + parents **½** (¼ chacun si 2 parents, ½ pour un seul) — art. 757-1
- Sans descendants ni parents : conjoint **toute la succession** — exclut ordres 3 et 4 (art. 757-2)

**Représentation** (art. 751-755) : si un héritier est prédécédé, ses propres descendants prennent sa place et se partagent sa part.

**Fente successorale** (art. 746-749) : pour les **ascendants ordinaires** seuls, séparation 50% ligne paternelle / 50% ligne maternelle.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body absent | "Corps de requête requis" | 400 |
| `nbDescendants` < 0 | "Nombre de descendants doit être ≥ 0" | 400 |
| `nbFreresSoeurs` < 0 | "Nombre de frères/sœurs doit être ≥ 0" | 400 |
| `nbFreresSoeursPredecedes` > `nbFreresSoeurs` | "Prédécédés ne peut excéder le total" | 400 |
| `optionConjoint` requis si conjoint survivant + descendants tous communs | "Option du conjoint requise (USUFRUIT ou QUART)" | 400 |
| Aucun héritier (pas de conjoint, pas de descendants, pas d'ascendants, pas de fratrie, pas d'ascendants ordinaires, pas de collatéraux) | "Succession en déshérence (art. 768) — aucun héritier identifiable" — `messages` lève alerte | 200 |
| Workspace pays ≠ FRANCE | "Outil non disponible pour le pays X — backlog jumeau F-FA-24-BE" | 400 |
| Dossier ≠ DROIT_FAMILLE | "Ce dossier n'est pas un dossier de droit de la famille" | 400 |
| Dossier d'un autre workspace | "Case file not found" | 404 |
| GET sans POST préalable | "Aucune analyse Dévolution légale trouvée pour ce dossier" | 404 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils décisionnels famille FR existants** : F-FA-14 Divorce faute, F-FA-15 Pension alimentaire, F-FA-17 Partage judiciaire, F-FA-19 Autorité parentale, F-FA-20 PACS dissolution, F-FA-21 Séparation corps, F-FA-22 Indivision, F-FA-23 Mesures urgentes — **classement** : déjà séparés un par situation (pattern F-DT-08/F-DT-10) → **non applicable** (chacun couvre une situation distincte).
- [x] **Autres pays** : Belgique → règles différentes (art. 731+ CC BE). **Backlog jumeau F-FA-24-BE** prévu (mention dans la mini-spec et message d'erreur explicite si workspace BE).
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_IMMIGRATION → **non applicable** (succession = strictement DROIT_FAMILLE).
- [x] **UI patterns** : section décisionnelle Angular F-IA-04 → **SF-FA-24-02 frontend** future.
- [x] **Auth / workspace** : pattern `CurrentUserResolver` + `WorkspaceMemberRepository` + gate `legalDomain == "DROIT_FAMILLE"` + gate `country == "FRANCE"` (cf. PartageJudiciaire) → **réutilisé tel quel**.

### Verdict

Pattern aligné F-FA-17 (PartageJudiciaire). Aucune duplication créée — outil isolé, single-country, single-domain.

---

## Impact par domaine métier

- **Sensibilité au domaine** : forte — feature 100% droit famille FR. Aucun impact DROIT_DU_TRAVAIL ou DROIT_IMMIGRATION.
- **Sensibilité au pays** : forte — règles successorales propres au Code civil français. Belgique = backlog jumeau **F-FA-24-BE** (CC BE art. 731+ avec quotités différentes).

---

## Parité des domaines métier (outil de niveau 5 — scoring)

L'outil est un **scoring/analyse de validité** (niveau 5 — détermine héritiers + quotes-parts). Application des règles de parité :

| Domaine | Équivalent existant | Décision |
|---------|---------------------|----------|
| DROIT_DU_TRAVAIL | N/A — concept inapplicable (employeur/salarié pas une succession) | Non applicable, justifié |
| DROIT_IMMIGRATION | N/A — concept inapplicable (pas de pendant immigration de la dévolution) | Non applicable, justifié |
| DROIT_FAMILLE FRANCE | **Cette SF** | En cours |
| DROIT_FAMILLE BELGIQUE | Règles successorales propres (CC BE art. 731+) — quotités, réserve, fente, etc. différentes | **Backlog jumeau F-FA-24-BE** à ouvrir |

---

## Critères d'acceptation

1. POST `/api/v1/case-files/{id}/devolution-legale-analysis` avec body valide (FR, DROIT_FAMILLE) → 200 + `heritiersDesignes` + `quotePartConjoint` + `representationActive` + `fenteApplicable` + `risquesContentieux` + `baseJuridique` + `formule` + `messages`.
2. **Ordre 1 — descendants seuls** (ex 3 enfants, pas de conjoint) → 3 héritiers à 33.33% chacun, ordre 1.
3. **Ordre 1 — descendants + conjoint tous communs option QUART** → conjoint 25% + descendants se partagent 75%.
4. **Ordre 1 — descendants + conjoint tous communs option USUFRUIT** → conjoint 100% en usufruit + descendants 100% nue-propriété.
5. **Ordre 1 — descendants non communs (recomposée)** → conjoint **forcément 25% pleine propriété** (option ignorée même si fournie) + descendants 75%.
6. **Ordre 2 — pas de descendants, 2 parents + 2 frères + conjoint** → conjoint 50% + parents 25% + frères se partagent 25%.
7. **Ordre 2 — pas de descendants ni parents, frères seuls** → conjoint exclut totalement frères → conjoint 100%.
8. **Ordre 3 — pas de conjoint, pas de descendants, pas de parents, ascendants ordinaires** → fente 50% paternelle / 50% maternelle.
9. **Ordre 4 — collatéraux ordinaires uniquement** → 100% partagé.
10. **Représentation** — descendants avec un héritier prédécédé ayant lui-même 2 enfants → représentation active, ses 2 enfants prennent sa quote-part.
11. **Sans héritier** → message "succession en déshérence" + `risquesContentieux` non vide.
12. POST sur workspace BE → 400 mentionnant `BELGIQUE` et backlog jumeau.
13. POST sur dossier DROIT_DU_TRAVAIL FR → 400.
14. POST sur dossier d'un autre workspace → 404.
15. POST avec champs manquants ou invalides → 400 ciblé (≥ 4 cas couverts).
16. POST upsert (2ème POST sur même dossier) → remplace l'analyse précédente (1 seule ligne en base via UNIQUE).
17. GET après POST → renvoie l'analyse persistée.
18. GET sans POST → 404.
19. `baseJuridique` contient `731`, `734`, `757`, `746` (fente).
20. Migration Liquibase 179 crée la table + UNIQUE + insert visibility rule ALWAYS_ON DROIT_FAMILLE FRANCE priority 88.

---

## Plan de test

### Tests unitaires (`DevolutionLegaleCalculatorTest`) — ≥ 18

1. Ordre 1 — 3 enfants seuls → 3 × 33.33%.
2. Ordre 1 — 1 enfant seul → 100%.
3. Ordre 1 — 2 enfants tous communs + conjoint option QUART → conjoint 25%, enfants 37.5% chacun.
4. Ordre 1 — 2 enfants tous communs + conjoint option USUFRUIT → conjoint 100% usufruit + enfants 100% NP.
5. Ordre 1 — 2 enfants non communs + conjoint → conjoint forcé 25%, enfants 37.5% chacun.
6. Ordre 1 — représentation : 1 enfant vivant + 1 enfant prédécédé ayant 2 petits-enfants → enfant 50%, chaque petit-enfant 25%.
7. Ordre 2 — 2 parents + 2 frères + conjoint → conjoint 50%, chaque parent 12.5%, chaque frère 12.5%.
8. Ordre 2 — 1 parent + conjoint → conjoint 75%, parent 25%.
9. Ordre 2 — 0 parent + 3 frères → conjoint exclut → conjoint 100%.
10. Ordre 2 sans conjoint — 2 parents + 4 frères → parents 50% (25% chacun), frères 50% (12.5% chacun).
11. Ordre 3 — ascendants ordinaires + fente 50/50 → fente active.
12. Ordre 4 — collatéraux ordinaires uniquement → 100% partagé entre eux.
13. Aucun héritier → succession en déshérence dans messages + risquesContentieux non vide.
14. Validation : `nbDescendants` négatif → IllegalArgumentException.
15. Validation : `nbFreresSoeursPredecedes > nbFreresSoeurs` → IllegalArgumentException.
16. Validation : option conjoint manquante avec conjoint + descendants tous communs → IllegalArgumentException.
17. Validation : country null → IllegalArgumentException.
18. Validation : country BELGIQUE → IllegalArgumentException mentionnant feature jumelle.
19. `baseJuridique` contient 731, 734, 738, 757, 746.
20. `formule` contient ordre actif + score.

### Tests intégration (`DevolutionLegaleControllerIT`) — ≥ 7

1. POST FR DROIT_FAMILLE descendants seuls → 200 + 3 héritiers.
2. POST FR DROIT_FAMILLE conjoint + descendants tous communs option QUART → 200 + quotePartConjoint = 25.
3. POST FR DROIT_FAMILLE conjoint + descendants non communs → 200 + quotePartConjoint = 25 (option ignorée).
4. POST workspace BE → 400.
5. POST DROIT_DU_TRAVAIL FR → 400.
6. POST autre workspace → 404.
7. POST nbDescendants négatif → 400.
8. POST upsert remplace → 200 + nouvelle valeur.
9. GET après POST → 200 + données persistées.
10. GET sans POST → 404.

### Isolation workspace

Test cross-workspace explicite (workspace A POST sur dossier de workspace B → 404).

---

## Tables / endpoints / composants impactés

### Tables
- **Nouvelle** : `devolution_legale_analyses` (1:1 case_files via UNIQUE) — créée par migration Liquibase **179-create-devolution-legale-analyses.xml**.
- **Modifiée** : `decision_tool_visibility_rules` — INSERT règle ALWAYS_ON DROIT_FAMILLE FRANCE tool_id `F-FA-24-devolution-legale` priority 88 UUID `f1a04001-0000-0000-0000-ee0000000179`.

### Endpoints
- `POST /api/v1/case-files/{caseFileId}/devolution-legale-analysis` — upsert
- `GET /api/v1/case-files/{caseFileId}/devolution-legale-analysis` — lecture

### Composants Java
- `DevolutionLegaleRequest` (record)
- `DevolutionLegaleResponse` (record)
- `DevolutionLegaleResult` (record)
- `DevolutionLegaleAnalysis` (entity JPA)
- `DevolutionLegaleRepository` (JpaRepository)
- `DevolutionLegaleCalculator` (final class — règles métier pures)
- `DevolutionLegaleService` (Spring service — orchestration + auth)
- `DevolutionLegaleController` (REST endpoint)

---

## Hors périmètre

- **Frontend** : SF-FA-24-02 (séquentiel après merge backend).
- **Belgique** : F-FA-24-BE (backlog jumeau).
- **Autres SF de F-FA-24** : testament (967+), donation (893+), réserve héréditaire (913+), action en réduction, partage successoral, indivision successorale, rapport à succession — chaque concept = SF dédiée.
- **Cas exotiques** non couverts en SF-01 :
  - Successions internationales (règlement UE 650/2012).
  - Pacte successoral, donation-partage.
  - Indignité successorale (art. 726-729).
  - Renonciation à succession (art. 804-808) — sortie d'un héritier de la dévolution.
  - Adoption simple vs plénière — l'outil considère tous les enfants juridiquement reconnus comme descendants ordre 1.
- **Détail du calcul de l'usufruit en valeur** (art. 669 CGI table fiscale) — la SF-01 indique simplement "usufruit total" sans valoriser.

---

## Contrat API

### POST /api/v1/case-files/{caseFileId}/devolution-legale-analysis

**Body**
```json
{
  "conjointSurvivant": true,
  "nbDescendants": 2,
  "tousDescendantsCommunsAvecConjoint": true,
  "nbDescendantsPredecedes": 0,
  "nbPetitsEnfantsParRepresentation": 0,
  "pereVivant": false,
  "mereVivant": false,
  "nbFreresSoeurs": 0,
  "nbFreresSoeursPredecedes": 0,
  "ascendantsOrdinaires": false,
  "collateralOrdinaires": false,
  "optionConjoint": "QUART"
}
```

**Réponse 200**
```json
{
  "caseFileId": "...",
  "ordreActif": "DESCENDANTS",
  "heritiersDesignes": [
    {"qualite": "CONJOINT", "ordre": 0, "quotePartPct": 25.0, "modalite": "PLEINE_PROPRIETE"},
    {"qualite": "DESCENDANT", "ordre": 1, "quotePartPct": 37.5, "modalite": "PLEINE_PROPRIETE"},
    {"qualite": "DESCENDANT", "ordre": 1, "quotePartPct": 37.5, "modalite": "PLEINE_PROPRIETE"}
  ],
  "quotePartConjoint": 25.0,
  "modaliteConjoint": "PLEINE_PROPRIETE",
  "representationActive": false,
  "fenteApplicable": false,
  "risquesContentieux": [],
  "baseJuridique": "Art. 731, 734, 738, 745-749, 751-755, 757 et s. Cciv",
  "formule": "Ordre DESCENDANTS actif + ...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

**Codes enum**
- `optionConjoint` : `USUFRUIT` | `QUART` | `null` (cas obligatoire pleine propriété ou pas de conjoint)
- `ordreActif` : `DESCENDANTS` | `PRIVILEGIES` | `ASCENDANTS_ORDINAIRES` | `COLLATERAUX_ORDINAIRES` | `CONJOINT_SEUL` | `DESHERENCE`
- `qualite` : `CONJOINT` | `DESCENDANT` | `PERE` | `MERE` | `FRERE_SOEUR` | `ASCENDANT_ORDINAIRE_PATERNEL` | `ASCENDANT_ORDINAIRE_MATERNEL` | `COLLATERAL_ORDINAIRE` | `REPRESENTANT`
- `modalite` : `PLEINE_PROPRIETE` | `USUFRUIT` | `NUE_PROPRIETE`

---

## Préoccupations transversales

- [x] **Outil décisionnel métier** : F-FA-24 = nouvel outil dédié à la dévolution successorale FR. Scan effectué : aucun outil existant ne le couvre. Le périmètre F-FA-24 (chantier successions) sera découpé en 8-10 SF, chacune = un outil pour une situation distincte (testament, donation, réduction, partage, indivision, rapport...).
- [x] **Auth / Principal** : pattern `OidcUser + Principal` réutilisé tel quel — aucun changement.
- [x] **Workspace context** : pattern `WorkspaceMemberRepository.findByUserAndPrimaryTrue` + gate FRANCE/DROIT_FAMILLE strictement copié de F-FA-17 — aucun changement.

Aucune des préoccupations critiques (auth, workspace, plans, navigation) n'est modifiée — pas besoin de smoke tests E2E.
