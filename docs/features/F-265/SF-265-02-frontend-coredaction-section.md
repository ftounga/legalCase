# Mini-spec — F-265 / SF-265-02 — Frontend : co-rédaction IA par section

> Programme Conclusions V2 / F-265. Frontend. Étape 0 GO + 0 bis GO avec ajustements. Consomme le contrat figé de SF-265-01 (mergé, PR #1624).

## Identifiant
`F-265 / SF-265-02`

## Statut
`ready`

## Branche
`feat/SF-265-02-frontend-coredaction-section`

## Objectif
> Dans le **mode édition** des conclusions, permettre à l'avocat de sélectionner une **section** de l'acte et de la **régénérer/renforcer** via une **instruction libre**, le markdown régénéré remplaçant **en place** la section dans le brouillon (éditable avant enregistrement).

## Comportement attendu

### Cas nominal
1. Conclusions `DONE` + `DRAFT` → « Modifier » → mode édition F-264.
2. Zone **« Co-rédaction IA »** (en tête de la colonne éditeur, repliable) :
   - **select** des sections détectées (parsing déterministe des titres markdown `##`/`###` du `draftContent`, libellé = texte du titre) ;
   - **input instruction** (« renforce la prescription ») ;
   - bouton **« Régénérer cette section »**.
3. Au clic → `POST …/versions/{versionId}/sections/regenerate` `{ sectionMarkdown, instruction }`.
4. Sur succès → le `regeneratedMarkdown` **remplace en place** le bloc de la section dans `draftContent` (string replace borné au bloc `[début titre, début titre suivant[`). Aperçu + textarea se rafraîchissent. Snackbar « Section régénérée — relisez puis enregistrez ».
5. L'avocat ajuste puis **« Enregistrer »** (PATCH content existant). Inchangé.

### Cas d'erreur / bords
| Situation | Comportement |
|---|---|
| Aucune section détectée (pas de titre `##`/`###`) | select vide + bouton désactivé + libellé « Aucune section détectée » |
| Instruction vide | bouton désactivé |
| Régénération en cours | zone + actions désactivées (`regenerating`) |
| `409`/`502`/réseau | snackbar erreur (message backend) ; `draftContent` inchangé |
| Section disparue du brouillon entre sélection et clic (édition manuelle) | si le `sectionMarkdown` n'est plus retrouvé → snackbar « Section introuvable, resélectionnez » (pas de remplacement hasardeux) |

## Critères d'acceptation
- [ ] Parsing des sections : titres `##`/`###` → liste {titre, début, fin} déterministe, ordre du document.
- [ ] Régénération : appel endpoint avec `sectionMarkdown` = bloc exact + `instruction`.
- [ ] Remplacement **in-place** : seul le bloc de la section sélectionnée change ; le reste du `draftContent` est byte-identique.
- [ ] Round-trip markdown : `draftContent` reste markdown valide (aperçu + export non régressés — aucune transformation hors remplacement de bloc).
- [ ] Désactivations : bouton désactivé si pas de section / instruction vide / régénération ou enregistrement en cours.
- [ ] Erreurs backend remontées en snackbar ; brouillon inchangé en cas d'échec.
- [ ] Zone visible **uniquement** en mode édition.

## Tables / endpoints / composants impactés
- **Service** : `ConclusionsService.regenerateSection(caseFileId, versionId, sectionMarkdown, instruction)` → `POST …/sections/regenerate`.
- **Composant** : `conclusions-section.component.ts` — signaux `regenerating`, `selectedSectionTitle`, `instruction` ; computed `sections()` (parsing) ; méthode `regenerateSection()` ; helper `replaceSectionInDraft()`.
- **Template** : zone « Co-rédaction IA » dans la colonne éditeur (`@if (editing())`).
- **Aucun** changement de modèle/route/guard.

## Hors périmètre
- Réordonnancement / suppression / ajout de bloc (backlog si signal).
- Édition d'une section hors mode édition.
- Backend (SF-265-01, mergé).

## Plan de test (Jest)
- [ ] `sections()` : parse correctement plusieurs `##`/`###`, gère 0 titre (liste vide).
- [ ] `replaceSectionInDraft()` : remplace le bon bloc, laisse le reste intact ; section introuvable → no-op + flag.
- [ ] `regenerateSection()` : appelle le service avec le bloc + instruction ; succès → draft mis à jour + snackbar ; erreur → snackbar, draft inchangé.
- [ ] Désactivations (bouton) selon état.

## Analyse d'impact transversal
- **Auth/workspace/plan/navigation** : aucun (réutilise le contexte dossier existant ; l'endpoint backend porte déjà le gate coût).
- **Outil décisionnel** : N/A.
- **Smoke E2E** : non requis (pas d'auth/workspace/navigation). Couvert par Jest + non-régression export.

## Dépendances
- SF-265-01 (endpoint, mergé #1624), F-264 (éditeur/aperçu), SF-98-49 (enregistrement) — `done`.

## Notes
- **Pre-merge endpoint check** : l'endpoint `POST …/sections/regenerate` existe sur master (SF-265-01 mergé d48a4952). ✓
