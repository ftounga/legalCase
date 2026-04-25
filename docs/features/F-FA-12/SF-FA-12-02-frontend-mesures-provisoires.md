# Mini-spec — F-FA-12 / SF-FA-12-02 Mesures provisoires (art. 254 Cciv) — FRONTEND

## Objectif

Composant Angular `<app-mesures-provisoires-section>` qui consomme l'API SF-FA-12-01 (PR #573 mergée) pour offrir au cabinet un assistant à la préparation de l'audience d'orientation et sur mesures provisoires (AOMP), avec pré-remplissage IA et validation cohérence F-IA-03.

**Outil single-country FR** (BE = F-FA-11). Intégré au panel F-IA-04 via `TOOL_REGISTRY` (tool_id `F-FA-12-mesures-provisoires`).

## Contrat API (importé de SF-FA-12-01)

### Endpoint
`POST` + `GET` `/api/v1/case-files/{caseFileId}/mesures-provisoires`

### Request (body POST)
```json
{
  "dateAudienceAOMP": "2026-06-15",
  "revenusEpouxDemandeurEur": 3500.00,
  "revenusEpouxDefendeurEur": 2000.00,
  "logementCommunDescription": "Maison Lyon 5e, 130m²",
  "logementProprietaire": "EN_INDIVISION",
  "enfantsMineurs": [{ "prenom": "Léa", "age": 8 }],
  "souhaitResidenceEnfants": "ALTERNEE",
  "violencesAlleguees": false,
  "patrimoineCommunIsignificatif": true,
  "demandeMesureConservatoire": false
}
```

### Response (200)
```json
{
  "caseFileId": "uuid",
  "dateAudienceAOMP": "2026-06-15",
  "revenusEpouxDemandeurEur": 3500.00,
  "revenusEpouxDefendeurEur": 2000.00,
  "logementCommunDescription": "...",
  "logementProprietaire": "EN_INDIVISION",
  "enfantsMineurs": [{ "prenom": "Léa", "age": 8 }],
  "souhaitResidenceEnfants": "ALTERNEE",
  "violencesAlleguees": false,
  "patrimoineCommunIsignificatif": true,
  "demandeMesureConservatoire": false,
  "country": "FRANCE",
  "differentielRevenus": 1500.00,
  "pensionAlimentairePropose": 750.00,
  "attributionLogementRecommande": "DEFENDEUR",
  "residenceEnfantsRecommande": "ALTERNEE",
  "contributionCharges": 1750.00,
  "mesureConservatoireRecommande": false,
  "scoreCohesionMesures": 100,
  "verdictAcceptabilite": "ELEVEE",
  "formule": "Mesures provisoires (art. 254 Cciv) : score cohésion 100/100 — ELEVEE. PA = 1500/2 = 750 € ; logement DEFENDEUR ; résidence ALTERNEE ; contribution 1750 €.",
  "baseJuridique": "Art. 254-256 Cciv + jurisprudence Cass. 1ère civ.",
  "messages": ["..."]
}
```

### Codes erreurs
- `400` — paramètre requis manquant ou enum invalide (logementProprietaire, souhaitResidenceEnfants), revenu négatif, âge enfant invalide.
- `404` — pas d'analyse persistée pour le dossier (réponse ignorée — on reste en mode formulaire).
- `403` — workspace différent / RBAC.

## Comportement nominal

1. Au mount, gate `workspaceCountry === 'FRANCE'`. Sinon : bannière info redirigeant vers F-FA-11 (BE), aucun appel HTTP.
2. `GET` initial : si 200 → form masqué, résultat affiché. Si 404 → form ouvert, on tente `prefillFromAi()`.
3. Form : datepicker `dateAudienceAOMP` (obligatoire), 2 numériques revenus, mat-select `logementProprietaire` (4 options), liste dynamique enfants, mat-select `souhaitResidenceEnfants` (3 options), 3 slide-toggles (`violencesAlleguees`, `patrimoineCommunIsignificatif`, `demandeMesureConservatoire`).
4. POST sur clic "Analyser" : succès → résultat affiché + `dashboardRefresh.triggerRefresh()` + snackbar OK ; erreur → snackbar rouge.
5. Bouton "Modifier" pour revenir au mode form depuis le résultat.

## Affichage du résultat

