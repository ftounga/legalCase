# Mini-spec — F-DT-20 / SF-DT-20-02 Frontend Rappel de salaire FR

## Identifiant
`F-DT-20 / SF-DT-20-02`

## Feature parente
`F-DT-20` — Calculateur rappel de salaire FR (art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail + CCN)

## Statut `ready` · Date `2026-04-25` · Branche `feat/SF-DT-20-02-frontend-rappel-salaire`

## Pattern de référence

- **Template canonique** : `harcelement-licenciement-nul-section` (F-DT-11-02), réf. skill `ai-skills/frontend-coherence-audit.md` §5.
- **Pattern sélecteur CCN** : `indemnite-preavis-section` (SF-DT-25-02) — chargement liste CCN via `ConventionReferentialService.list()` filtré FRANCE + alerte cohérence sur `conventionCollective`.
- **Pattern méthode CP enum + radio** : `conges-payes-section` (SF-DT-26-02) — `mat-radio-group` pour `methodeCpSurRappel` (3 options DIX_POURCENT / MAINTIEN / AUCUN).
- **Pattern période 2 datepickers** : `heures-sup-section` (F-DT-19) — 2 `<input type="date">` pour periodeDebut / periodeFin.
- **Helper partagé** : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
- **Contrat API importé de SF-DT-20-01 backend** (parallélisation autorisée — contrat figé dans la mini-spec backend).

---

## Contrat API (importé de SF-DT-20-01)

### Endpoint

```
POST + GET /api/v1/case-files/{caseFileId}/rappel-salaire
```

### Request

```typescript
export type MethodeCpSurRappel = 'DIX_POURCENT' | 'MAINTIEN' | 'AUCUN';

export interface RappelSalaireRequest {
  periodeDebut: string;                       // YYYY-MM-DD
  periodeFin: string;                         // YYYY-MM-DD (≥ periodeDebut)
  montantSalaireDuMensuelEur: number;         // > 0
  montantSalairePerVerseMensuelEur: number;   // ≥ 0, < dû
  conventionCollectiveCode?: string | null;
  ancienneteAnneesPrime: number;              // ≥ 0
  indexInseeRevalorise: boolean;
  tauxRevalorisationPct?: number | null;      // [0,100], requis si indexInseeRevalorise=true
  methodeCpSurRappel: MethodeCpSurRappel;
}
```

### Response

