# SF-DT-29-02 — Frontend crédit-temps / interruption de carrière BE

## Objectif (1 phrase)

Exposer côté Angular un outil décisionnel "Crédit-temps belge" (CCT 103 + AR
29/10/1997) consommant l'API `/api/case-files/{id}/credit-temps-be-analysis`
livrée par SF-DT-29-01, avec pré-remplissage IA + alertes de cohérence F-IA-03,
gate `workspaceCountry === 'BELGIUM'` (bannière info, pas masquage silencieux).

---

## Contrat API (importé de SF-DT-29-01 backend, figé dans la mini-spec)

### Endpoint

`POST /api/case-files/{caseFileId}/credit-temps-be-analysis`

### Body de requête

```ts
{
  regime: 'AVEC_MOTIF' | 'SANS_MOTIF' | 'FIN_CARRIERE';
  motif?: 'SOINS_ENFANT_LT_8_ANS' | 'SOINS_PARENT_MALADE' | 'FORMATION' | 'AUTRE';
  ancienneteEntrepriseMois: number;       // ≥ 0, entier
  tailleEntrepriseEtp: number;             // ≥ 0
  dureeReductionType: 'CINQUIEME' | 'MOITIE' | 'TEMPS_PLEIN';
  ageDemandeurAnnees: number;              // ≥ 0, ≤ 75
  dateDemande: string;                     // YYYY-MM-DD
}
```

### Réponse

```ts
{
  caseFileId: string;
  regime: 'AVEC_MOTIF' | 'SANS_MOTIF' | 'FIN_CARRIERE';
  eligible: boolean;
  scoreGlobal: number;                          // 0..100
  verdictEligibilite: 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
  criteresNonRemplis: string[];
  indemniteOnemMensuelle: number;               // EUR
  dureeMaximaleMois: number;
  baseJuridique: string;                        // texte JetBrains Mono italique
  formule: string;                              // texte JetBrains Mono
  messages: string[];                           // rendus via LegalCitationsPipe
}
```

### Codes erreurs

- 400 : body invalide (regime manquant, motif requis si AVEC_MOTIF, valeurs
  négatives, etc.).
- 403 : accès dossier interdit (filtre workspace).
- 404 : `caseFileId` inconnu.
- 409 : workspace non BE (renvoyé par le backend si le contrôleur applique
  l'invariant — frontend l'absorbe via snackbar erreur).

---

## Comportement nominal

1. Le composant est toujours instancié par le panel F-IA-04 dès que
   `decision_tool_visibility_rules` retourne `F-DT-29-credit-temps-be`.
2. Si `workspaceCountry !== 'BELGIUM'` → bannière info "Cet outil s'applique
   à la Belgique uniquement". Pas de POST, pas de form (pattern jumeau
   `motif-grave-be-section`, `avantages-conventionnels-be-section`).
3. À l'ouverture (BE) :
   - GET `/api/case-files/{id}/credit-temps-be-analysis` (silencieux 404)
     → si 200, valeurs persistées affichées et résultat rendu.
   - Si 404 et `aiData` présent → `prefillFromAi()` rempli les champs
     pré-remplissables (`ancienneteEntrepriseMois`, `ageDemandeurAnnees`)
     avec badge "Pré-rempli depuis l'analyse" (icône `auto_awesome`).
4. L'avocat complète le formulaire : `regime` + `motif?` + champs numériques
   + `dateDemande`.
5. Submit → POST → refresh `CaseDashboardRefreshService.triggerRefresh()` +
   snackbar succès. Le résultat affiche le bandeau verdict (navy/or selon
   éligibilité), indemnité ONEM mensuelle, durée max, base juridique,
   formule, messages.
6. Bouton "Modifier" pour revenir au formulaire (showForm=true).

## Cas d'erreur

- POST 400 → snackbar rouge `panelClass: 'snack-error'` avec le message
  backend (ou fallback "Erreur lors du calcul").
- POST 403/404/500 → snackbar rouge.
- GET 404 → fallback gracieux : reste en mode formulaire, applique pré-fill IA.

---

## Pré-remplissage IA (RÈGLE FONDAMENTALE — FAIL si absent)

`@Input() aiData?: TravailExtractedData | null` (type existant).

### Champs pré-remplissables

| Champ frontend | Source IA | Règle |
|---|---|---|
| `ancienneteEntrepriseMois` | `aiData.dateEntree` (YYYY-MM-DD) → calcul mois entiers depuis `dateEntree` jusqu'à aujourd'hui | Si `dateEntree` valide ET ≥ 0 |
| `ageDemandeurAnnees` | `aiData.ageDemandeurAnnees` (champ ajouté à `TravailExtractedData`) | Si entier > 0 et ≤ 75 |