- **Bannière verdict** (palette navy/or/rouge classique) : ELEVEE = navy clair, MOYENNE = or, FAIBLE = rouge classique.
- **Score** numérique X/100.
- **Cartes recommandations** : logement, résidence, pension alimentaire (€), contribution charges (€), mesure conservatoire (oui/non).
- **Messages** : liste avec citations juridiques en `<code>` JetBrains Mono via `legalCitations` pipe.
- **Base juridique** + **formule** en JetBrains Mono.

## Pré-remplissage IA + alertes F-IA-03

- `@Input() aiData?: MesuresProvisoiresAiData | null` (interface locale FamilleExtractedData rétro-compat, no-op gracieux).
- Champs pré-remplis : `revenusEpouxDemandeurEur`, `revenusEpouxDefendeurEur`, `dateAudienceAOMP`, `violencesAlleguees`, `patrimoineCommunIsignificatif`, `logementProprietaire` (provenance signal présent).
- Signal `provenanceXxx` par champ pré-rempli + badge `auto_awesome` "Pré-rempli depuis l'analyse".
- Handlers `onXxxChange()` qui remettent provenance à `null` au changement manuel.
- `coherenceAlerts` computed via `CoherenceAlertBuilder` partagé sur 4 champs : `REVENUS_DEMANDEUR`, `REVENUS_DEFENDEUR`, `DATE_AUDIENCE`, `VIOLENCES`.
- Sources F-IA-03 multi : IA / F96 / QUESTION_IA / PIECE_MANQUANTE.
- Directive `[appCoherencePopover]` câblée sur 4 fields (les 4 audités, > 3).
- Sévérité `CRITICAL` pour `VIOLENCES` quand l'IA a détecté des violences mais l'avocat ne les a pas reportées.

## Critères d'acceptation vérifiables

- AC1 : composant standalone `app-mesures-provisoires-section` mount sans erreur en FR.
- AC2 : gate BE → bannière info, aucun appel HTTP.
- AC3 : pré-fill IA fonctionnel (5+ champs pré-remplissables).
- AC4 : badges provenance IA visibles puis effacés au changement manuel.
- AC5 : `coherenceAlerts` réagit aux divergences IA / F96 / QUESTION_IA / PIECE_MANQUANTE (4 fields audités).
- AC6 : POST → résultat affiché + dashboardRefresh + snackbar succès.
- AC7 : form invalide → bouton submit disabled.
- AC8 : entrée TOOL_REGISTRY symétrique avec autres outils famille (`aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`).
- AC9 : self-check pré-commit pass tous seuils.

## Plan de test minimal (Jest)

1. mount sans erreur (FRANCE).
2. gate BELGIQUE → form non rendu, GET non appelé.
3. formValid faux si dateAudienceAOMP null.
4. formValid vrai si tous champs requis remplis.
5. formValid faux si revenu négatif.
6. GET 200 → result affiché, showForm=false, badges IA absents.
7. GET 404 → form ouvert, pré-fill IA appliqué.
8. pré-fill IA accepte revenusAnnuelsEpoux1Eur (annuel /12).
9. POST succès → result affiché, snackbar, dashboardRefresh appelé.
10. POST erreur 400 → snackbar rouge.
11. POST ignoré si form invalide.
12. onRevenusDemandeurChange efface badge IA.
13. onDateAudienceChange efface badge IA.
14. coherenceAlerts.REVENUS_DEMANDEUR si écart > 10 %.
15. coherenceAlerts.REVENUS_DEMANDEUR absent si écart ≤ 10 %.
16. coherenceAlerts.DATE_AUDIENCE si IA et user divergent.
17. coherenceAlerts.VIOLENCES sévérité CRITICAL si IA=true et user=false.
18. coherenceAlerts multi-source (IA + F96 + PIECE_MANQUANTE) → MULTI.
19. alertes masquées après résultat (showForm=false).
20. alertBadgeLabel reflète la source (IA / F96 / MULTI).
21. alertTooltip ajoute "Contredit" si > 1 contributor.
22. ngOnChanges propage les inputs.
23. toggleCollapse fonctionne.
24. editMode ré-affiche form.
25. addEnfant / updateEnfantPrenom / updateEnfantAge / removeEnfant.
26. verdictBannerClass renvoie strong/medium/weak.
27. residenceCardClass mappe les 4 valeurs.

