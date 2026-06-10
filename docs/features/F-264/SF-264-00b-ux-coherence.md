# F-264 — Cadrage cohérence écran (étape 0 bis)

> Éditeur document natif (Option A : markdown enrichi + aperçu live). Skill : `ai-skills/screen-coherence-challenger.md`. 2026-06-10.

## Verdict : **GO avec ajustements**

## Intention + comportement visible
En mode édition des conclusions (onglet Décision, `conclusions-section`), l'avocat dispose d'une **barre d'outils markdown** (titre, gras, italique, liste, citation) et d'un **aperçu formaté « acte » live** à côté de la zone d'édition, au lieu du textarea brut seul.

## Rappel étape 0
F-264 **GO avec ajustements**, décision PO **Option A** (markdown enrichi + aperçu live, sans nouvelle dépendance).

## Parcours écran réel
1. Onglet **Décision** → `conclusions-section`. Acte généré (`DONE`, cycle `DRAFT`).
2. L'avocat clique **« Modifier »** → mode édition.
3. **[NOUVEAU]** Zone d'édition (textarea markdown) **+ barre d'outils** (insère les marqueurs) **+ aperçu formaté live** (rendu acte) côte à côte.
4. Il enregistre (`PATCH …/content`, inchangé) → retour en lecture (rendu acte existant).
5. Validation / export / versions : inchangés.

## Cartographie
| Zone | Statut |
|---|---|
| `conclusions-section` mode édition (textarea) | ✅ existant (SF-98-49) |
| **Barre d'outils markdown + aperçu live** | 🆕 à ajouter |
| Rendu acte (`ConclusionDocumentComponent`) | ✅ existant (réutilisé pour l'aperçu) |

## Position candidate
Dans `conclusions-section`, **mode édition** uniquement : remplacer le bloc textarea seul par un layout **éditeur + aperçu** (côte à côte sur large écran, empilé/bascule sur étroit), avec une barre d'outils au-dessus de l'éditeur.

## Challenge placement
✅ Cohérent — l'édition vit déjà dans `conclusions-section` ; on enrichit ce même mode, pas un nouvel écran.

## Challenge lisibilité séquence
✅ La séquence (générer → relire rendu → modifier → enregistrer → rendu) est inchangée ; l'aperçu live **renforce** la continuité (l'avocat voit l'acte pendant qu'il édite). Ajustement : sur écran étroit, prévoir une **bascule éditeur/aperçu** (pas de côte-à-côte illisible).

## Challenge charge écran
✅ La section conclusions reste un bloc unique ; le mode édition gagne 2 sous-éléments (barre + aperçu). Pas de nouveau bloc primaire. Attention densité sur mobile → bascule.

## Challenge état final / continuité
✅ État terminal inchangé (« projet de conclusions » relu/validé/exporté). L'aperçu améliore la boucle d'édition sans la rallonger.

## Ajustements IA requis (mini-spec)
1. Barre d'outils markdown (insertion de marqueurs sur la sélection/curseur).
2. Aperçu formaté live (réutiliser `ConclusionDocumentComponent` / `marked`), synchronisé au `draftContent`.
3. Layout responsive : côte-à-côte (large) / bascule (étroit).

## Invariants anti-surcharge
1. Pas de nouvel écran ni bloc primaire : enrichissement du mode édition existant.
2. Réutiliser le rendu acte existant pour l'aperçu (pas de duplication de style).
3. Markdown-safe uniquement (cf. étape 0) ; `content` reste markdown.
4. Responsive : jamais de côte-à-côte illisible sur étroit.

## Décision finale
**GO avec ajustements.** Enrichissement naturel du mode édition de `conclusions-section` (barre + aperçu live), réutilisant le rendu acte existant, sans nouvel écran ni dépendance. Ajustement responsive (bascule sur étroit). Mini-spec peut démarrer.

## MAJ parcours de référence
`docs/business/parcours-ecran-dossier.md` : le mode édition des conclusions (onglet Décision) passe d'un textarea brut à un **éditeur markdown enrichi + aperçu formaté « acte » live**, le `content` restant markdown (export/versions intacts).
