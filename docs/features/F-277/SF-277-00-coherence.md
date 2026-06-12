# F-277 — Cadrage cohérence (étape 0)

> Feature : **Éditeur WYSIWYG des conclusions** (active l'Option B différée de F-264). Origine : double audit conclusions (2026-06-12) — friction UX #2 « markdown brut déroutant pour un avocat non technique ». Parkée 🔴 par la vague Conclusions V4 (lift élevé) → cadrage demandé par le PO.

## Verdict : **GO avec ajustements forts — conditionné à un spike de round-trip markdown**

## Intention
Remplacer/compléter l'édition actuelle (textarea **markdown brut** + barre d'outils + aperçu live, F-264) par une **surface d'édition WYSIWYG** (type TipTap/ProseMirror) où l'avocat voit l'acte mis en forme **pendant** qu'il l'édite, sans syntaxe `##`/`**`.

## Workflow métier cible (avocat)
Ouvrir le projet de conclusions → relire l'acte → **corriger/compléter directement le texte mis en forme** (titres, gras, listes, dispositif) → régénérer une section au besoin (F-265) → comparer les versions (F-280) → exporter (F-281). L'édition doit être **aussi naturelle que Word**, sans rupture vers de la syntaxe technique.

## Étape 0 — cohérence fonctionnelle

