# Mini-spec — F-FA-25 / SF-FA-25-06 Frontend extension majeurs-protégés

## Identifiant

`F-FA-25 / SF-FA-25-06`

## Feature parente

`F-FA-25` — Majeurs protégés (sauvegarde, habilitation, curatelle simple/renforcée, tutelle, mandat protection future)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-25-06-frontend-extension`

---

## Objectif

Combler la dette frontend de F-FA-25 : étendre le composant existant `majeurs-proteges-section` (créé en SF-FA-25-02 PR #610, qui ne supporte UI-side que SAUVEGARDE et HABILITATION) afin de permettre à l'avocat de saisir et de visualiser les **6 régimes** désormais supportés par le backend (SAUVEGARDE_JUSTICE, HABILITATION_FAMILIALE, CURATELLE_SIMPLE, CURATELLE_RENFORCEE, TUTELLE, MANDAT_PROTECTION_FUTURE) et les **4 nouveaux champs** spécifiques (`incapaciteGestionQuotidienne` art. 472, `altertationGrave` art. 440 al. 3, `mandatPrealableSigne` art. 477, `formeMandatProtection` NOTARIE/SOUS_SEING_PRIVE).

L'écran reste un seul composant — pas de fragmentation par régime — avec **affichage conditionnel** des champs selon le régime sélectionné (UX progressive disclosure).

---

## Contrat API (importé du backend SF-FA-25-03/04/05)

### POST `/api/v1/case-files/{caseFileId}/majeurs-proteges`

Body étendu (les nouveaux champs sont optionnels, backward-compatible) :

```json
{
  "regimeProtectionDemande": "SAUVEGARDE_JUSTICE | HABILITATION_FAMILIALE | CURATELLE_SIMPLE | CURATELLE_RENFORCEE | TUTELLE | MANDAT_PROTECTION_FUTURE",
  "altertationFacultesMentales": true,
  "altertationFacultesPhysiques": false,
  "certificatMedicalCirconstancie": true,
  "dateCertificatMedical": "2026-04-15",
  "consentementPersonneAProteger": false,
  "demandeurFamilial": "ENFANT_MAJEUR",
  "actesEnvisages": ["GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"],
  "urgencePatrimoniale": false,
  "patrimoineSignificatif": true,
  "isolementSocial": false,
  "incapaciteGestionQuotidienne": true,
  "altertationGrave": false,
  "mandatPrealableSigne": false,
  "formeMandatProtection": null
}
```

Réponse étendue :

```json
{
  "...": "champs existants",
  "incapaciteGestionQuotidienne": true,
  "altertationGrave": false,
  "mandatPrealableSigne": false,
  "formeMandatProtection": null,
  "eligible": true,
  "criteresNonRemplis": []
}
```

`formeMandatProtection` est `null | 'NOTARIE' | 'SOUS_SEING_PRIVE'`.

Codes d'erreur inchangés (400 si régime/demandeur null ou pays != FR ; 404 si dossier introuvable ; 200 sinon).

---

## Comportement nominal

1. **Radio régime** étendu de 2 à **6 options** (mat-select existant — déjà cardinalité 6 dans `REGIMES_PROTECTION_FR`, modèle TS déjà à jour). Aucun changement de structure côté modèle, juste l'utilisation effective.
2. **Champs conditionnels** affichés selon le régime sélectionné :
   - `CURATELLE_RENFORCEE` → toggle `incapaciteGestionQuotidienne` (art. 472 Cciv)
   - `TUTELLE` → toggles `altertationGrave` (art. 440 al. 3) + `incapaciteGestionQuotidienne`
   - `MANDAT_PROTECTION_FUTURE` → toggle `mandatPrealableSigne` (art. 477 — le mandat doit avoir été signé avant l'altération) ; si `mandatPrealableSigne === true` → radio supplémentaire `formeMandatProtection` (NOTARIE / SOUS_SEING_PRIVE)
   - `SAUVEGARDE_JUSTICE`, `HABILITATION_FAMILIALE`, `CURATELLE_SIMPLE` → aucun champ supplémentaire
3. **Validation form étendue** : `formValid()` exige les nouveaux champs ssi régime correspondant. Pour MANDAT_PROTECTION_FUTURE, `formeMandatProtection` requis ssi `mandatPrealableSigne === true`.
4. **Bandeau résultat** étendu :
   - Indicateur `eligible` (icône check verte / cross rouge à côté du verdict).
   - Liste `criteresNonRemplis` affichée en chips rouges si non éligible.
5. **Verdict ELEVEE/MOYENNE/FAIBLE** (palette navy/or/rouge classique) : inchangé (déjà OK).
6. **Pré-fill IA** : étendu pour pré-remplir les nouveaux champs depuis `aiData` quand disponibles (`incapaciteGestionQuotidienneDetected`, `altertationGraveDetected`, `mandatPrealableSigneDetected`, `formeMandatProtectionDetected`). Backward-compat : si l'IA ne détecte rien, le composant n'écrase aucune valeur.
7. **Provenance signal** ajouté pour les 4 nouveaux champs.
8. **F-IA-03 popover** : pas de nouveau builder d'alerte spécifique aux nouveaux champs (les 4 builders existants — DATE_CERTIFICAT, ALT_MENTALES, CONSENTEMENT, DEMANDEUR_FAMILIAL — couvrent les divergences les plus critiques. Les nouveaux champs `incapaciteGestionQuotidienne` / `altertationGrave` / `mandatPrealableSigne` sont des **paramètres affinés** déclenchés par le régime — ils ne sont pas pré-remplis depuis 4 sources hétérogènes mais peuvent éventuellement venir de `aiData` seul).

---

## Cas d'erreur

- Pays != FRANCE → bannière info (existant).
- POST 400 → snackbar rouge (existant).
- POST 200 avec `eligible=false` → affichage du bandeau verdict + chips `criteresNonRemplis`.
- Régime sans champ requis (ex. CURATELLE_RENFORCEE sans `incapaciteGestionQuotidienne` coché) → backend retournera `eligible=false` avec `criteresNonRemplis` adapté → affichage UI clair.

---

## Critères d'acceptation

1. Radio régime affiche bien les 6 options groupées visuellement par "intensité" (sauvegarde, habilitation, curatelles, tutelle, mandat).
2. Sélection CURATELLE_RENFORCEE → toggle `incapaciteGestionQuotidienne` apparaît.
3. Sélection TUTELLE → toggles `altertationGrave` + `incapaciteGestionQuotidienne` apparaissent.
4. Sélection MANDAT_PROTECTION_FUTURE → toggle `mandatPrealableSigne` apparaît.
5. Sélection MANDAT_PROTECTION_FUTURE + `mandatPrealableSigne=true` → radio `formeMandatProtection` apparaît.
6. POST envoie correctement les nouveaux champs typés sur tous les régimes.
7. Réponse `eligible=false` → affichage chips `criteresNonRemplis` visible.
8. Régression : SAUVEGARDE_JUSTICE et HABILITATION_FAMILIALE continuent de fonctionner (anciens tests Jest passent).
9. Pré-fill IA conserve son comportement existant + supporte les 4 nouveaux champs si présents dans `aiData`.
10. F-IA-03 popover trigger toujours câblé sur les 4 fields existants (pas de régression).

---

## Plan de test

### Unit Jest (≥ 12 tests)

1. Radio régime affiche 6 options (déjà couvert par test `regimesOptions.length === 6`).
2. Sélection CURATELLE_RENFORCEE → `showIncapaciteGestionQuotidienne()` retourne true.
3. Sélection TUTELLE → `showAltertationGrave()` ET `showIncapaciteGestionQuotidienne()` retournent true.
4. Sélection MANDAT_PROTECTION_FUTURE → `showMandatPrealableSigne()` retourne true.
5. MANDAT_PROTECTION_FUTURE + `mandatPrealableSigne=true` → `showFormeMandatProtection()` retourne true.
6. POST avec CURATELLE_RENFORCEE → body inclut `incapaciteGestionQuotidienne: true`.
7. POST avec TUTELLE → body inclut `altertationGrave: true` + `incapaciteGestionQuotidienne`.
8. POST avec MANDAT_PROTECTION_FUTURE NOTARIE → body inclut `mandatPrealableSigne: true` + `formeMandatProtection: 'NOTARIE'`.
9. POST avec MANDAT_PROTECTION_FUTURE sans mandatPrealable → body inclut `mandatPrealableSigne: false` + `formeMandatProtection: null`.
10. Réponse `eligible=false` + `criteresNonRemplis` → `result()!.eligible === false` + chips rendus.
11. Régression : test existant SAUVEGARDE_JUSTICE + HABILITATION_FAMILIALE doit passer (POST legacy sans champs SF-03+).
12. Pré-fill IA `incapaciteGestionQuotidienneDetected` → champ rempli + provenance IA.
13. Pré-fill IA `mandatPrealableSigneDetected` + `formeMandatProtectionDetected` → champs remplis + provenances IA.
14. Handler `onIncapaciteGestionQuotidienneChange()` efface badge IA.
15. Handler `onMandatPrealableSigneChange(false)` reset `formeMandatProtection` à null (cohérence form).
16. F-IA-03 popover `coherenceAlerts` câblé sur ALT_MENTALES toujours fonctionnel (régression).

`npx jest majeurs-proteges-section --silent` doit afficher PASS sur tous les tests.

### Self-check grep pré-commit (5/5 obligatoires)

```bash
grep -nE "CURATELLE_SIMPLE|CURATELLE_RENFORCEE|TUTELLE|MANDAT_PROTECTION_FUTURE" frontend/src/app/case-files/majeurs-proteges-section/majeurs-proteges-section.component.ts | head -10
grep -nE "incapaciteGestionQuotidienne|altertationGrave|mandatPrealableSigne|formeMandatProtection" frontend/src/app/case-files/majeurs-proteges-section/majeurs-proteges-section.component.ts | head -10
grep -nE "prefillFromAi|coherenceAlerts|CoherenceAlertBuilder" frontend/src/app/case-files/majeurs-proteges-section/majeurs-proteges-section.component.ts
```

---

## Tables / endpoints / composants impactés

### Frontend (modifié)

- `frontend/src/app/case-files/majeurs-proteges-section/majeurs-proteges-section.component.ts` — extension : 4 nouveaux signals input + handlers + provenance + helpers de visibilité conditionnelle + extension `prefillFromAi()` + extension `applyPersistedResult()` + extension `calculate()` body.
- `frontend/src/app/case-files/majeurs-proteges-section/majeurs-proteges-section.component.html` — extension : sections conditionnelles selon régime + chips `criteresNonRemplis` + indicateur `eligible`.
- `frontend/src/app/case-files/majeurs-proteges-section/majeurs-proteges-section.component.scss` — minor : style chips criteresNonRemplis + groupes mat-optgroup.
- `frontend/src/app/case-files/majeurs-proteges-section/majeurs-proteges-section.component.spec.ts` — extension : ≥ 12 nouveaux tests.
- `frontend/src/app/core/models/majeurs-proteges.model.ts` — ajout des 4 champs optionnels au `MajeursProtegesRequest` + 4 champs nullable au `MajeursProtegesResponse` + nouveau type `FormeMandatProtection`.
- `frontend/src/app/core/models/divorce-accepte.model.ts` (FamilleExtractedData) — ajout des 4 nouveaux champs optionnels `*Detected` (lecture seule depuis IA, backward-compat).

### Backend (non touché)

- Endpoints `GET/POST /api/v1/case-files/{id}/majeurs-proteges` déjà supportent les nouveaux champs (SF-03/04 mergées, SF-05 en parallèle).

---

## Hors périmètre

- Toute modification backend (les SF-03/04/05 portent ce contrat).
- Internationalisation (FR uniquement, déjà gate `isFrance()`).
- Rendu PDF / export (couvert par F-87).
- Audit cohérence visuelle global (couvert par F-155).
- Backlog BE administration provisoire art. 488 CC BE — feature séparée à venir.

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils décisionnels** — extension d'outil existant, pas de pattern nouveau. Pattern `progressive disclosure conditionnel sur enum` déjà éprouvé (cf. `divorce-accepte-section` choix `procedureType`).
- [x] **Autres pays** — outil **single-country FR** (gate déjà actif). Pas d'impact BE.
- [x] **Autres domaines** — DROIT_FAMILLE seul (gate déjà actif).
- [x] **UI patterns** — `mat-select` simple (avec future possibilité de `mat-optgroup` si besoin) + toggles existants. Pas de nouveau composant partagé.
- [x] **Flows transversaux** — auth + workspace context inchangés. Pas de nouvelle route.
- [x] **F-IA-03** — pré-fill et builders existants préservés ; nouveaux champs pré-remplissables via `aiData` mais sans nouveau builder d'alerte (les 4 builders existants couvrent les divergences critiques). Hiérarchie sources F-96 > Question IA > IA > Pièce manquante respectée pour les fields existants.
- [x] **F-IA-04 TOOL_REGISTRY** — entry existante conservée, contrat `inputs` inchangé (caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|-----------|
| Composant existant `majeurs-proteges-section` | OUI | Étendu (cette SF) |
| Modèle TypeScript `majeurs-proteges.model` | OUI | Étendu (4 champs) |
| Modèle `FamilleExtractedData` | OUI | Étendu (4 champs IA optionnels) |
| Backend `MajeursProtegesService` | NON | Couvert SF-03/04/05 |
| Pattern `mat-optgroup` | NON | Pas nécessaire — labels déjà clairs |

---

## Impact par domaine métier

Cette feature est **spécifique au domaine DROIT_FAMILLE FRANCE** (gate `legalDomain == DROIT_FAMILLE` + `country == FRANCE` déjà appliqué backend & frontend). Aucun comportement Belgique/Immigration/Travail à prévoir.

L'équivalent BE — administration provisoire art. 488 CC BE — est une feature distincte (backlog).

---

## Parité des domaines métier

L'outil est de niveau 5 (scoring d'éligibilité) déjà livré côté backend pour le domaine FAMILLE FR. Cette SF n'introduit pas un nouveau scoring, elle **expose** ceux déjà calculés. Les équivalents protection des majeurs vulnérables sur les autres domaines sont :

- **Travail (FR/BE)** : non applicable — la protection des majeurs est un sujet de droit civil pur, sans équivalent direct en droit du travail (sauf inaptitude médicale, déjà couverte par F-DT-15).
- **Immigration (FR/BE)** : non applicable — la protection des majeurs étrangers relève de la même procédure JAF côté FR (compétence universelle si résidence FR), donc aucun outil immigration spécifique.
- **Famille BE** : équivalent direct = administration provisoire art. 488 CC BE → feature backlog (non bloquante pour cette SF).

Justification pourquoi pas livré ici : la SF-FA-25-06 est une SF de **rattrapage frontend** d'un outil backend déjà livré, pas un nouvel outil décisionnel. La parité BE est traitée comme feature distincte.

---

## Préoccupations transversales

- **Outil décisionnel métier** : extension d'un outil existant. Pas de switch conditionnel multi-situation (le composant reste dédié au domaine FAMILLE FR — un outil = une situation respecté).
- **Auth / Principal** : inchangé.
- **Workspace context** : inchangé (`@AuthenticationPrincipal` + `caseFileId` resolution déjà OK).
- **Plans / limites** : pas de gate plan supplémentaire.
- **Navigation / routing** : aucune nouvelle route.
