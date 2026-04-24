# Mini-spec — F-IM-08 / SF-IM-08-03 OQTF SANS délai FR — BACKEND

## Identifiant
`F-IM-08 / SF-IM-08-03`

## Feature parente
`F-IM-08` — OQTF et contentieux éloignement (🔴 critique absolue)

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IM-08-03-oqtf-sans-delai-fr-backend`

## Objectif

Outil décisionnel dédié à **l'OQTF sans délai de départ volontaire** (art. L.731-1 CESEDA). Procédure distincte de l'OQTF avec DDV (SF-IM-08-01) — délai de recours **48 heures** devant le juge administratif statuant seul (L.614-8 et R.776-26), audience sous 96 heures, décision sous 6 semaines. Extrême urgence. **Invariant "un outil = une situation" respecté** : OQTF avec délai ≠ OQTF sans délai (procédures juridiquement distinctes).

## Comportement

### Règles (CESEDA + CJA)

- **L.731-1** : OQTF sans DDV prononcée en cas de risque de fuite, trouble à l'ordre public, ou OQTF précédente inexécutée.
- **L.614-8** : recours suspensif devant juge unique TA dans les **48 heures** de notification.
- **R.776-26** : audience dans les **96 heures** (4 jours) du recours.
- **R.776-27** : décision dans les **6 semaines** (42 jours).
- Effet suspensif du recours : éloignement impossible tant que TA n'a pas statué.

### Inputs

- `dateNotificationOqtf` : LocalDate + heure obligatoire → `dateHeureNotificationOqtf` : LocalDateTime
- `motifSansDelai` : enum :
  - `RISQUE_FUITE` (L.731-1 1°)
  - `TROUBLE_ORDRE_PUBLIC` (L.731-1 2°)
  - `OQTF_PRECEDENTE_INEXECUTEE` (L.731-1 3°)
  - `AUTRE`
- `placementCra` : boolean (indique si étranger placé en centre de rétention administrative)
- `recoursForme` : boolean
- `dateHeureRecours` : LocalDateTime nullable

### Outputs

- `dateHeureExpirationDelaiRecours` = notification + 48h
- `heuresRestantes` : long (heures restant avant expiration, peut être négatif)
- `statutDelaiRecours` :
  - `DISPONIBLE` (> 24h restantes)
  - `URGENT` (entre 0 et 24h)
  - `EXPIRE` (< 0h, pas de recours)
  - `RECOURS_FORME`
- `dateHeureAudiencePrevisionnelle` = recours + 96h (si formé)
- `dateDecisionPrevisionnelle` = recours + 42 jours
- `refereDisponibles` : toujours `[REFERE_LIBERTE_L521_2]` (L.521-1 moins pertinent ici vu le délai ultra-court). Si `placementCra=true` : ajouter `CONTROLE_JLD_LIBERTE` (L.742-1 CESEDA, contrôle judiciaire JLD toutes les 48h prolongation).
- `formule` : texte
- `baseJuridique` : "Art. L.731-1, L.614-8, R.776-26/27 CJA"
- `messages` :
  - "DÉLAI CRITIQUE : 48 heures à compter de la notification (art. L.614-8). Ce n'est PAS un délai franc — les heures comptent."
  - "Recours devant juge unique TA, audience sous 96 heures (R.776-26)."
  - "Effet suspensif : éloignement impossible pendant le recours."
  - Si motif `RISQUE_FUITE` : "Contester les indices de fuite retenus (art. L.731-1 1° : documents d'identité, adresse non déclarée, refus d'obtempérer précédent, etc.)."
  - Si motif `TROUBLE_ORDRE_PUBLIC` : "Contester la qualification (jurisprudence exige une menace réelle, actuelle, suffisamment grave)."
  - Si `placementCra=true` : "Étranger en CRA — audience du JLD L.742-1 toutes les 48h pour prolongation (max 90 jours). Coordonner les 2 procédures."

### Cas d'erreur
- dateHeureNotificationOqtf dans le futur → 400
- motifSansDelai null → 400
- dateHeureRecours avant notification → 400
- recoursForme=true sans dateHeureRecours → 400
- Workspace BELGIQUE → 400 (renvoi SF-IM-08-05)
- Dossier autre domaine → 400
- Workspace étranger → 404

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/oqtf-sans-delai`

**Request :**
```json
{
  "dateHeureNotificationOqtf": "2026-04-02T14:30:00",
  "motifSansDelai": "RISQUE_FUITE",
  "placementCra": true,
  "recoursForme": false,
  "dateHeureRecours": null
}
```

**Response :**
```json
{
  "caseFileId": "uuid",
  "dateHeureNotificationOqtf": "2026-04-02T14:30:00",
  "motifSansDelai": "RISQUE_FUITE",
  "placementCra": true,
  "recoursForme": false,
  "dateHeureRecours": null,
  "country": "FRANCE",
  "dateHeureExpirationDelaiRecours": "2026-04-04T14:30:00",
  "heuresRestantes": 44,
  "statutDelaiRecours": "DISPONIBLE",
  "dateHeureAudiencePrevisionnelle": null,
  "dateDecisionPrevisionnelle": null,
  "refereDisponibles": ["REFERE_LIBERTE_L521_2", "CONTROLE_JLD_LIBERTE"],
  "formule": "…",
  "baseJuridique": "Art. L.731-1, L.614-8, R.776-26/27 CJA",
  "messages": [...]
}
```

GET même réponse.

## Architecture

Pattern F-IM-08 SF-01 (OqtfAvecDelai*) avec adaptations :
- Utilisation de `LocalDateTime` au lieu de `LocalDate` (précision à l'heure pour calcul 48h)
- Calcul en `ChronoUnit.HOURS` au lieu de `DAYS`

Table `oqtf_sans_delai_analyses` (migration 117). Tool_id `F-IM-08-oqtf-sans-delai-fr`. **Règle visibility CONTEXTUAL** sur trigger `type_procedure_detectee=OQTF_SANS_DELAI`. UUID `f1a04001-0000-0000-0000-ee000000083a`, priority 58.

## Impact domaine

DROIT_IMMIGRATION FR. BE couvert par SF-IM-08-05 (procédure annexe 13 distincte).

## Critères

- [ ] Calcul heuresRestantes précis (ChronoUnit.HOURS, peut être négatif)
- [ ] 4 motifs + messages différenciés
- [ ] placementCra=true → ajoute CONTROLE_JLD_LIBERTE dans refereDisponibles + message spécifique
- [ ] Statuts DISPONIBLE (>24h) / URGENT (0-24h) / EXPIRE (<0h) / RECOURS_FORME
- [ ] Validation dates/enum
- [ ] Migration 117 + règle CONTEXTUAL
- [ ] Gate country==FRANCE strict
- [ ] ≥12 UT + ≥8 IT

## Hors scope

- Frontend (SF-IM-08-04 parallèle)
- OQT BE (SF-IM-08-05)
- Référés comme outils distincts (SF-IM-08-07/08)
- Contrôle JLD CRA comme outil distinct (backlog)