```typescript
export interface RappelSalaireResponse {
  caseFileId: string;
  periodeDebut: string;
  periodeFin: string;
  montantSalaireDuMensuelEur: number;
  montantSalairePerVerseMensuelEur: number;
  conventionCollectiveCode: string | null;
  ancienneteAnneesPrime: number;
  indexInseeRevalorise: boolean;
  tauxRevalorisationPct: number | null;
  methodeCpSurRappel: MethodeCpSurRappel;
  nbMoisPeriode: number;
  differentielMensuelEur: number;
  totalRappelBrutHorsRevalorisationEur: number;
  montantRevalorisationEur: number;
  primeAncienneteEur: number;
  totalRappelBrutEur: number;
  congesPayesSurRappelEur: number;
  totalAvecCpEur: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

---

## Objectif

Livrer le composant Angular `<app-rappel-salaire-section>` qui consomme l'endpoint `POST/GET /api/v1/case-files/{id}/rappel-salaire` (SF-DT-20-01) pour calculer le rappel de salaire dû à un salarié (différentiel × période + revalorisation INSEE + prime d'ancienneté CCN + congés payés sur rappel). FR uniquement (concept BE distinct — feature jumelle F-DT-20-BE au backlog).

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier FR (DROIT_DU_TRAVAIL, workspace `country = 'FRANCE'`).
2. Le panel décisionnel F-IA-04 affiche l'outil `F-DT-20-rappel-salaire` (règle `ALWAYS_ON` FR + travail, priority 57).
3. Le composant collapsible affiche en header `RAPPEL DE SALAIRE` + chip montant total avec CP si résultat persisté.
4. À l'ouverture (dépli) : GET → si 200 résultat précédent, affichage du mode résultat ; si 404, formulaire + pré-fill IA depuis `synthesis.travailExtractedData`.
5. Formulaire (2 colonnes responsive) :
   - `periodeDebut` (`<input type="date">`, obligatoire)
   - `periodeFin` (`<input type="date">`, obligatoire, ≥ periodeDebut)
   - `montantSalaireDuMensuelEur` (`<input type="number">`, > 0, obligatoire)
   - `montantSalairePerVerseMensuelEur` (`<input type="number">`, ≥ 0, obligatoire ; UI-gate < dû) — pré-rempli depuis `aiData.salaireBrutMensuel` si disponible (provenance IA)
   - `conventionCollectiveCode` (`<mat-select>`, optionnel) — chargé via `ConventionReferentialService.list()` filtré FRANCE ; pré-rempli depuis `aiData.conventionCollective` (matché via `normalizeCode`)
   - `ancienneteAnneesPrime` (`<input type="number">`, ≥ 0, obligatoire)
   - `indexInseeRevalorise` (`<mat-slide-toggle>`) — révèle `tauxRevalorisationPct` quand true
   - `tauxRevalorisationPct` (`<input type="number">`, [0,100], requis si indexInseeRevalorise) — affiché conditionnellement
   - `methodeCpSurRappel` (`<mat-radio-group>` 3 options : DIX_POURCENT / MAINTIEN / AUCUN, obligatoire)
6. Bouton "Calculer" disabled si form invalide ou calcul en cours (`<mat-spinner>`).
7. POST réussi → mode résultat + `MatSnackBar` succès + `triggerRefresh()` du dashboard.
8. Mode résultat (carte multi-blocs) :
   - **Récap période en haut** : `nbMoisPeriode` mois × `differentielMensuelEur` €/mois
   - **Tableau breakdown** :
     - Différentiel brut (`totalRappelBrutHorsRevalorisationEur`)
     - Revalorisation INSEE (`montantRevalorisationEur`)
     - Prime d'ancienneté CCN (`primeAncienneteEur`)
     - Total brut (`totalRappelBrutEur`)
     - CP sur rappel (`congesPayesSurRappelEur`)
     - **Total avec CP** (`totalAvecCpEur` — montant final mis en avant)
   - **`baseJuridique`** + **`formule`** affichés en JetBrains Mono
   - **`messages`** (incluant rappel prescription 3 ans L.3245-1) en liste avec citations juridiques
9. Bouton "Modifier" → repasse en mode formulaire avec valeurs hydratées (provenance IA effacée).

### Pré-fill IA + alertes F-IA-03

- **`salaireBrutMensuel`** → `montantSalaireDuMensuelEur` (champ de référence du salaire dû — l'avocat ajustera si versé < dû). Provenance affichée badge "Pré-rempli depuis l'analyse".
- **`conventionCollective`** → `conventionCollectiveCode` après `ConventionReferentialService.normalizeCode(...)` + match liste FRANCE.
- Alerte `SALAIRE` (WARNING) si `salaireBrutMensuel` IA et `montantSalaireDuMensuelEur` saisi divergent > 10 % (seuil aligné F-DT-25/26/27).
- Alerte `CONVENTION` (WARNING) si code IA normalisé ≠ code saisi.
- `coherenceAlerts = computed<Partial<Record<RappelAlertField, RappelCoherenceAlert>>>()` gate `showForm()` (anti-bug SF-IA-03-12).

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Workspace ≠ FRANCE | Bannière info "Outil France uniquement — concept distinct en BE (prescription 5 ans, pécule)". Pas de form. |
| GET 404 | Mode formulaire + pré-fill IA. |
| GET 5xx | Mode formulaire (fail-open). |
| POST 400 (dû ≤ versé / période invalide / taux hors plage) | `MatSnackBar` rouge avec message backend. |
| POST 4xx (autre) | `MatSnackBar` rouge. |
| POST 5xx | `MatSnackBar` rouge "Erreur lors du calcul". |
| `aiData` null | Pas de pré-fill, pas d'alerte. Form vide. |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Template canonique** : `harcelement-licenciement-nul-section` (F-DT-11-02). Suivi strictement (signals + provenance + computed alerts + CoherenceAlertBuilder).
- [x] **Patterns voisins FR** :
  - `indemnite-preavis-section` (F-DT-25) — sélecteur CCN identique → réutilise `ConventionReferentialService.list()` + `normalizeCode()` + match FRANCE.
  - `conges-payes-section` (F-DT-26) — pattern radio méthode CP enum (3 options ici au lieu de 2 + null).
  - `heures-sup-section` (F-DT-19) — pattern période 2 datepickers.
  - **Aucun composant frontend ne calcule un rappel de salaire.**
- [x] **Autres pays** : Belgique reportée (feature jumelle F-DT-20-BE au backlog) — pas applicable côté frontend FR.
- [x] **Autres domaines** : non applicable — concept exclusif droit du travail.
- [x] **TOOL_REGISTRY** : ajout symétrique d'une entrée `F-DT-20-rappel-salaire` (priority 57 alignée backend).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Composant Angular FR | Oui | Intégré `<app-rappel-salaire-section>` |
| Service `RappelSalaireService` | Oui | Wrapper HttpClient simple (POST + GET) |
| Modèle TS | Oui | `rappel-salaire.model.ts` (Request / Response / enum) |
| Sélecteur CCN | Oui | Réutilise `ConventionReferentialService` (existant) |
| TOOL_REGISTRY entry | Oui | Ajout dans `decisional-tools-panel.component.ts` |
| `prefillFromAi()` + alertes F-IA-03 | Oui (obligatoire) | 2 fields IA : SALAIRE + CONVENTION |
| Frontend BE | Hors périmètre | Bannière info FR-only |

### Décision

- [x] Frontend FR intégré dans cette SF (composant + service + model + spec + TOOL_REGISTRY).
- [x] Aucune harmonisation transversale requise (calque strict template canonique + patterns voisins).

---

## Nouveau pattern UI ou service partagé

Pas de nouveau pattern. La SF :
- Réutilise `ConventionReferentialService` (existant SF-129-01) pour la liste CCN.
- Réutilise `CoherenceAlertBuilder` partagé (SF-155-05).
- Réutilise `CoherencePopoverTriggerDirective` partagée.
- Calque la structure `harcelement-licenciement-nul-section` (signals, computed alerts, ngOnChanges, badges provenance).

Aucune divergence introduite.

---

## Impact par domaine métier

**Sensible au domaine** : spécifique DROIT_DU_TRAVAIL FRANCE.
- **Droit du travail FR** : cœur de la SF.
- **Droit du travail BE** : non applicable (concept distinct, prescription 5 ans, pécule de vacances) — feature jumelle F-DT-20-BE au backlog.
- **Immigration / Famille** : non applicable.

---

## Parité des domaines métier

Outil de **niveau 3** (calculateur). Règle de parité ≥5 ne s'applique pas. Pas d'équivalent en immigration / famille. La parité FR/BE est volontairement différée (SF backlog `F-DT-20-BE`).

---

## Critères d'acceptation

- [ ] **C1** : Composant standalone monté, GET au `ngOnInit()` quand `workspaceCountry='FRANCE'`.
- [ ] **C2** : `workspaceCountry='BELGIQUE'` → bannière FR-only, aucun appel HTTP.
- [ ] **C3** : GET 200 → mode résultat avec valeurs hydratées (tous les champs).
- [ ] **C4** : GET 404 → mode formulaire + pré-fill IA appliqué.
- [ ] **C5** : `formValid()` false si `periodeDebut` > `periodeFin` ou `montantSalaireDuMensuelEur` ≤ 0 ou versé ≥ dû ou méthode CP null ou (indexInseeRevalorise=true et taux invalide).
- [ ] **C6** : POST envoie le contrat exact (incl. enum methodeCpSurRappel + nullable conventionCollectiveCode + nullable tauxRevalorisationPct).
- [ ] **C7** : POST 200 → résultat affiché + snackbar succès + `triggerRefresh()` appelé.
- [ ] **C8** : POST 400 → snackbar rouge avec message backend.
- [ ] **C9** : Pré-fill IA `salaireBrutMensuel` → `montantSalaireDuMensuelEur` + badge IA visible.
- [ ] **C10** : Pré-fill IA `conventionCollective` (après normalize + match FRANCE) → `conventionCollectiveCode`.
- [ ] **C11** : Alerte `SALAIRE` (WARNING) si écart > 10 % entre IA et saisie.
- [ ] **C12** : Alerte `CONVENTION` (WARNING) si normalize(IA) ≠ code saisi.
- [ ] **C13** : Alertes masquées quand `showForm()=false` (anti-bug SF-IA-03-12).
- [ ] **C14** : `onMontantSaisiChange()` manuel efface badge provenance IA.
- [ ] **C15** : Mode résultat affiche la breakdown (récap période + différentiel + revalorisation + prime ancienneté + total brut + CP + total avec CP).
- [ ] **C16** : Messages incluent prescription 3 ans (L.3245-1) — affichés via `LegalCitationsPipe`.
- [ ] **C17** : `tauxRevalorisationPct` field affiché uniquement si `indexInseeRevalorise()=true`.
- [ ] **C18** : Entrée TOOL_REGISTRY `F-DT-20-rappel-salaire` symétrique (inputs : caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes).

---

## Périmètre

### Hors scope (explicite)

- **Backend** — déjà mergé via SF-DT-20-01 (PR #584).
- **Outil rappel de salaire BE** — feature jumelle F-DT-20-BE au backlog (concept distinct).
- **Calcul automatique du taux INSEE** — l'avocat saisit le taux. Pas d'API INSEE intégrée (V8+).
- **Génération document juridique** — l'outil ne produit pas de modèle de mise en demeure (autres outils F-DT).
- **Calcul du net** — la SF affiche le brut.

---

## Technique

### Fichiers créés

```
frontend/src/app/core/models/rappel-salaire.model.ts
frontend/src/app/core/services/rappel-salaire.service.ts
frontend/src/app/case-files/rappel-salaire-section/
  ├── rappel-salaire-section.component.ts
  ├── rappel-salaire-section.component.html
  ├── rappel-salaire-section.component.scss
  └── rappel-salaire-section.component.spec.ts
