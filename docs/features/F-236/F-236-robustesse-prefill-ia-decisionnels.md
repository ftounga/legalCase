# Feature — F-236 Robustesse pré-fill IA outils décisionnels frontend

## Identifiant

`F-236`

## Statut

`draft`

## Date de création

2026-05-10

---

## Objectif fonctionnel

Garantir que le badge `auto_awesome "Pré-rempli par l'IA (+N)"` du panel F-IA-04 s'affiche correctement sur **toutes** les cartes d'outils décisionnels avant ouverture, en alignant 54 composants frontend sur le pattern canonique SF-177-12 (`static getPrefillCount` + helper partagé `<ComponentName>PrefillRules`), et en bétonnant la non-régression par un garde-fou CI.

## Valeur utilisateur

Sans badge pré-fill avant ouverture, l'avocat n'a aucun signal visuel que l'IA a déjà rempli des champs : il ouvre les outils "à l'aveugle" et redécouvre le pré-fill une fois dedans. Avec le badge correctement affiché sur les ~58 outils décisionnels, l'avocat sait **immédiatement**, depuis la liste, où l'IA a déjà fait le travail. C'est la différenciation produit "outils décisionnels assistés par l'IA" vs "encore un formulaire". Sans cette robustesse, le moteur F-IA-04 reste fonctionnel mais sa promesse UX n'est tenue que sur 4 outils sur 58 (7 %).

---

## Périmètre V1

### Inclus

- Implémentation `static getPrefillCount(input)` sur les **54 composants** décisionnels qui en sont dépourvus (Travail FR+BE ~25, Immigration FR+BE ~14, Famille FR+BE ~15)
- Refactor des 4 composants déjà conformes (F-FA-05, F-FA-07, F-IM-05, F-IM-07) pour extraire un helper partagé `<ComponentName>PrefillRules` (objet de constantes + fonctions pures) consommé par `prefillFromAi()` runtime ET `static getPrefillCount` — élimine la divergence par construction
- Correction des divergences runtime/static observées (F-FA-07 minimum, plus toutes celles détectées par SF-236-01)
- Robustification mono-champ pour F-FA-07 (ajout de fallbacks tertiaires sur la détection de signature de convention)
- Ajout de fallbacks `synthesis.*` manquants (F-DT-09, F-DT-20, F-DT-25 a minima — liste complète issue de SF-236-01)
- Gating pays Immigration BE (~8 outils) — bannière info `mat-info-banner` quand `workspaceCountry` est en mismatch
- Garde-fou CI : nouveau test d'intégrité `DecisionToolPrefillCountIntegrityIT` (ou Jest `prefill-count-integrity.spec.ts`) qui échoue si un `tool_id` du `TOOL_REGISTRY` n'expose pas `static getPrefillCount` ou si la signature diverge du contrat
- Règle CLAUDE.md de blocage automatique mise à jour : "Composant décisionnel sans `static getPrefillCount` ou avec divergence runtime/static → REFUS"

### Exclus (hors périmètre)

- Refonte du système de visibilité F-IA-04 (couvert par F-IA-04 lui-même)
- Création de nouveaux outils décisionnels (couvert par F-220 à F-223 selon C×D)
- Refonte du panel F-IA-04 lui-même (déjà livré SF-177-12)
- Modification du contrat `TOOL_REGISTRY` au-delà de l'ajout de `static getPrefillCount` (déjà figé par SF-177-03b/05/07/12)
- Pré-fill IA sur les composants `*-section.component.ts` qui ne sont **pas** intégrés au TOOL_REGISTRY (49 composants hors périmètre)

---

## Sous-fonctionnalités (Subfeatures)

| ID | Titre | Statut | Dépendances |
|----|-------|--------|-------------|
| SF-236-01 | Audit fin + plan de migration (matrice 58 outils × parité runtime/static + extraction du contrat helper partagé) | `ready` | — |
| SF-236-02 | Implémentation `static getPrefillCount` + helper `PrefillRules` sur les 54 composants — **3 vagues parallèles** par domaine (Travail / Immigration / Famille) | `draft` | SF-236-01 |
| SF-236-03 | Correction des divergences runtime/static détectées par SF-236-01 (a minima F-FA-07) | `draft` | SF-236-02 |
| SF-236-04 | Robustification : fallbacks tertiaires F-FA-07 + fallbacks `synthesis.*` (F-DT-09/20/25) + gating pays Immigration BE (~8 outils) | `draft` | SF-236-03 |
| SF-236-05 | Garde-fou CI `DecisionToolPrefillCountIntegrityIT` + règle CLAUDE.md de blocage automatique | `draft` | SF-236-02 |

---

## Dépendances techniques

### Tables impactées

Aucune. Feature 100 % frontend + CI.

