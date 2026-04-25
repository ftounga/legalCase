# Mini-spec — F-DT-23 / SF-DT-23-02 Frontend requalification intérim → CDI (FR)

## Identifiant

`F-DT-23 / SF-DT-23-02`

## Feature parente

`F-DT-23` — Requalification intérim → CDI (art. L.1251-40, L.1251-1 à L.1251-12 Code du travail)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-23-02-frontend-requalification-interim-cdi`

---

## Objectif

Livrer le composant Angular `requalification-interim-cdi-section` qui consomme l'API SF-DT-23-01 (POST/GET `/api/v1/case-files/{id}/requalification-interim-cdi`) afin que l'avocat français puisse instruire un dossier de requalification intérim → CDI : motif de recours, motif interdit éventuel, succession de missions auprès de la même entreprise utilisatrice, délai de carence, durée totale des missions, salaire — et obtenir un score de probabilité de requalification, l'indemnité de requalification (≥ 1 mois L.1251-41) cumulée à l'indemnité de fin de mission (10 % L.1251-32).

---

## Comportement attendu

### Cas nominal

1. L'utilisateur ouvre un dossier de droit du travail FRANCE. Le panel F-IA-04 affiche la section "REQUALIFICATION INTÉRIM → CDI" (tool_id `F-DT-23-requalification-interim-cdi`, règle ALWAYS_ON FR + travail seed migration backend SF-DT-23-01).
2. Section repliée par défaut (collapsed). Click/Enter → expand.
3. `ngOnInit` :
   - Si `workspaceCountry !== 'FRANCE'` → bannière info BE (régime distinct loi du 24/07/1987 sur le travail temporaire — pas d'équivalent direct), pas de GET.
   - Sinon GET `/api/v1/case-files/{id}/requalification-interim-cdi`.
     - GET 200 → mode lecture (form masqué, valeurs hydratées, bouton "Modifier").
     - GET 404 → mode formulaire vide ; `prefillFromAi()` depuis `aiData.salaireBrutMensuel`.
4. L'avocat saisit :
   - `motifInterimInvoque` (mat-select, 6 options : `ACCROISSEMENT_TEMPORAIRE`, `REMPLACEMENT_SALARIE`, `EMPLOI_SAISONNIER`, `EMPLOI_USAGE`, `MISSION_PEPINIERE`, `AUTRE`).
   - `motifInterdit` (mat-slide-toggle). Si `true`, mat-select `motifInterditType` apparaît (4 options : `EMPLOI_PERMANENT`, `REMPLACEMENT_GREVISTE`, `TRAVAUX_DANGEREUX`, `AUTRE`).
   - `successionMissions` : liste éditable d'objets `{ dateDebut, dateFin, motif, entrepriseUtilisatrice }`. UX : 2 inputs date + un input texte "motif" + un input texte "entreprise utilisatrice" + bouton "Ajouter". Chaque mission ajoutée apparaît sous forme de mat-chip avec bouton suppression et label affichant l'entreprise. Optionnel (peut rester vide).
   - `delaiCarenceRespecte` (mat-slide-toggle, défaut `true`).
   - `dureeMissionsTotaleMois` (input number, > 0, step 0.25).
   - `salaireMensuelBrutEur` (input number, > 0, step 0.01) — **pré-fill IA** depuis `aiData.salaireBrutMensuel`.
   - `dateFinDerniereMission` (`<input type="date">`, ISO YYYY-MM-DD).
   - `memeEntrepriseUtilisatrice` (mat-slide-toggle) — indique si toutes les missions ont été effectuées chez la même entreprise utilisatrice (relation triangulaire jurisprudentielle).
5. Submit → POST avec body conforme au contrat figé SF-DT-23-01.
6. Réponse 200 :
   - Bannière verdict (probabilité requalification : `ELEVEE` rouge alerte / `MOYENNE` or / `FAIBLE` navy) avec score et pictogramme (`gavel` / `balance` / `info_outline`).
   - 3 cartes montants en JetBrains Mono : indemnité de requalification (`indemniteRequalificationEur`), indemnité de fin de mission 10 % (`indemniteFinMissionInterimEur`), total (`totalDommagesIndemniteEur`).
   - Liste messages avec rappel prescription 12 mois L.1471-1 + référence jurisprudence Cass. soc. relation triangulaire ETT/EU/salarié + références juridiques en `<code>` JetBrains Mono.
   - `baseJuridique` + `formule` en JetBrains Mono.
   - Bouton "Modifier" → retour formulaire avec valeurs pré-remplies.
   - `MatSnackBar` succès, `CaseDashboardRefreshService.triggerRefresh()` appelé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `motifInterimInvoque` manquant | Submit désactivé (form invalide) | — |
| `motifInterdit=true` sans `motifInterditType` | Submit désactivé | — |
| `dureeMissionsTotaleMois` ≤ 0 | Submit désactivé | — |
| `salaireMensuelBrutEur` ≤ 0 | Submit désactivé | — |
| `dateFinDerniereMission` vide ou non ISO | Submit désactivé | — |
| Backend 400 (validation) | `MatSnackBar` rouge, message backend remonté | 400 |
| Dossier hors workspace ou hors travail | `MatSnackBar` erreur | 400/404 |
| GET inexistant | Reste en mode formulaire (404 attendu — pas de snackbar) | 404 |
| Erreur réseau POST | `MatSnackBar` rouge | 5xx |
| `workspaceCountry !== 'FRANCE'` | Bannière info — pas de GET, pas de form | — |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier (droit du travail)** : F-DT-22 (requalification CDD → CDI) est le **jumeau direct** structurellement et juridiquement — mêmes champs (motif/motifInterdit/succession/délai de carence/durée/salaire/dateFin), mêmes seuils, palette navy/or/rouge identique. Différences clés : champ supplémentaire `entrepriseUtilisatrice` par mission (relation triangulaire ETT/EU), toggle `memeEntrepriseUtilisatrice`, indemnité 10 % (vs 10 % précarité CDD distincte mécaniquement). **Pattern intégré** : copie quasi-intégrale de F-DT-22 + adaptation MissionInterim.
- [x] **Pattern de référence canonique** : `harcelement-licenciement-nul-section` (F-DT-11-02, identifié dans `ai-skills/frontend-coherence-audit.md` §5). Pattern jumeau structurel : `requalification-cdd-cdi-section` (F-DT-22-02, identique sauf champ entreprise + toggle même EU).
- [x] **Autres pays** : FRANCE only. Belgique : régime intérim distinct (loi du 24/07/1987 et CCT 36 du CNT, sanction propre — bannière info renvoyant vers un futur outil BE (F-DT-23-BE backlog hypothétique). Pas d'équivalent direct.
- [x] **Autres domaines** : non applicable — spécifique droit du travail FR.
- [x] **Autres UI patterns** : pré-fill IA (SF-155-04), alertes cohérence F-IA-03 via `CoherenceAlertBuilder` (SF-155-05/06), refresh dashboard (SF-IA-02-03), pipe `legalCitations` pour rendu références juridiques — tous réutilisés. **Aucun nouveau pattern partagé introduit**.

### Niveaux de vérification

- [x] Modèle TypeScript + interface contrat API (importé SF-DT-23-01)
- [x] Service Angular wrapping HttpClient (POST + GET)
- [x] Composant Angular consommateur avec pré-fill IA + validation F-IA-03
- [x] Spec Jest ≥ 15 tests couvrant mount + form valid + POST + erreur + IA + alertes
- [x] Entrée TOOL_REGISTRY symétrique (`aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`)

---

## Impact par domaine métier

- **Droit du travail (FR)** : feature centrale (objet de la SF). Spécifique au travail temporaire / intérim français.
- **Droit du travail (BE)** : non couvert — bannière info uniquement. Le régime belge (loi 24/07/1987, CCT 36 CNT) prévoit un mécanisme distinct. Backlog : F-DT-23-BE jumeau hypothétique à ouvrir si demande utilisateur.
- **Droit de l'immigration** : non applicable.
- **Droit de la famille** : non applicable.

---

## Parité des domaines métier (outil décisionnel niveau ≥ 5 — scoring)

L'outil produit un `scoreRequalification` (0-100) + `verdictProbabiliteRequalification` (`ELEVEE` / `MOYENNE` / `FAIBLE`) → niveau **5 (scoring / analyse validité)**.

| Domaine | Équivalent existant ? |
|---|---|
| Travail FR | **Oui** — c'est cette SF (F-DT-23) — jumelle de F-DT-22 (CDD). |
| Travail BE | Non — pas de mécanisme miroir direct. Régime intérim belge (loi 24/07/1987) à modéliser séparément si feature jumelle ouverte au backlog (F-DT-23-BE potentiel). |
| Immigration | Non applicable — concept de requalification intérim spécifique au droit du travail. |
| Famille | Non applicable. |

**Conclusion** : pas d'asymétrie nouvelle créée. La parité est traitée par la convention "outil = situation métier" (cf. `feedback_decision_tools_one_per_situation.md`). Le concept "requalification intérim" est strictement français pour l'instant.

---

## Nouveau pattern UI ou service partagé

Aucun. Le composant ne crée ni service partagé, ni directive transversale, ni DTO réutilisable, ni composant générique. Il consomme :
- Le `CoherenceAlertBuilder` partagé (`shared/coherence-popover/coherence-alert-builder.ts`) — existant SF-155-05.
- La directive `CoherencePopoverTriggerDirective` — existant SF-IA-03-15b.
- Le pipe `LegalCitationsPipe` — existant.
- Les modèles `TravailExtractedData`, `ProcedureCheck`, `AiQuestion`, `PieceManquanteEntry` — existants.

---

## Critères d'acceptation

1. Composant Angular `RequalificationInterimCdiSectionComponent` standalone publié dans `frontend/src/app/case-files/requalification-interim-cdi-section/`.
2. Modèle TypeScript dans `frontend/src/app/core/models/requalification-interim-cdi.model.ts` avec types `MotifInterimInvoque`, `MotifInterditTypeInterim`, `MissionInterim`, `RequalificationInterimCdiRequest`, `RequalificationInterimCdiResponse`, et `MOTIF_INTERIM_INVOQUE_OPTIONS`, `MOTIF_INTERDIT_TYPE_INTERIM_OPTIONS`.
3. Service `RequalificationInterimCdiService` avec méthodes `calculate(caseFileId, request)` (POST) et `get(caseFileId)` (GET).
4. Form valide uniquement quand `motifInterimInvoque` ≠ null, `dureeMissionsTotaleMois > 0`, `salaireMensuelBrutEur > 0`, `dateFinDerniereMission` non vide, et `motifInterdit=true ⇒ motifInterditType ≠ null`.
5. POST envoie le body au schéma exact figé par SF-DT-23-01.
6. Affichage résultat : bannière verdict colorée (palette navy/or/rouge), score, 3 cartes montants en JetBrains Mono, messages, baseJuridique, formule, bouton Modifier.
7. **Pré-fill IA fonctionnel** : `aiData.salaireBrutMensuel` pré-remplit `salaireMensuelBrutEur` avec badge "Pré-rempli depuis l'analyse" (icône `auto_awesome`). Saisie manuelle efface le badge.
8. **Validation F-IA-03 fonctionnelle** : alerte de cohérence sur `salaireMensuelBrutEur` quand divergence > 10 % vs `aiData`. Multi-sources `IA` / `F96` / `QUESTION_IA` / `PIECE_MANQUANTE` consolidées via `CoherenceAlertBuilder`.
9. Gate `workspaceCountry`: bannière info si BE (jamais masquage silencieux).
10. `CaseDashboardRefreshService.triggerRefresh()` appelé après POST 200.
11. `MatSnackBar` pour erreurs HTTP, jamais `alert`/`confirm`.
12. JetBrains Mono pour `baseJuridique`, `formule`, montants, dates ISO. Inter pour le reste.
13. Entrée TOOL_REGISTRY ajoutée (`F-DT-23-requalification-interim-cdi`), avec inputs symétriques aux autres outils du panel.
14. Spec Jest avec ≥ 15 tests : mount, form validators, GET 200/404, POST succès/erreur, pré-fill, alertes IA-03, gate BE, slide-toggle motifInterdit, ajout/suppression mission, edit mode, toggle collapse.
15. `tsc --noEmit -p tsconfig.app.json` et `npx jest --testPathPattern=requalification-interim-cdi` passent verts.

---

## Plan de test minimal

### Unitaires (Jest, ≥ 15)

1. `mount sans erreur (FRANCE)` — composant créé, options enums exposées.
2. `6 motifInterimInvoque options exposées` — codes attendus présents.
3. `4 motifInterditType options exposées` — codes attendus présents.
4. `formValid faux si motifInterimInvoque null`.
5. `formValid faux si motifInterdit=true sans motifInterditType`.
6. `formValid faux si dureeMissionsTotaleMois ≤ 0`.
7. `formValid faux si salaireMensuelBrutEur ≤ 0`.
8. `formValid faux si dateFinDerniereMission vide`.
9. `formValid vrai sur cas nominal complet`.
10. `GET 200 → form masqué, valeurs hydratées, pas de badge IA`.
11. `GET 404 → reste en mode formulaire, pré-fill IA appliqué`.
12. `calculate() POST → résultat affiché + snackbar succès + dashboardRefresh`.
13. `calculate() erreur 400 → snackbar rouge, pas de refresh`.
14. `pré-fill IA salaireMensuelBrutEur si aiData.salaireBrutMensuel > 0`.
15. `onSalaireChange manuel efface le badge IA`.
16. `coherenceAlerts.SALAIRE_MENSUEL présent si écart > 10 % vs IA`.
17. `coherenceAlerts absent si écart ≤ 10 %`.
18. `addMissionInterim() ajoute une entrée à la liste avec entrepriseUtilisatrice`.
19. `removeMissionInterim() supprime une entrée par index`.
20. `slide-toggle motifInterdit révèle/masque motifInterditType`.
21. `workspaceCountry BELGIQUE → bannière info, pas de GET`.
22. `editMode ré-affiche le form ; toggleCollapse fonctionne`.
23. `ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide`.
24. `alertes masquées après showForm=false (anti-bug SF-IA-03-12)`.
25. `memeEntrepriseUtilisatrice envoyé dans le POST`.

### Intégration

Smoke test via tests Jest avec `HttpClientTestingModule` mocké couvrant le flux complet GET → form → POST → résultat. Pas de e2e dédié (couvert par les e2e existants `case-detail.spec.ts` du panel).

### Isolation workspace

Non applicable côté frontend (le backend filtre `workspace_id`). Le composant respecte la règle en passant `caseFileId` opaque ; aucune fuite cross-workspace possible côté UI.

---

## Tables / endpoints / composants impactés

### Endpoints consommés (figés SF-DT-23-01)

- `POST /api/v1/case-files/{caseFileId}/requalification-interim-cdi` → `RequalificationInterimCdiResponse`.
- `GET /api/v1/case-files/{caseFileId}/requalification-interim-cdi` → `RequalificationInterimCdiResponse` (404 si absent).

### Fichiers créés

- `frontend/src/app/core/models/requalification-interim-cdi.model.ts`
- `frontend/src/app/core/services/requalification-interim-cdi.service.ts`
- `frontend/src/app/case-files/requalification-interim-cdi-section/requalification-interim-cdi-section.component.ts`
- `frontend/src/app/case-files/requalification-interim-cdi-section/requalification-interim-cdi-section.component.html`
- `frontend/src/app/case-files/requalification-interim-cdi-section/requalification-interim-cdi-section.component.scss`
- `frontend/src/app/case-files/requalification-interim-cdi-section/requalification-interim-cdi-section.component.spec.ts`
- `docs/features/F-DT-23/SF-DT-23-02-frontend-requalification-interim-cdi.md`

### Fichiers modifiés

- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (+ entrée TOOL_REGISTRY).

---

## Hors périmètre

- Backend SF-DT-23-01 (déjà figé, contrat importé).
- Régime BE (loi 24/07/1987) — bannière info uniquement.
- Action contre l'entreprise utilisatrice (responsabilité solidaire ETT/EU) — couvert si feature dédiée future.
- Génération PDF de la fiche de requalification — couvert par F-DT-04 (fiche prudhomale) si étendu.
- Détection IA automatique du `motifInterimInvoque` — backlog futur (extraction enrichie LLM).
- Calculateur indemnité jurisprudentielle (au-delà du minimum légal) — couvert par F-DT-09 comparateur.

---

## Contrat API (importé de SF-DT-23-01)

```typescript
export type MotifInterimInvoque =
  | 'ACCROISSEMENT_TEMPORAIRE'
  | 'REMPLACEMENT_SALARIE'
  | 'EMPLOI_SAISONNIER'
  | 'EMPLOI_USAGE'
  | 'MISSION_PEPINIERE'
  | 'AUTRE';

