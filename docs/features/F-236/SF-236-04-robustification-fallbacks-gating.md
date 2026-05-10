# Mini-spec — F-236 / SF-236-04 Robustification : fallbacks tertiaires + `synthesis.*` + gating pays

## Identifiant

`F-236 / SF-236-04`

## Feature parente

`F-236` — Robustesse pré-fill IA outils décisionnels frontend

## Statut

`draft`

## Date de création

2026-05-10

## Branche Git

`feat/SF-236-04-robustification-fallbacks`

---

## Objectif

Réduire la fragilité du pré-fill IA en ajoutant : (a) des fallbacks tertiaires sur les composants ancrés sur un seul champ IA (F-FA-07 a minima), (b) des fallbacks `synthesis.*` sur les composants où la donnée est exposée mais non consommée (F-DT-09, F-DT-20, F-DT-25 a minima), (c) un gating pays cohérent (bannière info `mat-info-banner`) sur les ~8 outils Immigration BE.

---

## Comportement attendu

### Cas nominal

#### (a) Fallbacks tertiaires F-FA-07

Aujourd'hui : pré-fill ssi `aiData.dateAcceptationPV` valide.
Cible : pré-fill aussi si :
- `synthesis.divorceConsentementMutuelDetection.detected === true` (F-152 scoring DC) — alors pré-cocher l'étape "DC envisagé"
- `aiData.divorce_dc_envisage === true` (flag boolean côté Famille extracted) — alors pré-cocher l'étape "DC envisagé"
- Détection texte "convention signée" / "PV signé" via heuristique sur les pièces uploadées (signal indirect issu de `synthesis.piecesAnalysees`)

Le compteur `getPrefillCount` reflète chaque source supplémentaire activée.

#### (b) Fallbacks `synthesis.*` manquants

Pour chaque composant où SF-236-01 a identifié une donnée disponible mais non consommée :
- F-DT-09 (comparateur indemnités) : ajouter fallback sur `synthesis.ruptureConvValidityDetection` et `synthesis.licenciementValidityDetection` pour enrichir les alertes de cohérence (pas le pré-fill direct, mais la couverture F-IA-03)
- F-DT-20, F-DT-25 et autres listés par SF-236-01 : fallback sur `synthesis.conventions` ou autre source pertinente

#### (c) Gating pays Immigration BE

Pour chaque outil Immigration spécifique BE listé par SF-236-01 :
- Vérifier la présence d'un `<mat-info-banner>` ou équivalent affiché quand `workspaceCountry !== 'BELGIQUE'`
- Si absent, ajouter le bloc HTML conditionnel
- Texte standard : « Cet outil est conçu pour la procédure belge. Vous travaillez actuellement en contexte FR — les résultats peuvent ne pas s'appliquer. »

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| Le champ `synthesis.*` proposé en fallback n'existe pas dans le record | Anomalie — escalade vers SF-236-03 ou backlog F-237 selon gravité |
| Le gating pays existait déjà mais était silencieux (masquage) | Remplacer par bannière info |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : tous ceux listés par SF-236-01 dans les anomalies (C), (D), (E)
- [x] **Autres pays** : FR + BE pour le gating Immigration
- [ ] **Autres domaines** : applicable si l'audit en révèle (Famille / Travail BE peuvent aussi avoir du gating manquant)
- [x] **Autres UI patterns** : la bannière `mat-info-banner` est un pattern à harmoniser
- [ ] **Autres flows transversaux** : non applicable

### Niveaux de vérification

- [x] Modèle TypeScript / API exposée
- [x] Service / logique métier (helper étendu pour les nouveaux fallbacks)
- [x] Record / DTO backend (vérifier que les champs `synthesis.*` consommés existent vraiment)
- [x] Tests existants — étendus pour couvrir les fallbacks

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable.

### Cas spécifique : nouveau pattern UI ou service partagé

**Applicable** : la bannière `mat-info-banner` doit être harmonisée. Si elle n'existe pas comme composant partagé, vérifier les implémentations ad hoc et envisager la création d'un `<app-country-mismatch-banner>`.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-FA-07 ancrage mono-champ | Oui | Fallbacks tertiaires |
| F-DT-09/20/25 fallbacks synthesis | Oui | Ajout des branches synthesis |
| ~8 outils Immigration BE gating | Oui | Bannière info ajoutée |
| Autres composants identifiés en SF-236-01 | Oui | Liste finalisée par SF-236-01 |

