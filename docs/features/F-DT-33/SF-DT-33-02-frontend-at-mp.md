# Mini-spec — F-DT-33 / SF-DT-33-02 Frontend AT/MP (FR)

## Identifiant

`F-DT-33 / SF-DT-33-02`

## Feature parente

`F-DT-33` — Accident du travail / Maladie professionnelle (FR)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-33-02-frontend-at-mp`

---

## Objectif

Composant Angular qui consomme l'endpoint `POST/GET
/api/v1/case-files/{id}/at-mp-analysis` (figé en SF-DT-33-01, mergé PR #649).
3 dispositifs FR : `RECONNAISSANCE_AT` / `RECONNAISSANCE_MP` /
`CONTESTATION_TAUX_IPP`. Verdict ELEVEE / MOYENNE / FAIBLE + délai d'instruction
+ compétence (CPAM / CRRMP / CMRA / TJ_POLE_SOCIAL). Section affichée
conditionnellement par le panel F-IA-04 (tool_id `F-DT-33-at-mp`).

---

## Contrat API (importé de SF-DT-33-01)

### POST `/api/v1/case-files/{caseFileId}/at-mp-analysis`

Voir SF-DT-33-01 pour les schémas exacts. Champs request conditionnels selon
`dispositif` :

- `RECONNAISSANCE_AT` → `dateAccident` + `lieuTravail` +
  `declarationEmployeurDansLes48h` + `certificatMedicalInitial`
- `RECONNAISSANCE_MP` → `numeroTableau` (string ou `HORS_TABLEAU`) +
  `delaiPriseEnChargeRespecte` + `dateExposition` + `certificatMedicalInitial`
- `CONTESTATION_TAUX_IPP` → `tauxFixeParCpam` + `tauxRevendique` +
  `expertiseMedicaleProduite` + `datePremierAvisCpam`

Response 200 : `AtMpResponse` (tous les champs des 3 dispositifs sont présents,
les non applicables sont `null`).

---

## Comportement attendu

### Cas nominal

1. Avocat ouvre la section `<app-at-mp-section>` depuis le panel décisionnel.
2. Composant fait `GET` au mount (FR uniquement) :
   - 200 → bandeau verdict + détails (mode résultat).
   - 404 → mode formulaire vierge.
3. Avocat sélectionne un dispositif via `mat-radio-group` → champs conditionnels
   affichés selon le dispositif.
4. Pré-fill IA gracieux : `aiData.dateLicenciement` → `dateAccident`
   (RECONNAISSANCE_AT). Pas d'autre champ extrait par le pipeline IA actuel.
5. Validation F-IA-03 sur 1 field (`DATE_ACCIDENT`) — divergence si IA détecte
   une date de licenciement (proxy d'accident) et l'avocat saisit une date
   différente.
6. Bouton Analyser → POST → bandeau résultat + `dashboardRefresh`.

### Cas d'erreur frontend

| Situation | Comportement UI |
|-----------|------------------|
| `workspaceCountry = BELGIQUE` | Bannière info "Outil français uniquement — équivalent FEDRIS BE = backlog" |
| Form invalide | Bouton Analyser disabled |
| Erreur backend (400/404/500) | `MatSnackBar` rouge `panelClass: 'snack-error'` |
| Champs conditionnels manquants | Form invalide (gate avant POST) |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] Composants jumeaux FR uniquement : `protection-rp-section` (F-DT-30,
  PR #634), `pse-section` (F-DT-14, PR #627), `refere-prudhomal-section`
  (F-DT-34, PR #618), `mesures-eloignement-section` (F-IM-20). Pattern de
  référence : `protection-rp-section` (verdict 3 niveaux, palette navy/or/rouge,
  bannière BE info).
- [x] Composants multi-dispositifs avec champs conditionnels :
  `mesures-eloignement-section` (Expulsion / IRTF / IAT) — pattern radio
  + champs masqués selon dispositif réutilisé.
- [x] FR vs BE : équivalent BE = procédure FEDRIS — feature jumelle au backlog
  (F-DT-33-BE), confirmé en SF-DT-33-01.
- [x] Domaines : strictement DROIT_DU_TRAVAIL FR.
- [x] UI patterns : palette canonique navy / or / rouge (rouge réservé
  verdict critique — ici "FAIBLE" reste warning, pas critical — pas d'urgence
  procédurale absolue type vice de procédure L.2422-1).
- [x] Pré-fill IA : `aiData.dateLicenciement` → `dateAccident` proxy
  (le pipeline IA n'extrait pas encore `dateAccident` dédié — gracieux).
- [x] Validation F-IA-03 : 1 field (`DATE_ACCIDENT`) multi-sources IA / F96 /
  QUESTION_IA / PIECE_MANQUANTE via `CoherenceAlertBuilder`.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern verdict 3 niveaux + palette | Oui | Réutilisé `protection-rp-section` |
| Pattern multi-dispositifs radio + champs conditionnels | Oui | Réutilisé `mesures-eloignement-section` |
| Helper `CoherenceAlertBuilder` (SF-155-05) | Oui | Réutilisé tel quel |
| `LegalCitationsPipe` (SF-155-01) | Oui | Réutilisé pour `messages` + `baseJuridique` |
| `CaseDashboardRefreshService.triggerRefresh()` | Oui | Câblé après POST succès |
| Entrée TOOL_REGISTRY `F-DT-33-at-mp` | Oui | Ajoutée dans cette SF |
| F-DT-33-BE FEDRIS | Oui | Backlog feature jumelle (déjà tracké en SF-DT-33-01) |

### Décision

- [x] Pattern réutilisé tel quel — pas de nouveau composant partagé.
- [x] Pré-fill IA + validation F-IA-03 sur 1 field obligatoires (RÈGLE FONDAMENTALE).
- [x] Bannière BE info (pas masquage silencieux).

---

## Nouveau pattern UI ou service partagé

Aucun. Composant strictement dédié à F-DT-33. Pas de DTO réutilisable, pas
d'endpoint transversal, pas de directive partagée. Pattern existant respecté.

---

## Impact par domaine métier

- DROIT_DU_TRAVAIL : oui — la procédure AT/MP est strictement spécifique à ce
  domaine (livre IV CSS).
- DROIT_IMMIGRATION : non applicable.
- DROIT_FAMILLE : non applicable.
- FR vs BE : FR uniquement (BE = FEDRIS, feature jumelle au backlog
  confirmée par SF-DT-33-01).

---

## Parité des domaines métier

Hérité de SF-DT-33-01 : aucun équivalent en immigration ni en famille
(concept AT/MP propre au droit du travail). Pas d'asymétrie créée.

---

## Critères d'acceptation

- [x] `<app-at-mp-section>` standalone Angular 19 — palette canonique navy/or/rouge
- [x] `mat-radio-group` dispositif (3 valeurs) → champs conditionnels affichés
- [x] `RECONNAISSANCE_AT` : dateAccident + lieuTravail + declarationEmployeur48h + certificatMedicalInitial
- [x] `RECONNAISSANCE_MP` : numeroTableau (input + bouton "HORS_TABLEAU") + dateExposition + delaiPriseEnChargeRespecte + certificatMedicalInitial
- [x] `CONTESTATION_TAUX_IPP` : tauxFixeParCpam + tauxRevendique + expertiseMedicaleProduite + datePremierAvisCpam
- [x] Gate FR : isFrance() true → GET au mount + form actif. BE → bannière info.
- [x] Pré-fill IA `aiData.dateLicenciement` → `dateAccident` (RECONNAISSANCE_AT seul)
- [x] Validation F-IA-03 sur `DATE_ACCIDENT` multi-sources IA / F96 / QUESTION_IA / PIECE_MANQUANTE via `CoherenceAlertBuilder`
- [x] Bandeau verdict ELEVEE/MOYENNE/FAIBLE — couleurs palette navy/or
- [x] Affichage délai instruction + compétence + expertiseRequise + documentsRequis + risqueRefus
- [x] `MatSnackBar` succès / erreur (pas alert/confirm)
- [x] `CaseDashboardRefreshService.triggerRefresh()` après POST succès
- [x] Inter pour le texte, JetBrains Mono pour `baseJuridique` et `formule`
- [x] `LegalCitationsPipe` sur messages + baseJuridique
- [x] Entrée TOOL_REGISTRY `'F-DT-33-at-mp'` symétrique aux autres
- [x] ≥ 12 tests Jest (gate pays + 3 dispositifs + pré-fill + F-IA-03 + erreurs + ngOnChanges)
- [x] Self-check 5/5 : palette / picker / gate pays / refresh / snackbar

---

## Périmètre

### Hors scope

- Génération PDF de la déclaration AT, du recours CMRA ou de l'assignation TJ
  Pôle Social (autre SF / autre feature future)
- Belgique / FEDRIS (feature jumelle backlog F-DT-33-BE)
- Calcul des prestations en espèces (autre SF)
- Pré-fill IA exhaustif sur RECONNAISSANCE_MP et CONTESTATION_TAUX_IPP
  (le pipeline n'extrait pas encore numéro de tableau, taux IPP, etc.)

---

## Plan de test (Jest, ≥ 12)

### Tests unitaires

1. `FRANCE → isFrance() true, GET appelé au ngOnInit`
2. `BELGIQUE → isFrance() false, aucun appel HTTP (gate pays)`
3. `charge le résultat existant si GET 200 (mode résultat)`
4. `reste en mode formulaire si GET 404`
5. `pré-fill IA : aiData.dateLicenciement → dateAccident pour RECONNAISSANCE_AT`
6. `pré-fill ne s'applique pas pour RECONNAISSANCE_MP ou CONTESTATION_TAUX_IPP`
7. `formValid AT : exige dispositif + dateAccident`
8. `formValid MP : exige dispositif + numeroTableau + dateExposition`
9. `formValid IPP : exige dispositif + tauxFixe + tauxRevendique + datePremierAvisCpam + tauxRevendique > tauxFixe`
10. `calculate() POST AT → résultat ELEVEE + snackbar succès + triggerRefresh`
11. `calculate() POST IPP → tauxRevendique > tauxFixe → POST envoyé`
12. `calculate() erreur backend → snackbar rouge`
13. `coherenceAlerts.DATE_ACCIDENT présent si IA diverge de saisie`
14. `coherenceAlerts.DATE_ACCIDENT absent si form non en mode AT`
15. `bannerClass mappe verdict → classe CSS attendue`
16. `dispositifLabel renvoie le libellé humain`