export type MotifInterditTypeInterim =
  | 'EMPLOI_PERMANENT'
  | 'REMPLACEMENT_GREVISTE'
  | 'TRAVAUX_DANGEREUX'
  | 'AUTRE';

export interface MissionInterim {
  dateDebut: string;                  // ISO YYYY-MM-DD
  dateFin: string;                    // ISO YYYY-MM-DD
  motif: string;                      // texte libre (motif de recours)
  entrepriseUtilisatrice: string;     // raison sociale EU
}

export interface RequalificationInterimCdiRequest {
  motifInterimInvoque: MotifInterimInvoque;
  motifInterdit: boolean;
  motifInterditType?: MotifInterditTypeInterim | null;
  successionMissions: MissionInterim[];
  delaiCarenceRespecte: boolean;
  dureeMissionsTotaleMois: number;
  salaireMensuelBrutEur: number;
  dateFinDerniereMission: string;
  memeEntrepriseUtilisatrice: boolean;
}

export interface RequalificationInterimCdiResponse {
  caseFileId: string;
  motifInterimInvoque: MotifInterimInvoque;
  motifInterdit: boolean;
  motifInterditType: MotifInterditTypeInterim | null;
  successionMissions: MissionInterim[];
  delaiCarenceRespecte: boolean;
  dureeMissionsTotaleMois: number;
  salaireMensuelBrutEur: number;
  dateFinDerniereMission: string;
  memeEntrepriseUtilisatrice: boolean;
  scoreRequalification: number;
  verdictProbabiliteRequalification: 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
  indemniteRequalificationEur: number;
  indemniteFinMissionInterimEur: number;
  totalDommagesIndemniteEur: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

---

## Références

- Pattern canonique : `ai-skills/frontend-coherence-audit.md` §5.
- Pattern jumeau direct : `frontend/src/app/case-files/requalification-cdd-cdi-section/` (F-DT-22-02).
- Pattern jumeau verdict + multi-cartes : `frontend/src/app/case-files/divorce-faute-section/` (F-FA-09-02).
- Pré-fill IA : `frontend/src/app/case-files/immigration-title-decision-section/` (F-IM-05-03).
- Builder F-IA-03 : `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts` (SF-155-05).
- Backend contrat figé : `docs/features/F-DT-23/SF-DT-23-01-backend-requalification-interim-cdi.md` (à mergé en parallèle).
