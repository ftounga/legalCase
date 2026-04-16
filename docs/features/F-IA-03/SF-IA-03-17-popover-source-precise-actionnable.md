# Mini-spec — F-IA-03 / SF-IA-03-17 Popover source précise et actionnable (correctif)

## Identifiant

`F-IA-03 / SF-IA-03-17`

## Feature parente

`F-IA-03` — Contrôle de cohérence sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-16

## Branche Git

`feat/SF-IA-03-17-popover-source-precise-actionnable`

---

## Objectif

Correctif suite à validation staging 2026-04-16 : le popover livré par la série SF-IA-03-15a/b/c masque la source précise de l'incohérence et son bouton "Voir la source" est caché dans la majorité des cas. Cette SF refond la visibilité de la source dans le popover et rend le bouton d'action utile pour tous les types de sources (document, question IA, chat, checklist F-96, pièce manquante).

---

## Contexte — gaps observés

| Symptôme | Cause technique |
|---|---|
| Nom du document / intitulé question / code F96 invisible ou relégué en header 12px gris uppercase | `CoherencePopoverComponent` affiche le label dans `.source-type-label` uniquement (header), pas dans le corps |
| "Raison" affichée = template générique ("Le nombre de jours attendu est X") sans mention d'où vient X | Template Java côté `reasonFor()` dans chaque composant ne référence pas la source |
| Bouton "Voir la source" caché dans 80 % des cas | Seul `sourceType=DOCUMENT` a un anchor résolu. Pour QUESTION_AI / CHECKLIST_F96 / CHAT / MISSING_PIECE, Haiku ne reçoit pas les IDs en input → aucun anchor généré → `actionType=NONE` → lien masqué côté front |

---

## Comportement attendu

### Cas nominal

Au survol d'un badge d'incohérence, le popover affiche **3 zones structurées** au lieu d'un header+2 paragraphes :

```
┌──────────────────────────────────┐
│ [icône type]  MOTIF DÉTECTÉ      │
│ La convention BTP prévoit        │
│ une prime de 12%                 │
├──────────────────────────────────┤
│ [icône source]  SOURCE           │
│ 📄 contrat_dupont.pdf            │
│ « Clause 6.2 — 12% à 15 ans »    │  ← sentence Haiku ou fallback
├──────────────────────────────────┤
│ [→ Ouvrir contrat_dupont.pdf]    │  ← bouton visible si sourceType connu
└──────────────────────────────────┘
```

Selon le `sourceType` :

| sourceType | Zone SOURCE | Bouton |
|---|---|---|
| `DOCUMENT` | 📄 `{label}` + sentence | "Ouvrir *{label}*" |
| `QUESTION_AI` | ❓ *{label}* (= texte question) + réponse | "Voir la question complémentaire" |
| `CHECKLIST_F96` | ☑️ `{label}` (= code F96) + raison | "Voir la checklist procédurale" |
| `CHAT` | 💬 "Message du chat" + extrait | "Ouvrir le panneau chat" |
| `MISSING_PIECE` | ⚠️ *{label}* (= intitulé pièce) | "Voir les pièces manquantes" |
| `ANALYSIS_DETECTION` | ✨ "Synthèse du dossier" + sentence | Masqué |
| `MULTI` | 🔗 Liste de 2-3 sources | "Voir la synthèse" |

### Backend — enrichissement du prompt Haiku

Le `SourceExplanationGenerator` passe en input à Haiku, en plus des documents :

```
# Questions IA du dossier (pour sourceType QUESTION_AI)
- [id=q1] « Quelle est l'ancienneté ? » → réponse : « 15 ans »
- [id=q2] « Quelle convention collective ? » → réponse : « BTP »

# Checklist procédurale F-96 (pour sourceType CHECKLIST_F96)
- [code=FR_CONVOCATION, statut=NON_COMPLIANT] Pas de LRAR envoyée
- [code=FR_MOTIVATION, statut=VERIFIED] Motivation conforme

# Pièces manquantes (pour sourceType MISSING_PIECE)
- [index=0] Contrat de travail signé
- [index=1] Lettre de licenciement

# Messages chat récents (pour sourceType CHAT)
- [id=m1] (12/04) « L'employeur a mentionné 12% »
```

