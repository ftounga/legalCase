# Mini-spec — F-264 / SF-264-01 — Éditeur markdown enrichi + aperçu live « acte »

> Programme Conclusions V2 / F-264. Décision PO Option A. **Frontend-only** (le `content` reste markdown ; endpoint d'édition inchangé).

## Identifiant
`F-264 / SF-264-01`

## Statut
`ready` (étape 0 GO + 0 bis GO avec ajustements)

## Branche
`feat/SF-264-01-editeur-markdown-apercu`

## Objectif
> En mode édition des conclusions, remplacer le textarea brut par un **éditeur markdown avec barre d'outils + aperçu formaté « acte » live**, sans changer le stockage (markdown) ni l'export.

## Comportement attendu

### Cas nominal
1. Conclusions `DONE` + cycle `DRAFT` → l'avocat clique « Modifier ».
2. Mode édition : **zone d'édition markdown** (textarea/contenteditable) + **barre d'outils** (Titre H2/H3, **Gras**, *Italique*, liste, citation) qui insèrent le marqueur markdown sur la sélection/au curseur + **aperçu formaté live** (rendu « acte ») synchronisé à la saisie.
3. Layout : **côte à côte** sur large écran ; **bascule éditeur/aperçu** sur écran étroit.
4. « Enregistrer » → `PATCH …/conclusions/versions/{versionId}/content` (inchangé) → retour en lecture (rendu acte existant).

### Cas d'erreur / bords
| Situation | Comportement |
|---|---|
| Markdown invalide/partiel pendant la saisie | l'aperçu rend au mieux (marked tolérant) ; pas de crash |
| Sauvegarde échoue | `MatSnackBar` (comportement SF-98-49 existant conservé) |
| Écran étroit | bascule éditeur/aperçu (pas de côte-à-côte illisible) |

## Analyse de cohérence transversale
- **Réutilise** `ConclusionDocumentComponent` (rendu acte) / `marked` pour l'aperçu — pas de duplication de style.
- **Stockage inchangé** : `content` markdown ; export Word/PDF (SF-98-50/51) et versions (SF-98-52) **intacts** (markdown-safe only).
- **Pattern barre d'outils markdown** : réutilisable ailleurs (corpus style, blog…) → composant isolable.

### Résultat du scan
| Cible | Applicable | Traitement |
|---|---|---|
| Export Word/PDF | Oui | **Non-régression** : markdown préservé → exports intacts (test) |
| Versions / cycle de vie | Oui | Édition seulement `DONE`+`DRAFT` (inchangé) |
| Rendu acte `ConclusionDocumentComponent` | Oui | Réutilisé pour l'aperçu |

### Décision
- [x] Markdown-safe only ; stockage inchangé ; rendu réutilisé.

## Conformité F-IA-04
- [x] **Non applicable** — pas de composant décisionnel ; enrichissement UX de l'éditeur de conclusions.

## Critères d'acceptation
- [ ] En mode édition : barre d'outils insère correctement les marqueurs markdown (titre/gras/italique/liste/citation) sur la sélection ou au curseur.
- [ ] Aperçu formaté live synchronisé à la saisie (rendu « acte »).
- [ ] Layout responsive : côte à côte (large) / bascule (étroit).
- [ ] « Enregistrer » envoie le même `content` markdown (endpoint inchangé) ; lecture/rendu inchangés.
- [ ] **Non-régression export** : un acte édité via le nouvel éditeur s'exporte Word/PDF sans perte (markdown valide).
- [ ] Édition uniquement en `DONE` + `DRAFT` (inchangé).

## Périmètre
### Hors scope
- Vrai WYSIWYG TipTap/ProseMirror (Option B) — backlog si signal.
- Formats non sérialisables en markdown (couleur, taille custom).
- Backend (aucun changement : `content` markdown, endpoint existant).

## Technique
- **Frontend uniquement.** `conclusions-section` (mode édition) : barre d'outils + aperçu.
- Composant barre d'outils markdown isolable (ex. `markdown-toolbar`) opérant sur le `draftContent` (insertion de marqueurs).
- Aperçu : réutiliser `ConclusionDocumentComponent` (ou `marked.parse`) lié au `draftContent` (signal).
- Aucune nouvelle dépendance (`marked` déjà présent).
- `OnPush` + `markForCheck()` si nécessaire.

## Plan de test (Jest)
- [ ] Barre d'outils : chaque action insère le bon marqueur (gras enveloppe la sélection, titre préfixe la ligne, etc.).
- [ ] Aperçu : reflète le `draftContent` (markdown → rendu).
- [ ] Sauvegarde : appelle l'endpoint avec le `content` markdown (inchangé).
- [ ] Responsive : bascule éditeur/aperçu sur étroit (classe/état).

## Analyse d'impact
- [x] **Aucune préoccupation transversale** (frontend ; pas d'auth/workspace/plan/navigation ; pas d'endpoint/schéma).
### Smoke E2E
- [ ] Aucun (couvert par Jest + non-régression export).

## Dépendances
- SF-98-49 (édition), SF-98-50/51 (export), `ConclusionDocumentComponent` (rendu) — `done` (réutilisés).

## Notes
- **Round-trip garanti** : markdown ↔ markdown ; l'aperçu et l'export consomment le même `content`.
- Composant barre d'outils isolé → réutilisable (corpus style, blog) en évolution.
