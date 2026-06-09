# SF-98-57 — Cadrage cohérence (étape 0)

> Feature : **Bordereau de pièces dans les conclusions** (Phase 3 de l'audit F-98).
> Skill : `ai-skills/feature-coherence-challenger.md`. Date : 2026-06-09.

## Verdict : **GO avec ajustements** (1 décision PO — périmètre MVP vs fiabilisation amont de la numérotation)

---

## Intention métier (1 phrase)

Annexer au projet de conclusions un **bordereau de pièces** — la liste numérotée des pièces communiquées — comme le fait tout avocat qui dépose des conclusions, afin que les renvois « Pièce n° X » de l'acte soient adossés à une liste explicite et exploitable par le juge et la partie adverse.

---

## Workflow métier réel de l'avocat (contentieux écrit)

> Source : pratique standard du contentieux civil/social (les conclusions sont déposées **avec** un bordereau de pièces communiquées — art. 768, 802 CPC) + audit F-98 (dossier LEMAIRE).

1. L'avocat réunit les pièces du dossier (contrat, lettres, bulletins…).
2. Il les **numérote** et leur donne un intitulé (« Pièce n° 3 — Lettre de licenciement »).
3. Il rédige ses conclusions en **renvoyant** à ces numéros dans son argumentation (« comme en atteste la Pièce n° 3 »).
4. Il joint en fin d'acte (ou en document séparé communiqué) un **bordereau de pièces** : la liste numérotée de tout ce qu'il communique.
5. Il dépose conclusions + bordereau + pièces ; la partie adverse et le juge s'y réfèrent.
6. À chaque jeu de conclusions ultérieur, le bordereau est mis à jour (pièces ajoutées).

**La feature se situe à l'étape 4 (et garantit la cohérence avec les renvois de l'étape 3).**

---

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. Réunion des pièces | F-43 upload documents | ✅ Livrée |
| 2. Numérotation + intitulé des pièces | F-145 identification des pièces (type + `label` éditable) ; numéro **calculé à la volée** (`loadNumberedPieces`, tri `createdAt DESC`), **non persisté** | 🟡 partiel |
| 3. Renvois « Pièce n° X » dans l'argumentation | F-98 (section « PIÈCES NUMÉROTÉES » injectée au prompt) | ✅ Livrée |
| **4. Bordereau de pièces annexé à l'acte** | **— pour F-98 : AUCUN** (existe pour la fiche prud'homale : `pdf-export.service.ts` « Bordereau de pièces ») | ❌ Manquant (F-98) |
| 5. Dépôt conclusions + bordereau | F-98 export Word/PDF (SF-98-50/51) | ✅ Livrées (sans bordereau) |
| 6. MAJ à chaque jeu | F-98 régénération + versions (SF-98-52/53) | ✅ Livrées |

---

## Position de la nouvelle feature

SF-98-57 s'insère à **l'étape 4** : une annexe « Bordereau de pièces » en fin d'acte généré, alimentée par la **même source de numérotation** que les renvois de l'étape 3 (`loadNumberedPieces`).

Couture technique repérée :
- Source : `CaseConclusionService.loadNumberedPieces(caseFileId)` → `List<NumberedPiece(number, label, type)>` (déjà passée au builder pour la section « PIÈCES NUMÉROTÉES »).
- Injection : une nouvelle section/consigne dans `CaseConclusionPromptBuilder` pour que l'acte se termine par un bordereau numéroté, OU un assemblage déterministe du bordereau hors-LLM (à trancher en mini-spec).

---

## Challenge amont

**Question** : chaque étape avant la feature est-elle couverte ?