```

### Fichiers modifiés

```
frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts  (ajout TOOL_REGISTRY)
```

### Composant

- **Standalone** : `standalone: true`
- **Inputs** :
  - `@Input() caseFileId: string` (required)
  - `@Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE'`
  - `@Input() aiData?: TravailExtractedData | null`
  - `@Input() procedureChecks?: ProcedureCheck[] | null`
  - `@Input() aiQuestions?: AiQuestion[] | null`
  - `@Input() piecesManquantes?: PieceManquanteEntry[] | null`
- **Signals** form : `periodeDebut`, `periodeFin`, `montantSalaireDuMensuelEur`, `montantSalairePerVerseMensuelEur`, `conventionCollectiveCode`, `ancienneteAnneesPrime`, `indexInseeRevalorise`, `tauxRevalorisationPct`, `methodeCpSurRappel`.
- **Signals** UI : `collapsed`, `loading`, `calculating`, `showForm`, `result`.
- **Signals** provenance : `provenanceMontantDu`, `provenanceConvention`.
- **Computed** : `coherenceAlerts`, `alertsSummary`.
- **Services injectés** : `RappelSalaireService`, `ConventionReferentialService`, `MatSnackBar`, `CaseDashboardRefreshService` (optional), `SourceExplanationService` (optional).
- **Imports modules** : `CommonModule`, `FormsModule`, `DecimalPipe`, `MatButtonModule`, `MatIconModule`, `MatFormFieldModule`, `MatInputModule`, `MatSelectModule`, `MatRadioModule`, `MatProgressSpinnerModule`, `MatSlideToggleModule`, `LegalCitationsPipe`, `CoherencePopoverTriggerDirective`.

