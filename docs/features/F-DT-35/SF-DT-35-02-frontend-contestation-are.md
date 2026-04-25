# Mini-spec — F-DT-35 / SF-DT-35-02 — Frontend Contestation ARE France Travail

## Identifiant

`F-DT-35 / SF-DT-35-02`

## Feature parente

`F-DT-35` — Outil décisionnel "Contestation ARE / France Travail (ex-Pôle emploi)" (FR uniquement)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-35-02-frontend-contestation-are`

---

## Objectif

Livrer la section frontend Angular `<app-contestation-are-section>` qui consomme l'API
SF-DT-35-01 (`POST/GET /api/v1/case-files/{caseFileId}/contestation-are`) pour aider
l'avocat à structurer une contestation de décision France Travail (ex-Pôle emploi)
et à choisir entre recours hiérarchique et contentieux TA.

---

## Comportement attendu

### Cas nominal

1. Le panel F-IA-04 affiche la section quand le tool `F-DT-35-contestation-are-fr`
   est visible (workspaceCountry FRANCE).
2. Au mount, le composant tente un GET. Si 200 → mode résultat. Si 404 → mode formulaire +
   pré-fill IA depuis `aiData.salaireBrutMensuel`, `aiData.dateLicenciement` (fallback
   pour `dateNotificationDecision`) — gracieux.
3. L'avocat saisit : type de décision contestée (6 valeurs), motif (5 valeurs), dates
   (notification, recours hiérarchique proposé), preuves (multiple, 6 valeurs), montant
   contesté €, 2 toggles booléens.
4. Au submit, POST → mode résultat avec :
   - bannière verdict (4 valeurs : RECOURS_HIERARCHIQUE_PRIORITAIRE,
     CONTENTIEUX_TA_DIRECT_POSSIBLE, INSUFFISAMMENT_FONDE, AUTRE_VOIE)
   - score `scoreSuccessProbable` (/100)
   - 2 cartes parallèles (recours hiérarchique 2 mois / contentieux TA 2 mois) avec
     `delaiRecoursHierarchiqueJoursOk` et `delaiRecoursContentieuxTaJoursOk`
   - délai d'instruction prévisionnel (en mois)
   - bandeau "expertise traitement salaire recommandée" si flag true
   - liste de messages
   - `baseJuridique` + `formule` en JetBrains Mono
5. Bouton "Modifier" retourne en mode formulaire avec valeurs pré-remplies depuis le
   résultat persisté.
6. `CaseDashboardRefreshService.triggerRefresh()` appelé après POST 200.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Champ obligatoire manquant | Bouton submit désactivé | — |
| workspaceCountry ≠ FRANCE | Bannière info "France uniquement" + form masqué | — |
| POST 400 (validation backend) | MatSnackBar erreur 5s | 400 |
| POST 403 (workspace ≠) | MatSnackBar "Accès refusé" | 403 |
| POST 5xx | MatSnackBar erreur générique | 5xx |
| GET 404 (aucune analyse) | Mode formulaire + pré-fill IA gracieux | 404 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils décisionnels existants** : 56 outils — patterns établis (référés admin
      F-IM-08-08, harcèlement F-DT-11-02). Pas de duplication de logique métier.
- [x] **Autres pays** : Belgique — pas d'équivalent (l'ONEM belge a sa propre procédure ;
      F-DT-35 reste FR uniquement comme la SF backend).
- [x] **Autres domaines** : transversale au domaine droit du travail FR. Pas applicable
      à immigration ou famille.
- [x] **UI patterns** : pré-fill IA + validation F-IA-03 (alertes cohérence) déjà
      consommés via `CoherencePopoverTriggerDirective` + `CoherenceAlertBuilder` (shared).
- [x] **Aucun nouveau pattern partagé introduit** — réutilise toute la stack existante.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — alertes sur `DATE_NOTIFICATION` (vs
      `aiData.dateLicenciement` fallback) et `MONTANT_CONTESTE` (vs salaire mensuel × N).
- [x] **Refresh dashboard (F-IA-02)** : oui — `triggerRefresh()` après POST.
- [x] **Pré-remplissage IA** : oui — `prefillFromAi()` invoqué dans `ngOnInit()` et
      `ngOnChanges()`.
- [x] **Persistance des inputs** : pas applicable côté frontend (géré par backend
      SF-DT-35-01).
- [x] **Masquage conditionnel selon type** : géré par F-IA-04 backend (visibilité tool).
- [x] **Alertes actives après calcul** : gate `!this.showForm()` strict — pas de
      duplication d'alertes en mode résultat.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pré-fill IA `dateNotification` depuis `aiData.dateLicenciement` | Oui | Intégré (fallback gracieux) |
| Pré-fill IA `motifContestation` | Non | Pas de mapping IA fiable côté Travail |
| Validation F-IA-03 `DATE_NOTIFICATION` | Oui | Intégré (divergence avec dateLicenciement) |
| Validation F-IA-03 `MONTANT_CONTESTE` | Non — l'IA n'a pas de référence | Skip — pas de source de vérité IA |
| Belgique équivalent | Non | Pas dans le périmètre F-DT-35 (procédure ONEM différente) |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable Belgique : justifié (procédure ONEM différente, hors périmètre
      F-DT-35 stricte FR)

---

## Contrat API (importé de SF-DT-35-01)

```typescript
export type TypeDecisionContestee =
  | 'REFUS_OUVERTURE_DROITS'
  | 'MONTANT_INDEMNITE'
  | 'DUREE_INDEMNITE'
  | 'RADIATION'
  | 'TROP_PERCU'
  | 'AUTRE';

