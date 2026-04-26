# Mini-spec — F-IM-13 / SF-IM-13-02 Frontend naturalisation (Code civil 21+)

## Identifiant

`F-IM-13 / SF-IM-13-02`

## Feature parente

`F-IM-13` — Naturalisation française (6 voies du Code civil)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-13-02-frontend-naturalisation`

---

## Objectif

Exposer côté frontend Angular l'outil décisionnel de **recevabilité de la naturalisation française** (F-IM-13) en consommant l'API figée par SF-IM-13-01 (PR #639) et l'intégrer au panel décisionnel via TOOL_REGISTRY (`F-IM-13-naturalisation`). Single-country FR — Belgique : bannière info (CNB belge backlog).

---

## Contrat API (importé de SF-IM-13-01 — backend mergé PR #639)

### POST `/api/v1/case-files/{caseFileId}/naturalisation-analysis`

Body :

```jsonc
{
  "voieNaturalisation": "DECRET" | "MARIAGE" | "ASCENDANT" | "MINEUR" | "REINTEGRATION" | "OPPOSITION",
  "dureeResidenceReguliereAnnees": 5,    // ≥ 0, requis DECRET / ASCENDANT
  "dureeMariageAnnees": 4,               // ≥ 0, requis MARIAGE
  "cohabitationContinue": true,          // boolean — MARIAGE
  "ageDemandeur": 67,                    // ≥ 0, requis ASCENDANT (≥65)
  "ascendantDirectFrancais": true,       // boolean — ASCENDANT
  "parentAcquiertNationalite": true,     // boolean — MINEUR
  "vitAvecParentAcquereur": true,        // boolean — MINEUR
  "ancienFrancais": true,                // boolean — REINTEGRATION
  "casierJudiciaireVierge": true,        // boolean (default true) — commun
  "assimilationLangueB1": true,          // boolean — DECRET / MARIAGE
  "ressourcesStables": true,             // boolean — DECRET
  "oppositionGouvernementaleActive": false, // boolean — bloque tout
  "etudesSuperieuresFrance": false       // boolean — réduit DECRET à 2 ans
}
```

### Réponse 200

```jsonc
{
  "caseFileId": "uuid",
  "country": "FRANCE",
  "voieNaturalisation": "DECRET",
  "voieRecommandee": "Naturalisation par décret (art. 21-15+)",
  "verdictRecevabilite": "ELEVEE" | "MOYENNE" | "FAIBLE",
  "criteresNonRemplis": ["..."],
  "documentsAFournir": ["..."],
  "delaiInstructionMois": 18,
  "baseJuridique": "Code civil art. 21-15 à 21-25-1",
  "formule": "Naturalisation — ... : verdict ...",
  "messages": ["..."]
}
```

### GET — récupère l'analyse persistée

### Codes d'erreur

| Code | Cas |
|------|-----|
| 400 | body absent / voieNaturalisation null / valeur non supportée / champ négatif / workspace BELGIQUE / domaine ≠ DROIT_IMMIGRATION |
| 404 | case file d'un autre workspace / GET sans POST préalable |

---

## Comportement attendu

### Cas nominal (FR)

1. Le panel décisionnel charge `F-IM-13-naturalisation` (visibility ALWAYS_ON, DROIT_IMMIGRATION + FRANCE, priority 73).
2. La section affiche un formulaire avec :
   - **Radio voie** (6 voies : DECRET / MARIAGE / ASCENDANT / MINEUR / REINTEGRATION / OPPOSITION).
   - **Champs conditionnels** selon la voie sélectionnée (cf. tableau ci-dessous).
   - **Toggles communs** : casier vierge (default ON), opposition active (default OFF).
3. À la soumission, POST → résultat affiché avec :
   - Bandeau verdict 3 niveaux (navy/or/rouge) avec libellé clair.
   - Liste `documentsAFournir` en chips (navy).
   - Liste `criteresNonRemplis` en chips (rouge si bloquant).
   - Messages avec citations juridiques (LegalCitationsPipe).
   - Formule + base juridique en JetBrains Mono.
4. **Pré-fill IA** depuis `ImmigrationExtractedData` :
   - `nationaliteUe = false` → suggère voie DECRET (titre tier-state typique). Pas d'auto-set, juste hint silencieux.
   - **Pré-fill réel limité** : la naturalisation n'expose pas de champs IA directs équivalents (durée résidence/mariage = données dossier-spécifiques). Le pré-fill se fait sur les champs disponibles (à minima : aucun champ vraiment pré-fillable depuis l'IA actuelle).
   - **Honest fallback** : la méthode `prefillFromAi()` existe et est invoquée mais reste no-op si `ImmigrationExtractedData` n'a pas le champ pertinent. Pattern conforme au template canonique.
5. **Validation F-IA-03 au changement** : `coherenceAlerts` computed déclenche une alerte sur les fields où l'IA / F-96 / Question IA / Pièce manquante apporte une expectedValue divergente. Au minimum un field surveillé (VOIE_NATURALISATION) si une procédure F-96 ou question IA pointe vers une voie spécifique.

### Cas BELGIQUE

- Bannière info "régime Code civil français — l'équivalent belge (CNB art. 12bis) sera couvert par F-IM-13-BE au backlog".
- Aucun appel HTTP émis (gate côté front).

### Cas d'erreur

| Cas | Comportement |
|-----|--------------|
| Form invalide (champ requis manquant) | Bouton submit disabled |
| POST 400 | MatSnackBar rouge + message backend |
| POST 5xx | MatSnackBar rouge "Erreur lors de l'analyse" |
| GET 404 (jamais calculé) | Mode formulaire (load → fallback prefillFromAi) |

---

## Critères d'acceptation

- [ ] Composant `app-naturalisation-section` standalone créé avec 4 fichiers (ts/html/scss/spec).
- [ ] Modèle `naturalisation.model.ts` exposant les types Request / Response + enum VoieNaturalisation + libellés.
- [ ] Service `naturalisation.service.ts` exposant `calculate()` et `get()`.
- [ ] Gate FR avec bannière info BE (pas masquage silencieux).
- [ ] Pré-fill IA + signal `provenance*` + handlers qui effacent provenance au changement manuel.
- [ ] Validation F-IA-03 : `coherenceAlerts = computed<Partial<Record<Field, Alert>>>()` câblée sur ≥ 1 field via `CoherenceAlertBuilder`.
- [ ] Champs conditionnels selon voie (DECRET → résidence/langue/ressources, MARIAGE → mariage/cohabitation, ASCENDANT → âge/résidence/lien, MINEUR → parent/cohabitation, REINTEGRATION → ancienFrancais, OPPOSITION → info-only).
- [ ] Bandeau verdict navy (ELEVEE), or (MOYENNE), rouge (FAIBLE).
- [ ] Liste `documentsAFournir` + `criteresNonRemplis` en chips.
- [ ] `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [ ] MatSnackBar pour erreurs (pas alert/confirm).
- [ ] Entrée `F-IM-13-naturalisation` ajoutée à TOOL_REGISTRY avec inputs symétriques.
- [ ] Tests Jest ≥ 12 (mount + lifecycle, pré-fill IA, form validation, calculate, F-IA-03, ngOnChanges, misc).
- [ ] Self-check grep 5/5 (template canonique, palette, refresh, snackbar, F-IA-03).

