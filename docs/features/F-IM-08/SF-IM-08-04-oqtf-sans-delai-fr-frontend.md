# Mini-spec — F-IM-08 / SF-IM-08-04 OQTF SANS délai FR — FRONTEND

## Identifiant
`F-IM-08 / SF-IM-08-04`

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IM-08-04-oqtf-sans-delai-fr-frontend`

## Objectif

Composant Angular `<app-oqtf-sans-delai-section>` consommant l'API SF-IM-08-03. Bannière **alerte extrême** (48h seulement). Développé en parallèle de SF-IM-08-03, contrat API figé.

## Contrat API (importé)

POST + GET `/api/v1/case-files/{caseFileId}/oqtf-sans-delai`.

```typescript
export type MotifSansDelai = 'RISQUE_FUITE' | 'TROUBLE_ORDRE_PUBLIC' | 'OQTF_PRECEDENTE_INEXECUTEE' | 'AUTRE';
export type StatutDelaiSd = 'DISPONIBLE' | 'URGENT' | 'EXPIRE' | 'RECOURS_FORME';

export interface OqtfSansDelaiRequest {
  dateHeureNotificationOqtf: string; // ISO datetime
  motifSansDelai: MotifSansDelai;
  placementCra: boolean;
  recoursForme: boolean;
  dateHeureRecours?: string | null;
}

export interface OqtfSansDelaiResponse {
  caseFileId: string;
  dateHeureNotificationOqtf: string;
  motifSansDelai: MotifSansDelai;
  placementCra: boolean;
  recoursForme: boolean;
  dateHeureRecours: string | null;
  country: 'FRANCE';
  dateHeureExpirationDelaiRecours: string;
  heuresRestantes: number;
  statutDelaiRecours: StatutDelaiSd;
  dateHeureAudiencePrevisionnelle: string | null;
  dateDecisionPrevisionnelle: string | null;
  refereDisponibles: string[];
  formule: string;
  baseJuridique: string;
  messages: string[];
}
```

## Form

- `@Input() caseFileId`, `@Input() workspaceCountry`
- Si BELGIQUE → bannière info + masquer form
- dateHeureNotificationOqtf (input datetime-local, required, pas futur)
- motifSansDelai (mat-select, 4 options : "Risque de fuite", "Trouble à l'ordre public", "OQTF précédente inexécutée", "Autre")
- placementCra (mat-slide-toggle "Placement en CRA")
- recoursForme (mat-slide-toggle)
- Si recoursForme=true : dateHeureRecours (input datetime-local)

## Affichage résultat

**Bannière critique 48h** — toujours rouge soutenu pour DISPONIBLE et URGENT (pas or cette fois — 48h = urgence absolue). EXPIRE = rouge foncé. RECOURS_FORME = vert.

- `heuresRestantes` en très grande police (affiche heures, ex "32 heures restantes")
- Date/heure expiration
- Si placementCra : badge "⚠ CRA" en gold
- Chips `refereDisponibles` (en particulier L.521-2 pour 48h décision)
- Messages (ul)
- Base juridique monospace

## Composants
- `frontend/src/app/core/models/oqtf-sans-delai.model.ts`
- `frontend/src/app/core/services/oqtf-sans-delai.service.ts`
- `frontend/src/app/case-files/oqtf-sans-delai-section/*.{ts,html,scss,spec.ts}`
- TOOL_REGISTRY entry `F-IM-08-oqtf-sans-delai-fr`

## Tests
- Service POST/GET
- Mount FR → form visible, BE → bannière
- Submit valide → bannière critique affichée
- placementCra=true → badge CRA visible
- Erreur HTTP → snackbar

## Design system

Palette rouge autorisée pour urgence absolue 48h. Typo JetBrains Mono sur formule+base.

## Pattern de référence
`frontend/src/app/case-files/oqtf-avec-delai-section/` (SF-IM-08-02 qui vient d'être mergée).

## Hors scope
- Timer en temps réel (hors scope — rafraîchit à chaque POST/GET)
- Référés comme outils distincts