### Endpoints créés ou modifiés

Aucun.

### Composants Angular

- 54 composants `*-section.component.ts` du dossier `frontend/src/app/case-files/` (liste exhaustive issue de SF-236-01)
- 4 composants déjà conformes refactorés : `partage-immobilier-section`, `divorce-checklist-section`, `immigration-title-decision-section`, `immigration-work-right-section`
- `decisional-tools-panel.component.ts` — aucun changement (le panel consomme déjà `getPrefillCount` quand il existe, cf. SF-177-12)
- Nouveau test d'intégrité : `frontend/src/app/case-files/decisional-tools-panel/prefill-count-integrity.spec.ts` (ou équivalent backend si souhaité)

---

## Dépendances externes

### Features préalables requises

| Feature | Raison | Statut |
|---------|--------|--------|
| F-IA-04 (panel décisionnel) | Le panel est le consommateur de `static getPrefillCount` | `done` |
| SF-177-12 (pattern `getPrefillCount`) | Définit le contrat à généraliser | `done` |
| F-164 SF-164-01 (`KNOWN_FRONTEND_TOOL_IDS`) | Pattern de référence pour le garde-fou CI de SF-236-05 | `done` |

### Subfeatures externes requises

Aucune.

### Impact si dépendance absente

Sans objet — toutes les dépendances sont mergées.

### Statut global des dépendances externes

`toutes résolues`

---

## Questions ouvertes liées

Aucune (pas d'OPEN_QUESTION en jeu).

---

## Critères d'acceptation de la feature

- [ ] Les 58 outils décisionnels du `TOOL_REGISTRY` exposent `static getPrefillCount(input): number` avec le contrat unifié
- [ ] Aucune divergence runtime/static n'est observée dans la matrice d'audit (SF-236-01) après application de SF-236-02 et SF-236-03
- [ ] Chaque composant a un helper partagé `<ComponentName>PrefillRules` consommé par les deux chemins (runtime + static) — divergence impossible par construction
- [ ] Le test d'intégrité CI échoue si l'on tente de pousser un composant `*-section` référencé dans `TOOL_REGISTRY` qui n'expose pas `static getPrefillCount`
- [ ] La règle CLAUDE.md "Composant décisionnel sans `static getPrefillCount` → REFUS" est ajoutée et liée au test
- [ ] Aucune régression sur les badges déjà en place (F-FA-05, F-FA-07, F-IM-05, F-IM-07 continuent d'afficher leur badge correct)
- [ ] Sur un dossier de test multi-domaines, le panel F-IA-04 affiche un badge `auto_awesome (+N)` avec N > 0 sur **chaque** outil pertinent dont l'IA a alimenté au moins une donnée

---

## Notes et décisions

### Stratégie anti-divergence (tranchée 2026-05-10)

Décision retenue : **helper partagé par composant** (`<ComponentName>PrefillRules`).

Raison : la duplication runtime/static + un test de comparaison post-hoc reste fragile (le test ne couvre que les inputs explorés ; un nouveau guard ajouté côté runtime sans miroir côté static ne sera détecté que si le test simule cet input précis). Avec un helper partagé, runtime et static appellent **les mêmes fonctions pures** sur les mêmes inputs — la divergence devient structurellement impossible.

Coût estimé : +30 % effort sur SF-236-02 vs duplication, mais gain de robustesse à vie pour toute future feature qui ajoute un champ pré-rempli.

### Périmètre P0/P1/P2 unifié dans F-236 (tranchée 2026-05-10)

Décision : tout est dans F-236 (5 SF), pas de découpage F-237 séparé pour P1/P2. La cohérence d'ensemble du pré-fill IA est traitée comme un bloc unique pour éviter de laisser une dette résiduelle après merge de F-236-P0.

### Ordonnancement et parallélisation

- SF-236-01 (audit fin) bloque SF-236-02 — séquentiel obligatoire
- SF-236-02 (3 vagues domaine en parallèle) — chaque vague sur sa propre branche `feat/SF-236-02-{travail|immigration|famille}` pour respecter la règle "deux SF parallèles ne partagent pas la même branche"
- SF-236-03 et SF-236-05 peuvent être lancées en parallèle après SF-236-02
- SF-236-04 séquentielle après SF-236-03 (utilise les helpers stabilisés par SF-236-03)

### Risque churn et coordination

Le scope touche 58 fichiers. Pendant que SF-236-02 est en cours, **suspendre tout ajout de nouveau composant décisionnel** (F-220+ peuvent attendre 1 semaine) pour éviter le merge conflict massif. Si une feature critique doit avancer, sa SF doit déjà être livrée selon le nouveau pattern (helper + static) — coordination via le fichier MEMORY ou une note dans CLAUDE.md le temps de la fenêtre.
