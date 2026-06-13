# Mini-spec — F-289 / SF-289-01 — Vue d'ensemble du dossier (poste de pilotage / journal) — V1

> Feature parente : **F-289**. Étape 0 : `SF-289-00-coherence.md` (GO). Étape 0 bis : `SF-289-00b-ux-coherence.md` (GO avec ajustements). Statut : `ready` (brouillon hors repo) · Date : 2026-06-13 · Branche cible : `feat/SF-289-01-vue-ensemble`.

## Objectif
Offrir un **onglet « Vue d'ensemble »** en tête du détail dossier qui agrège — en lecture seule, via un endpoint d'agrégation `fail-open` — l'état du dossier, la to-do actionnable, le fil chronologique (passé/présent/futur) avec accès aux pièces, et des raccourcis vers les zones de travail.

## Comportement attendu

### Cas nominal
1. À l'ouverture d'un dossier, l'onglet **Vue d'ensemble** (index 0) est actif par défaut et appelle `GET /api/v1/case-files/{id}/overview`.
2. **① PILOTER** — bandeau d'état (phase courante, à qui le tour, prochain couperet daté, santé = recevabilité + prescription + risque avocat) + **bloc « ce qui requiert ton attention »** (≤ 5 items priorisés : échéances proches, pièces à obtenir, questions IA sans réponse, analyse obsolète). Chaque item porte une action (`[Générer]`, `[Relancer l'analyse]`, `[Répondre →]`, `[Marquer obtenue]`).
3. **② PARCOURIR** — le **fil vertical** : événements triés par date, séparés par le repère **Aujourd'hui** (réalisé en haut / à-venir en bas, pointillés). Voies : Procédure (phases), Échanges (rounds nous/adverse), Pièces (vagues), Production (analyse / stratégie / conclusions), Échéance. Chips de filtre (défaut = Tout). Passé replié au-delà de 8 événements.
4. **Accès pièces** : un événement porteur de pièces (vague → ses N documents ; round → `sourceDocumentId`/`sourceConclusionId` si renseignés) est **dépliable** (accordéon fermé par défaut) et expose chaque pièce avec `[aperçu]` (`/preview`) et `[télécharger]` (`/download`).
5. **Agir depuis le fil** : `[+ Échange]`, `[+ Phase]`, `[+ Échéance]` (réutilisent les dialogues/POST existants), `[fait ✓]` sur une échéance (`PATCH …/deadlines/{id}/validate`), `[Générer ma réplique]` sur le round « à vous » (route vers Décision/Conclusions).
6. **③ APPROFONDIR** — barre de raccourcis (Stratégie, Outils N/M, Conclusions+statut, Synthèse, Jurisprudence) routant via `selectedTabIndex` + ancre, et `[⤓ Exporter le dossier]` (`GET …/export` existant).

### Cas d'erreur / dégradation
| Situation | Comportement |
|---|---|
| Une source de l'agrégat échoue | `fail-open` : sa contribution est omise, le reste s'affiche (jamais d'écran blanc) |
| Dossier neuf (rien) | états vides honnêtes : invite à qualifier (intake) ; aucun événement/échéance inventé |
| Round sans `sourceDocumentId` | pas de lien pièce sur cet événement (dégradation gracieuse) |
| Document supprimé entre-temps | ligne pièce masquée, pas d'erreur |
| Dossier d'un autre workspace | 403/404 (isolation via `case_file`) |

## Technique

### Backend — endpoint d'agrégation (lecture seule)
| Méthode | URL | Rôle |
|---|---|---|
| GET | `/api/v1/case-files/{caseFileId}/overview` | LAWYER (workspace) — agrège pilotage + attention + fil |

**`OverviewService.overview(caseFileId, user)`** : fan-out **lecture seule** sur les services existants (`ContradictoireService`, `CasePhaseService`, `PiecesWaveService`, `EcheancierService`, `CaseIntakeService`, `CaseStrategyService`, `CaseFileDashboardService` pour le score avocat, `CaseAnalysis` pour statut/pièces manquantes, `AiQuestion`, conclusions versions). **`fail-open` par source** (try/catch par contributeur, log warn, on continue — calque `CaseFileDashboardService.assembleTiles`). **Aucune écriture. Aucune nouvelle table. Aucun appel LLM.**

**DTO `OverviewResponse`** :
```
OverviewResponse(
  Pilotage pilotage,
  List<AttentionItem> attention,   // trié par urgence, tronqué à 5 + total
  List<TimelineEvent> fil          // trié par date asc
)
Pilotage(String currentPhaseLabel, ContradictoireParty awaitingParty, EcheancierItem nextDeadline,
         Sante sante, boolean analysisStale, int pendingPiecesCount)
Sante(String admissibility, String prescriptionStatus, String riskLevelAvocat)
AttentionItem(AttentionType type, String label, String urgency, Action action)   // type: ECHEANCE|PIECE_MANQUANTE|QUESTION_IA|ANALYSE_OBSOLETE
TimelineEvent(LocalDate date, Voie voie, Acteur acteur, String titre, String detail,
              Temps temps, String urgency,
              List<Attachment> attachments, Action action)   // temps: PASSE|AUJOURDHUI|FUTUR