export type MotifContestationAre =
  | 'ERREUR_CALCUL_REMUNERATION_REFERENCE'
  | 'MAUVAISE_QUALIFICATION_RUPTURE'
  | 'OMISSION_PERIODES_TRAVAIL'
  | 'REFUS_INJUSTIFIE'
  | 'AUTRE';

export type PreuveAre =
  | 'BULLETIN_PAIE'
  | 'ATTESTATION_EMPLOYEUR'
  | 'CERTIFICAT_TRAVAIL'
  | 'LETTRE_LICENCIEMENT'
  | 'JUGEMENT_PRUDHOMAL'
  | 'AUTRE';

export type VerdictAre =
  | 'RECOURS_HIERARCHIQUE_PRIORITAIRE'
  | 'CONTENTIEUX_TA_DIRECT_POSSIBLE'
  | 'INSUFFISAMMENT_FONDE'
  | 'AUTRE_VOIE';

export interface ContestationAreRequest {
  typeDecisionContestee: TypeDecisionContestee;
  motifContestation: MotifContestationAre;
  dateNotificationDecision: string;       // YYYY-MM-DD
  dateRecoursHierarchiquePropose: string; // YYYY-MM-DD
  preuvesProduites: PreuveAre[];
  montantContesteEur: number;
  demandeurDejaSaisiTribunal: boolean;
  delaiContestationRespecte: boolean;
}

