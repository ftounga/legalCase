# Mini-spec — F-DT-19 / SF-DT-19-02 Calculateur heures supplémentaires — FRONTEND

## Identifiant
`F-DT-19 / SF-DT-19-02`

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-DT-19-02-heures-sup-frontend`

## Objectif

Composant Angular `<app-heures-sup-section>` consommant l'API `/api/v1/case-files/{caseFileId}/heures-sup`. Intégré au panel F-IA-04.

**Parallélisation** : développé en parallèle de SF-DT-19-01 (backend). Contrat API figé dans `SF-DT-19-01-backend.md`. Tests mockés du service.

## Contrat API (importé)

POST + GET `/api/v1/case-files/{caseFileId}/heures-sup`.

### Modèle TypeScript

```typescript
export interface HeuresSupRequest {
  tauxHoraireBrut: number;
  // FR
  heuresSupDeclarees25pct?: number;
  heuresSupDeclarees50pct?: number;
  heuresHorsContingent?: number;
  tauxMajoration25?: number;
  tauxMajoration50?: number;
  // BE
  heuresSupSemaine?: number;
  heuresDimancheJoursFeries?: number;
}

export interface HeuresSupResponse {
  caseFileId: string;
  tauxHoraireBrut: number;
  heuresSupDeclarees25pct: number | null;
  heuresSupDeclarees50pct: number | null;
  heuresHorsContingent: number | null;
  tauxMajoration25: number | null;
  tauxMajoration50: number | null;
  heuresSupSemaine: number | null;
  heuresDimancheJoursFeries: number | null;
  country: 'FRANCE' | 'BELGIQUE';
  rappelMajoration25pct: number;
  rappelMajoration50pct: number;
  rappelMajoration100pct: number;
  rappelTotal: number;
  reposCompensateurHeuresDues: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
```

## Form

Dépend de `workspaceCountry` Input :

**FRANCE :**
- tauxHoraireBrut (number, > 0)
- heuresSupDeclarees25pct (number, ≥ 0)
- heuresSupDeclarees50pct (number, ≥ 0)
- heuresHorsContingent (number, ≥ 0)
- tauxMajoration25 (number, default 25, range 10-50)
- tauxMajoration50 (number, default 50, range 10-50)

**BELGIQUE :**
- tauxHoraireBrut
- heuresSupSemaine
- heuresDimancheJoursFeries

## Affichage

- Rappel total € en gras
- Décomposition : rappel 25% / rappel 50% / rappel 100% (si > 0)
- Repos compensateur heures (si > 0)
- Formule monospace
- Base juridique monospace
- Messages liste ul

Déclenche `CaseDashboardRefreshService.triggerRefresh()` après POST succès.

## Composants à créer
- `frontend/src/app/core/models/heures-sup.model.ts`
- `frontend/src/app/core/services/heures-sup.service.ts`
- `frontend/src/app/case-files/heures-sup-section/*.{ts,html,scss,spec.ts}`
- TOOL_REGISTRY entry `F-DT-19-heures-sup`

## Tests

- Service POST/GET URL
- mount FRANCE → form FR visible
- mount BELGIQUE → form BE visible
- Submit valide → affichage résultat
- Erreur HTTP → snackbar

## Design system

Palette navy/or, Inter + JetBrains Mono, mat-form-field outline, MatSnackBar.

## Pattern de référence

`frontend/src/app/case-files/harcelement-licenciement-nul-section/` (pattern récent avec workspaceCountry Input).
