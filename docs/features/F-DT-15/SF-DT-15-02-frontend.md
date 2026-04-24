# Mini-spec — F-DT-15 / SF-DT-15-02 Licenciement pour inaptitude — FRONTEND

## Identifiant
`F-DT-15 / SF-DT-15-02`

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-DT-15-02-inaptitude-frontend`

## Objectif

Composant Angular standalone `<app-inaptitude-section>` permettant à l'avocat de saisir les critères et d'afficher le calcul des indemnités de licenciement pour inaptitude. Intégré au panel F-IA-04.

**Parallélisation** : développé en parallèle de SF-DT-15-01 (backend). Contrat API figé dans cette mini-spec de référence (voir `SF-DT-15-01-backend.md`). Tests mockés du service.

---

## Contrat API (importé de SF-DT-15-01 — figé)

### Endpoint
- POST `/api/v1/case-files/{caseFileId}/inaptitude`
- GET `/api/v1/case-files/{caseFileId}/inaptitude`

### Modèle TypeScript

```typescript
export type OrigineInaptitude =
  | 'PROFESSIONNELLE' | 'NON_PROFESSIONNELLE'    // FR
  | 'PROFESSIONNELLE_BE' | 'NON_PROFESSIONNELLE_BE'; // BE

export interface InaptitudeRequest {
  salaireMensuelReference: number;
  ancienneteAnnees: number;
  origineInaptitude: OrigineInaptitude;
  reclassementRespecte: boolean;
  avisMedecinTravailDate?: string; // YYYY-MM-DD
}

export interface InaptitudeResponse {
  caseFileId: string;
  salaireMensuelReference: number;
  ancienneteAnnees: number;
  origineInaptitude: OrigineInaptitude;
  reclassementRespecte: boolean;
  avisMedecinTravailDate: string | null;
  country: 'FRANCE' | 'BELGIQUE';
  indemniteLegale: number;
  indemniteCompensatricePreavis: number;
  damagesReclassement: number;
  total: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
```

---

## Comportement

- Composant standalone avec `@Input() caseFileId`, `@Input() workspaceCountry: 'FRANCE'|'BELGIQUE'`.
- Form réactif : salaire (number >0), ancienneté (int ≥ 0), origine (select filtré par pays : 2 options FR ou 2 options BE), reclassement (checkbox), avis médecin date (datepicker optionnel).
- Submit → POST, persiste, affiche résultat.
- Déclenche `CaseDashboardRefreshService.triggerRefresh()`.
- Résultat :
  - Total en gras (grande police).
  - Décomposition indemnité légale + préavis + damages (si > 0).
  - Formule en monospace.
  - Base juridique + liste messages.

---

## Composants à créer

- `frontend/src/app/core/models/inaptitude.model.ts`
- `frontend/src/app/core/services/inaptitude.service.ts`
- `frontend/src/app/case-files/inaptitude-section/inaptitude-section.component.{ts,html,scss,spec.ts}`
- TOOL_REGISTRY entry : `F-DT-15-inaptitude` → `InaptitudeSectionComponent`.

## Plan de test

- Service POST/GET URL + headers corrects.
- Composant : mount FRANCE → 2 options origine FR, BELGIQUE → 2 options BE.
- Submit valide → affichage résultat + refresh.
- Erreur HTTP → snackbar.
- Form invalide → bouton disabled.

## Impact par domaine

DROIT_DU_TRAVAIL FR + BE.

## Hors scope
- Détection IA auto origine (pro / non-pro) — hors scope.
- Intégration dans synthèse PDF — hors scope.

## Pattern de référence
`frontend/src/app/case-files/harcelement-licenciement-nul-section/` (dernière SF parallèle F-DT-11-02, même pattern).
