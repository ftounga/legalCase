# Mini-spec — F-295 / SF-295-01 — Stratégie : carte compacte repliable

> Étape 1. Étape 0 (cohérence fonctionnelle) exemptée (refactor de présentation, workflow inchangé). Étape 0 bis : `SF-295-00b-ux-coherence.md` (GO avec ajustements, 10 invariants anti-surcharge).

---

## Identifiant

`F-295 / SF-295-01`

## Feature parente

`F-295` — Re-design de l'affichage de la stratégie générée

## Statut

`draft`

## Date de création

2026-06-15

## Branche Git

`feat/SF-295-01-strategie-carte-compacte`

---

## Objectif

> En une phrase.

Rendre la carte « Stratégie de dossier » (`case-strategy`, coiffe de l'onglet Décision) **compacte par défaut** — un résumé des 2 sections clés (Voie procédurale, Posture) + un aperçu de la priorisation — avec un bouton **« Voir le détail »** qui déplie le markdown complet in situ, afin de cesser de repousser les outils décisionnels vers le bas, **sans rien changer à la génération** (F-286 intact).

---

## Comportement attendu

### Cas nominal (recommandation disponible, `hasReco()`)

1. La carte s'ouvre **repliée par défaut** (`expanded = false`).
2. Elle affiche un **résumé compact** dérivé du markdown existant (`strategy().content`), parsé par les 4 titres `##` **contractuels** du prompt F-286 (`Voie procédurale`, `Posture`, `Priorisation des chefs de demande`, `Séquencement`) :
   - **Voie procédurale** — 1ʳᵉ phrase de la section ;
   - **Posture** — 1ʳᵉ phrase de la section ;
   - **Priorisation** — aperçu : nombre de chefs (items de la liste) ou 1er chef.
3. Un bouton **« Voir le détail » / « Réduire »** (toggle `expanded`) déplie/replie **le markdown complet** (les 4 sections, rendu actuel `contentHtml()` inchangé), **in situ** (pas de modal, pas de route).
4. Le **pied** (date de génération, lien « Rédiger mes conclusions », bouton « Régénérer ») reste **visible en permanence**, replié comme déplié.

### Fallback parsing (robustesse)

- Si les titres `##` attendus ne sont **pas** trouvés (LLM qui dévie du format, contenu legacy) : le résumé n'est pas construit ; la carte affiche un **aperçu tronqué** du markdown rendu (hauteur max CSS + dégradé de fondu) + le même bouton « Voir le détail ». **Jamais** de carte vide ni d'erreur.

### États non concernés (inchangés)

- `loading()`, `generating()` (spinner), `emptyInput()` (encart F-258), état initial « jamais généré » (CTA Générer) : **strictement inchangés** — le repli ne concerne QUE l'état `hasReco()`.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Titres `##` absents du markdown | Fallback troncature visuelle + « Voir le détail » ; jamais d'erreur |
| Section présente mais vide / 1ʳᵉ phrase introuvable | Ligne de résumé omise pour cette section (les autres restent) ; si tout est vide → fallback troncature |
| `content` absent alors que `hasReco()` (incohérence) | Comportement `hasReco()` déjà gardé par `!!content` côté `computed` existant — aucune régression |

---

## Analyse de cohérence transversale

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Composant `case-strategy` | Oui | Cible directe. |
| Génération stratégie (F-286 backend, prompt, table `case_strategy`) | Non | **Intouchée** (INV-7). Frontend pur. |
| Autres rendus markdown `[innerHTML]` (ex. conclusions, synthèse) | Oui (scan) | Le pattern « résumé + repli » pourrait servir ailleurs (ex. longues synthèses) → **classé backlog** (pas de migration immédiate ; on n'introduit pas de composant partagé dans cette SF, le repli reste local à `case-strategy`). |
| Affichage F-289 (Vue d'ensemble — ligne « Stratégie générée ») | Non | Simple event de fil chronologique, n'affiche pas le contenu ; inchangé. |
| Auth / Workspace / Plans / Navigation | Non | Aucun. |

### Décision

- [x] Étendu à la cible directe (`case-strategy`).
- [x] Backlog (non prioritaire) : généralisation du pattern « résumé + repli » à d'autres rendus markdown longs — **pas** de composant partagé créé ici (éviter la sur-ingénierie tant qu'une 2ᵉ cible n'est pas confirmée).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : `case-strategy` n'est **pas** un outil décisionnel du panel F-IA-04 (pas d'entrée `TOOL_REGISTRY`, pas de formulaire, pas de verdict, pas de `getPrefillCount`). C'est une carte d'affichage en lecture seule. Aucune des 5 sous-sections (cohérence visuelle/pré-fill/F-IA-03/registry/parité domaines) ne s'applique.

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — pas de formulaire ni de champ saisissable.

---

## Critères d'acceptation

- [ ] **CA1 (compact par défaut)** : à l'affichage d'une stratégie `READY`, la carte est **repliée** ; le markdown complet n'est pas visible tant que « Voir le détail » n'est pas cliqué.
- [ ] **CA2 (résumé fidèle)** : le résumé affiche la 1ʳᵉ phrase de « Voie procédurale » et de « Posture » + un aperçu de la priorisation, **extraits du contenu existant** (aucune nouvelle production, aucun appel réseau/LLM — INV-6/INV-7).
- [ ] **CA3 (dépli in situ)** : « Voir le détail » déplie le markdown complet **dans la carte** (pas de modal ni de changement de route) ; « Réduire » le replie. État porté par un signal local (`expanded`).
- [ ] **CA4 (actions + date préservées — INV-2)** : date de génération, « Rédiger mes conclusions » et « Régénérer » sont visibles **replié comme déplié**.
- [ ] **CA5 (fallback robuste — INV-1)** : si les titres `##` manquent, la carte affiche un aperçu tronqué + « Voir le détail », **jamais** vide ni en erreur ; le détail complet reste accessible.
- [ ] **CA6 (états non régressés — INV-8)** : `loading` / `generating` / `emptyInput` / état initial sont **inchangés**.
- [ ] **CA7 (charte — INV-10)** : résumé/chips réutilisent navy/or/divider et Merriweather/Inter ; **aucune 4ᵉ couleur**.
- [ ] **CA8 (responsive — INV-9)** : compact + dépli fonctionnent < 1024 px et au breakpoint 640 px existant.
- [ ] **CA9 (backend intact — INV-7)** : `git diff` ne touche **aucun** fichier backend ; F-286 (génération, `case_strategy`, prompt) inchangé ; le GET reste lecture seule.

---

## Périmètre

### Hors scope (explicite)

- Toute modification de la **génération** de la stratégie (prompt, contenu, table `case_strategy`) — F-286 reste tel quel.
- **Extraction de mots-clés** type « Voie : RÉFÉRÉ » (nécessiterait du NLP non fiable sur du texte narratif) → le résumé reste « titre — 1ʳᵉ phrase ».
- Composant **partagé** de repli markdown réutilisable (classé backlog, voir scan).
- Déplacement de la carte hors de l'onglet Décision (INV-5 : même zone, même route).

---

## Technique

### Endpoint(s)

> **Aucun.** Lecture via le `CaseStrategyService.get` existant, inchangé.

### Tables impactées

> **Aucune.** Frontend pur.

### Migration Liquibase

- [x] **Non applicable.**

### Composants Angular

- `CaseStrategyComponent` (`frontend/src/app/case-files/case-strategy/`) — ajouts :
  - signal `expanded = signal(false)` + `toggle()` (`markForCheck` OnPush) ;
  - `computed` `strategySections()` : parse `content` par les 4 titres `##` → `{ voie, posture, priorisationItems[], sequencement }` (ou `null` si format non reconnu) ;
  - `computed` `summary()` : dérive les lignes de résumé (1ʳᵉ phrase Voie/Posture + aperçu priorisation) depuis `strategySections()` ;
  - `computed` `parseOk()` : `strategySections() !== null`.
  - Template : bloc résumé (si `parseOk()`) sinon bloc tronqué ; `@if (expanded())` pour le markdown complet ; bouton toggle ; footer inchangé.
  - SCSS : classe `.strat-reco--collapsed` (hauteur max + fondu) pour le fallback ; styles résumé (chips/lignes) sur la palette existante.

---

## Plan de test

### Tests Jest (composant)

- [ ] Rendu `READY` → carte **repliée** par défaut, markdown complet absent du DOM (CA1).
- [ ] `summary()` extrait correctement la 1ʳᵉ phrase de Voie et Posture + compte les chefs, sur un markdown aux 4 titres (CA2).
- [ ] `toggle()` : clic « Voir le détail » → markdown complet présent ; re-clic → replié (CA3).
- [ ] Date + « Rédiger mes conclusions » + « Régénérer » présents dans les **deux** états (CA4).
- [ ] Markdown **sans** les titres attendus → `parseOk()` false → fallback tronqué + « Voir le détail », pas d'erreur (CA5).
- [ ] États `loading` / `generating` / `emptyInput` / initial → rendu **inchangé** (CA6).
- [ ] Self-check : aucune régression des `data-testid` existants (`strat-reco`, `strat-date`, `strat-to-conclusions`, `strat-regenerate`).

### Isolation workspace

- [x] **Non applicable** — frontend pur, lecture via service existant déjà isolé.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** (Auth / Workspace / Plans / Navigation non touchés). SF frontend isolée à un composant d'affichage.

### Smoke tests E2E concernés

- [x] **Aucun** — pas de changement auth/workspace/navigation. Validation par Jest + revue visuelle staging.

---

## Dépendances

### Subfeatures bloquantes

- Aucune. F-286 (génération + carte) livrée ; les 4 titres `##` du prompt sont contractuels (`CaseStrategyPromptBuilder.SYSTEM_PROMPT`).

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Robustesse du résumé** : repose sur les 4 titres `##` **imposés par le prompt** F-286 (`CaseStrategyPromptBuilder` : « EXACTEMENT ces titres de niveau 2, dans cet ordre »). Le parsing est donc fiable, pas du fuzzy. **Fallback troncature** si jamais le LLM dévie → jamais de carte vide (CA5 / INV-1).
- **Ajustement vs mockup PO** : les sections étant narratives, le résumé est « titre — 1ʳᵉ phrase » (et non un mot-clé « Voie : RÉFÉRÉ », non extractible sans NLP). Même intention de compacité, fidèle au contenu réel.
- **Frontend pur** : 0 backend, 0 migration, 0 LLM, F-286 intact (INV-7 / CA9).
- Respect des 10 invariants anti-surcharge de `SF-295-00b-ux-coherence.md`.