---

## Périmètre

### Hors scope

- Backend (SF-IM-13-01 mergée PR #639).
- Belgique (F-IM-13-BE backlog).
- Génération automatique du dossier de demande.
- Suivi de l'instruction préfectorale.
- Enrichissement du prompt IA Immigration pour extraire la durée de résidence (futur).

---

## Composants impactés

| Fichier | Opération |
|---------|-----------|
| `frontend/src/app/case-files/naturalisation-section/naturalisation-section.component.ts` | Nouveau |
| `frontend/src/app/case-files/naturalisation-section/naturalisation-section.component.html` | Nouveau |
| `frontend/src/app/case-files/naturalisation-section/naturalisation-section.component.scss` | Nouveau |
| `frontend/src/app/case-files/naturalisation-section/naturalisation-section.component.spec.ts` | Nouveau |
| `frontend/src/app/core/models/naturalisation.model.ts` | Nouveau |
| `frontend/src/app/core/services/naturalisation.service.ts` | Nouveau |
| `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` | Modifié — entrée TOOL_REGISTRY |

---

## Plan de test (≥ 12 Jest)

### 1. Mount + lifecycle
1. FRANCE → `isFrance()` true, GET émis au `ngOnInit`
2. BELGIQUE → `isFrance()` false, aucun appel HTTP (gate pays)
3. GET 200 → résultat hydraté + showForm=false
4. GET 404 → mode formulaire (showForm=true)

### 2. Pré-fill IA
5. Pré-fill no-op gracieux sans aiData (pas d'erreur, pas de provenance)

### 3. Form validation
6. `formValid()` false initialement
7. `formValid()` true quand voie + champs requis remplis pour DECRET
8. Champs conditionnels masqués/montrés selon voie sélectionnée

### 4. Calculate / submit
9. `calculate()` POST → résultat + snackbar succès + triggerRefresh
10. `calculate()` ignoré si form invalide
11. `calculate()` erreur backend → snackbar rouge

### 5. F-IA-03 cohérence
12. `coherenceAlerts` vide quand pas de divergence
13. `coherenceAlerts` rempli pour VOIE_NATURALISATION via F-96 divergent

### 6. Misc
14. `bannerClass` mappe verdict → classe CSS attendue
15. `editMode()` ré-affiche le form
16. `voieLabel` retourne le libellé humain

---

## Impact par domaine métier

- **Droit du travail (FR + BE)** : non applicable.
- **Immigration FR** : cible exacte de cette feature.
- **Immigration BE** : non couvert ici (CNB régime distinct — F-IM-13-BE backlog, indiqué via bannière info).
- **Famille FR + BE** : non applicable.

---

## Parité des domaines métier (niveau 5 — scoring)

Délégué à SF-IM-13-01 (déjà documenté). Cette SF est purement frontend — pas de scoring métier additionnel introduit.

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Composants décisionnels frontend** : pattern de référence `changement-statut-section` (PR #640, le plus récent immigration FR avec multi-fieldsets + F-IA-03).
- [x] **Pays** : FR uniquement, gate via `workspaceCountry`. Bannière info BE pour transparence.
- [x] **Domaine** : DROIT_IMMIGRATION (visibility côté backend).
- [x] **UI patterns** : palette navy/or/rouge alignée DESIGN_SYSTEM.md ; `MatRadioButton` + `MatSlideToggle` + `MatFormField` ; champs conditionnels via `computed`.
- [x] **Flows transversaux** : `CaseDashboardRefreshService.triggerRefresh()` après POST ; pas de touche auth/workspace/nav.

### Décision

- [x] Composant créé symétrique à `changement-statut-section` (template canonique).
- [x] TOOL_REGISTRY entrée `F-IM-13-naturalisation` avec inputs symétriques.
- [x] Pas de nouveau pattern UI introduit — réutilisation strictes des conventions existantes.

---

## Self-check grep pré-commit (OBLIGATOIRE 5/5)

| Item | Pattern grep | PASS si |
|------|--------------|---------|
| 1. Template canonique référencé | `grep -i "changement-statut\|protection-rp" naturalisation-section.component.ts` | ≥ 1 mention |
| 2. Palette canonique (pas de couleurs hors charte) | `grep -E "#[0-9a-fA-F]{6}" naturalisation-section.component.scss` | uniquement navy `#1a375f` / or `#c9a54b` / rouge `#b00020` / tons gris `#1a2330`/`#6a7485`/`#e6e1d4` |
| 3. CaseDashboardRefreshService | `grep "CaseDashboardRefreshService\|triggerRefresh" naturalisation-section.component.ts` | ≥ 1 mention + appel après POST succès |
| 4. MatSnackBar (pas alert/confirm) | `grep -E "alert\(\|confirm\(" naturalisation-section.component.ts` | 0 occurrence ; `MatSnackBar` présent |
| 5. F-IA-03 + builder | `grep "CoherenceAlertBuilder\|coherenceAlerts" naturalisation-section.component.ts` | ≥ 2 mentions (import builder + computed) |

---

## Notes

- **Pré-fill IA limité** : `ImmigrationExtractedData` n'expose pas (encore) la durée de résidence régulière, l'âge ou la durée de mariage. Le pré-fill reste no-op gracieux pour ces champs. Une future SF d'enrichissement du prompt IA Immigration (backlog) ajoutera ces champs ; le pattern `prefillFromAi()` est en place pour les recevoir sans changement structurel.
- **Field surveillé F-IA-03** : `VOIE_NATURALISATION` (1 field) — détecte une divergence si l'IA détecte une procédure NATURALISATION_X via `procedureChecks` avec une voie expectedValue ≠ saisie avocat. Sinon, `coherenceAlerts` reste vide (no-op gracieux).
- **OPPOSITION** : voie info-only — pas de champ supplémentaire à saisir, le calculateur backend renvoie verdict MOYENNE et messages explicatifs.
