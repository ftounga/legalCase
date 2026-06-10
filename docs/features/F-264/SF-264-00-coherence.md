# F-264 — Cadrage cohérence (étape 0)

> Feature : **Éditeur document natif des conclusions** — éditer l'acte dans un rendu formaté, pas un textarea brut.
> Programme « Conclusions V2 », levier UX n°1. Skill : `ai-skills/feature-coherence-challenger.md`. 2026-06-10.

## Verdict : **GO avec ajustements** (décision PO sur l'approche éditeur)

---

## Intention métier (1 phrase)

Permettre à l'avocat de **relire et corriger ses conclusions dans un rendu « acte »** (titres numérotés, dispositif formaté, bordereau lisible) plutôt que dans un textarea de markdown brut, pour une expérience professionnelle et un contrôle fin avant dépôt.

---

## Workflow métier réel de l'avocat

1. LegalCase génère le projet de conclusions (markdown).
2. L'avocat **relit** l'acte — aujourd'hui : un rendu formaté existe (`ConclusionDocumentComponent`, façon acte).
3. Il **corrige** — aujourd'hui : il bascule sur un **textarea de markdown brut** (SF-98-49), perd le rendu pendant l'édition.
4. Il valide (cycle DRAFT→VALIDATED→DEPOSITED, SF-98-52), exporte Word/PDF (SF-98-50/51).

**F-264 améliore l'étape 3 : éditer SANS quitter le rendu acte (ou avec un aperçu live).**

## Cartographie features actuelles ↔ workflow

| Étape | Feature / composant | Statut |
|---|---|---|
| 1. Génération (markdown) | F-98 | ✅ |
| 2. Rendu formaté « acte » (lecture) | `ConclusionDocumentComponent` (marked → HTML, style acte) | ✅ **déjà là** |
| 3. **Édition** | textarea brut (SF-98-49) `PATCH …/content` | 🟡 fonctionnel mais brut |
| 4. Versions / export | SF-98-52 / 50 / 51 (tokenisent le markdown) | ✅ |

## Position de la nouvelle feature

F-264 = remplacer/enrichir l'**édition** (étape 3). Le contenu reste **markdown** (ne pas changer le modèle de stockage — l'export et les versions en dépendent).

## Challenge amont

✅ Tout l'amont existe : contenu markdown (F-98), rendu formaté (`ConclusionDocumentComponent`), tokenisation partagée (`markdown-tokens.ts`). **Aucun trou amont.**

## Challenge aval

✅ Export Word/PDF (SF-98-50/51) et versions (SF-98-52) **survivent si l'éditeur produit du markdown valide** (ils tokenisent le `content` markdown, sans hardcode de format). **Invariant aval : le `content` édité reste du markdown round-trippable** — pas de format enrichi (couleur/taille) non sérialisable.

## STOPs / pré-requis

Aucun STOP. Le seul arbitrage = **l'approche éditeur** (décision PO), car elle détermine l'effort, la dépendance et le risque de round-trip.

## Décision PO requise — approche éditeur

| Option | Principe | Round-trip | Dépendance | Effort | Avis |
|---|---|---|---|---|---|
| **A — Markdown enrichi + aperçu live** | Édition markdown (textarea/contenteditable) **avec barre d'outils** (titre/gras/liste…) et **aperçu formaté live** (réutilise `ConclusionDocumentComponent` / `marked`) côte à côte ou en bascule | **Garanti** (markdown↔markdown) | **Aucune** (marked déjà là) | Faible/Moyen | ✅ Sûr, forte valeur immédiate, zéro perte |
| **B — Vrai WYSIWYG (TipTap/ProseMirror)** | Édition **dans** le rendu acte, sérialisation markdown via schéma restreint « markdown-safe » | Géré par schéma (risque résiduel) | **Nouvelle lib** (~30 KB+, adapter markdown) | Élevé | ⚖️ meilleure UX « waouh », mais lourd + risque round-trip |
| **C — Statu quo enrichi minimal** | Garder le textarea, ajouter juste un aperçu formaté à côté | Garanti | Aucune | Très faible | ⚠️ gain modeste |

## Invariants anti-gadget pour la mini-spec

1. **Markdown préservé** : le `content` sauvegardé reste du markdown valide → export/versions intacts (test de non-régression export obligatoire).
2. **Pas de format non sérialisable** (couleur/taille custom) en V1 — sous-ensemble markdown-safe.
3. **Cycle de vie respecté** : édition seulement en `DONE` + `DRAFT` (SF-98-52) ; régénération écrase comme aujourd'hui.
4. **Réutiliser le rendu existant** (`ConclusionDocumentComponent`) plutôt que dupliquer le style acte.
5. **Design system** : titres Merriweather, corps Inter, mono JetBrains ; palette marine/or (déjà appliquée au rendu acte).

## Décision finale

**GO avec ajustements.** Amont et aval complets (markdown round-trippable, rendu acte déjà existant, export tokenisé). Le seul arbitrage est **l'approche éditeur**.

### ✅ DÉCISION PO (2026-06-10) : **Option A — markdown enrichi + aperçu live « acte ».**
Édition avec **barre d'outils** (titre/gras/liste/citation…) + **aperçu formaté live** réutilisant le rendu acte existant (`ConclusionDocumentComponent`/`marked`). Round-trip markdown **garanti**, **aucune nouvelle dépendance**. Le `content` reste markdown (export/versions intacts). Étape 0 bis requise (impact écran : barre d'outils + panneau aperçu dans `conclusions-section`). F-264 : `Backlog` → `À faire`.
