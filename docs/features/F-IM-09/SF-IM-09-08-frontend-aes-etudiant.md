# Mini-spec — F-IM-09 / SF-IM-09-08 AES voie étudiante — FRONTEND

## Identifiant
`F-IM-09 / SF-IM-09-08`

## Feature parente
`F-IM-09` — AES 4 motifs distincts (🔴 critique)

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-IM-09-08-frontend-aes-etudiant`

## Contrat API (importé de SF-IM-09-04 backend, mergée PR #505)

- **POST** `/api/v1/case-files/{caseFileId}/aes-etudiant` — calcul + persistance.
- **GET** `/api/v1/case-files/{caseFileId}/aes-etudiant` — lecture (404 si jamais calculé).
- Request body :
  - `dateEntreeFrance` : ISO date YYYY-MM-DD (requis, non futur)
  - `dureePresenceMois` : entier 0-600 (requis)
  - `anneesScolariteEnFranceConsecutives` : entier 0-20 (requis)
  - `niveauEtudesActuel` : string enum requis (`LYCEE` | `BAC_PLUS_1_2` | `BAC_PLUS_3_4` | `BAC_PLUS_5_PLUS`)
  - `resultatsAcademiques` : string enum requis (`EXCELLENT` | `MOYEN_PASSABLE` | `DIFFICULTES_REPETEES`)
  - `inscriptionEtablissementReconnu` : boolean
  - `moyensSubsistance` : boolean
  - `menaceOrdrePublic` : boolean
  - `parcoursCoherent` : boolean
  - `dateDepotDemande` : ISO date nullable (≥ dateEntreeFrance)
- Response :
  - `caseFileId`, all input fields,
  - `country: 'FRANCE'`
  - `presence3AnsOk: boolean`, `scolarite2AnsConsecutivesOk: boolean`, `resultatsAcceptables: boolean`,
    `inscriptionValide: boolean`, `moyensOk: boolean`, `pasMenace: boolean`, `parcoursCoherentOk: boolean`
  - `scoreGlobal: int (0-100)`
  - `verdictProbabiliteAcceptation: 'ELEVEE' | 'MOYENNE' | 'FAIBLE'`
  - `criteresNonRemplis: string[]` (libellés français)
  - `dateExpirationInstructionSiDemande: string | null` (depot + 6 mois)
  - `formule: string`, `baseJuridique: string`, `messages: string[]`
- Codes erreur backend : 400 (validation, niveau/résultat invalides, dureePresenceMois > 600, etc.), 404 (workspace mismatch ou jamais calculé sur GET).

## Objectif

Implémenter la section frontend **AES voie étudiante (FR)** consommant l'API SF-IM-09-04. Section autonome conforme au template canonique F-155 SF-155-04 — pré-fill IA, alertes de cohérence F-IA-03, gate FRANCE, palette navy/or, JetBrains Mono pour `formule` / `baseJuridique`.

## Comportement nominal

1. Au mount : GET → si 200, affiche le résultat persistant + remplit form ; si 404, mode formulaire avec pré-fill IA optionnel.
2. L'avocat saisit / confirme les 9 champs (8 utiles + niveau/résultats par enum select).
3. POST → backend calcule + persiste.
4. Affichage du résultat :
   - Bannière verdict (vert si `verdictProbabiliteAcceptation='ELEVEE'`, or si `MOYENNE`, rouge si `FAIBLE` — alerte critique justifiée par rejet AES = risque éloignement).
   - 7 cartes critères (présence / scolarité / résultats / inscription / moyens / ordre public / parcours), icônes check_circle ↔ cancel.
   - Score global affiché en JetBrains Mono.
   - Date d'expiration de l'instruction si dépôt fait.
   - chips `criteresNonRemplis`.
   - messages avec `legalCitations` pipe.
5. Le bouton "Modifier" repasse en mode formulaire ; rappel des valeurs précédentes pré-remplies.

## Cas d'erreur

- 400 backend (validation enum, dureePresenceMois > 600) : snackbar rouge avec message backend.
- 404/erreur réseau : snackbar rouge "Erreur lors du calcul AES" — pas de crash.
- Workspace BELGIQUE : bannière info "AES voie étudiante — régime français uniquement" + form masqué (pas d'appel HTTP).
- aiData non disponible : pas de pré-fill, form vide.

## Critères d'acceptation vérifiables

- [x] Composant standalone `aes-etudiant-section`.
- [x] Form 9 champs (date entrée FR, durée présence mois 0-600, années scolarité 0-20, select niveau études, select résultats académiques, slide-toggle inscription établissement, slide-toggle moyens subsistance, slide-toggle menace ordre public, slide-toggle parcours cohérent, date dépôt optionnelle).
- [x] Validation form : `dateEntreeFrance` non futur + `dureePresenceMois` ∈ [0..600] + `anneesScolariteEnFranceConsecutives` ∈ [0..20] + niveau et résultats sélectionnés + dateDepotDemande ≥ dateEntreeFrance si fournie.
- [x] POST envoie tous les champs ; `dateDepotDemande` omis si vide.
- [x] Bannière verdict palette navy/or/rouge (rouge réservé `verdictProbabiliteAcceptation='FAIBLE'`).
- [x] 7 cartes critères avec status (check_circle ↔ cancel).
- [x] `scoreGlobal` rendu en JetBrains Mono dans une carte info.
- [x] `dateExpirationInstructionSiDemande` rendue en JetBrains Mono si dépôt fait.
- [x] chips `criteresNonRemplis` (mat-chip-set).
- [x] `messages[]` rendus en `<ul>` avec `legalCitations` pipe (citations L.412-1 / circulaire Valls).
- [x] `baseJuridique` + `formule` en JetBrains Mono.
- [x] Pré-fill IA : `aiData?.dateDepotProcedure → dateDepotDemande` (no-op gracieux pour les autres champs car `ImmigrationExtractedData` n'expose pas date d'entrée FR ni durée présence ni scolarité).
- [x] Coherence F-IA-03 : `coherenceAlerts` computed sur `DATE_ENTREE_FRANCE` (fail-open, F96 + pièce manquante) + `DUREE_PRESENCE` (F96 + pièce manquante) + `DATE_DEPOT_DEMANDE` (IA divergence + F96 + pièce manquante). Builders en place + popovers + `CoherenceAlertBuilder` partagé (pas de définition locale d'interface `CoherenceAlert`).
- [x] Gate `workspaceCountry` : si `BELGIQUE`, affiche bannière info + cache form/résultat (pas d'appel HTTP).
- [x] `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [x] `MatSnackBar` pour erreurs (pas d'`alert`/`confirm`).
- [x] `<input type="date">` (pas MatDatepicker) — convention F-155 SF-155-07.
- [x] Tests Jest ≥ 12 (mount, validators, submit ok ✓ ELEVEE / submit ok ✗ FAIBLE, submit error, pré-fill IA, badge effacement, coherence alert F96, coherence alert IA divergence, gate BE, gate FR, collapse, edit mode).

## Plan de test

### Tests unitaires Jest (≥12)

1. mount : ngOnInit → GET ; 404 → mode formulaire.
2. mount + GET 200 → result rendu + showForm=false + champs pré-remplis.
3. form invalide bloque le bouton submit (date manquante / mois hors plage / niveau manquant).
4. submit POST → body correct ; verdict='ELEVEE' → bannière succès + snackbar.
5. submit POST → verdict='FAIBLE' → bannière danger + chips criteresNonRemplis.
6. submit POST → erreur 400 → snackbar rouge ; calculating=false.
7. pré-fill IA : `aiData.dateDepotProcedure='2026-04-01'` → champ rempli + provenance='IA'.
8. onDateDepotDemandeChange manuel → provenance effacée.
9. ngOnChanges(aiData) tardif rafraîchit le pré-fill si form vide.
10. coherenceAlerts.DATE_DEPOT_DEMANDE présente si divergence avec aiData.
11. coherenceAlerts.DUREE_PRESENCE depuis F96.
12. coherenceAlerts vides en mode résultat (showForm=false).
13. workspaceCountry=BELGIQUE → bannière info, pas d'appel HTTP.
14. workspaceCountry=FRANCE → form visible, GET émis.
15. toggleCollapse fonctionne.

### Tests d'intégration / non-régression

- Compilation TypeScript stricte : `tsc --noEmit -p tsconfig.app.json` vert.
- Tests existants (1500+ specs) inchangés.

### Tests d'isolation workspace

- Pas applicable côté frontend (backend gère). Tests backend SF-IM-09-04 couvrent.

## Tables / endpoints / composants impactés

- **Endpoints** : POST/GET `/api/v1/case-files/{id}/aes-etudiant` (consommés, déjà déployés).
- **Composants nouveaux** :
  - `frontend/src/app/case-files/aes-etudiant-section/aes-etudiant-section.component.{ts,html,scss,spec.ts}`
  - `frontend/src/app/core/models/aes-etudiant.model.ts` (Request + Response)
  - `frontend/src/app/core/services/aes-etudiant.service.ts` (HttpClient wrapper)
- **Composants modifiés** : aucun. TOOL_REGISTRY non modifié dans ce scope (mission stricte) ; entrée `F-IM-09-aes-etudiant` ajoutée par push d'intégration ultérieur.

## Hors périmètre

- Modification de `decisional-tools-panel.component.ts` (TOOL_REGISTRY) — explicitement hors scope de la mission.
- Pré-fill IA pour `dureePresenceMois`, `anneesScolariteEnFranceConsecutives`, `niveauEtudesActuel`, `resultatsAcademiques` — `ImmigrationExtractedData` ne les expose pas aujourd'hui (no-op gracieux, structurellement prêt pour activation future).
- Tests E2E Playwright (couverts par smoke globaux).
- Backend : déjà mergé PR #505.

## Analyse de cohérence transversale

| Cible | État | Action |
|---|---|---|
| Pattern verdict banner navy/or/rouge | Identique aes-metiers-tension + aes-famille | Réutilise CSS conventions `--ds-accent-gold` ; rouge réservé `verdictProbabiliteAcceptation='FAIBLE'` |
| Pattern pré-fill IA + provenance signal | Identique aes-metiers-tension SF-IM-09-05 | Adapté au champ `dateDepotProcedure` |
| `<input type="date">` (pas MatDatepicker) | Convention F-155 SF-155-07 | Appliqué aux 2 champs date |
| Gate `workspaceCountry` bannière info | Convention F-IM-08 OQTF FR + AES Métiers en tension | Bannière `aes-banner--info` si BELGIQUE |
| `CoherenceAlertBuilder` partagé | Convention F-155 SF-155-05 | Importé de `shared/coherence-popover/coherence-alert-builder` |
| Coherence popover trigger F-IA-03-15c | Convention F-DT-11 / F-IM-08 / aes-metiers-tension | `CoherencePopoverTriggerDirective` + `SourceExplanationService` injection optionnelle |
| `CaseDashboardRefreshService.triggerRefresh()` après POST | Convention canonique | Appelé en succès POST |
| Citations juridiques `legalCitations` pipe | Convention F-155 SF-155-01 | `[innerHTML]` sur `baseJuridique` + messages |

Toutes les autres voies AES (SF-IM-09-01/02/03 backend ; SF-IM-09-05/06/07 frontend) sont alignées sur le même pattern. Pas de mutualisation prématurée — invariant F-DT-08 (un outil = une situation).

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern. Réutilise tous les patterns existants F-155 SF-155-04/05 (CoherenceAlertBuilder, CoherenceAlert<F>, CoherencePopoverTriggerDirective, LegalCitationsPipe, CaseDashboardRefreshService, MatSnackBar).

## Impact par domaine métier

Cette feature est sensible au domaine **Immigration / FR** uniquement.
- **France** : régime juridique français spécifique (circulaire Valls 28/11/2012 actualisée Darmanin — art. L.412-1 CESEDA), couvert ici.
- **Belgique** : non applicable (système belge distinct ; gate UI bannière info).

Pas d'application Droit du travail / Famille — concept étranger à ces domaines.

## Parité des domaines métier

Outil de niveau **5 (scoring / analyse validité)** car évalue 7 critères pondérés et produit un score 0-100 + verdict ELEVEE/MOYENNE/FAIBLE + `criteresNonRemplis`.

Parité des 3 domaines :
- **Droit du travail** : équivalents fonctionnels niveau 5 = F-DT-08 (validity licenciement) + F-DT-10 (validity rupture conventionnelle) — livrés FR + BE.
- **Famille** : équivalents niveau 5 = F-152 (divorce consentement validity) — livré FR + BE.
- **Immigration** : F-IM-08 (validité OQTF + recours), F-IM-05 (arbre titre), F-IM-09-05 (AES Métiers en tension), F-IM-09-06 (AES famille), F-IM-09-07 (AES humanitaire). Cette SF complète le quatuor AES.

Pas de feature jumelle requise — la SF est la 4ᵉ et dernière voie AES (FR uniquement). La parité globale immigration FR/BE est suivie par F-IM-14 (BE étendue).

## TOOL_REGISTRY (snippet à intégrer ultérieurement, hors scope SF)

```ts
// À ajouter à decisional-tools-panel TOOL_REGISTRY au push d'intégration :
['F-IM-09-aes-etudiant', {
  component: AesEtudiantSectionComponent,
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
