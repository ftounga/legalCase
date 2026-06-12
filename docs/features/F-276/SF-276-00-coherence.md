# SF-276-00 — Cadrage cohérence (F-276 : Sommaire / navigation par section de l'acte)

> Skill : `ai-skills/feature-coherence-challenger.md` — Programme « Conclusions V4 » (UX ⑥).

## 1. Workflow métier réel de l'avocat

1. L'avocat ouvre le dossier, complète le stade procédural, lance une analyse.
2. Il calcule ses outils décisionnels, puis **génère le projet de conclusions** (F-98).
3. Il **ouvre la page dédiée « Projet de conclusions »** (F-267) — page pleine largeur.
4. Il **relit** l'acte (5 à 15 pages), section par section : en-tête / faits / discussion (moyens) / dispositif.
5. Il **édite** (F-264 markdown enrichi + aperçu), régénère une section (F-265 co-rédaction), exporte (F-266).
6. Il dépose / transmet l'acte.

Point de douleur ciblé (audit 2026-06-12, friction UX #1) : l'**étape 4 (et 5)**. Sur un acte long,
l'avocat **scrolle à l'aveugle** pour retrouver « la prescription », « le dispositif », « tel moyen ».
Pas de table des matières ni de saut de section, alors que l'acte EST structuré en titres (`##`/`###`).

## 2. Cartographie des features existantes sur ce workflow

| Étape | Feature existante | État |
|-------|-------------------|------|
| Génération | F-98 | livré |
| Page dédiée | F-267 (`case-file-conclusions-page`) | livré |
| Rendu « feuille » markdown→HTML | F-259 (`conclusion-document`) | livré |
| Édition markdown + aperçu live | F-264 | livré |
| Co-rédaction par **section** (parsing `parseMarkdownSections`) | F-265 | livré |
| Export Word/PDF + en-tête cabinet | F-266 | livré |

**Le découpage en sections existe déjà** : `parseMarkdownSections(content)` (déterministe, titres `##`/`###`)
est exporté par `conclusions-section.component.ts` et utilisé par F-265 pour la co-rédaction. F-276 **réutilise
exactement cette fonction** — aucune nouvelle logique de parsing.

## 3. Challenge cohérence

### Amont (les pré-requis existent-ils ?)
- ✅ Contenu structuré en titres markdown par construction (gardes d'ossature DISCUSSION).
- ✅ Parsing en sections existant et testé (F-265).
- ✅ Rendu HTML (`marked`) produit des `<h2>`/`<h3>` dans le même ordre que `parseMarkdownSections`.
- ✅ Convention de saut déjà présente : `document.getElementById(id)` +
  `scrollIntoView({ behavior: 'smooth', block: 'start' })` (`case-file-detail`, `synthesis`, stepper).

### Aval (la sortie est-elle exploitable ?)
- Le sommaire est une aide à la lecture/navigation : sa sortie est l'œil de l'avocat sur la bonne
  section. Aucune étape ultérieure n'en dépend ; aucun risque de casser export / versions / round-trip
  markdown (purement présentationnel, ne touche pas au `content` stocké).

### Anti-gadget
- **Pas un doublon** de F-265 : F-265 *régénère* une section (action IA) ; F-276 *navigue* vers une section
  (lecture). Responsabilités distinctes, même source (`parseMarkdownSections`).
- **Valeur réelle** : sur un acte de 10+ pages, retrouver le dispositif sans scroller = gain d'ergonomie
  concret et quotidien. C'est la friction #1 de l'audit.
- **Risque de surcharge** : faible si le sommaire reste discret (masqué quand l'acte est court).

## 4. Verdict

**GO** — frontend-only, réutilise l'existant, aucune nouvelle table / endpoint / dépendance.

## 5. Invariants anti-gadget que la mini-spec doit respecter

1. **Réutiliser `parseMarkdownSections`** — ne pas réimplémenter un parsing concurrent.
2. **Ne jamais muter le `content` markdown** — sommaire et ancres purement HTML/présentation
   (round-trip export/versions intact, comme l'annotation pièces F-266).
3. **Fonctionne en lecture ET en édition** : les deux modes rendent l'acte via `<app-conclusion-document>` ;
   héberger sommaire + ancres dans ce composant → portée uniforme (exigée).
4. **Discrétion** : pas de sommaire si l'acte n'a pas au moins 2 sections.
5. **Saut conforme** à la convention produit (`scrollIntoView` smooth/start).
6. **Aucune régression** des specs existantes de `conclusion-document` (testids, sanitization,
   décoration pièces F-266).
