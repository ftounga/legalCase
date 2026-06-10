# F-266 — Cadrage cohérence (étape 0)

> Feature : **Conclusions V2 ⑥ — Traçabilité au survol + export « acte déposable »** — survoler un fait → la pièce ; un montant → le calcul ; un article → le texte. + export sur papier à en-tête du cabinet.
> Programme « Conclusions V2 », levier UX/esthétique n°3 (confiance + démo). Skill : `ai-skills/feature-coherence-challenger.md`. 2026-06-10.
> ⚠️ Dépendance directive PO : F-266 dépend de F-263 (montant→calcul). **F-263 a été clos à la fondation (doublon, `SF-263-00-coherence.md`)** → la traçabilité **montant→calcul est HORS PÉRIMÈTRE F-266**.

## Verdict : **GO avec ajustements** — périmètre **réduit** à (a) traçabilité **fait → pièce** au survol + (b) export à **en-tête cabinet** (per-export, sans persistance). **Montant→calcul ABANDONNÉ** (gadget sans ancrage déterministe, cf. F-263).

---

## Intention métier (1 phrase)

Renforcer la **confiance** (et la **démo**) de l'avocat sur l'acte généré : pouvoir **vérifier l'ancrage** d'une assertion d'un coup d'œil (survoler « Pièce n° 4 » → voir la pièce) et **exporter un document présentable** sur l'en-tête du cabinet plutôt qu'un markdown brut.

---

## Constat central — 3 ancrages promis, 1 seul est déterministe

F-266 (intention initiale) promettait 3 survols : **fait→pièce**, **montant→calcul**, **article→texte**. Le cadrage révèle des fiabilités très différentes :

| Ancrage promis | Marqueur dans le `content` markdown | Déterministe ? | Verdict |
|---|---|---|---|
| **Fait → pièce** | `Pièce n° X` (imposé par tous les prompts + SF-98-55 règle 2 + bordereau SF-98-57 ; `X` = `piece_number` persistant F-260) | ✅ **OUI** — motif regex `Pièce n°\s*\d+`, map 1:1 vers la pièce | **RETENU** |
| **Montant → calcul** | montant en clair dans la prose (« 29 100 € ») | ❌ **NON** — aucun marqueur outil ; même montant possible pour des chefs ≠ ; arrondis/reformulations | **ABANDONNÉ** (= F-263, clos doublon/gadget) |
| **Article → texte** | visa d'article (« art. L. 1235-3 ») | 🟡 partiel — extractible par regex MAIS « le texte » suppose une **base de textes légaux** (non présente au produit) ; lien vers Légifrance = hors périmètre, fiabilité variable | **DIFFÉRÉ backlog** (pas de source de texte légal fiable embarquée) |

➡️ **Seul `fait → pièce` est un ancrage déterministe et fiable** : c'est la valeur réelle, non-gadget, de la traçabilité au survol. Les deux autres survols seraient des gadgets (montant) ou supposeraient une capacité absente (texte légal) — exclus, conformément à l'invariant anti-gadget du programme.

---

## Workflow métier réel de l'avocat