Attachment(UUID documentId, String filename)
Action(ActionKind kind, String targetTab, String anchor, String route)  // kind: NAVIGATE|GENERATE_REPLY|RELAUNCH_ANALYSIS|VALIDATE_DEADLINE|OPEN_DOC|ANSWER_QUESTION|MARK_PIECE
```

### Migration Liquibase
- [ ] **Aucune** (agrégation pure).

### Composants Angular
- `CaseOverviewComponent` (`app-case-overview`) — l'onglet, pièce vedette design.
- Sous-composants : `OverviewPilotageComponent` (bandeau + attention), `OverviewTimelineComponent` (le fil + accordéons pièces + actions inline), `OverviewShortcutsComponent` (raccourcis + export).
- `OverviewService` + modèle `Overview`.
- Insertion d'un onglet index 0 dans `case-file-detail` (les 4 onglets existants décalent à 1-4) ; `selectedTabIndex` par défaut = 0.
- Réutilise : `DocumentService.downloadUrl/preview`, dialogues existants (round/phase/échéance), mécanisme `selectedTabIndex` + scroll ancre.

## Critères d'acceptation
- [ ] `GET /overview` retourne pilotage + attention + fil, **isolation workspace** vérifiée (A ≠ B → 403/404).
- [ ] **`fail-open`** : si une source jette, l'agrégat reste servi sans elle (test dédié par contributeur simulé en erreur).
- [ ] Onglet « Vue d'ensemble » en index 0, **actif par défaut**, sans casser les 4 onglets existants (décalage d'index propagé : Dossier=1, Analyse=2, Décision=3, Suivi=4).
- [ ] Fil trié par date, **repère Aujourd'hui** présent, passé replié au-delà de 8 événements, futur en pointillés.
- [ ] **Accès pièces** : vague → ses documents ouvrables (`/preview`) ; round sourcé → document/conclusion ouvrable ; round non sourcé → pas de lien (dégradation).
- [ ] **Bloc attention ≤ 5 items**, trié par urgence, chaque item actionnable.
- [ ] Actions inline opérationnelles : ajouter échange/phase/échéance, `[fait ✓]` (validate), `[Générer ma réplique]` (route), `[Relancer l'analyse]` (`/re-analyze`).
- [ ] Raccourcis routent vers le bon onglet + ancre ; `[Exporter]` déclenche `/export`.
- [ ] États vides honnêtes (dossier neuf) ; **rien d'inventé**.
- [ ] **Conforme `DESIGN_SYSTEM.md`** (navy/or, Merriweather/Inter/JetBrains Mono, 4px), cohérent avec les frises existantes, **zéro AI-generic**.
- [ ] **Revue visuelle PO** (la beauté est un critère).

## Périmètre — Hors scope V1 (explicite)
- **Répondre à une question IA / arbitrer un risque / marquer une pièce obtenue INLINE** → V1 **route** vers l'écran dédié ; l'action inline directe = **V1.1**.
- **Documents versés pendant une phase** (dérivation par fenêtre de dates) → **V1.1**.
- **Export PDF du journal** lui-même (V1 réutilise l'export ZIP existant) → **V1.1**.
- **Panneau d'aperçu latéral** enrichi → V1.1.
- **Absorption / remplacement du stepper** → hors scope (composant partagé, risque transversal).
- **Diff d'analyse, personnalisation, partage avancé** → hors scope.
- **Timeline des FAITS du litige** → reste dans la Synthèse (anti-doublon), la vue y route.

## Analyse d'impact / transversal
### Préoccupations transversales
- [x] **Navigation / routing** — ajout d'un **onglet index 0** dans `case-file-detail` → **liste des composants impactés** : `case-file-detail.component.ts/html` (insertion onglet + décalage index + défaut 0), tout test/ancre référençant un index d'onglet en dur. **Smoke E2E navigation à passer avant push** (`cd e2e && npm test`).
- [x] **Outil décisionnel** — **aucun** : la vue est en lecture/routage, n'altère/observe aucun outil au sens `TOOL_REGISTRY` (invariant « 1 outil = 1 situation » intact).
- [x] Auth/Principal, Workspace context, Plans/limites — **aucun** (réutilise l'isolation via `case_file`).

### Conformité F-IA-04 (SF frontend décisionnelle)
- [x] **Non applicable** — pas un outil décisionnel, pas de pré-fill, pas d'endpoint POST décisionnel.

## Plan de test
### Unitaires backend
- [ ] `OverviewService` — assemblage pilotage (phase/tour/couperet/santé/analyseObsolète), tri du fil, troncature attention à 5.
- [ ] **`fail-open`** — un contributeur simulé en exception → agrégat servi sans sa contribution.
### Intégration
- [ ] `GET /overview` 200 forme complète ; **403/404 isolation workspace** ; fail-open bout-en-bout.
### Frontend (Jest)
- [ ] Rendu des 3 registres ; repère Aujourd'hui ; repli passé ; accordéon pièces (preview/download) ; filtres voie ; actions inline (validate, +round, route réplique) ; états vides.
### Smoke E2E
- [ ] Navigation détail dossier après insertion de l'onglet 0 (les onglets existants restent atteignables).