export interface ContestationAreResponse {
  caseFileId: string;
  // Inputs reflétés
  typeDecisionContestee: TypeDecisionContestee;
  motifContestation: MotifContestationAre;
  dateNotificationDecision: string;
  dateRecoursHierarchiquePropose: string;
  preuvesProduites: PreuveAre[];
  montantContesteEur: number;
  demandeurDejaSaisiTribunal: boolean;
  delaiContestationRespecte: boolean;
  // Outputs
  delaiRecoursHierarchiqueJoursOk: boolean;
  delaiRecoursContentieuxTaJoursOk: boolean;
  scoreSuccessProbable: number; // 0..100
  verdictRecommandation: VerdictAre;
  delaiInstructionMoisPrevisionnel: number;
  expertiseTraitementSalaireRecommandee: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

Endpoints :

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|--------------|
| POST | `/api/v1/case-files/{caseFileId}/contestation-are` | Oui | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/contestation-are` | Oui | LAWYER |

---

## Critères d'acceptation

- [x] Section `<app-contestation-are-section>` standalone créée.
- [x] Mat-select sur `typeDecisionContestee` (6 options) avec labels FR.
- [x] Mat-select sur `motifContestation` (5 options) avec labels FR.
- [x] Mat-select multiple sur `preuvesProduites` (6 options) avec labels FR.
- [x] 2 datepickers `<input type="date">` (notif + recours hiérarchique proposé).
- [x] Champ numérique `montantContesteEur` (≥ 0).
- [x] 2 mat-slide-toggle (`demandeurDejaSaisiTribunal`, `delaiContestationRespecte`).
- [x] POST envoie payload conforme au contrat ; affichage verdict + scores en réponse.
- [x] Bannière verdict avec gradation palette (navy/or selon statut, rouge réservé alerte critique).
- [x] 2 cartes recours hiérarchique vs contentieux TA avec délais OK/KO.
- [x] Délai d'instruction affiché en mois.
- [x] Bandeau "expertise salaire recommandée" si flag true.
- [x] Liste de messages avec citations juridiques.
- [x] `baseJuridique` + `formule` en JetBrains Mono.
- [x] Mention "France Travail" partout (ex-Pôle emploi explicite dans titre).
- [x] Pré-fill IA `aiData.dateLicenciement` → `dateNotificationDecision` si vide.
- [x] Provenance IA `provenanceDateNotification` avec badge "Pré-rempli depuis l'analyse".
- [x] Validation F-IA-03 `DATE_NOTIFICATION` quand divergence avec `aiData.dateLicenciement`.
- [x] Gate `workspaceCountry === 'FRANCE'` : bannière info si BE (pas masquage silencieux).
- [x] `CaseDashboardRefreshService.triggerRefresh()` appelé après POST 200.
- [x] `MatSnackBar` pour erreurs HTTP (pas alert/confirm).
- [x] Service `ContestationAreService` (HttpClient) avec `analyze()` + `get()`.
- [x] Tests Jest ≥ 10 cas couverts (mount, form valid, POST, erreur, gate FR, pré-fill IA).
- [x] `tsc --noEmit` PASS.

---

## Périmètre

### Hors scope

- Belgique : pas d'équivalent dans cette SF (procédure ONEM différente).
- Génération PDF/courrier : pas dans cette SF (pourrait être F-153 ou backlog).
- Intégration `TOOL_REGISTRY` du panel décisionnel : oui mais minimaliste — l'entrée
  est ajoutée pour mappage tool_id `F-DT-35-contestation-are-fr`.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] mount du composant collapsed par défaut
- [ ] toggleCollapse ouvre/ferme la section
- [ ] form invalide tant que champs obligatoires vides → bouton disabled
- [ ] form valide → bouton enabled
- [ ] POST appelle service avec payload conforme au contrat
- [ ] POST OK → `result()` set, `showForm()` false, snackBar appelé, `triggerRefresh` appelé
- [ ] POST erreur → snackBar erreur, `analyzing()` false
- [ ] GET au mount : 200 → mode résultat, 404 → mode formulaire avec pré-fill IA
- [ ] gate workspaceCountry !== FRANCE : bannière info affichée, form masqué
- [ ] pré-fill IA `dateLicenciement` → `dateNotificationDecision` si vide ; provenance 'IA'
- [ ] handler `onDateNotificationChange` efface provenance IA
- [ ] alerte F-IA-03 `DATE_NOTIFICATION` quand user diverge de aiData.dateLicenciement

### Tests d'intégration

Pas applicable côté frontend (consommé par backend SF-DT-35-01 ; intégration E2E à
valider après merge des deux PRs).

### Isolation workspace

- [x] Non applicable — la subfeature frontend n'accède pas directement à la base.
      L'isolation est gérée par le backend (SF-DT-35-01).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (composant section purement décisionnel, pas de route).

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-35-01` — backend Contestation ARE — statut : in-progress (parallélisation
  validée, contrat API figé).

### Questions ouvertes impactées

- [x] Aucune.

---

## Notes et décisions

- Convention "France Travail (ex-Pôle emploi)" mentionnée explicitement dans le titre
  + bannière info pour la cohérence historique avec les avocats.
- Palette navy/or par défaut (palette standard) : pas d'urgence < 72h justifiant
  la palette rouge dominante.
- Pas de mapping IA pour `motifContestation` car les motifs ARE sont spécifiques à la
  décision France Travail elle-même (pas extractibles depuis un dossier de licenciement
  classique). Pré-fill restreint à `dateNotificationDecision` (fallback dateLicenciement).

