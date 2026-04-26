# Mini-spec — F-FA-27 / SF-FA-27-02 Frontend PMA / GPA / bioéthique

## Identifiant

`F-FA-27 / SF-FA-27-02`

## Feature parente

`F-FA-27` — PMA / GPA / bioéthique : reconnaissance anticipée PMA couples
(loi bioéthique 2/8/2021, art. 342-9 et s. Cciv), transcription état civil
GPA (Cass. 4 arrêts 18/12/2022, Ménesson), accès aux origines pour enfants
nés de don de gamètes (art. 16-8-1 Cciv, depuis 1/9/2022).

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-27-02-frontend-pma-gpa-bioethique`

---

## Objectif

Exposer dans l'UI Angular l'outil décisionnel PMA / GPA / bioéthique
(art. 342-9 Cciv, Cass. 18/12/2022, art. 16-8-1 Cciv) en consommant
l'API SF-FA-27-01 (PR #656).

---

## Contrat API (importé de SF-FA-27-01-backend)

`POST + GET /api/v1/case-files/{caseFileId}/pma-gpa-bioethique`

Voir SF-FA-27-01-backend-pma-gpa-bioethique.md pour le détail. 3 dispositifs
mutuellement exclusifs :

- `PMA_RECONNAISSANCE_ANTICIPEE`
- `GPA_TRANSCRIPTION_ETAT_CIVIL`
- `DON_GAMETES_ACCES_ORIGINES`

Verdicts : `ELEVEE` / `MOYENNE` / `FAIBLE`. Risque refus : `FAIBLE` /
`MOYEN` / `ELEVE`. Pays : FRANCE.

---

## Comportement attendu

### Cas nominal

1. Le panel F-IA-04 affiche l'outil pour un dossier `DROIT_FAMILLE` FR
   (visibility rule UUID `f1a04001-0000-0000-0000-ee0000000180`,
   priority 89).
2. Au mount : GET. Si 200, mode résultat hydraté ; si 404, mode formulaire.
3. L'avocat sélectionne un **dispositif** (radio) :
   - **PMA** : toggle consentement notarié, date reconnaissance, date PMA,
     toggle conditions d'accès.
   - **GPA** : radio pays (USA / Canada / Royaume-Uni / Grèce / Belgique /
     Autre), toggle parent biologique avéré, toggle décision étrangère
     produite, toggle adoption simple demandée.
   - **DON_GAMETES** : date du don (compare au 1/9/2022), toggle demande
     formelle CAPADD, input numérique âge demandeur.
4. Au submit : POST → bandeau verdict + procédures + risque + délai +
   documents requis + formule + base juridique + messages.
5. Refresh dashboard via `CaseDashboardRefreshService.triggerRefresh()`.

### Cas d'erreur

| Situation | UI |
|----------|------|
| Workspace BE | Bannière info "Outil français uniquement — équivalent BE backlog (loi 6/7/2007 PMA BE)" |
| Form invalide | Bouton désactivé |
| Backend 400 / 404 / 500 | `MatSnackBar` rouge `panelClass: snack-error` |

---

## Analyse de cohérence transversale

- [x] **Autres outils métier** : pattern aligné sur `communaute-universelle-section`
  (PR #654) + `partage-judiciaire-section` (PR #638).
- [x] **Autres pays** : single-country FR (bannière info BE — backlog SF jumelle).
- [x] **Autres domaines** : DROIT_FAMILLE seul (gate côté backend).
- [x] **Autres UI patterns** : section collapsible, bandeau verdict, fieldset
  conditionnel par dispositif.
- [x] **Autres flows transversaux** : auth OidcUser + workspace member resolver.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : 2 fields candidats — `dispositif` (issu
  potentiellement de l'analyse IA) et `dateDon`. Pas de pré-fill multi-source
  identifié dans `FamilleExtractedData` actuel — placeholders en lecture des
  signaux mais aucune entrée IA spécifique ne diverge encore. **Implémenté
  avec convention squelette** + builder partagé (`CoherenceAlertBuilder`)
  pour fields `DISPOSITIF` et `DATE_DON` — montera en charge dès qu'un champ
  IA sera ajouté à `FamilleExtractedData`.
- [x] **Refresh dashboard (F-IA-02)** : `triggerRefresh()` après POST OK.
- [x] **Pré-remplissage IA** : 1 champ `dispositifBioethiqueDetecte`
  (string) ajouté à `FamilleExtractedData`. Pré-fill = no-op gracieux si
  absent.
- [x] **Persistance des inputs** : tous les champs côté backend (SF-FA-27-01).
- [x] **Masquage conditionnel selon type** : ALWAYS_ON FR DROIT_FAMILLE
  (priority 89 — migration 180).
- [x] **Alertes actives après calcul** : N/A.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|--------------|------------|
| Backend SF-FA-27-01 | Oui | Mergé PR #656 |
| Frontend SF-FA-27-02 | Oui | Cette SF |
| BE PMA jumelle (loi 6/7/2007) | Oui | Backlog |

### Décision

- [x] FR uniquement (BE backlog SF jumelle)
- [x] Bannière info BE
- [x] Pré-fill IA stub (champ unique `dispositifBioethiqueDetecte`)

---

## Impact par domaine métier

Cette feature **est sensible au domaine** : `DROIT_FAMILLE` exclusivement.

- **DROIT_FAMILLE FR** : rendu actif, toutes les fonctionnalités UI.
- **DROIT_FAMILLE BE** : bannière info — BE backlog.
- **DROIT_DU_TRAVAIL** : non rendu (visibility rule).
- **DROIT_IMMIGRATION** : non rendu (visibility rule).

---

## Parité des domaines métier

Outil de **niveau 5 (scoring de recevabilité)**. SF-FA-27-01 a justifié
l'absence d'équivalent dans les autres domaines.

---

## Critères d'acceptation

- [x] Composant standalone Angular avec 4 fichiers (TS / HTML / SCSS / SPEC)
- [x] Modèle TS + Service HttpClient (`/api/v1/case-files/{id}/pma-gpa-bioethique`)
- [x] Entrée TOOL_REGISTRY `'F-FA-27-pma-gpa'` (alignée sur tool_id migration 180)
- [x] Gate FR : bannière info si workspaceCountry !== 'FRANCE'
- [x] Form 3 dispositifs avec champs conditionnels exclusifs
- [x] Bandeau verdict (ELEVEE info / MOYENNE warning / FAIBLE critical)
- [x] Liste documents requis + procédures à suivre + délai instruction
- [x] Pré-fill IA stub (`dispositifBioethiqueDetecte`)
- [x] Validation F-IA-03 squelette (`CoherenceAlertBuilder`) sur fields
  `DISPOSITIF` et `DATE_DON`
- [x] `CaseDashboardRefreshService.triggerRefresh()` après POST OK
- [x] `MatSnackBar` succès + erreur (panelClass `snack-error`)
- [x] Self-check 5/5 (palette, datepicker, gate pays, refresh, helper)
- [x] ≥ 12 tests Jest

---

## Périmètre

### Hors scope (explicite)

- Backend (SF-FA-27-01)
- BE PMA / GPA (backlog SF jumelle)
- Génération PDF (acte notarié, requête CAPADD)
- Suivi instruction CRPMA / CAPADD

---

## Plan de test (Jest ≥ 12)

1. Init FRANCE → GET appelé
2. Init BELGIQUE → aucun GET (gate)
3. GET 200 → mode résultat hydraté
4. GET 404 → mode formulaire
5. Switch dispositif PMA → GPA réinitialise les champs PMA
6. Switch dispositif GPA → DON réinitialise les champs GPA
7. formValid PMA tous champs OK
8. formValid GPA tous champs OK
9. formValid DON tous champs OK + ageDemandeur ≥ 0
10. POST PMA dispatch → snackbar succès + showForm false
11. POST GPA dispatch → request body cohérent
12. POST DON dispatch → request body cohérent
13. Erreur backend → snackbar rouge
14. toggleCollapse / editMode
15. bannerClass mappe verdict → classe CSS
16. dispositifLabel renvoie le libellé humain ou code en fallback
17. Pré-fill IA `dispositif` ← `aiData.dispositifBioethiqueDetecte`
18. ngOnChanges(aiData) refresh prefill si form vierge

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — N/A frontend
- [ ] Workspace context — N/A
- [ ] Plans / limites — N/A
- [ ] Navigation / routing frontend — entrée TOOL_REGISTRY uniquement
- [x] Aucune préoccupation transversale — composant isolé

### Smoke tests E2E concernés

- [x] Aucun

---

## Dépendances

### Subfeatures bloquantes

- SF-FA-27-01 (backend) mergée PR #656.

---

## Notes et décisions

- Tool_id : `F-FA-27-pma-gpa` (visibility rule UUID
  `f1a04001-0000-0000-0000-ee0000000180`).
- URL backend confirmée : `/api/v1/case-files/{id}/pma-gpa-bioethique`
  (et NON `pma-gpa-bioethique-analysis`) — alignée sur PmaGpaBioethiqueController.
- Pattern de référence : `communaute-universelle-section` (PR #654, SF-FA-16-02).
- Le pré-fill IA ne dispose pour l'instant que d'un champ stub dans
  `FamilleExtractedData` — extension possible quand le pipeline détectera
  d'autres champs (dateDon, ageDemandeur).
