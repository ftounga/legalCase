# Mini-spec — F-IM-08 / SF-IM-08-02 OQTF avec délai FR — FRONTEND

## Identifiant
`F-IM-08 / SF-IM-08-02`

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IM-08-02-oqtf-avec-delai-fr-frontend`

## Objectif

Composant Angular `<app-oqtf-avec-delai-section>` consommant l'API SF-IM-08-01. Affiché via panel F-IA-04 quand `type_procedure_detectee = OQTF_AVEC_DELAI`.

**Parallélisation** : développé en parallèle de SF-IM-08-01 backend. Contrat API figé dans la mini-spec backend. Tests mockés du service.

## Contrat API (importé)

POST + GET `/api/v1/case-files/{caseFileId}/oqtf-avec-delai`.

### Modèle TypeScript

```typescript
export type MotifOqtf = 'REFUS_TITRE' | 'EXPIRATION_TITRE' | 'SEJOUR_IRREGULIER' | 'RETRAIT_TITRE' | 'AUTRE';
export type StatutDelai = 'DISPONIBLE' | 'URGENT' | 'EXPIRE' | 'RECOURS_FORME';

export interface OqtfAvecDelaiRequest {
  dateNotificationOqtf: string; // YYYY-MM-DD
  motifOqtf: MotifOqtf;
  recoursForme: boolean;
  dateRecours?: string | null;
}

export interface OqtfAvecDelaiResponse {
  caseFileId: string;
  dateNotificationOqtf: string;
  motifOqtf: MotifOqtf;
  recoursForme: boolean;
  dateRecours: string | null;
  country: 'FRANCE';
  dateExpirationDdv: string;
  dateExpirationDelaiRecours: string;
  joursRestantsAvantExpirationDelai: number;
  statutDelaiRecours: StatutDelai;
  dateAudiencePrevisionnelle: string | null;
  dateDecisionTaPrevisionnelle: string | null;
  referedDisponibles: string[];
  formule: string;
  baseJuridique: string;
  messages: string[];
}
```

## Form

- `@Input() caseFileId: string`, `@Input() workspaceCountry: 'FRANCE'|'BELGIQUE'`
- Si `workspaceCountry !== 'FRANCE'` : afficher bannière "OQTF procédure française uniquement" + masquer le form
- dateNotificationOqtf (input date, required, pas dans le futur)
- motifOqtf (mat-select, 5 options avec labels humains : "Refus de titre", "Expiration de titre", "Séjour irrégulier", "Retrait de titre", "Autre")
- recoursForme (mat-slide-toggle)
- Si recoursForme=true : dateRecours (input date, required, >= notification)
- Bouton "Analyser" disabled si form invalide

## Affichage résultat

Grande bannière colorée selon `statutDelaiRecours` :
- DISPONIBLE : fond navy clair + icône info
- URGENT : fond or soutenu + icône warning
- EXPIRE : fond rouge + icône error
- RECOURS_FORME : fond vert + icône check

Contenu :
- Jours restants (grand format) si applicable
- Date expiration DDV
- Date expiration recours
- Si recoursForme : dateAudience + dateDecision prévisionnelles
- Liste `referedDisponibles` (chips)
- Liste messages (ul)
- Base juridique (monospace)

Déclenche `CaseDashboardRefreshService.triggerRefresh()` après POST.

## Composants à créer

- `frontend/src/app/core/models/oqtf-avec-delai.model.ts`
- `frontend/src/app/core/services/oqtf-avec-delai.service.ts`
- `frontend/src/app/case-files/oqtf-avec-delai-section/*.{ts,html,scss,spec.ts}`
- TOOL_REGISTRY entry `'F-IM-08-oqtf-avec-delai-fr'` → `OqtfAvecDelaiSectionComponent`

## Tests

- Service POST/GET URL
- mount FRANCE → form visible
- mount BELGIQUE → bannière d'info, pas de form
- Submit valide recoursForme=false → résultat, pas de dateAudience
- Submit valide recoursForme=true + dateRecours → dateAudience affichée
- Statut EXPIRE → bannière rouge
- Erreur HTTP → snackbar

## Design system

Navy/or/rouge selon statut (rouge réservé aux cas d'alerte — DESIGN_SYSTEM.md autorisé pour statuts d'urgence). Inter + JetBrains Mono. mat-form-field outline. MatSnackBar.

## Pattern de référence

`frontend/src/app/case-files/inaptitude-section/` (dernier pattern mergé, datepicker + form adaptatif).

## Hors scope

- Référés (SF ultérieure, seuls mentionnés en chips).
- OQTF sans délai (SF-IM-08-04).
- Annexe 13 BE (SF-IM-08-06).