- Étapes 1-3 : ✅ couvertes (upload, pièces F-145 avec `label`/`type` éditables, renvois dans l'acte).
- **Étape 2 — numérotation : 🟡 point dur.** Le numéro de pièce **n'est pas persisté** : il est recalculé à chaque génération depuis l'ordre `createdAt DESC`. Conséquence : si une pièce est ajoutée/supprimée entre deux générations, les numéros **glissent**, et un acte déjà produit citant « Pièce n° 5 » peut devenir incohérent. C'est la racine du défaut relevé à l'audit.

**Mais** : pour un bordereau **généré dans la même passe que l'acte**, le bordereau et les renvois « Pièce n° X » partagent le **même snapshot** de numérotation → **cohérents par construction au sein d'une génération donnée**. L'instabilité ne se manifeste qu'entre régénérations / modifications de pièces — exactement ce que SF-98-53 (bandeau « conclusions à régénérer ») signale déjà.

→ Deux périmètres possibles (décision PO) :
- **MVP (recommandé)** : livrer le bordereau **comme annexe de l'acte**, alimenté par la numérotation existante → cohérence intra-génération garantie ; la **fiabilisation/persistance de la numérotation** reste un sujet amont distinct au backlog.
- **Amont d'abord** : fiabiliser la numérotation (champ `piece_number` persisté + ordre maîtrisé par l'avocat) AVANT le bordereau → plus lourd (migration `document_pieces`, UX de (re)numérotation, ordre des documents), mais supprime l'instabilité inter-générations à la racine.

## Challenge aval

**Question** : la sortie est-elle exploitable par les étapes aval ?

- ✅ Oui : le bordereau intégré à l'acte est relu/édité (SF-98-49), exporté Word/PDF (SF-98-50/51), versionné (SF-98-52). Aucun trou aval. Cohérence avec le bordereau **déjà existant de la fiche prud'homale** (même intitulé « Bordereau de pièces ») → pas de divergence de libellé.

---

## STOPs / pré-requis à ajouter au backlog

- **Fiabilisation/persistance de la numérotation des pièces** (champ `piece_number` persisté, ordre maîtrisé) — **pré-requis seulement si l'option « Amont d'abord » est retenue** ; sinon évolution backlog. À inscrire au backlog quel que soit le choix (le défaut d'instabilité inter-générations existe indépendamment du bordereau).

---

## Invariants anti-gadget pour la mini-spec

1. **Cohérence renvois ↔ bordereau** : le bordereau et les « Pièce n° X » de l'acte sont produits à partir du **même snapshot** `loadNumberedPieces` d'une génération → un numéro cité dans l'acte existe toujours dans le bordereau, et inversement.
2. **Pas de pièce inventée** : le bordereau ne liste **que** les pièces réelles du dossier (documents/pièces F-145) ; aucune entrée hallucinée. Si l'assemblage est confié au LLM, garde explicite ; sinon assemblage déterministe hors-LLM (plus sûr — à privilégier en mini-spec).
3. **Section absente si 0 pièce** : aucun document → pas de rubrique bordereau vide / « néant ».
4. **Libellé unique** : intitulé « Bordereau de pièces » (aligné sur la fiche prud'homale), placé en **fin d'acte** (annexe).
5. **Intitulés lisibles** : chaque ligne = numéro + `label` métier de la pièce (jamais le nom de fichier brut ni un type technique seul) — cohérent avec l'anti-jargon SF-98-55.
6. **Limite assumée tracée** : si MVP, la non-persistance de la numérotation (glissement inter-générations) est documentée comme limite connue (et SF-98-53 reste le filet : « conclusions à régénérer »).

---

## Décision finale

**GO avec ajustements.** La chaîne amont est complète pour un bordereau **cohérent intra-génération** ; le seul arbitrage était le **périmètre**.

### ✅ DÉCISION PO (2026-06-09) : **« Les deux, numérotation puis bordereau »**.
On fiabilise d'abord la numérotation à la racine, puis on pose le bordereau dessus. Conséquence : **SF-98-57 dépend de F-260** (« Numérotation persistante & ordre des pièces communiquées », nouvelle feature amont). SF-98-57 reste `Backlog` tant que F-260 n'est pas livrée. Séquence : **F-260 (étape 0 → … → dev)** puis **SF-98-57 (étape 0 bis → mini-spec → dev)** par-dessus une numérotation désormais stable.

> Étape 0 bis (cohérence écran) : requise — le bordereau ajoute un contenu visible dans l'acte généré (onglet Décision) et dans les exports. À produire une fois le périmètre tranché.
