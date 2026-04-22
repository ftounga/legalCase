# Mini-spec — F-146 / SF-146-03 Propagation `sourceRef` aux exports DOCX + PDF

## Identifiant · `F-146 / SF-146-03`
## Date · `2026-04-23` · Branche · `feat/SF-146-03-propagation-exports`

## Objectif
Propager la richesse `sourceRef` (doc·pièce·page) dans les exports DOCX et PDF de la synthèse, pour que l'avocat qui imprime le dossier retrouve la même précision de source qu'à l'écran.

## Contexte
- SF-146-01 : backend produit `sourceRef` dans chaque `AnalysisItem`.
- SF-146-02 : frontend affiche `<app-source-ref>` cliquable dans Faits / Points juridiques / Risques.
- SF-146-03 : aujourd'hui, `DocxExportService` affiche `[Source : item.source]` (legacy string uniquement). `PdfExportService` n'affiche même pas la source. On harmonise.

### Pourquoi les outils décisionnels NE sont PAS intégrés ici
Les outils décisionnels (F-DT-07 à F-FA-07) utilisent déjà le mécanisme **F-IA-03** (`SourceExplanation` + popover cliquable avec QA/check/chat). C'est un système distinct et complémentaire de `<app-source-ref>`. La cohabitation est voulue — F-IA-03 gère les multi-sources contextuelles (document OU question OU checklist OU chat), `<app-source-ref>` gère uniquement la citation directe d'une pièce de document.

Enrichir F-IA-03 pour porter `pieceLabel + pageStart + pageEnd` au niveau de chaque `SourceExplanation` de type `DOCUMENT` est une évolution distincte — à ajouter au backlog si le besoin se confirme (voir "Hors scope").

## Comportement nominal

### A — DocxExportService
Pour chaque `AnalysisItem` (faits, points, risques) :
- Si `item.sourceRef` présent + `pieceType`/`pieceLabel`/`pageStart` → rendre `[Source : <docName> · <pieceTypeLabel> « <pieceLabel> » · p. X[-Y]]` (italique).
- Sinon fallback sur le comportement actuel `[Source : item.source]`.

### B — PdfExportService
Aujourd'hui le PDF n'affiche pas du tout la source dans les items numérotés. On ajoute une ligne secondaire discrète sous chaque item quand `sourceRef` ou `source` est disponible : `<docName> · <pieceTypeLabel> « <pieceLabel> » · p. X[-Y]` (petit, gris, italique).

### C — Pas de rupture
- Analyses pré-F-146 (sourceRef null) → format legacy `[Source : contrat.pdf — « extrait »]`
- Analyses post-F-146 → format enrichi `[Source : dossier.pdf · Contrat « Contrat Dupont » · p. 1-2]`

## Critères d'acceptation
- [ ] Utilitaire partagé `formatSourceRef(item)` qui retourne la chaîne de format approprié (enrichi ou legacy)
- [ ] `DocxExportService` utilise ce helper pour les 3 sections (Faits, Points, Risques)
- [ ] `PdfExportService` affiche une ligne secondaire source sous chaque item de Faits / Points juridiques / Risques
- [ ] Tests unitaires helper : sourceRef complet, sourceRef avec label absent, sourceRef partiel (pageEnd null), legacy source seule, rien du tout
- [ ] Tests PDF/DOCX : pas de régression des tests existants
- [ ] Full build frontend vert, suite tests verte

## Plan de test minimal
- U-01 : `formatSourceRef` renvoie `"dossier.pdf · Contrat « Contrat Dupont » · p. 1-2"` quand sourceRef complet
- U-02 : utilise `pieceTypeLabel` quand `pieceLabel` vide
- U-03 : affiche `"p. 3"` quand pageStart === pageEnd
- U-04 : fallback legacy `"contrat.pdf"` quand sourceRef null + source renseignée
- U-05 : renvoie `null` quand sourceRef + source tous deux absents
- U-06 : extrait appendu en italique `" — « … »"` si présent
- U-07 : DocX export : texte généré contient le nouveau format enrichi quand sourceRef disponible
- U-08 : PDF export : ligne source secondaire présente dans les items

## Tables / endpoints / composants impactés
### Frontend
- `core/utils/format-source-ref.ts` (nouveau) — helper pur
- `core/services/docx-export.service.ts` — utilise le helper
- `core/services/pdf-export.service.ts` — utilise le helper, ajoute ligne secondaire
- Tests associés

### Pas impacté
- Backend (pas de changement)
- Composant `<app-source-ref>` (continue à servir l'écran)
- F-IA-03 / SourceExplanation (reste indépendant — cohabitation)

## Impact par domaine métier (FR + BE × 3 domaines)
| Domaine | Impact | Adaptation |
|---|---|---|
| **Droit du travail** (FR + BE) | Exports DOCX/PDF affichent doc·pièce·page. Valeur immédiate pour les dossiers prud'homaux exportés pour signature client. | Aucune — le helper est agnostique au domaine |
| **Immigration** (FR + BE) | Idem pour les exports de dossiers OQTF, recours, titre de séjour (pièces précises citées dans le PDF du recours). | Aucune |
| **Famille** (FR + BE) | Idem pour divorce / garde / liquidation. | Aucune |

Le helper `formatSourceRef` utilise `documentPieceTypeLabel` (SF-145-09) qui couvre déjà les 25 types × 3 domaines. Pas de code spécifique par domaine.

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|---|---|---|
| `<app-source-ref>` (SF-146-02) | Le helper partage la logique de formatage mais ne fusionne pas avec le composant (le composant doit rester cliquable, le helper est pour les exports statiques). | Intégré — logique partagée |
| F-IA-03 popovers outils décisionnels | Cohabitation maintenue. Les outils décisionnels continuent à utiliser `SourceExplanation` + popover. Pas de régression. | Non applicable ici |
| PDF `exportChecklist` (checklist procédurale) | La checklist n'a pas de `sourceRef` (use `critereCode` pour F-IA-03). Non concernée. | Non applicable |
| PDF `buildFicheSynthese` (fiche prud'homale, etc.) | Ce sont des outputs d'outils décisionnels dédiés (pas de `AnalysisItem`). | Non applicable |

### Nouveau helper partagé
Le helper `formatSourceRef` est un utilitaire pur réutilisable partout où l'on veut afficher une source en texte (non cliquable). Zones possibles de réutilisation future :
- Export PDF checklist procédurale → non applicable (critereCode, pas sourceRef)
- Export PDF fiche prud'homale → non applicable (outputs d'outil décisionnel)
- Tout futur export / impression qui manipule `AnalysisItem` → helper déjà disponible
Pattern concurrent identifié : la logique inline dans `docx-export.service.ts` (3 copies identiques actuellement). Le helper consolide.

## Préoccupations transversales
- **Auth / Principal** : aucun impact.
- **Workspace context** : aucun impact.
- **Plans / limites** : aucun impact.
- **Navigation / routing** : aucun impact.

## Hors scope
- Intégration F-IA-03 + `sourceRef` (enrichir les `SourceExplanation` de type DOCUMENT avec piece+page) → à évaluer en backlog si feedback utilisateur le demande.
- Refonte visuelle des exports (fonts, layout, couleurs) — purement additif.
- Rendu cliquable dans le PDF/DOCX — inapplicable sur un fichier statique.
- Backfill des analyses pré-F-146 — pas traité, chaque re-analyse enrichit automatiquement.
