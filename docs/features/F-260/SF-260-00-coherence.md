# F-260 — Cadrage cohérence (étape 0)

> Feature : **Numérotation persistante & ordre des pièces communiquées.**
> Amont de SF-98-57 (bordereau). Skill : `ai-skills/feature-coherence-challenger.md`. Date : 2026-06-09.

## Verdict : **GO** (1 point de cohérence à trancher en mini-spec : granularité document vs pièce)

---

## Intention métier (1 phrase)

Donner à chaque pièce communiquée un **numéro stable et persistant**, maîtrisable dans son ordre par l'avocat, afin que les renvois « Pièce n° X » des actes (conclusions F-98, fiche prud'homale) restent valides dans le temps malgré l'ajout/suppression de documents.

---

## Workflow métier réel de l'avocat

> Source : pratique standard du contentieux (numérotation des pièces communiquées, stable d'un jeu de conclusions au suivant) + audit F-98 (LEMAIRE).

1. L'avocat verse les pièces au dossier (upload).
2. LegalCase identifie chaque pièce (type + intitulé) — F-145.
3. L'avocat **numérote** ses pièces dans un ordre choisi (1, 2, 3…) — *ordre stable, qui ne bouge pas quand il ajoute une pièce n° 12 plus tard*.
4. Il rédige conclusions / fiche en renvoyant à ces numéros.
5. Il communique pièces + bordereau ; le numéro « Pièce n° 3 » désigne toujours la même pièce à tous les stades.
6. À un jeu ultérieur, il **ajoute** des pièces (n° 13, 14…) sans renuméroter les précédentes.

**La feature couvre l'étape 3 (numéro stable + ordre maîtrisé), pré-requis des renvois étape 4.**

---

## Cartographie features actuelles ↔ workflow

| Étape workflow | Feature LegalCase | Statut |
|---|---|---|
| 1. Upload | F-43 | ✅ |
| 2. Identification pièce (type + `label`) | F-145 (`DocumentPiece.type/label` éditables) | ✅ |
| **3. Numéro stable + ordre maîtrisé** | **F-260 (la feature)** ; aujourd'hui numéro **calculé à la volée** (`createdAt DESC`, non persisté) | ❌ Manquant |
| 4. Renvois « Pièce n° X » | F-98 (`loadNumberedPieces`) + fiche prud'homale (`buildPiecesList`) | ✅ (mais sur numéro instable) |
| (aval) Bordereau | SF-98-57 | 🟡 Backlog (dépend de F-260) |

---

## Position de la feature

F-260 s'insère à **l'étape 3**, entre F-145 (pièce identifiée) et les actes (F-98 / fiche). Elle remplace la numérotation volatile par un numéro **persistant**.

État technique (reconnaissance) :
- `DocumentPiece` (`document_pieces`) : `type`, `label`, `pageStart/End`, `orderIndex` (ordre **dans** un document composite), **pas de numéro de pièce dossier**.
- `CaseConclusionService.loadNumberedPieces` : itère `documents` triés `createdAt DESC` → pour chaque, ses `DocumentPiece` triées `orderIndex ASC` → `number++` volatile.
- `PrudhomeFicheService.buildPiecesList` : 1 **document** = 1 pièce, numérotée par position (`createdAt DESC`).
- **Divergence existante** : F-98 numérote au niveau **pièce** (`DocumentPiece`), la fiche au niveau **document**. F-260 doit choisir l'unité de numérotation et **réconcilier** les deux consommateurs.

---

## Challenge amont

**Question** : les étapes avant F-260 existent-elles ?

- ✅ Étape 1 (upload) et 2 (F-145 : pièces identifiées, `label`/`type` éditables, `orderIndex`) sont livrées. La matière à numéroter existe.
- **Aucun trou amont bloquant.** F-260 est une **fiabilisation** (persistance + ordre), pas une création ex-nihilo. Le pré-requis (un modèle de pièces) est là.

## Challenge aval

**Question** : la sortie (numéro persistant) est-elle exploitable ?

- ✅ Oui, par les **deux** consommateurs existants : `loadNumberedPieces` (F-98) et `buildPiecesList` (fiche prud'homale), qui basculeront vers la lecture du numéro persistant. Et par SF-98-57 (bordereau).
- **Invariant aval** : la bascule doit être **simultanée** sur les deux consommateurs (sinon F-98 et la fiche numéroteraient différemment) → cohérence transversale à traiter dans la mini-spec.

---

## STOPs / pré-requis à ajouter au backlog

Aucun STOP. Points à figer en **mini-spec** (pas des features à part) :
1. **Granularité** : numéroter au niveau **pièce** (`DocumentPiece`) — c'est l'unité communiquée et déjà celle de F-98 ; la fiche prud'homale (aujourd'hui par document) s'aligne dessus (un document mono-pièce = 1 entrée).
2. **Schéma de stabilité** : numéro attribué une fois et **conservé** ; suppression → soit on laisse un trou, soit renumérotation explicite par l'avocat (à trancher — recommandation : conserver le numéro, gérer la suppression sans glissement automatique).
3. **Ordre maîtrisé** : réordonnancement par l'avocat (impact écran → étape 0 bis).
4. **Backfill** : attribuer un numéro persistant aux pièces existantes (migration de données, ordre courant `createdAt`/`orderIndex` comme base initiale).

---

## Invariants anti-gadget pour la mini-spec

1. **Stabilité réelle** : ajouter/supprimer une pièce ne doit pas renuméroter silencieusement les autres — sinon la feature ne corrige pas le défaut qu'elle vise.
2. **Source unique** : F-98 et la fiche prud'homale lisent le **même** numéro persistant (fin de la double logique volatile).
3. **Backfill non destructif** : les dossiers existants reçoivent une numérotation déterministe sans casser les pièces/labels F-145.
4. **Pas de régression F-98/fiche** : les actes continuent de citer des numéros valides ; les tests existants de `loadNumberedPieces`/`buildPiecesList` sont adaptés, pas supprimés.
5. **Ordre = intention avocat** : l'ordre par défaut reste déterministe, mais devient modifiable ; aucune renumérotation subie à l'usage.

---

## Décision finale

**GO.** Pas de trou amont (la matière « pièces » existe via F-145), aval clair (deux consommateurs + SF-98-57). F-260 est une fiabilisation fondatrice. Le seul vrai arbitrage — **granularité document vs pièce et schéma de stabilité (trou vs renumérotation)** — relève de la mini-spec (étape 1), avec la recommandation : numéroter au niveau pièce, numéro conservé, ordre modifiable.

> Étape 0 bis (cohérence écran) **requise** : F-260 ajoute un réordonnancement/numéro visible côté pièces (onglet Dossier). À produire avant la mini-spec.
