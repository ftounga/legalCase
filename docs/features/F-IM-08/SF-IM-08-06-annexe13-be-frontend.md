# Mini-spec — F-IM-08 / SF-IM-08-06 Annexe 13 BE — FRONTEND

## Identifiant
`F-IM-08 / SF-IM-08-06`

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IM-08-06-annexe13-be-frontend`

## Objectif

Composant Angular `<app-annexe13-be-section>` consommant l'API SF-IM-08-05. Urgence variable selon délai imposé (0 à 30j) + badge transfert imminent.

## Contrat API (importé)

POST + GET `/api/v1/case-files/{caseFileId}/annexe13-be`.

```typescript
export type MotifOqt = 'SEJOUR_IRREGULIER_ART_7' | 'REFUS_SEJOUR_APRES_DEMANDE' | 'FIN_SEJOUR_REGULIER' | 'AUTRE';
export type TypeRecours = 'ANNULATION_30J' | 'EXTREME_URGENCE_5JO';
export type StatutRecoursAnnul = 'DISPONIBLE' | 'URGENT' | 'EXPIRE' | 'RECOURS_FORME';

export interface Annexe13BeRequest {
  dateNotificationAnnexe13: string;
  delaiDepartImposeJours: number;
  motifOqt: MotifOqt;
  transfertImminent: boolean;
  recoursForme: boolean;
  dateRecours?: string | null;
  typeRecours?: TypeRecours | null;
}

export interface Annexe13BeResponse {
  caseFileId: string;
  dateNotificationAnnexe13: string;
  delaiDepartImposeJours: number;
  motifOqt: MotifOqt;
  transfertImminent: boolean;
  recoursForme: boolean;
  dateRecours: string | null;
  typeRecours: TypeRecours | null;
  country: 'BELGIQUE';
  dateExpirationDelaiDepart: string;
  dateExpirationRecoursAnnulation: string;
  dateExpirationRecoursExtremeUrgence: string;
  joursRestantsAvantExpirationAnnulation: number;
  statutRecoursAnnulation: StatutRecoursAnnul;
  dateAudiencePrevisionnelle: string | null;
  dateDecisionPrevisionnelle: string | null;
  referedDisponibles: string[];
  formule: string;
  baseJuridique: string;
  messages: string[];
}
```

## Form

- `@Input() caseFileId`, `@Input() workspaceCountry`
- Si FRANCE → bannière "Annexe 13 procédure belge — en France voir outil OQTF"
- dateNotificationAnnexe13 (input date, pas futur, required)
- delaiDepartImposeJours (input number, 0-30, default 30)
- motifOqt (mat-select 4 options)
- transfertImminent (mat-slide-toggle)
- recoursForme (mat-slide-toggle) + si true : typeRecours (select) + dateRecours (date)
- Bouton "Analyser" disabled si form invalide

## Affichage

Bannière coloriée selon `statutRecoursAnnulation` :
- DISPONIBLE (>3j) : navy
- URGENT (≤3j) : or
- EXPIRE : rouge
- RECOURS_FORME : vert

Si `transfertImminent=true` : badge gold "⚠ TRANSFERT IMMINENT" + message recours extrême urgence mis en avant.

Contenu :
- joursRestants annulation (grand format)
- dateExpirationDelaiDepart
- dateExpirationRecoursAnnulation + dateExpirationRecoursExtremeUrgence (si pertinent)
- dates audience + décision CCE si recours formé
- chips referedDisponibles
- Messages ul
- Base juridique monospace

Refresh dashboard après POST.

## Composants
- `frontend/src/app/core/models/annexe13-be.model.ts`
- `frontend/src/app/core/services/annexe13-be.service.ts`
- `frontend/src/app/case-files/annexe13-be-section/*.{ts,html,scss,spec.ts}`
- TOOL_REGISTRY entry `F-IM-08-annexe13-be`

## Tests
- mount FR → bannière info
- mount BE → form visible
- submit valide → résultat
- transfertImminent=true → badge visible
- Erreur HTTP → snackbar

## Design system
Navy/or/rouge (rouge pour EXPIRE uniquement). Inter + JetBrains Mono.

## Pattern
`frontend/src/app/case-files/oqtf-avec-delai-section/` (récent, similar flow).

## Hors scope
- Recours extrême urgence comme outil distinct
- Audition CCE booking / suivi
