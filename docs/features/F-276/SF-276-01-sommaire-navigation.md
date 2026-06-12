# SF-276-01 — Sommaire / navigation cliquable par section de l'acte

> Programme « Conclusions V4 » — UX ⑥ (F-276). Frontend-only. Étapes 0 / 0bis : **GO**.

## Objectif (une phrase)

Afficher en tête de la feuille de conclusions un **sommaire cliquable** des sections de l'acte (titres
`##`/`###`) qui, au clic, **fait défiler** la page jusqu'à la section visée, en lecture comme en édition.

## Comportement nominal

- Le composant `conclusion-document` (hôte unique du rendu en lecture et de l'aperçu d'édition) :
  1. parse le `content` markdown avec **`parseMarkdownSections`** (réutilisé, ré-exporté) pour obtenir la
     liste ordonnée des titres ;
  2. injecte des **ancres déterministes** (`id="cd-heading-{i}"`) sur les `<h2>`/`<h3>` du HTML rendu, dans
     le même ordre que les sections parsées ;
  3. rend, **au-dessus de la feuille**, un bloc « Sommaire » listant chaque section (libellé = titre, indenté
     pour les `###`) en `<button>` cliquables.
- Au clic sur une entrée, on `document.getElementById('cd-heading-{i}')` puis
  `scrollIntoView({ behavior: 'smooth', block: 'start' })` (convention produit).
- Le niveau (`##` vs `###`) est déduit du nombre de `#` du titre parsé pour l'indentation visuelle.

## Cas d'erreur / limites

- **< 2 sections** → aucun sommaire affiché (pas de bruit). 0 ou 1 section = pas de navigation utile.
- **content vide / null** → composant déjà vide (état géré par le parent), pas de sommaire.
- **id introuvable au clic** (cas théorique) → `getElementById` renvoie `null`, on ne fait rien (pas de crash).
- **Round-trip markdown** : le `content` n'est jamais muté ; seules les ancres `id` et le bloc sommaire sont
  ajoutés au HTML/au template → export Word/PDF et versions inchangés.

## Critères d'acceptation (vérifiables)

1. `parseMarkdownSections` est exporté et réutilisé (pas de parsing dupliqué).
2. Pour un acte à ≥ 2 sections, un bloc `[data-testid="conclusions-summary"]` est rendu avec une entrée
   `[data-testid="summary-item"]` par section, dans l'ordre du document.
3. Les `<h2>`/`<h3>` rendus portent `id="cd-heading-{i}"` correspondant aux entrées du sommaire.
4. Le clic sur l'entrée `i` appelle `getElementById('cd-heading-{i}')` puis `scrollIntoView` smooth/start.
5. Acte à 0 ou 1 section → **aucun** bloc sommaire.
6. Le `content` (markdown) n'est pas modifié par l'ajout des ancres/sommaire.
7. Indentation : entrée d'un titre `###` visuellement décalée vs un `##`.
8. Aucune régression des specs existantes de `conclusion-document` (testids, sanitization XSS, pièces F-266).
9. Identique en mode lecture et en aperçu d'édition (même composant).

## Plan de test minimal (Jest)

- **≥ 2 sections** → sommaire rendu, n entrées = n sections, libellés = titres.
- **1 section / 0 section** → pas de sommaire.
- **ids injectés** : `h2`/`h3` rendus portent `cd-heading-0..n` dans l'ordre.
- **clic** : mock `getElementById` → élément factice avec `scrollIntoView` jest.fn → vérifier l'appel
  `{ behavior: 'smooth', block: 'start' }` et le bon id.
- **niveau/indentation** : entrée d'un `###` porte la classe/attribut de sous-niveau.
- **non-mutation** : `component.content` inchangé après rendu.
- **non-régression** : la suite existante de `conclusion-document.component.spec.ts` reste verte
  (sanitization, pièces F-266, footer, testids).

Isolation workspace : N/A (frontend pur, aucun appel réseau, aucune donnée cross-tenant).

## Composants impactés

| Fichier | Changement |
|---------|-----------|
| `conclusion-document.component.ts` | importer `parseMarkdownSections` ; `summarySections` computed ; injection des `id` sur `<h2>/<h3>` du HTML rendu ; `jumpTo(i)` (scrollIntoView) |
| `conclusion-document.component.html` | bloc « Sommaire » au-dessus de la feuille (si ≥ 2 sections) |
| `conclusion-document.component.scss` | styles du sommaire (compact, indentation `###`) |
| `conclusion-document.component.spec.ts` | tests sommaire + ancres + saut + non-mutation |

**Aucun backend, aucune table, aucun endpoint, aucune route, aucune dépendance npm nouvelle.**

## Hors périmètre

- Panneau latéral sticky (Option B écartée 0bis).
- Surlignage de la section active au scroll (scroll-spy) — non demandé, gadget potentiel.
- Sommaire dans l'export Word/PDF (F-266 / F-281 couvrent l'export ; ici navigation **écran** seulement).
- Toute modification du backend, du `content` stocké, des versions ou de la co-rédaction (F-265).

## Préoccupations transversales

- Auth / Principal : non.
- Workspace context : non.
- Plans / limites : non.
- Navigation / routing : **non** — saut intra-page via `scrollIntoView`, aucune route ni guard touché.
- Outil décisionnel métier : non.

→ Aucune préoccupation transversale cochée ⇒ pas de smoke E2E requis (feature de présentation pure).
