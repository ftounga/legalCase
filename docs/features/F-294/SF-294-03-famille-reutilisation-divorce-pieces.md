# Mini-spec — F-294 / SF-294-03 — Famille : socle de pièces par réutilisation de `DIVORCE_PIECES`

> Étape 1. Étape 0 couverte par `SF-294-00-coherence.md` (mécanisme F-294, invariant #5 anti-doublon). Backend pur.

---

## Identifiant

`F-294 / SF-294-03`

## Feature parente

`F-294` — Référentiel de pièces attendues par situation procédurale

## Statut

`draft`

## Branche Git

`feat/SF-294-03-famille-reutilisation-divorce-pieces`

## Date de création

2026-06-16

---

## Objectif

> En une phrase.

Faire bénéficier les dossiers **Droit de la famille** du socle de pièces attendues + de la canonisation F-194, **en réutilisant le référentiel `DIVORCE_PIECES` existant** (DB-first + fallback `DivorceChecklistReferentiel`) via `getExpectedPieces`, **sans dupliquer** le contenu en `EXPECTED_PIECES` ni inventer de pièce.

---

## Comportement attendu

### Cas nominal

1. `LegalReferentialService.getExpectedPieces("DROIT_FAMILLE", country, procedureStage)` :
   - **délègue** à `getDivorcePieces(country)` (qui fait déjà DB-first sur `DIVORCE_PIECES` + fallback `DivorceChecklistReferentiel`) ;
   - **mappe** chaque `DivorcePiece(code, label, country, description, obligatoire)` → `ExpectedPiece(code, label, country, stages = List.of(), obligatoire, ordre = index)`.
2. Les pièces divorce étant **génériques à la procédure** (pas de découpage par stade), elles sont renvoyées **quel que soit `procedureStage`** (`stages` vide ⇒ génériques, conforme à la sémantique F-294 « pièce générique »).
3. En aval, **rien d'autre ne change** : injection du socle dans `EnrichedAnalysisService`, canonisation dans `PieceManquanteAlignmentService`, overlay statut F-194, affichage F-289 — tout le mécanisme SF-294-01 s'applique tel quel aux dossiers famille.

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| `getDivorcePieces(country)` renvoie vide (pays non couvert / DB+Java vides) | Socle vide → comportement actuel (100 % LLM), aucune erreur |
| Exception dans le mapping | Fail-open : log warn + liste vide (le run de synthèse aboutit) |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `DIVORCE_PIECES` / `getDivorcePieces` / `DivorceChecklistReferentiel` | Oui | **Réutilisés** (source unique de vérité famille). **Pas de nouveau seed `EXPECTED_PIECES` Famille** (anti-doublon, invariant #5 étape 0). |
| `getExpectedPieces` (SF-294-01) | Oui | Ajout d'une branche `DROIT_FAMILLE` ; branches `DROIT_DU_TRAVAIL` (DB `EXPECTED_PIECES` + fallback Travail) **inchangées**. |
| Travail FR (SF-294-01) | Non | Intouché. |
| Immigration | Non | **Hors scope** — `IMMIGRATION_PIECES` est indexé par titre, pas par stade → F-294 inadapté (STOP documenté). |
| Auth / Workspace / Plans / Navigation | Non | Aucun. |

### Décision

- [x] Étendu à la cible (Famille FR + BE par réutilisation).
- [x] Non applicable Immigration (mismatch sémantique titre vs stade — STOP anti-gadget).

---

## Conformité F-IA-04 / Pré-fill IA

- [x] **Non applicable** — SF backend pure (extension d'un service de résolution), pas d'outil décisionnel, pas de formulaire, pas d'écran.

---

## Critères d'acceptation

- [ ] **CA1** : un dossier **Famille FR** reçoit comme socle les pièces `DIVORCE_PIECES` FR (mappées en `ExpectedPiece`), quel que soit le stade.
- [ ] **CA2** : idem **Famille BE** avec les pièces `DIVORCE_PIECES` BE.
- [ ] **CA3 (DB-first — CA11 hérité)** : la résolution passe par `getDivorcePieces` → une entrée DB `DIVORCE_PIECES` prime sur le fallback Java ; override workspace respecté.
- [ ] **CA4 (zéro duplication)** : **aucune** nouvelle entrée `EXPECTED_PIECES` Famille seedée (`git diff` : aucune migration de seed Famille) ; le contenu reste la seule source `DIVORCE_PIECES`.
- [ ] **CA5 (additif/canonisation hérités)** : le socle famille est additif (union LLM ∪ socle) et la canonisation par libellé exact s'applique (une pièce divorce « obtenue » ne réapparaît pas).
- [ ] **CA6 (non-régression)** : Travail FR (DB `EXPECTED_PIECES`) et Immigration (inchangé) se comportent comme avant ; build complet vert.
- [ ] **CA7 (fail-open)** : exception ou pays non couvert → liste vide, run abouti.

---

## Périmètre

### Hors scope

- **Immigration** (SF-294-04) : STOP — mécanisme par titre déjà en place, F-294 par stade inadapté.
- **Travail BE** (SF-294-02) : contenu juridique belge à sourcer/valider — non inventé ici.
- **Pièces famille par stade** (mesures provisoires vs fond) : génériques en V1 ; raffinement futur possible via `EXPECTED_PIECES` si signal terrain.
- Toute modification de `DIVORCE_PIECES` (contenu inchangé).

---

## Technique

### Endpoint(s)

> Aucun. Extension interne de `getExpectedPieces`.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `legal_referentials` | SELECT (via `getDivorcePieces`, inchangé) | Aucun INSERT, aucune nouvelle entrée. |

### Migration Liquibase

- [x] **Non applicable** (aucun seed nouveau — réutilisation pure).

### Backend — à modifier

- `LegalReferentialService.getExpectedPieces(...)` : ajouter la branche `"DROIT_FAMILLE".equals(legalDomain)` → `getDivorcePieces(country)` mappé en `List<ExpectedPiece>` (stages vides, ordre = index). Branche Travail inchangée. Fail-open conservé.

---

## Plan de test

### Tests unitaires (`LegalReferentialServiceTest`)

- [ ] `getExpectedPieces("DROIT_FAMILLE","FRANCE",null)` → pièces divorce FR mappées (CA1).
- [ ] `getExpectedPieces("DROIT_FAMILLE","BELGIQUE","FOND")` → pièces divorce BE (CA2).
- [ ] Famille : un `procedureStage` quelconque renvoie les **mêmes** pièces (génériques) (CA1).
- [ ] DB `DIVORCE_PIECES` prime sur fallback Java (CA3) — réutilise le test existant `getDivorcePieces`.
- [ ] Pays non couvert / exception → liste vide, pas d'erreur (CA7).
- [ ] Non-régression Travail FR : `getExpectedPieces("DROIT_DU_TRAVAIL","FRANCE","FOND")` inchangé (CA6).

### Tests d'intégration

- [ ] Dossier Famille FR : `analysis_result.pieces_manquantes` matérialisé inclut au minimum les pièces divorce FR (réutilise le pattern `ExpectedPiecesSeedIT`).

### Isolation workspace

- [x] Applicable (indirect) — via `getDivorcePieces`/`legal_referentials` déjà isolé (override workspace). Inchangé.

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale** (Auth/Workspace/Plans/Navigation).
- [x] **Aucun smoke E2E** — backend isolé.

---

## Dépendances

- SF-294-01 (mécanisme `getExpectedPieces`, canonisation) — **Done** (mergée #1687).
- `DIVORCE_PIECES` / `DivorceChecklistReferentiel` — livrés (migration 067).

---

## Notes et décisions

- **Décision (2026-06-16) — réutilisation, pas duplication.** Le contenu pièces famille existe déjà (`DIVORCE_PIECES`, source de vérité DB + fallback Java). Re-seeder en `EXPECTED_PIECES` créerait deux sources divergentes (dette). On délègue donc `getExpectedPieces` Famille à `getDivorcePieces`. Respecte l'invariant #5 (pas de 2ᵉ taxonomie / pas de doublon) et le principe « ne pas réinventer un mécanisme existant ».
- **Pièces génériques** : les pièces divorce ne sont pas découpées par stade → `stages = List.of()` (génériques). Cohérent avec la sémantique F-294.
- **Immigration STOP** : `IMMIGRATION_PIECES` indexé par titre de séjour, pas par stade → F-294 inadapté ; documenté hors scope (anti-gadget).
- **Travail BE** : contenu juridique non disponible dans le repo → non inventé ; remonté au PO.