`ancienneteEntrepriseMois` est calculé depuis `dateEntree` côté frontend :
`(today.year - dateEntree.year) * 12 + (today.month - dateEntree.month)`,
floor à 0.

### Pattern obligatoire

- Méthode privée `prefillFromAi()` invoquée dans `ngOnInit()` (après GET 404)
  ET dans `ngOnChanges()` si `aiData` change avant première résolution.
- Signal `provenance<Field> = signal<'IA' | null>(null)` par champ pré-rempli :
  - `provenanceAnciennete`
  - `provenanceAge`
- Badge UI `<mat-icon>auto_awesome</mat-icon> Pré-rempli depuis l'analyse`
  affiché si `provenance<Field>() === 'IA'`.
- Handler `onXxxChange()` qui remet le signal à `null` au changement manuel.

### Modification `TravailExtractedData`

Ajouter le champ `ageDemandeurAnnees?: number | null` avec commentaire JSDoc
SF-DT-29-02. Pas-de-régression : champ optionnel, pipeline backend l'extrait
ultérieurement (no-op gracieux côté frontend tant que le backend ne le remonte
pas — provenance reste null).

---

## Validation IA F-IA-03 (RÈGLE FONDAMENTALE — FAIL si absent)

`coherenceAlerts = computed<Partial<Record<CreditTempsAlertField, ...>>>()`
construit avec `CoherenceAlertBuilder` (helper partagé `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`).

### Fields audités