### Amont (les pré-requis existent-ils ?)
- ✅ **F-264 livré** : éditeur markdown enrichi (barre d'outils `markdown-toolbar` + aperçu `conclusion-document` via `marked`). Point de départ à faire évoluer.
- ✅ Le `content` est stocké/édité comme **markdown** (`draftContent`, PATCH `.../content`).
- ✅ Signal terrain PO réel (adoption) — **ce n'est pas un gadget** : un avocat non technique bute sur `##`/`**`.

### Aval (la sortie est-elle exploitable par les étapes suivantes ?) — **LE point dur**
Le `content` markdown est la **source de vérité** consommée par **5 systèmes** :
| Consommateur | Fichier | Ce qu'il exige du markdown |
|---|---|---|
| Export Word/PDF | `docx-export.service.ts`, `pdf-export.service.ts` (`marked`) | structure markdown propre |
| Sommaire / navigation (F-276) | `conclusion-sections.util.ts` (`parseMarkdownSections` sur `##`/`###`) | **titres intacts** |
| Co-rédaction par section (F-265) | idem + regenerate in-place | **frontières de sections stables** |
| Diff de versions (F-280) | `conclusion-diff.util.ts` (diff **ligne à ligne**) | **pas de reformatage parasite** |
| Alerte placeholders (SF-266-03) | `conclusion-export-content.util.ts` (`extractPlaceholders`, regex `[…]`) | **`[…]` préservés** |

➡️ **Conséquence** : un WYSIWYG n'est cohérent que s'il garantit un **aller-retour markdown sans perte ni reformatage** (markdown → doc WYSIWYG → markdown **identique** pour le contenu non modifié). Sinon il **casse** silencieusement : titres perdus (sommaire/co-rédaction KO), bruit de diff (F-280 illisible), placeholders mutés (alerte SF-266-03 faussée), montants/renvois « Pièce n° X » altérés.

### Verdict
**GO avec ajustements forts.** Valeur métier réelle, pré-requis présents, MAIS la cohérence aval **dépend entièrement** de la fidélité du round-trip markdown → **exiger un spike technique de de-risking AVANT le dev complet** (si le round-trip ne peut être garanti proprement, repli = STOP/différer, l'éditeur F-264 reste).

## Invariants anti-gadget / anti-régression (que la mini-spec DEVRA respecter)
1. **Markdown reste la source de vérité stockée** : le WYSIWYG est une *surface d'édition*, pas un nouveau format. `content` reste du markdown ; aucun changement de schéma/stockage/export.
2. **Round-trip garanti, prouvé par test** : suite de non-régression `markdown → WYSIWYG → markdown == original` sur un **corpus d'actes réels** (DURAND/LEMAIRE + cellules variées), en **gate CI**. Doivent survivre intacts : titres `##/###`, renvois « Pièce n° X », placeholders `[…]`, montants, dispositif.
3. **Zéro bruit de diff (F-280)** : éditer puis ré-ouvrir sans modifier ne doit produire **aucun** diff.
4. **Sections détectables (F-276/F-265)** : `parseMarkdownSections` doit continuer de découper correctement → co-rédaction et sommaire intacts.
5. **Export Word/PDF inchangés** (F-266/F-281) : consomment le markdown produit.
6. **Fallback markdown** : conserver un accès à l'édition **source markdown** (toggle), pour les cas limites et la confiance.
7. **Poids maîtrisé** : dépendance (TipTap + sérialiseur markdown) lazy-loadée sur la route conclusions, pas dans le bundle principal.
8. **Co-existence F-264** : la barre d'outils et l'aperçu live restent cohérents (l'aperçu peut devenir redondant en WYSIWYG — à arbitrer en 0 bis).

## Périmètre
- **Inclus** : surface WYSIWYG d'édition de l'acte, sérialisation markdown bidirectionnelle, gate de round-trip, fallback source.
- **Exclus** : changement de format de stockage ; réécriture de l'export/diff/sections (ils restent sur markdown) ; collaboration temps réel.

## Risques
- **Round-trip imparfait** (risque #1) → de-risking par spike obligatoire.
- **Poids/perf** de la lib → lazy-load + budget.
- **Redondance aperçu live** (F-264) → arbitrage écran (étape 0 bis).

## Spike round-trip markdown — RÉSULTAT (2026-06-12)
Test exécuté : acte représentatif (titres `##/###`, gras, listes, citation, « Pièce n° 3 », montants, placeholders `[…]`) passé dans le **sérialiseur markdown canonique de TipTap** (`prosemirror-markdown` : `defaultMarkdownParser` → `defaultMarkdownSerializer`).

| Invariant | Résultat |
|---|---|
| Titres `##/###` (F-276/F-265) | ✅ préservés |
| « Pièce n° X », montants, **gras**, citations | ✅ préservés |
| **Placeholders `[…]`** | ❌ **échappés `\[…\]`** (`[Nom et qualité de l'avocat]` → `\[Nom et qualité de l'avocat\]`) |
| **Listes `- `** | ❌ converties en `* ` |
| Round-trip identique | ❌ non |

**Deux défauts, dont un critique :**
1. 🔴 **Échappement des `[…]`** → l'avocat voit des `\` dans l'acte ; **SF-266-03 (alerte placeholders) et le bloc signature sont corrompus**. Inacceptable en l'état.
2. 🟠 **`- ` → `* `** → le markdown change **à chaque sauvegarde même sans édition** → **bruit de diff massif** (F-280) + churn de contenu (viole l'invariant 3).

**Mitigation identifiée (faisable) :** un **`MarkdownSerializer` custom** :
- ne PAS échapper `[`/`]` (ou modéliser les placeholders comme un nœud/mark sérialisé verbatim) ;
- forcer le marqueur de liste `-` (option `bulletListMarker`) ;
- **gate CI d'idempotence** : `markdown → WYSIWYG → markdown == markdown` sur un corpus d'actes réels (DURAND/LEMAIRE + cellules variées), sinon build rouge.

**Conséquence sur le verdict :** GO **reste possible** mais **uniquement** avec ce sérialiseur custom + le gate d'idempotence — ce n'est **pas** un « drop-in TipTap ». Le spike **confirme** le classement 🔴 (effort réel, risque concret maîtrisable). Sans engagement sur le sérialiseur custom + gate → **différer** (F-264 markdown enrichi reste opérant).

## Suite gouvernance
F-277 est **à impact écran** (remplace la surface d'édition) → **étape 0 bis (cohérence écran) OBLIGATOIRE** avant la mini-spec, puis : mini-spec → readiness → **spike round-trip** → dev → review → PR. Statut PRODUCT_SPEC : reste **À faire** (GO conditionné au spike), passe `Bloqué` si le spike échoue.
