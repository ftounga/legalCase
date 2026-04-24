# Mini-spec — F-IM-09 / SF-IM-09-05 AES Métiers en tension — FRONTEND

## Identifiant
`F-IM-09 / SF-IM-09-05`

## Feature parente
`F-IM-09` — AES 4 motifs distincts (🔴 critique)

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-IM-09-05-frontend-aes-metiers-tension`

## Contrat API (importé de SF-IM-09-01 backend, mergée PR #504)

- **POST** `/api/v1/case-files/{caseFileId}/aes-metiers-tension` — calcul + persistance.
- **GET** `/api/v1/case-files/{caseFileId}/aes-metiers-tension` — lecture (404 si jamais calculé).
- Request body :
  - `dateEntreeFrance` : ISO date YYYY-MM-DD (requis, non futur)
  - `moisActiviteSalarieeDernieres24Mois` : entier 0-24 (requis)
  - `metierEstEnTension` : boolean
  - `codeMetier` : string nullable (ROME, ex. "N1101")
  - `menaceOrdrePublic` : boolean
  - `contratOuPromesseValide` : boolean
  - `dateDepotDemande` : ISO date nullable (≥ dateEntreeFrance)
- Response :
  - `caseFileId`, all input fields,
  - `country: 'FRANCE'`
  - `presence3Ans: boolean`, `activite12MoisOk: boolean`, `conditionsReunies: boolean`
  - `criteresNonRemplis: string[]` (libellés human-readable français avocat)
  - `dateEligibiliteAtteinte: string | null` (date à laquelle les 3 ans seront atteints)
  - `dateExpirationInstructionSiDemande: string | null` (depot + 6 mois)
  - `formule: string`, `baseJuridique: string`, `messages: string[]`
- Codes erreur backend : 400 (validation), 403 (workspace mismatch), 404 (workspace étranger / jamais calculé sur GET).

## Objectif

Implémenter la section frontend **AES Métiers en tension (FR)** consommant l'API SF-IM-09-01. Section autonome conforme au template canonique F-155 SF-155-04 — pré-fill IA, alertes de cohérence F-IA-03, gate FRANCE, palette navy/or, JetBrains Mono pour `formule`/`baseJuridique`.

## Comportement nominal

1. Au mount : GET → si 200, affiche le résultat persistant ; si 404, mode formulaire avec pré-fill IA optionnel.
2. L'avocat saisit / confirme les 7 champs ; valide le form.
3. POST → backend calcule + persiste.
4. Affichage du résultat : bannière verdict (vert si `conditionsReunies=true`, rouge sinon — alerte critique justifiée par le rejet potentiel d'un dossier d'éloignement), 2 cartes critères (`presence3Ans` ✓/✗, `activite12MoisOk` ✓/✗), date d'éligibilité atteinte si applicable, date d'expiration de l'instruction si dépôt fait, chips `criteresNonRemplis`, messages avec `legalCitations` pipe.
5. Le bouton "Modifier" repasse en mode formulaire ; rappel des valeurs précédentes pré-remplies.

## Cas d'erreur

- 400 backend (validation) : snackbar rouge.
- 403/erreur réseau : snackbar rouge "Erreur lors du calcul AES" — pas de crash.
- Workspace BELGIQUE : bannière info "AES Métiers en tension — régime français uniquement" + form masqué (pas d'appel HTTP).
- aiData non disponible : pas de pré-fill, form vide.

## Critères d'acceptation vérifiables

- [x] Composant standalone `aes-metiers-tension-section` importé via TOOL_REGISTRY existant (clé `F-IM-09-aes-metiers-tension`) — **note** : modification du registry hors scope (CLAUDE.md règle "ne pas modifier decisional-tools-panel"). Le panel résoudra automatiquement le tool_id en y ajoutant l'entrée à un push ultérieur.
- [x] Form 7 champs (date entrée FR, mois activité 0-24, slide-toggle métier en tension, code métier optionnel et conditionnel, slide-toggle menace ordre public, slide-toggle contrat valide, date dépôt optionnelle).
- [x] codeMetier visible UNIQUEMENT si `metierEstEnTension=true`.
- [x] Validation form : dateEntreeFrance présent + moisActivite ∈ [0..24].
- [x] POST envoie tous les champs présents ; null/empty omis pour `codeMetier` + `dateDepotDemande`.
- [x] Bannière verdict : palette **navy** (`--ds-accent-gold`/success) si conditions réunies, **rouge** uniquement si `conditionsReunies=false` (justification : rejet AES + risque éloignement).
- [x] 2 cartes status (présence 3 ans / activité 12/24 mois) avec icône check_circle / cancel.
- [x] `dateEligibiliteAtteinte` formatée + libellé "Éligibilité atteinte le …".
- [x] `dateExpirationInstructionSiDemande` formatée + libellé "Expiration instruction".
- [x] chips `criteresNonRemplis` (mat-chip-set).
- [x] `messages[]` rendus en `<ul>` avec `legalCitations` pipe (citations L.435-4 / loi 2024-42).
- [x] `baseJuridique` + `formule` en `JetBrains Mono`.
- [x] Pré-fill IA : `aiData?.dateDepotProcedure → dateDepotDemande` (no-op gracieux pour `dateEntreeFrance` car `ImmigrationExtractedData` n'expose pas ce champ aujourd'hui).
- [x] Coherence F-IA-03 : `coherenceAlerts` computed sur `DATE_ENTREE_FRANCE` (pour quand l'IA exposera le champ — fail-open) + `MOIS_ACTIVITE` (jamais déclenché en pratique sans source IA — pattern préventif). Builders en place + popovers.
- [x] Gate `workspaceCountry` : si `BELGIQUE`, affiche bannière info + cache form/résultat (pas d'appel HTTP).
- [x] `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [x] `MatSnackBar` pour erreurs (pas d'`alert`/`confirm`).
- [x] Tests Jest ≥ 10 (mount, validators, codeMetier conditionnel, submit ok ✓, submit ok ✗, submit error, pré-fill IA, badge effacement, coherence alert, gate BE, gate FR form, collapse).

## Plan de test

### Tests unitaires Jest (≥10)

1. mount : ngOnInit → GET ; 404 → mode formulaire.
2. mount + GET 200 → result rendu + showForm=false + champs pré-remplis.
3. form invalide bloque le bouton submit (date manquante / mois hors plage).
4. codeMetier visible si metierEstEnTension=true ; masqué sinon.
5. submit POST + body correct (champs optionnels omis si vides) ; conditionsReunies=true → snackbar succès.
6. submit POST conditionsReunies=false → bannière rouge + chips criteresNonRemplis.
7. submit POST → erreur 400 → snackbar rouge ; calculating=false.
8. pré-fill IA : `aiData.dateDepotProcedure='2026-04-01'` → champ rempli + provenance='IA'.
9. onDateDepotChange manuel → provenance effacée.
10. coherenceAlerts vide en mode résultat (showForm=false).
11. workspaceCountry=BELGIQUE → bannière info, pas d'appel HTTP.
12. workspaceCountry=FRANCE → form visible, GET émis.
13. toggleCollapse fonctionne.

### Tests d'intégration / non-régression

- Compilation TypeScript stricte : `tsc --noEmit` vert.
- Tests existants (1500+ specs) inchangés.

### Tests d'isolation workspace

- Pas applicable côté frontend (backend gère). Tests backend SF-IM-09-01 couvrent.

## Tables / endpoints / composants impactés

- **Endpoints** : POST/GET `/api/v1/case-files/{id}/aes-metiers-tension` (consommés, déjà déployés).
- **Composants nouveaux** :
  - `frontend/src/app/case-files/aes-metiers-tension-section/aes-metiers-tension-section.component.{ts,html,scss,spec.ts}`
  - `frontend/src/app/core/models/aes-metiers-tension.model.ts` (Request + Response)
  - `frontend/src/app/core/services/aes-metiers-tension.service.ts` (HttpClient wrapper)
- **Composants modifiés** : aucun (TOOL_REGISTRY non modifié dans ce scope par règle CLAUDE.md ; activation panel ultérieure).

## Hors périmètre

- Modification de `decisional-tools-panel.component.ts` (TOOL_REGISTRY) — ajout d'une entrée `F-IM-09-aes-metiers-tension` reporté à un push d'intégration ultérieur (règle CLAUDE.md).
- Pré-fill IA pour `dateEntreeFrance` (champ absent de `ImmigrationExtractedData` v1) — sera ajouté quand le pipeline IA exposera la date d'entrée.
- Tests E2E Playwright (couverts par smoke globaux).
- Backend : déjà mergé PR #504.

## Analyse de cohérence transversale

| Cible | État | Action |
|---|---|---|
| Pattern verdict banner navy/rouge | Identique à F-IM-08 OQTF FR + F-DT-15 inaptitude | Réutilise CSS conventions `--ds-accent-gold`, rouge classique réservé `conditionsReunies=false` |
| Pattern pré-fill IA + provenance signal | Identique à harcelement-licenciement-nul (F-DT-11) | Adapté au modèle ImmigrationExtractedData |
| `<input type="date">` (pas MatDatepicker) | Convention F-155 SF-155-07 | Appliqué aux 2 champs date |
| Gate `workspaceCountry` bannière info | Convention F-IM-08 OQTF FR | Bannière `aes-banner--info` si BELGIQUE |
| Coherence popover trigger F-IA-03-15c | Convention F-DT-11 / F-IM-08 | `CoherencePopoverTriggerDirective` + `SourceExplanationService` injection optionnelle |
| `CaseDashboardRefreshService.triggerRefresh()` après POST | Convention canonique | Appelé en succès POST |
| Citations juridiques `legalCitations` pipe | Convention F-155 SF-155-01 | `[innerHTML]` sur baseJuridique + messages |

Toutes les autres voies AES (SF-IM-09-02 famille, SF-IM-09-03 humanitaire, SF-IM-09-04 étudiant) sont déjà mergées côté backend et ont leurs propres SF frontend (ad81e30 / a92b17a / etc.) — pas de mutualisation prématurée (chaque outil = une situation, invariant F-DT-08).

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern. Réutilise tous les patterns existants F-155 SF-155-04 (CoherenceAlertBuilder, CoherenceAlert<F>, CoherencePopoverTriggerDirective, LegalCitationsPipe, CaseDashboardRefreshService, MatSnackBar).

## Impact par domaine métier

Cette feature est sensible au domaine **Immigration / FR** uniquement. Régime juridique français spécifique (loi 26/01/2024 — voie expérimentale 2024-2026). Pas d'équivalent BE (système de permis unique distinct, traité par F-IM-14). Pas d'application Droit du travail / Famille.

Pour les 2 pays :
- **France** : couvert ici.
- **Belgique** : non applicable (gate UI bannière info).

## Parité des domaines métier

Outil de niveau **5 (scoring/analyse validité)** car évalue 5 critères cumulatifs et produit un verdict booléen `conditionsReunies` + `criteresNonRemplis`.

Parité des 3 domaines :
- **Droit du travail** : équivalent fonctionnel = F-DT-08 (validity licenciement) + F-DT-10 (validity rupture conventionnelle) — déjà livrés FR + BE.
- **Famille** : équivalent fonctionnel = F-152 (divorce consentement validity) — déjà livré FR + BE.
- **Immigration** : F-IM-08 (validité OQTF + recours) + F-IM-05 (arbre titre) couvrent déjà du scoring. Cette SF est l'outil dédié AES — la parité globale immigration FR/BE est suivie par F-IM-14 (BE étendue).

Pas de feature jumelle requise — la SF est elle-même la parité du Droit du travail (F-DT-08) côté immigration FR voie métiers en tension.

## TOOL_REGISTRY (snippet à intégrer ultérieurement)

```ts
// À ajouter à decisional-tools-panel TOOL_REGISTRY au push d'intégration :
['F-IM-09-aes-metiers-tension', {
  component: AesMetiersTensionSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.synthesis?.immigrationExtractedData,
    procedureChecks: ctx.procedureChecks,
    aiQuestions: ctx.aiQuestions,
    piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
  }),
}],
```