- `ANCIENNETE` — divergence si l'IA a `dateEntree` calculée et l'avocat saisit
  un nombre de mois éloigné (> 6 mois d'écart absolu).
- `AGE` — divergence si l'IA a `ageDemandeurAnnees` et l'avocat saisit un
  nombre éloigné (> 1 an d'écart absolu).

### Sources

- `IA` — analyse du dossier (`aiData`).
- `F96` — `procedureChecks` avec `critereCode === 'CREDIT_TEMPS_ANCIENNETE'`
  ou `'CREDIT_TEMPS_AGE'`.
- `QUESTION_IA` — `aiQuestions` avec mêmes critères et réponse "oui".
- `PIECE_MANQUANTE` — `piecesManquantes` avec mêmes codes (contributor
  enrichissant — pas accroche solo, pattern canonique).

### Directive

Chaque field clé porte `<app-coherence-popover-trigger>` (`appCoherencePopover`)
câblé sur l'alerte computed.

### Hiérarchie sources F-IA-03

F96 > QUESTION_IA > IA > PIECE_MANQUANTE (règle F-IA-03 ; first-source-wins
dans le builder).

---

## Critères d'acceptation vérifiables

1. Composant `CreditTempsBeSectionComponent` standalone, créé sous
   `frontend/src/app/case-files/credit-temps-be-section/`.
2. `@Input() caseFileId: string` requis + `@Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE'`.
3. Bannière info si `workspaceCountry !== 'BELGIQUE'` (pas de masquage).
4. Pré-fill IA fonctionnel (signals, badges, handlers).
5. Validation F-IA-03 fonctionnelle (`coherenceAlerts` + popover trigger).
6. Service `CreditTempsBeService` (POST + GET) avec URL
   `/api/v1/case-files/{id}/credit-temps-be-analysis`.
7. Modèles TS dans `frontend/src/app/core/models/credit-temps-be.model.ts`.
8. Entrée `TOOL_REGISTRY` `'F-DT-29-credit-temps-be'` ajoutée au panel.
9. ≥ 10 tests Jest qui passent (mount, form valid, POST, erreur, pré-fill IA,
   F-IA-03, gate country, ngOnChanges).
10. Self-check grep pré-commit : 3 patterns OK (pas FAIL).
11. SCSS aligné palette navy/or — pas de rouge dominant (calcul indemnitaire,
    pas urgence).

---

## Plan de test minimal

### Unitaires (Jest, ≥ 10)

1. `BELGIUM` → form rendu.
2. `FRANCE` → bannière info, pas de form.
3. GET 200 → résultat affiché, showForm=false, pas de badge IA.
4. GET 404 → reste en formulaire.
5. Pré-fill IA `ancienneteEntrepriseMois` depuis `dateEntree` calculée.
6. Pré-fill IA `ageDemandeurAnnees` depuis aiData.
7. `onAncienneteChange` efface badge IA.
8. `onAgeChange` efface badge IA.
9. `formValid` exhaustif (regime, motif si AVEC_MOTIF, valeurs > 0, date).
10. POST → snackbar succès + `triggerRefresh()`.
11. POST 400 → snackbar erreur.
12. `coherenceAlerts.ANCIENNETE` présent si écart IA > 6 mois.
13. `coherenceAlerts.ANCIENNETE` absent si écart ≤ 6 mois.
14. `coherenceAlerts.AGE` présent si écart IA > 1 an.
15. `ngOnChanges(aiData)` post-mount rafraîchit le pré-fill si form vide.
16. `ngOnChanges(aiData)` post-saisie n'écrase pas la saisie manuelle.

### Intégration

Pas de test backend dans cette SF (frontend-only).

### Isolation workspace

Le filtre workspace est appliqué côté backend ; le frontend ne fait que
consommer l'endpoint avec le `caseFileId`. Pas de leak côté UI (la bannière
country n'est qu'une UX, pas une garantie d'isolation).

---

## Tables / endpoints / composants impactés

### Endpoints

- `POST /api/v1/case-files/{id}/credit-temps-be-analysis` — consommé.
- `GET /api/v1/case-files/{id}/credit-temps-be-analysis` — consommé (silencieux 404).

### Composants

- **Nouveau** : `CreditTempsBeSectionComponent` (+html/scss/spec).
- **Modifié** : `decisional-tools-panel.component.ts` (entrée TOOL_REGISTRY +
  import).
- **Modifié** : `case-analysis.model.ts` (ajout champ
  `ageDemandeurAnnees?: number | null` à `TravailExtractedData`).

### Services

- **Nouveau** : `CreditTempsBeService` (HttpClient wrapper).

### Modèles

- **Nouveau** : `credit-temps-be.model.ts` (Request + Response + enum
  `CreditTempsRegime` + `CreditTempsMotif` + `DureeReductionType`).

---

## Hors périmètre

- Backend SF-DT-29-01 (autre worktree, mergé indépendamment).
- Extraction IA `ageDemandeurAnnees` (le champ est déclaré côté front,
  mais le pipeline backend l'extrait dans une SF ultérieure si
  pertinent — no-op gracieux).
- Dynamisation totale des codes commission paritaire (hors scope, déjà
  documenté SF-DT-28-02).
- Tests E2E.

---

## Analyse de cohérence transversale (RÈGLE CLAUDE.md)

### Autres outils décisionnels FR/BE

- F-DT-08 (Licenciement validity) — séparé FR/BE.
- F-DT-10 (Rupture conv FR) / F-DT-27 (Motif grave BE) — séparés.
- F-DT-28 (Avantages BE) — pattern jumeau utilisé comme référence.
- F-DT-29 (Crédit-temps BE) — cette SF, BE only.

### Autres pays/domaines

- Pas d'équivalent FR direct (l'ARP / RCC / interruption de carrière française
  est un mécanisme différent — pas dans backlog V8). Hors scope.

### Nouveau pattern UI ou service partagé

Aucun. Cette SF réutilise :
- `CoherenceAlertBuilder` (SF-155-05 partagé).
- `CoherencePopoverTriggerDirective` (F-IA-03-15c).
- `CaseDashboardRefreshService`, `MatSnackBar`, `LegalCitationsPipe`.
- Pattern canonique `harcelement-licenciement-nul-section` + jumeau BE
  `avantages-conventionnels-be-section`.

---

## Impact par domaine métier (RÈGLE CLAUDE.md)

- **Droit du travail** : OUI, sensible. Outil décisionnel travail BE.
- **Famille** : non applicable (mécanisme employeur/employé).
- **Immigration** : non applicable.
- Pays : BE uniquement (pas d'équivalent FR direct livré).

Pas de risque d'asymétrie métier — concept BE-only documenté en backlog
PRODUCT_SPEC F-DT-29.

---

## Parité des domaines métier (RÈGLE CLAUDE.md)

Niveau 5 (scoring `verdictEligibilite` ELEVEE/MOYENNE/FAIBLE).

- Famille : non applicable.
- Immigration : non applicable.
- Droit du travail FR : pas d'équivalent V8 (mécanismes ARP / temps partiel
  thérapeutique sont différents et déjà couverts par d'autres outils ou
  laissés au backlog ultérieur — non bloquant).

Pas d'ouverture de feature jumelle nécessaire.