Haiku retourne `anchorQuestionId`, `anchorF96Code`, `anchorChatMessageId`, ou un index de pièce manquante en plus de `anchorDocName`. Le parsing côté Java résout ces anchors (lookups vérifiant que l'ID/code existe dans le dossier) ; si l'ID n'existe pas, fallback graceful (bouton affiche le type générique sans cible précise mais reste cliquable pour naviguer vers la section).

### Frontend — refonte `CoherencePopoverComponent`

- Trois zones sémantiques (MOTIF / SOURCE / ACTION) clairement séparées par des filets `#E5E7EB`.
- Label de source affiché en **bold** dans le corps, pas en uppercase grisé.
- Bouton d'action **toujours visible** quand `sourceType ≠ ANALYSIS_DETECTION` et `≠ MULTI ou avec navigation générique`. Libellé dynamique (ouvre contrat, voir la question, etc.).
- Mode "fallback graceful" : si `sentence` Haiku absente mais `sourceType` connu → libellé générique ("Détecté dans la checklist procédurale du dossier") + bouton navigation générique.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Dossier analysé avant cette SF (pas d'explanations en DB) | Fallback = template + mention "Source : analyse du dossier" + bouton "Voir la synthèse" |
| Haiku renvoie un `anchorQuestionId` qui n'existe plus en DB | Résolution graceful : le backend détecte l'absence et met `anchorQuestionId=null` dans la response. Front tombe en bouton générique. |
| Haiku KO / timeout | Comportement actuel (fail-open, explanations absentes → fallback frontend) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : 10 outils F-IA-03 — **la refonte affecte les 10 automatiquement** (composant partagé).
- [x] **Autres pays** : FR + BE couverts nativement (prompt Haiku génère dans la juridiction, composant neutre).
- [x] **Autres domaines** : Travail / Famille / Immigration couverts (même générateur, même composant).
- [x] **Autres UI patterns** : 5 zones connexes en backlog (F-69/F-92/F-93/F-94/F-96) — elles bénéficieront de la refonte quand elles consommeront la directive.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification

- [x] **Modèle TypeScript** : `SourceExplanation` déjà en place — potentiellement à enrichir (ex. `secondaryText` pour extrait court).
- [x] **Record / DTO backend** : `SourceExplanationResponse` — potentiellement ajout d'un champ `secondaryText` ou `excerpt`.
- [x] **Service / logique métier** : `SourceExplanationGenerator` — prompt enrichi, résolution anchors multi-types.
- [x] **Entité JPA + DB** : table `case_analysis_source_explanations` à étendre si on persiste un champ supplémentaire (ex. `excerpt VARCHAR(500)`). **Migration 076 à planifier si besoin.**
- [x] **Tests existants** : specs composants (7 outils, directive, service) à adapter au nouveau template + nouvelles valeurs mock avec anchors non-document.

### Cas spécifique : nouveau pattern UI ou service partagé

**Aucun nouveau pattern créé** — modification du pattern existant (popover, directive, générateur, endpoint). Par construction :
- Modifier le composant = affecte les 10 consommateurs
- Modifier le prompt = affecte tous les dossiers (FR+BE, 3 domaines)
- Pas de risque de fork / divergence : pas de composant parallèle introduit

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| 10 outils décisionnels (F-DT-07/08/09/10, F-FA-05/06/07, F-IM-05/06/07) | Oui | **Intégré automatiquement** via composant partagé |
| FR + BE | Oui | Intégré automatiquement via générateur unique |
| 3 domaines | Oui | Intégré automatiquement |
| Dossiers analysés avant merge | Partiel | Fallback graceful frontend — ré-analyse recommandée pour bénéficier du plein contenu |

### Décision

- [x] Étendu nativement à toutes les cibles via les composants partagés
- [x] Ré-analyse post-merge requise pour bénéficier du contenu Haiku enrichi (documenté dans release notes)

---

## Critères d'acceptation

- [ ] Le popover affiche 3 zones distinctes : MOTIF / SOURCE / ACTION, chacune séparée par un filet visuel.
- [ ] Le nom du document / intitulé question / code F96 / intitulé pièce est affiché **en bold dans le corps** du popover (pas uniquement dans un header discret).
- [ ] Le bouton d'action est visible pour `DOCUMENT`, `QUESTION_AI`, `CHECKLIST_F96`, `CHAT`, `MISSING_PIECE`, avec un libellé dynamique contextualisé.
- [ ] Pour `ANALYSIS_DETECTION`, le bouton reste caché (aucune cible précise).
- [ ] `SourceExplanationGenerator` reçoit en input les questions IA, points F-96, pièces manquantes et messages chat du dossier avec leurs IDs. Prompt Haiku mis à jour.
- [ ] Haiku renvoie les anchors correspondants (`anchorQuestionId`, `anchorF96Code`, `anchorChatMessageId`, + champ `anchorPieceIndex` si besoin).
- [ ] Backend résout les anchors via lookup en DB : si l'ID n'existe pas, l'explanation est quand même persistée avec le sourceType mais anchor null (bouton générique côté front).
- [ ] `SourceExplanationResponse` inclut un champ optionnel `secondaryText` (extrait court, réponse question, raison F96) affiché dans la zone SOURCE.
- [ ] Specs composant popover adaptés aux 3 zones + nouveaux inputs.
- [ ] Spec directive `CoherencePopoverTriggerDirective` : non-régression.
- [ ] Tests backend `SourceExplanationGeneratorTest` enrichis : cas multi-sources (question/F96/chat), cas anchor orphelin, cas fallback.
- [ ] Build backend vert, build frontend vert, tests 860+/974+ verts.

---

## Périmètre

### Hors scope

- Remplissage rétroactif des dossiers analysés avant cette SF (endpoint `/regenerate-explanations`) — reste en backlog.
- Scan rétrospectif zones connexes (F-69/F-92/F-93/F-94/F-96) — reste en backlog.
- Traduction multi-langue (FR only).
- Surlignage précis de l'extrait dans le document viewer.

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|---|---|---|
| `anchor_piece_index` (nouveau si ajouté) | Non | Integer ≥ 0 |
| `secondary_text` (nouveau) | Non | VARCHAR(500) |

---

## Technique

### Endpoints

| Méthode | URL | Changement |
|---|---|---|
| GET | `/api/v1/case-files/{id}/source-explanations` | `SourceExplanationResponse` étendu (`secondaryText` + optionnellement `anchorPieceIndex`) |

### Tables impactées

- `case_analysis_source_explanations` : **migration 076** si on ajoute `secondary_text` (nullable) et `anchor_piece_index` (nullable). À confirmer lors de l'impl — si `secondary_text` peut être dérivé de la synthèse sans persistance, on évite la migration.

### Migration Liquibase

- [ ] Oui — `076-add-secondary-text-to-source-explanations.xml` (à confirmer, décision impl).
- [x] Possiblement Non — si `secondary_text` est dérivé à la volée côté Java depuis l'analyse JSON sans besoin de persistance.

**Décision à trancher en dev** : privilégier le **non-persistant** (dérivation à la volée) sauf contrainte de perf.

### Composants backend

- `SourceExplanationGenerator` — prompt enrichi, input élargi avec questions/F96/pieces/chat.
- `SourceExplanationService` — résolution anchors multi-types (actuellement seul DOCUMENT résolu).
- `SourceExplanationResponse` — +1 champ `secondaryText`, possiblement +1 `anchorPieceIndex`.

### Composants frontend

- `CoherencePopoverComponent` — refonte template en 3 zones, styles ajustés.
- `SourceExplanation` (modèle) — +1 champ `secondaryText`.
- Éventuellement mise à jour de `CoherenceSourceNavigator` pour supporter une action `MISSING_PIECE` avec index.

---

## Plan de test

### Tests backend

- [ ] `SourceExplanationGeneratorTest` : prompt contient bien les questions/F96/pieces quand fournis.
- [ ] Parsing : anchor `anchorQuestionId` résolu si question existe, null sinon (graceful).
- [ ] Parsing idem F96/chat/piece.
- [ ] Non-régression : tests existants (DOCUMENT, fallback) restent verts.

### Tests frontend

- [ ] `CoherencePopoverComponent` : rendu des 3 zones MOTIF/SOURCE/ACTION.
- [ ] Rendu pour chaque `sourceType` (6 cas).
- [ ] Libellé du bouton contextualisé selon `sourceType`.
- [ ] Fallback : sentence absente → template générique affiché.
- [ ] Non-régression directive + 7 composants outils (974 specs).

### Isolation workspace

- [x] Non applicable — déjà couverte par l'endpoint.

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Aucune préoccupation structurelle** (pas d'auth / workspace / plans / navigation).

### Composants impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `CoherencePopoverComponent` | Template refait, styles ajustés | Spec existant à étendre + nouveaux cas |
| 7 composants consommateurs (via directive) | Aucun changement d'API — directive inchangée | Non-régression des specs outils |
| `SourceExplanationGenerator` | Prompt étendu, input élargi | Tests unitaires existants + 3-4 nouveaux |

### Smoke tests E2E

- [x] Aucun concerné.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IA-03-15a/b/c Done` — infrastructure + propagation aux 10 outils.

### Questions ouvertes

- [x] Aucune.

---

## Notes et décisions

- **Refonte visuelle vs reprise** : l'erreur initiale (SF-15a) était d'afficher la source dans le header discret. Le design corrigé met la source au même niveau que le motif, ce qui correspond à la demande utilisateur ("je veux voir la source précise").
- **Pourquoi une SF dédiée et pas patcher 15a** : 15a est mergée + déployée, plus propre de tracer le correctif dans sa propre PR avec release notes claire ("ré-analyser les dossiers pour bénéficier du contenu enrichi").
- **Décision persistance `secondary_text`** : préférer dérivation à la volée si possible (moins de dette). Migration 076 activable si perf pose problème.
- **Fallback graceful anchors** : important pour les dossiers analysés avant — le popover affiche sourceType + bouton générique de navigation (ex. "Voir la synthèse") sans cible précise. Évite une dégradation UX brutale.
