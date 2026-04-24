# Mini-spec — F-IM-14 / SF-IM-14-05 9bis humanitaire BE — FRONTEND

## Identifiant

`F-IM-14 / SF-IM-14-05`

## Feature parente

`F-IM-14` — Régularisations BE (9bis humanitaire, 9ter médical, 40bis cohabitant UE, 40ter familial belge)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-14-05-frontend-9bis-humanitaire-be`

---

## Objectif

Livrer la section Angular `<app-belgian-9bis-section>` qui consomme l'API `POST + GET /api/v1/case-files/{caseFileId}/belgian-9bis` (SF-IM-14-01 backend) pour permettre à l'avocat BE d'évaluer la probabilité d'aboutissement d'une demande 9bis humanitaire (Loi 15/12/1980 art. 9bis).

Contrat importé de SF-IM-14-01-9bis-humanitaire-be-backend.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier BE Immigration. Le composant `<app-belgian-9bis-section>` est inclus via le panel décisionnel (TOOL_REGISTRY mapping `F-IM-14-9bis-humanitaire-be`).
2. Au montage, GET `/api/v1/case-files/{id}/belgian-9bis` :
   - 200 → résultat affiché en mode lecture (verdict + score + détails)
   - 404 → mode formulaire (avec pré-fill IA si `aiData` fourni)
3. Formulaire : 1 champ date (entrée Belgique), 1 champ nombre (durée présence mois), 4 toggles (circonstances exceptionnelles, liens familiaux BE, liens professionnels, scolarité enfants BE), 1 toggle menace OP, 1 date optionnelle (dépôt demande).
4. Submit POST → résultat persisté + `CaseDashboardRefreshService.triggerRefresh()` + snackbar "Analyse 9bis humanitaire enregistrée".

### Cas d'erreur

| Situation | Comportement | Source |
|-----------|--------------|--------|
| GET 404 | Mode formulaire, pas d'erreur affichée | service |
| POST 4xx/5xx | MatSnackBar erreur avec message backend ou fallback | composant |
| Workspace FRANCE | Bannière info "Outil applicable à la BE uniquement" + redirige vers F-IM-08 OQTF / F-IM-09 AES | composant |
| Form invalide | Bouton "Analyser" disabled | composant |

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Composants BE existants** : `annexe13-be-section`, `motif-grave-be-section` → palette navy/or, `<input type="date">`, `MatSnackBar`, `CaseDashboardRefreshService.triggerRefresh()`. Pattern repris.
- **Pré-fill IA** : `immigration-title-decision-section`, `annexe13-be-section` (4 champs IA + provenance signal). Pattern repris partiellement (2 champs IA candidats : `dateDepotProcedure` → `dateDepotDemande` ; pas de field `dateEntreeBelgique` aujourd'hui — no-op gracieux).
- **F-IA-03 coherence popover** : `CoherencePopoverTriggerDirective` + `CoherenceAlertBuilder`. Activé sur `dateDepotDemande` (champ avec source IA). Les autres champs (4 toggles + dureePresenceMois + dateEntreeBelgique) n'ont pas de source IA actuelle → pas d'alerte.
- **TOOL_REGISTRY** : entrée `F-IM-14-9bis-humanitaire-be` à ajouter (responsabilité de la SF d'intégration au panel — ce SF documente le snippet exigé).
- **Autres outils décisionnels métier** scannés : palette navy/or classique appliquée (pas de rouge dominant — pas d'urgence 48h ici).

### Niveaux de vérification

- Modèle TypeScript : nouveau `belgian-9bis.model.ts`
- Service : nouveau `belgian-9bis.service.ts`
- Composant : nouveau `belgian-9bis-section/`
- Tests : ≥10 cas

### Classement des cibles applicables

- Composant frontend `<app-belgian-9bis-section>` : intégré dans cette SF
- Mapping `TOOL_REGISTRY` : exposé dans la PR (snippet ci-dessous, intégration via SF d'intégration ultérieure pour ne pas bloquer cette PR atomique — la mission interdit explicitement la modification de `decisional-tools-panel.component.ts`)

---

## Nouveau pattern UI ou service partagé

Pas de nouveau pattern partagé. Réemploi strict du template canonique (`harcelement-licenciement-nul-section` + `annexe13-be-section`).

---

## Impact par domaine métier

DROIT_IMMIGRATION BE uniquement. Outil non pertinent en France (équivalents : F-IM-08 OQTF / F-IM-09 AES). Bannière info gérée côté composant si `workspaceCountry === 'FRANCE'`.

---

## Parité des domaines métier

Cet outil est un scoring (niveau 5) pour BE Immigration. Les équivalents domaines :
- DROIT_DU_TRAVAIL : non pertinent (pas de notion 9bis)
- DROIT_FAMILLE : non pertinent (pas de notion 9bis)
- FRANCE Immigration : géré par F-IM-08 (OQTF) et F-IM-09 (AES) — pas de procédure exactement équivalente, le 9bis BE est spécifique au territoire belge.

Pas de feature jumelle requise — le scoring 9bis est intrinsèquement belge.

---

## Contrat API (importé de SF-IM-14-01)

### Request POST `/api/v1/case-files/{caseFileId}/belgian-9bis`

```json
{
  "dateEntreeBelgique": "YYYY-MM-DD",
  "dureePresenceMois": 36,
  "circonstancesExceptionnelles": true,
  "liensFamiliauxBe": true,
  "liensProfessionnels": false,
  "scolariteEnfantsBe": true,
  "menaceOrdrePublic": false,
  "dateDepotDemande": "YYYY-MM-DD"
}
```

### Response

```json
{
  "caseFileId": "...",
  "dateEntreeBelgique": "...",
  "dureePresenceMois": 36,
  "circonstancesExceptionnelles": true,
  "liensFamiliauxBe": true,
  "liensProfessionnels": false,
  "scolariteEnfantsBe": true,
  "menaceOrdrePublic": false,
  "dateDepotDemande": "...",
  "country": "BELGIQUE",
  "presence3AnsOk": true,
  "liensConstitutifsOk": true,
  "pasMenace": true,
  "scoreGlobal": 100,
  "verdictProbabilite": "ELEVEE",
  "criteresNonRemplis": [],
  "dateExpirationInstructionPrevisionnelle": "...",
  "formule": "...",
  "baseJuridique": "Loi 15/12/1980 art. 9bis + AR 17/05/2007",
  "messages": [...]
}
```

Seuils : ELEVEE ≥75, MOYENNE 40-74, FAIBLE <40.

---

## Critères d'acceptation

- [ ] Form rendu uniquement si `workspaceCountry === 'BELGIQUE'`. Sinon bannière info "Outil applicable à la BE uniquement".
- [ ] Tous les champs requis (date entrée, durée mois, 5 toggles) bloquent submit.
- [ ] POST formulaire valide → résultat affiché + snackbar succès + `triggerRefresh()`.
- [ ] POST erreur → MatSnackBar erreur avec message backend.
- [ ] Bannière verdict colorée (or = ELEVEE, navy = MOYENNE, or atténué = FAIBLE — pas de rouge).
- [ ] Score global affiché en grand (JetBrains Mono).
- [ ] Chips `criteresNonRemplis`.
- [ ] Messages backend rendus en `<ul>` avec citations Loi 15/12/1980 art. 9bis.
- [ ] `baseJuridique` et `formule` rendus en JetBrains Mono.
- [ ] Pré-fill IA partiel (`dateDepotProcedure` → `dateDepotDemande`) avec badge "Pré-rempli depuis l'analyse" effacé au moindre changement manuel.
- [ ] Bouton "Modifier" en mode lecture pour réintervenir.
- [ ] Collapse/expand fonctionnel.
- [ ] ≥10 tests Jasmine, tous verts.

---

## Plan de test (≥10)

1. mount + collapsed par défaut
2. BELGIQUE → GET appelé au ngOnInit ; 404 → mode formulaire
3. FRANCE → bannière info, pas d'appel GET, pas de form
4. GET 200 → mode lecture avec verdict ELEVEE
5. formValid : champs obligatoires
6. formValid : durée présence ≥ 0
7. submit OK → POST + snackbar succès + triggerRefresh
8. submit error → snackbar erreur
9. pré-fill IA : `dateDepotProcedure` → `dateDepotDemande` + provenance IA
10. badge IA effacé au changement manuel
11. collapse toggle
12. score affiché en grand

---

## Composants impactés

### Création

- `frontend/src/app/core/models/belgian-9bis.model.ts`
- `frontend/src/app/core/services/belgian-9bis.service.ts`
- `frontend/src/app/case-files/belgian-9bis-section/belgian-9bis-section.component.ts`
- `frontend/src/app/case-files/belgian-9bis-section/belgian-9bis-section.component.html`
- `frontend/src/app/case-files/belgian-9bis-section/belgian-9bis-section.component.scss`
- `frontend/src/app/case-files/belgian-9bis-section/belgian-9bis-section.component.spec.ts`

### À documenter (hors périmètre PR — interdit par mission)

`decisional-tools-panel.component.ts` — TOOL_REGISTRY entry à intégrer dans une SF d'intégration ultérieure :

```ts
['F-IM-14-9bis-humanitaire-be', {
  component: Belgian9bisSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.synthesis?.immigrationExtractedData,
  }),
}],
```

---

## Hors périmètre

- Modification de `decisional-tools-panel.component.ts` (mission)
- Modification de `docs/PRODUCT_SPEC.md` (mission — F-IM-14 statut sera mis à jour quand toutes les SF seront mergées)
- Backend (déjà mergé via SF-IM-14-01)
- Champ `dateEntreeBelgique` dans `ImmigrationExtractedData` (nouveau pré-fill IA — backlog si demandé)