## Tables / endpoints / composants impactés

- **Endpoint** : aucun nouveau, consomme `/api/v1/case-files/{id}/mesures-provisoires` (SF-FA-12-01).
- **Frontend model** : `frontend/src/app/core/models/mesures-provisoires.model.ts` (nouveau).
- **Frontend service** : `frontend/src/app/core/services/mesures-provisoires.service.ts` (nouveau).
- **Frontend composant** : `frontend/src/app/case-files/mesures-provisoires-section/` (nouveau, 4 fichiers).
- **TOOL_REGISTRY** : ajout entrée `F-FA-12-mesures-provisoires` dans `decisional-tools-panel.component.ts`.

## Hors périmètre

- Backend : déjà mergé (PR #573).
- Pipeline IA Famille étendu (FamilleExtractedData partagée) : interface locale `MesuresProvisoiresAiData` suffit ici, harmonisation centrale = SF future.
- Génération de l'écrit JAF (placeholder F-DT-XX générateur de document) : hors scope V1.
- Intégration F-IA-04 backend visibility rule (ALWAYS_ON FR DROIT_FAMILLE) : déjà migrée par SF-FA-12-01.

## Pattern de référence (cohérence frontend §5 du skill)

- **Template canonique** : `harcelement-licenciement-nul-section` (F-DT-11-02).
- **Miroirs famille** : `divorce-alteration-section` (F-FA-08-02), `divorce-faute-section` (F-FA-09-02), `divorce-accepte-section` (F-FA-10-02).
- Palette navy/or/rouge classique — pas de rouge dominant (pas d'urgence < 72h).
- Datepicker `<input type="date">` natif.
- `CoherenceAlertBuilder` partagé pour les alertes — pas d'interface locale ad-hoc.

## Impact par domaine métier

Cette feature est **sensible au domaine** (droit famille) et **single-country FR** (art. 254-256 Cciv).
- Droit du travail : non applicable (procédure JAF spécifique au divorce).
- Immigration : non applicable.
- Famille FR : couverte ici.
- Famille BE : couverte par F-FA-11 (procédure différente — désunion irrémédiable, art. 229 CC).

## Parité des domaines métier

L'outil livre un **scoring (niveau 5)** : `scoreCohesionMesures` 0-100 + verdict ELEVEE/MOYENNE/FAIBLE.
- Travail : pas d'équivalent strict (l'AOMP est une procédure JAF — pas d'équivalent prud'hommes).
- Immigration : pas d'équivalent (les mesures provisoires immigration sont déjà dans F-IM-08 OQTF + F-IM-06 recours).
- Famille BE : F-FA-11 est l'équivalent procédural — différent algorithme (loi belge, pas la même fourchette de mesures).

Asymétrie justifiée par la spécificité procédurale (AOMP ≠ équivalent direct des autres domaines).

## Préoccupations transversales

- Auth / Principal : aucun nouveau type, consomme l'API existante.
- Workspace context : isolation par `workspace_id` déjà gérée backend.
- Plans / limites : aucun nouveau gate — outil intégré au plan standard.
- Navigation / routing : aucune nouvelle route — composant section dans panel F-IA-04 existant.
- Outil décisionnel métier : nouveau composant — scan canonique vs `harcelement-licenciement-nul-section` réalisé via §6 checklist (palette navy/or/rouge classique, `<input type="date">`, gate FR via bannière info, `CaseDashboardRefreshService.triggerRefresh()`, `MatSnackBar`, JetBrains Mono pour formule + baseJuridique, pré-fill IA + F-IA-03 avec `CoherenceAlertBuilder` partagé).

## Nouveau pattern UI ou service partagé

Aucun. Le composant suit strictement le template canonique et utilise les services partagés existants (`CoherencePopoverTriggerDirective`, `CoherenceAlertBuilder`, `CaseDashboardRefreshService`, `LegalCitationsPipe`).

## Analyse de cohérence transversale

- Outils famille existants (F-FA-05/06/07/08/09/10/13) : tous suivent le pattern canonique avec `aiData` + `procedureChecks` + `aiQuestions` + `piecesManquantes` — entrée TOOL_REGISTRY symétrique.
- BE : F-FA-11 sera le miroir BE de F-FA-12 (à venir, procédure désunion irrémédiable distincte).
- Pas de nouveau pattern UI introduit.
