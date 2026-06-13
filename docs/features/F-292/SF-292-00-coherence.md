# F-292 — Cadrage cohérence (étape 0)

> Feature : **États visuels des outils décisionnels (vierge / pré-rempli / calculé)**.
> Origine : test 2026-06-13 — l'avocat ne distingue pas visuellement un outil **pré-rempli par l'IA mais non calculé** d'un outil **vierge**.

## 1. Workflow métier réel (avocat cible)

1. L'avocat uploade les pièces → lance l'analyse (asynchrone).
2. L'IA **pré-remplit les champs** des outils décisionnels pertinents (pipeline `prefillFromAi`, F-159/F-246).
3. L'avocat ouvre l'onglet **Décision** (panneau F-IA-04) : il doit **relire** les champs pré-remplis, les corriger si besoin, puis **cliquer « calculer »** → l'outil produit un **verdict**.
4. Les verdicts **calculés** (et eux seuls) alimentent les conclusions (cf. F-258 : un pré-rempli non cliqué n'entre PAS dans l'acte).

**Point de friction observé** : entre l'étape 2 et l'étape 3, l'avocat n'a **aucun repère visuel** sur le panneau pour savoir *quels* outils l'IA a préparés. Il doit ouvrir chaque section pour le découvrir. Conséquence : des outils pré-remplis restent non calculés (donc absents de l'acte) sans que l'avocat s'en rende compte — exactement le trou que F-258 signale *a posteriori* au checkpoint.

## 2. Cartographie des features existantes sur ce workflow

| Feature | Rôle | Rapport à F-292 |
|---|---|---|
| **F-IA-04** (panneau décisionnel) | affiche les cartes outils | **surface cible** — F-292 enrichit le rendu de la carte |
| **F-159 / F-246** (pré-remplissage IA) | remplit les champs + `prefillCountFor(toolId)` | **fournit la donnée** « pré-rempli » par outil |
| **F-177** (`DecisionToolSummary`) | verdict synthétique + `alertLevel` | **fournit la donnée** « calculé » (summary non null) |
| **F-258** (alerte outils non calculés) | compte les non-calculés au **checkpoint de génération** | **complémentaire** — F-292 est le pendant *sur le panneau*, en amont |
| **F-288** (écran de composition) | curation au **clic Générer** | distinct (post-calcul, choix d'inclusion) |

## 3. Challenge de cohérence

**Amont (les pré-requis existent-ils ?)** ✅
- La donnée « pré-rempli » est déjà calculée au niveau panneau (`prefillCountFor(toolId)` > 0).
- La donnée « calculé » est déjà connue (présence d'un `DecisionToolSummary` avec `primaryValue`).
- → **Aucun pré-requis manquant. A priori frontend pur, zéro backend** (à reconfirmer au dev : la carte reçoit déjà `[prefillCount]`).

**Aval (la sortie est-elle exploitable ?)** ✅
- L'avocat voit immédiatement les outils « préparés par l'IA, à valider » → il les traite → ils deviennent « calculés » → ils alimentent l'acte. La boucle se referme sur F-258 (moins d'outils oubliés).

## 4. Verdict : **GO**

Cohérence amont et aval validées. Feature à fort effet de levier (rend visible le chaînon manquant de F-258), petite, sans backend probable. → statut PRODUCT_SPEC : `Backlog` → **`À faire`** après étape 0 bis GO.

## 5. Invariants anti-gadget que la mini-spec devra respecter

1. **Effet réel** : les 3 états doivent être **distinguables au premier coup d'œil** sans ouvrir la section. Si l'avocat doit cliquer pour savoir → échec.
2. **Vérité de l'état « pré-rempli »** : il doit refléter une **vraie pré-extraction IA** (`prefillCount > 0`), pas un simple « formulaire ouvrable » ni un champ saisi à la main.
3. **Pas de collision sémantique** : l'état « pré-rempli » emprunte un **canal visuel distinct** des couleurs verdict (OK/WARNING/ALERT) — jamais une 4ᵉ couleur sémantique (sinon « préparé » et « tout va bien » se confondent). Arbitrage précis renvoyé à l'étape 0 bis.
4. **Lecture seule** : aucun calcul déclenché, aucun outil modifié/réordonné/masqué (invariant « 1 outil = 1 situation » intact).
5. **Non-régression F-258 / F-288** : F-292 ne remplace pas l'alerte checkpoint ni l'écran de composition — il les **précède** sur le panneau.