### Décision

- [x] Étendu à toutes les cibles applicables (selon liste SF-236-01)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

### 1. Cohérence visuelle

- [ ] Bannière info en `navy/or` (jamais rouge — c'est un avertissement, pas une alerte critique)
- [ ] Pas de masquage silencieux — toujours afficher avec contexte
- [ ] Tous les autres éléments visuels préservés

### 2. Pré-fill IA

- [ ] Helper du composant étendu pour consommer les nouvelles sources
- [ ] `prefillFromAi()` runtime appelle les fonctions étendues du helper
- [ ] `getPrefillCount` static appelle les **mêmes** fonctions étendues
- [ ] Provenance signals étendus si une nouvelle source pré-remplit un champ
- [ ] Badges UI préservés/étendus

### 3. Validation F-IA-03

- [ ] `coherenceAlerts` étendu pour intégrer les nouvelles sources `synthesis.*` (F-DT-09 notamment)
- [ ] Hiérarchie F-96 > Question IA > IA > Pièce manquante préservée
- [ ] `CoherenceAlertBuilder` consommé pour les nouvelles alertes

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [ ] Entrée TOOL_REGISTRY préservée
- [ ] Static `getPrefillCount` étendu
- [ ] Parité runtime/static garantie par helper partagé
- [ ] Tests Jest étendus pour couvrir les nouveaux fallbacks (cas spécifique de chaque fallback)

### 5. Parité des domaines métier

- [x] **Non applicable** — refactor sans création d'outil de niveau ≥ 5

---

## Critères d'acceptation

- [ ] F-FA-07 pré-remplit dans au moins 3 scénarios distincts (pas seulement `dateAcceptationPV`)
- [ ] F-DT-09 affiche des alertes de cohérence F-IA-03 alimentées par `synthesis.ruptureConvValidityDetection` et `synthesis.licenciementValidityDetection` quand pertinent
- [ ] Tous les composants identifiés par SF-236-01 dans les anomalies (D) consomment leurs fallbacks `synthesis.*`
- [ ] Tous les outils Immigration BE listés affichent une bannière info en cas de mismatch pays
- [ ] Aucun masquage silencieux n'est conservé
- [ ] Tests Jest ajoutés pour chaque fallback (un test par scénario distinct)
- [ ] `npm run build` passe
- [ ] `npm test` passe

---

## Périmètre

### Hors scope (explicite)

- Création de nouveaux composants partagés (`<app-country-mismatch-banner>`) — si nécessaire, à arbitrer dans la SF (in-scope) ou backlog F-237
- Audit de gating sur Travail / Famille (si SF-236-01 ne l'identifie pas comme une lacune) — backlog
- Garde-fou CI (couvert par SF-236-05)

---

## Plan de test

### Tests unitaires

- [ ] Pour chaque nouveau fallback : test Jest qui simule l'input "fallback uniquement" et vérifie que le pré-fill se déclenche
- [ ] Pour chaque gating pays : test Jest qui simule un workspace FR avec un outil BE et vérifie l'affichage de la bannière

### Tests d'intégration

Non applicable.

### Isolation workspace

Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Auth / Principal** — non
- [ ] **Workspace context** — partiellement (gating utilise `workspaceCountry`, déjà géré)
- [ ] **Plans / limites** — non
- [ ] **Navigation / routing frontend** — non
- [x] **Aucune préoccupation transversale critique**

### Smoke tests E2E concernés

- [x] Aucun smoke test critique

---

## Dépendances

### Subfeatures bloquantes

- SF-236-02 — doit être `done` (helpers partagés déployés)
- SF-236-03 — doit être `done` (parité bétonnée avant d'étendre)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

### Liste finalisée des composants

À produire dans SF-236-01. Le périmètre exact peut s'élargir ou se rétrécir selon ce que l'audit révèle. Si le périmètre dépasse 2 jours, scinder en SF-236-04a et SF-236-04b par catégorie d'anomalie (C/D/E).