### Service

- `RappelSalaireService` : 2 méthodes `calculate(caseFileId, request): Observable<RappelSalaireResponse>` + `get(caseFileId): Observable<RappelSalaireResponse>`.

### Modèle

- `RappelSalaireMethodeCpSurRappel` (string union 'DIX_POURCENT' | 'MAINTIEN' | 'AUCUN').
- `RappelSalaireRequest` / `RappelSalaireResponse` interfaces.
- `MethodeCpOption` + `METHODES_CP` const pour les radios.

### TOOL_REGISTRY

```typescript
['F-DT-20-rappel-salaire', {
  component: RappelSalaireSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.synthesis?.travailExtractedData,
    procedureChecks: ctx.procedureChecks,
    aiQuestions: ctx.aiQuestions,
    piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
  }),
}],
```

---

## Plan de test

### Tests unitaires (`rappel-salaire-section.component.spec.ts`) — ≥ 14

- [ ] FRANCE → GET appelé au ngOnInit
- [ ] BELGIQUE → aucun appel HTTP, pas de form actif
- [ ] GET 200 → mode résultat avec valeurs hydratées
- [ ] GET 404 → mode formulaire + pré-fill IA
- [ ] formValid false si périodeFin < périodeDebut
- [ ] formValid false si montantSalaireDuMensuelEur ≤ 0
- [ ] formValid false si versé ≥ dû
- [ ] formValid false si indexInseeRevalorise=true et taux invalide
- [ ] formValid false si methodeCpSurRappel null
- [ ] calculate() POST → contrat correct envoyé + résultat + snackbar + triggerRefresh
- [ ] calculate() erreur backend → snackbar rouge
- [ ] calculate() ignoré si form invalide
- [ ] pré-fill IA salaire → montantSalaireDuMensuelEur + badge IA
- [ ] pré-fill IA conventionCollective normalisé + matché
- [ ] aiData null → pas de pré-fill
- [ ] onMontantDuChange manuel efface le badge IA
- [ ] coherenceAlerts.SALAIRE présent si écart > 10 % vs IA
- [ ] coherenceAlerts.CONVENTION présent si codes diffèrent
- [ ] alertes masquées après showForm=false
- [ ] toggleCollapse / editMode helpers
- [ ] tauxRevalorisationPct visible uniquement si indexInseeRevalorise=true