### Self-check 5/5

| Item | Statut |
|------|--------|
| Palette navy/or (rouge réservé alerte critique) | OK — verdict FAIBLE = warning, pas critical |
| Datepicker convention `<input type="date">` | OK |
| Gate `workspaceCountry` (bannière info BE) | OK |
| `CaseDashboardRefreshService.triggerRefresh()` post-POST | OK |
| `MatSnackBar` (pas alert/confirm) | OK |

---

## Tables / endpoints / composants impactés

| Composant | Impact |
|-----------|--------|
| `frontend/src/app/case-files/at-mp-section/` | Création (4 fichiers) |
| `frontend/src/app/core/models/at-mp.model.ts` | Création |
| `frontend/src/app/core/services/at-mp.service.ts` | Création |
| `decisional-tools-panel.component.ts` | +1 entrée registry `'F-DT-33-at-mp'` |
| `decisional-tools-panel.component.spec.ts` | (optionnel) +1 cas tool |

---

## Analyse d'impact

### Préoccupations transversales

- [x] Aucune préoccupation transversale — section frontend isolée sur un dossier.

### Smoke tests E2E

- [x] Aucun smoke test concerné — outil métier indépendant.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-33-01` (mergée PR #649) — backend.

### Frontend planifié

- Aucun. Cette SF clôt F-DT-33 (FR). Le BE FEDRIS est une feature jumelle à
  scoper séparément.

---

## Notes

- Pré-fill IA volontairement minimal : le pipeline IA n'extrait pas encore
  `dateAccident` dédié (le champ `aiData.dateLicenciement` est utilisé comme
  proxy gracieux, no-op si absent).
- Le bouton "Hors tableau" du dispositif MP set `numeroTableau = HORS_TABLEAU`
  → CRRMP affiché en compétence calculée.
- Le verdict FAIBLE utilise la palette warning (or) — pas la palette critical
  (rouge) qui est réservée aux vices de procédure absolus type L.2422-1
  (réintégration de droit). Ici, "FAIBLE" signifie "à peaufiner", pas
  "vice fatal".