1. L'avocat génère ses conclusions (F-98), les relit dans l'**aperçu « acte »** (F-259/F-264).
2. Il lit « … comme en atteste la **Pièce n° 4** … ». Pour vérifier, il doit aujourd'hui **quitter l'acte** et retrouver la pièce 4 dans la table des documents.
3. Besoin : **survoler** « Pièce n° 4 » → voir immédiatement **de quelle pièce il s'agit** (libellé + type) sans quitter l'acte → confiance.
4. Une fois satisfait, il veut **remettre un document présentable** : aujourd'hui l'export est volontairement **neutre** (« à reprendre sur l'en-tête du cabinet », cf. `DocxExportService` / `PdfExportService`). Besoin : **stamper son en-tête** (nom du cabinet, adresse, barreau) en tête de l'export.

**F-266 (réduit) couvre l'étape 3 (fait→pièce au survol) et l'étape 4 (export en-tête cabinet).**

## Cartographie features actuelles ↔ workflow

| Étape | Feature LegalCase | Statut |
|---|---|---|
| Génération + renvois « Pièce n° X » | F-98 + SF-98-55 (règle 2) + SF-98-57 (bordereau) | ✅ |
| Numéro de pièce persistant fiable | F-260 (`piece_number`) | ✅ |
| Aperçu « acte » formaté | F-259 / F-264 (`ConclusionDocumentComponent`) | ✅ |
| Liste des pièces (n°, libellé, type) disponible au front | `DocumentService.listDocuments` → `DocumentPieceSummary{pieceNumber,type,label}` (déjà chargée par `case-file-detail`) | ✅ |
| **Survol fait → pièce** | — aucun | ❌ **trou réel = valeur SF-266-01** |
| Export Word/PDF | SF-98-50 / SF-98-51 (neutre, sans en-tête) | ✅ mais sans en-tête |
| **Export à en-tête cabinet** | — aucun | ❌ **trou réel = valeur SF-266-02** |

## Challenge amont

- ✅ **Fait→pièce** : tous les pré-requis présents **côté frontend** : l'aperçu rend déjà le markdown en HTML (`ConclusionDocumentComponent`) ; la liste des pièces (`pieceNumber` + `type` + label, F-260) est déjà chargée par `case-file-detail`. → **frontend-only, aucun backend, aucun endpoint, aucune migration.**
- ✅ **Export en-tête** : les services d'export (`DocxExportService` / `PdfExportService`) existent et acceptent déjà un contenu + un nom de fichier. Ajouter un **bloc d'en-tête en tête de document** est une extension de présentation **client-side**.
- ⚠️ **Identité cabinet** : il n'existe **aucune** identité cabinet persistée (Workspace = `name` seul ; `/me` ne porte pas de raison sociale/adresse/barreau ; pas de table « cabinet profile »). Persister une identité cabinet = **nouvelle table/colonnes + endpoint + écran réglages** = décision data-model lourde et partiellement **irréversible** → **différée** (voir arbitrage). **Défaut réversible retenu** : en-tête **saisi à l'export** (champ libre multi-lignes), **sans persistance**.

## Challenge aval

- ✅ **Fait→pièce** : pur affichage (tooltip au survol d'un `Pièce n° X` reconnu) — aucune mutation, aucun effet sur le `content` ni l'export → **markdown-safe par construction** (on n'altère pas le markdown, on décore le rendu HTML de l'aperçu).
- ✅ **Export en-tête** : le bloc d'en-tête est **préfixé au rendu d'export uniquement** (Word/PDF), le `content` markdown stocké reste **inchangé** → non-régression versions/édition/aperçu. Round-trip garanti.
- ⚠️ Garde : l'en-tête saisi à l'export ne doit **jamais** être réinjecté dans le `content` (sinon pollution markdown + duplication à la prochaine génération). Il vit **uniquement** dans le document exporté.

## STOPs / invariants anti-gadget pour la mini-spec

1. **Montant→calcul exclu** (F-263 doublon/gadget) ; **article→texte différé** (pas de base légale embarquée). F-266 = **fait→pièce** + **export en-tête** uniquement.
2. **Fait→pièce déterministe** : seul le motif `Pièce n°\s*\d+` est reconnu ; un numéro **sans pièce correspondante** (pièce supprimée, hors liste) → **pas de tooltip** (jamais d'info inventée), pas d'erreur.
3. **Aucune altération du markdown** : la décoration de survol porte sur le **HTML rendu de l'aperçu**, pas sur le `content`. Export/versions/édition non régressés.
4. **En-tête à l'export, jamais persisté dans le content** : champ libre saisi au moment de l'export, vit dans le seul fichier exporté.
5. **Identité cabinet persistée = backlog** : aucune nouvelle table/colonne workspace dans F-266 (décision irréversible non prise en aveugle).
6. **3 domaines** : le mécanisme (regex pièce + tooltip + préfixe d'export) est **uniforme** aux 3 domaines ; aucun contenu métier.

## Arbitrages (réversibles — décidés par défaut, tracés)

| Décision | Choix par défaut | Pourquoi | Réversible |
|---|---|---|---|
| Périmètre traçabilité | **fait→pièce seul** | seul ancrage déterministe ; montant=F-263 clos, article=pas de source légale | oui (rouvrir article→texte si base légale ajoutée) |
| Identité cabinet | **en-tête saisi à l'export, non persisté** | évite une table/écran réglages irréversible construite en aveugle ; livre la valeur tout de suite | oui (backlog : persister un « profil cabinet » réutilisable) |
| Surface du survol | **aperçu « acte » (`ConclusionDocumentComponent`) en lecture** | c'est la surface de confiance/démo ; pas l'éditeur textarea | oui |

## Découpage proposé (impact écran → étape 0 bis requise pour les deux)

- **SF-266-01 (frontend-only)** — Traçabilité **fait→pièce au survol** dans l'aperçu « acte ». `ConclusionDocumentComponent` reçoit la liste des pièces (`pieceNumber`,`type`,`label`) ; après rendu markdown→HTML, décore les `Pièce n° X` reconnus d'un `title`/tooltip (« Bulletin de paie — Pièce n° 4 »). Aucun backend.
- **SF-266-02 (frontend-only)** — Export **à en-tête cabinet**. Champ libre « En-tête du cabinet » (multi-lignes) saisi avant export ; `DocxExportService`/`PdfExportService` préfixent un bloc d'en-tête au document. `content` inchangé. Aucun backend.

## Décision finale

**GO avec ajustements.** Périmètre F-266 = **SF-266-01 (fait→pièce au survol)** + **SF-266-02 (export en-tête cabinet, non persisté)**, **frontend-only**, markdown-safe, uniforme aux 3 domaines. **Montant→calcul abandonné** (F-263 clos doublon) ; **article→texte différé** (pas de base légale) ; **identité cabinet persistée différée** (backlog). F-266 : `Backlog` → `À faire`.
