# Mini-spec — F-283 / SF-283-02 — Vagues de pièces (ajout incrémental lisible)

> Feature parente : **F-283**. Étape 0 : `SF-283-00-coherence.md` (GO). Étape 0 bis : `SF-283-00b-ux-coherence.md` (GO avec ajustements). Statut : `ready` · Date : 2026-06-12 · Branches : `feat/SF-283-02-wave-back` (backend) // `feat/SF-283-02-wave-front` (frontend).

## Objectif
Rendre **lisible l'ajout incrémental de pièces** : « N nouvelles pièces depuis la dernière analyse → les voici, et voici l'action » — via une **carte « vague de pièces »** en tête de l'onglet Dossier, au lieu d'un avertissement plat et d'une ré-analyse opaque.

## Comportement attendu

### Cas nominal
1. Le backend calcule, pour un dossier, la **vague de pièces en attente** = les documents `created_at` **après** la fin de la dernière analyse dossier réussie (`AnalysisJob` `CASE_ANALYSIS` `status=DONE`, horodatage `updatedAt`).
2. Si **aucune analyse réussie** n'existe encore → ce n'est pas une « vague » mais un dossier neuf : la carte ne s'affiche pas (l'onboarding analyse initial est déjà couvert ailleurs). `pendingCount = 0`, `analyzedAt = null`.
3. Si `pendingCount > 0` → une **carte « vague de pièces »** s'affiche en tête de l'onglet **Dossier** : badge compteur, libellé « N pièce(s) ajoutée(s) depuis votre dernière analyse (du JJ/MM/AAAA) », liste compacte des pièces récentes (nom + date `JetBrains Mono`, max 5 + « et N autres »), CTA primaire navy **« Relancer l'analyse »** (route vers l'action d'analyse existante de l'onglet Analyse).
4. Si `pendingCount = 0` → **pas de carte** (état « à jour » discret, pas de bloc vide criard).
5. L'avertissement plat existant (onglet Analyse, `synthesis-outdated`) est **simplifié** en un renvoi « Voir les pièces récentes » → bascule sur l'onglet Dossier (une seule source structurée du delta).

### Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| Dossier d'un autre workspace | accès refusé | 404 |
| Dossier inexistant | 404 | 404 |
| Aucune analyse réussie | `pendingCount=0, analyzedAt=null` (pas d'erreur) | 200 |

## Analyse de cohérence transversale
- **Aucune nouvelle pipeline d'analyse** : SF-283-02 **lit** un delta déjà calculable et **route** vers l'action existante (`CaseAnalysisService` côté front, `lancerAnalyse`). Zéro nouveau job, zéro nouvel appel IA, zéro gate.
- **Anti-doublon** : la logique front `outdatedDocuments` (delta vs synthèse) est **remplacée par l'endpoint serveur** (source unique, fiable, indépendante de l'état chargé du front). L'avertissement Analyse est simplifié, pas dupliqué.
- **Outils décisionnels** : aucun touché.

## Conformité F-IA-04 (SF frontend décisionnelle)
- [x] **Non applicable** — carte de lisibilité, pas d'outil décisionnel.

## Champs IA à extraire (pré-remplissage)
- [x] **Aucun** — calcul déterministe (dates), pas d'IA.

## Critères d'acceptation
- [ ] Endpoint `GET /api/v1/case-files/{id}/pieces-wave` → `{ analyzedAt, pendingCount, pendingPieces: [{ documentId, filename, createdAt }] }`, **isolation workspace** (404 si workspace A lit dossier B).
- [ ] `analyzedAt` = `updatedAt` du dernier `AnalysisJob` `CASE_ANALYSIS` `DONE` ; `null` si aucun.
- [ ] `pendingPieces` = documents `created_at > analyzedAt`, triés par `created_at` desc.
- [ ] Si `analyzedAt = null` → `pendingCount = 0` (dossier neuf, pas une vague).
- [ ] **Carte `app-pieces-wave-card`** en tête de l'onglet Dossier, visible **uniquement** si `pendingCount > 0`, **conforme `DESIGN_SYSTEM.md`** (navy/or, Merriweather/Inter/JetBrains Mono, 4px), CTA primaire navy « Relancer l'analyse » émettant un `Output` que le détail relie à l'action d'analyse existante.
- [ ] L'avertissement plat `synthesis-outdated` (Analyse) simplifié en renvoi vers Dossier (anti-doublon).
- [ ] **Revue visuelle PO** de la carte (beauté = critère).
- [ ] Aucun nouveau job d'analyse / appel IA introduit.

## Périmètre — Hors scope V1 (explicite)
- Analyse **ciblée uniquement sur la vague** (delta-only re-analysis) → **V1.1** (V1 route vers la ré-analyse dossier existante).
- Groupement des pièces par « vague » horodatée multiple → **V1.1** (V1 = une vague = tout ce qui suit la dernière analyse).
- Marquage manuel « cette pièce ne nécessite pas d'analyse » → **V1.1**.

## Technique
### Contrat API (figé — parallélisation back//front)
- `GET /api/v1/case-files/{caseFileId}/pieces-wave` → `PiecesWaveResponse`
- `PiecesWaveResponse { analyzedAt: Instant|null, pendingCount: int, pendingPieces: PendingPiece[] }`
- `PendingPiece { documentId: UUID, filename: String, createdAt: Instant }`

### Tables
| Table | Opération | Notes |
|---|---|---|
| (aucune) | **lecture seule** | Calcul dérivé de `analysis_jobs` + `documents`. **Aucune migration.** |

### Endpoints
| Méthode | URL | Rôle |
|---|---|---|
| GET | `/api/v1/case-files/{caseFileId}/pieces-wave` | LAWYER (workspace) |

### Composants Angular
- `PiecesWaveCardComponent` (`app-pieces-wave-card`, carte, onglet Dossier en tête) — pièce design.
- `PiecesWaveService` + `PiecesWave` model.
- Édition `case-file-detail.component.html` : insertion carte en tête Dossier + simplification de l'avertissement Analyse.

## Plan de test
### Unitaires (backend)
- [ ] `PiecesWaveService` — `analyzedAt=null` → `pendingCount=0` ; delta correct (docs après analyse) ; tri desc ; isolation.
### Intégration
- [ ] `GET` → 200 forme correcte avec/sans analyse ; **404 isolation workspace** (A ≠ B).
### Isolation workspace
- [x] Applicable — testée.
### Jest (frontend)
- [ ] Carte absente si `pendingCount=0` ; rendue avec compteur + liste (max 5 + « et N autres ») si `>0` ; CTA émet l'Output ; états chargement/erreur soignés.

## Analyse d'impact
### Préoccupations transversales
- [x] **Aucune nouvelle** : pas de nouvelle route Angular (composant interne onglet Dossier), pas d'auth/Principal modifié (isolation `case_file` réutilisée), pas de plan/limite, **pas de nouvelle pipeline/job**, aucun outil décisionnel.
- Composants impactés : `case-file-detail.component.html` (carte en tête Dossier + simplification avertissement Analyse), `case-file-detail.component.ts` (Output → action analyse existante ; suppression de la dépendance front `outdatedDocuments` au profit de l'endpoint).
### Smoke E2E
- [x] Aucun smoke bloquant. Couvert par IT backend + Jest.