### Isolation workspace

- [x] Couvert backend (SF-DT-20-01). Frontend délègue : pas de logique d'isolation.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context (lecture seule via `@Input() workspaceCountry`)
- [ ] Plans / limites
- [ ] Navigation / routing
- [x] **F-IA-04 visibility rule** — la SF backend a posé la règle ; le frontend ajoute uniquement l'entrée TOOL_REGISTRY.
- [x] **Outil décisionnel métier** — nouveau composant, scan effectué (cf. cohérence transversale ci-dessus). Calque template canonique.

### Composants / endpoints impactés

- `decisional-tools-panel.component.ts` : ajout d'une entrée TOOL_REGISTRY (additif, aucune régression).
- Aucun autre composant existant n'est modifié.

### Smoke tests E2E concernés

- [x] Aucun — composant nouveau, intégration via TOOL_REGISTRY déjà couverte par les smoke tests F-IA-04 existants.

---

## Dépendances

### Subfeatures bloquantes

- SF-DT-20-01 (backend rappel salaire) — **mergé via PR #584**.
- SF-IA-04-01/02/03 (panel + TOOL_REGISTRY) — done.
- SF-129-01 (`ConventionReferentialService`) — done.
- SF-155-05 (`CoherenceAlertBuilder`) — done.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Décision 1** : pré-fill `salaireBrutMensuel` mappé sur `montantSalaireDuMensuelEur` (le salaire dû, pas versé). L'IA extrait le salaire contractuel/dû — l'avocat ajustera le versé manuellement (info absente du dossier généralement).
- **Décision 2** : pas de pré-fill `montantSalairePerVerseMensuelEur` ni `tauxRevalorisationPct` ni `ancienneteAnneesPrime` — ces champs ne sont pas extraits par l'IA travail FR aujourd'hui (champ procédural absent du prompt).
- **Décision 3** : `methodeCpSurRappel` pas de pré-fill, défaut UI `DIX_POURCENT` (pratique majoritaire).
- **Décision 4** : alerte `SALAIRE` seulement si écart > 10 % (seuil canonique aligné F-DT-25/26/27).
- **Décision 5** : champ `tauxRevalorisationPct` masqué tant que `indexInseeRevalorise()=false` — UX cohérente avec slide-toggle "révèle".
- **Décision 6** : pas de comparateur 2 méthodes (≠ F-DT-26) — la méthode CP sur rappel est saisie par l'avocat, pas optimisée. Cohérent avec le contrat backend SF-DT-20-01 qui n'expose pas de variantes alternatives.
- **Décision 7** : palette navy/or standard (pas d'urgence < 72h, pas de palette rouge dominante). Aligné F-DT-25/26.
