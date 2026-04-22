# Mini-spec — F-146 / SF-146-01 Backend `SourceRef` + prompts IA enrichis

## Identifiant · `F-146 / SF-146-01`
## Date · `2026-04-23` · Branche · `feat/SF-146-01-backend-source-ref`

## Objectif
Enrichir le pipeline IA pour que chaque citation produite dans les analyses (document, case, enriched) référence non plus uniquement `"contrat.pdf"` mais `document + pièce + page(s)`. Débloque l'affichage UX précis dans SF-146-02/03.

## Contexte
Aujourd'hui `AnalysisItem.source` est une simple string (`"contrat.pdf"`). Pour un dossier prud'homal avec un upload composite de 20 pages incluant contrat + CNI + SMS + attestations, l'avocat voit juste "dossier.pdf" dans les faits/points juridiques — impossible de remonter à la preuve exacte.

F-145 a livré la détection des pièces (`document_pieces` table + enum 25 types × 3 domaines). F-146 exploite cette richesse dans les analyses IA.

F-IA-03 fait déjà ça pour les **10 outils décisionnels** (F-DT, F-IM, F-FA) via `source_explanations` — mais la synthèse (F-55/F-56) et le reste restent au niveau doc.

## Comportement nominal

### A — Prompt pipeline enrichi
Le prompt système de `CaseAnalysisService`, `EnrichedAnalysisService`, `DocumentAnalysisService` reçoit désormais **en input supplémentaire** :
- Liste des pièces de chaque document du dossier : `[{docName, pieces: [{id, type, label, pageStart, pageEnd}]}]`
- Instruction : *"Pour chaque fait / point juridique / risque / question que tu cites, remplis le champ `sourceRef` avec `{documentName, pieceType, pieceLabel, pageStart, pageEnd}` identifiant précisément la preuve. Utilise les marqueurs `=== PAGE N ===` et la liste des pièces ci-dessus."*

### B — Format JSON de sortie enrichi
Chaque item passe de :
```json
{ "texte": "…", "source": "contrat.pdf", "extrait": "…" }
```
à :
```json
{
  "texte": "…",
  "source": "contrat.pdf",              // rétrocompat conservée
  "sourceRef": {
    "documentName": "dossier_complet.pdf",
    "pieceType": "CONTRAT",
    "pieceLabel": "Contrat de travail Dupont",
    "pageStart": 1,
    "pageEnd": 2
  },
  "extrait": "…"
}
```

### C — Parseur côté backend
`CaseAnalysisResponse.extractItemList` parse le nouveau champ `sourceRef` (fail-open : absent = null). Nouveau record `SourceRef(documentName, pieceType, pieceLabel, pageStart, pageEnd)`.

### D — Pas de rupture
Les analyses **déjà en DB** n'ont pas de `sourceRef` → affichage legacy conservé (le `source` string continue à fonctionner). Pas de migration de données.

## Critères d'acceptation
- [ ] Nouveau record `SourceRef` dans `fr.ailegalcase.analysis`
- [ ] `AnalysisItem` enrichi d'un champ `sourceRef: SourceRef` (nullable)
- [ ] `CaseAnalysisResponse.extractItemList` parse le champ
- [ ] `CaseAnalysisService`, `EnrichedAnalysisService`, `DocumentAnalysisService` : prompt système étendu avec la liste des pièces du dossier + instruction explicite
- [ ] Les pièces du dossier sont résolues via `DocumentPieceRepository.findByDocument_IdOrderByOrderIndexAsc` et injectées dans le contexte
- [ ] DTO `CaseAnalysisResponse` expose `sourceRef` dans l'API
- [ ] Tests unitaires : parse d'item avec/sans `sourceRef` (6 cas)
- [ ] Tests : prompt contient bien la liste des pièces formatée
- [ ] Full suite backend verte

## Plan de test minimal
- U-01 : `extractItemList` parse un item avec `sourceRef` complet
- U-02 : `extractItemList` parse un item sans `sourceRef` (legacy) → `sourceRef = null`
- U-03 : `extractItemList` parse un item avec `sourceRef` partiel (manquant pageEnd) → null fields OK
- U-04 : prompt `CaseAnalysisService` contient la section `"Pièces des documents"` avec la liste formatée
- U-05 : aucun document avec pièces détectées → prompt mentionne section vide mais reste valide
- U-06 : fallback — ancien format JSON (pas de `sourceRef`) continue à parser correctement

## Tables / endpoints / composants impactés
### Backend
- `SourceRef.java` (nouveau record)
- `AnalysisItem.java` (+champ `sourceRef`, rétrocompat 3-args préservée)
- `CaseAnalysisResponse.java` (parseur enrichi)
- `CaseAnalysisService.java` + `EnrichedAnalysisService.java` + `DocumentAnalysisService.java` (prompts enrichis)
- Helper partagé (ou dans `LegalDomainPromptBuilder`) : `buildPiecesContextBlock(caseFileId)` retourne la section texte à injecter
- Tests associés

### Pas impacté
- Migration DB : aucune (`analysis_result` est un `text`/`json` qui accepte naturellement le nouveau champ)
- Frontend : aucun changement dans cette SF (SF-146-02 s'en charge)
- F-IA-03 : inchangé — il continue à gérer les sources pour les outils décisionnels via son propre mécanisme `source_explanations`. Cohabitation.

## Impact par domaine métier (FR + BE × 3 domaines)
| Domaine | Impact | Adaptation |
|---|---|---|
| **Droit du travail** (FR + BE) | Les faits cités depuis un contrat, un bulletin, un SMS pointent désormais à la pièce + page. Gain immédiat sur les dossiers composites (ex: dossier prud'homal). | Aucune spécifique |
| **Immigration** (FR + BE) | Les points juridiques cités depuis un TITRE_DE_SEJOUR, RECEPISSE, DECISION_OQTF référencent précisément. Critique pour les recours contentieux où la preuve page précise est essentielle. | Aucune spécifique |
| **Famille** (FR + BE) | Citations précises depuis JUGEMENT_DIVORCE, ACTE_MARIAGE, LIVRET_FAMILLE. | Aucune spécifique |

La liste des types disponibles dans le prompt est déjà filtrée par domaine via SF-145-09 (`buildSystemPrompt(legalDomain)`). Aucun travail supplémentaire côté domaine dans cette SF.

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|---|---|---|
| F-55/F-56 pipeline analyse | Enrichi directement (les prompts sont dans ces services) | Intégré |
| F-IA-03 sources outils décisionnels | Cohabitation — F-IA-03 reste en place, F-146 s'applique aux sections de synthèse. Pas de conflit, les deux mécanismes sont complémentaires. | Intégré |
| F-145 pièces | Source principale des données — le prompt consomme `document_pieces` | Intégré |
| Format JSON d'analyse | Extension rétrocompatible (nouveau champ optionnel) | Intégré |

## Préoccupations transversales
- **Plans / limites** : aucun impact (prompts légèrement plus longs → +5-10% tokens input, négligeable)
- **Auth / Principal** : aucun impact
- **Workspace context** : le prompt consomme les pièces du case file via workspace (isolation préservée)
- **Navigation / routing** : aucun impact

## Hors scope
- Composant frontend `<source-ref>` cliquable → **SF-146-02**
- Intégration dans les outils décisionnels (checklist, ancienneté, etc.) → **SF-146-03**
- Backfill des analyses existantes → pas traité, les anciennes analyses gardent leur affichage legacy jusqu'à une ré-analyse manuelle
- Support multilingue des labels de pièces → les labels produits par SF-145 sont déjà en français
