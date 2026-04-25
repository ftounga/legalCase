# SF-DT-13-02 — Frontend Licenciement économique (FR)

## Métadonnées

- **Feature parente** : F-DT-13 — Outil décisionnel "Risque de requalification d'un licenciement économique" (FR uniquement)
- **Type** : Frontend Angular (UI + service + model + intégration TOOL_REGISTRY)
- **Domaine** : Droit du travail
- **Pays** : FRANCE uniquement (BE → bannière info renvoyant vers F-DT-14 PSE Loi Renault)
- **Backend prérequis** : SF-DT-13-01 (PR #585, mergée)
- **Pattern de référence** : `harcelement-licenciement-nul-section` (canonique F-IA-03) + `divorce-faute-section` (multi-select + 3 cartes scores)
- **Skill** : `ai-skills/frontend-coherence-audit.md` §5

## Objectif (1 phrase)

Exposer dans le panel décisionnel F-IA-04 l'outil d'analyse du risque de requalification d'un licenciement économique (art. L.1233-3/4/5/45 Code du travail) avec pré-fill IA et alertes de cohérence F-IA-03.

## Contrat API (importé de SF-DT-13-01)

- `POST /api/v1/case-files/{caseFileId}/licenciement-economique`
- `GET /api/v1/case-files/{caseFileId}/licenciement-economique`

### Request body
```ts
{
  motifEconomiqueInvoque: 'DIFFICULTES_ECONOMIQUES' | 'MUTATIONS_TECHNOLOGIQUES'
    | 'REORGANISATION_COMPETITIVITE' | 'CESSATION_ACTIVITE' | 'AUTRE',
  preuvesMotif: ('BAISSE_CHIFFRE_AFFAIRES' | 'PERTES_EXPLOITATION' | 'BAISSE_COMMANDES'
    | 'BAISSE_TRESORERIE' | 'BAISSE_ENE' | 'MUTATION_TECHNOLOGIQUE_PROUVEE'
    | 'RAPPORT_EXPERT' | 'AUTRE')[],
  criteresOrdreAppliques: ('AGE' | 'ANCIENNETE' | 'CHARGES_FAMILLE'
    | 'QUALITES_PROFESSIONNELLES' | 'SITUATION_HANDICAP')[],
  salarieAge: number | null,
  salarieAncienneteMois: number | null,
  salarieChargesFamille: number | null,
  salarieQualitesProf: 'EXCELLENT' | 'BON' | 'MOYEN' | 'INSUFFISANT' | null,
  tentativesReclassement: ('FORMATION_INTERNE' | 'MUTATION_GROUPE'
    | 'OFFRE_POSTE_GROUPE' | 'OFFRE_POSTE_EXTERIEUR' | 'AUCUNE')[],
  prioriteReembauchePropose: boolean,
  congeReclassementPropose: boolean,
  dateNotification: string  // YYYY-MM-DD
}
```

### Response (champs supplémentaires renvoyés)
- `caseFileId: UUID`
- `scoreCausalite: number` (0-100)
- `scoreCriteresOrdre: number` (0-100)
- `scoreReclassement: number` (0-100)
- `scoreGlobal: number` (0-100, moyenne arrondie)
- `verdictRisqueRequalification: 'FAIBLE' | 'MOYENNE' | 'ELEVEE'`
- `criteresOrdreManquants: CritereOrdre[]`
- `criteresOrdreObligatoiresOk: boolean`
- `obligationReclassementRespectee: boolean`
- `baseJuridique: string` (`Art. L.1233-3 + L.1233-4 + L.1233-5 + L.1233-45 Code du travail`)
- `formule: string`
- `messages: string[]`
- `country: 'FRANCE'`

### Codes d'erreur
- `400` — paramètres invalides (motif manquant, pays autre que FRANCE, listes nulles, valeurs négatives)
- `404` — case file inconnu / hors workspace, OU pas encore d'analyse persistée (GET)

## Comportement nominal + cas d'erreur

### Nominal
1. L'avocat ouvre le panel décisionnel F-IA-04 sur un dossier droit du travail FR.
2. Le composant `<app-licenciement-economique-section>` s'affiche (collapsé par défaut).
3. Au déploiement : `GET` initial. Si 200 → mode résultat. Si 404 → mode formulaire avec pré-fill IA si `aiData` disponible.
4. L'avocat coche/sélectionne motif, preuves, critères d'ordre, qualités prof, tentatives de reclassement, toggles, date notification.
5. L'avocat clique "Analyser" → `POST` → résultat (verdict + 3 sous-scores + score global + chips critères manquants + messages + base juridique).
6. Refresh dashboard via `CaseDashboardRefreshService.triggerRefresh()`.

### Cas d'erreur
- Workspace BELGIQUE → bannière info "Outil applicable à la France uniquement — pour la Belgique, voir F-DT-14 PSE Loi Renault" (form non rendu).
- POST 400/500 → `MatSnackBar` rouge `panelClass: 'snack-error'`.
- GET 404 → reste en mode formulaire (pré-fill IA si `aiData`).

## Critères d'acceptation vérifiables

- [ ] Composant standalone, intégré au TOOL_REGISTRY sous `F-DT-13-licenciement-economique`.
- [ ] Form 5 mat-selects (motif simple, preuves multiple, critères multiple, qualités simple, tentatives multiple) + 3 inputs numériques (âge, ancienneté mois, charges famille) + 2 mat-slide-toggles (priorité réembauche, congé reclassement) + 1 input `type="date"` (date notification).
- [ ] Mode résultat : bannière verdict navy/or/rouge selon verdictRisqueRequalification (FAIBLE=navy, MOYENNE=or, ELEVEE=rouge).
- [ ] 3 cartes sous-scores affichées (Causalité / Critères ordre / Reclassement) + score global en bannière.
- [ ] Chips `criteresOrdreManquants` affichés.
- [ ] `baseJuridique` + `formule` en JetBrains Mono.
- [ ] Messages cités via `LegalCitationsPipe`.
- [ ] Pré-fill IA via `@Input() aiData?: TravailExtractedData` (champs : `salaireBrutMensuel` non utilisé ici, `dateLicenciement` → `dateNotification`, `motifLicenciement` → motif si mappable). Méthode `prefillFromAi()`.
- [ ] Validation F-IA-03 : `coherenceAlerts = computed<Partial<Record<F, CoherenceAlert<F>>>>()` via `CoherenceAlertBuilder` partagé pour les fields `MOTIF_ECONOMIQUE`, `DATE_NOTIFICATION`.
- [ ] Refresh dashboard appelé après POST succès.
- [ ] MatSnackBar pour erreurs HTTP.
- [ ] Gate FRANCE strict : si `workspaceCountry === 'BELGIQUE'`, bannière info, pas de GET ni POST.

## Plan de test minimal

### Unitaires (Jest)
1. mount sans erreur (FRANCE).
2. `formValid()` faux si motif manquant ou date manquante.
3. `formValid()` faux si listes critères/tentatives ne contiennent que `AUCUNE`.
4. `formValid()` vrai avec valeurs minimales valides.
5. GET 200 → résultat affiché, pas de pré-fill IA.
6. GET 404 → formulaire affiché, pré-fill IA appliqué (date notification depuis `dateLicenciement`).
7. POST → résultat affiché, snackbar succès, refresh dashboard appelé.
8. POST 400 → snackbar rouge.
9. POST ignoré si form invalide.
10. `onMotifChange()` efface badge IA.
11. coherenceAlerts.MOTIF_ECONOMIQUE présent si IA détecte motif différent.
12. coherenceAlerts.DATE_NOTIFICATION présent si IA dateLicenciement différent.
13. Gate BELGIQUE : pas de GET ni POST.
14. `verdictBannerClass()` mappe correctement (FAIBLE/MOYENNE/ELEVEE → navy/or/rouge).
15. Alerte masquée si showForm=false.

### Intégration
- Pas requise pour cette SF (frontend pur consommant un endpoint mocké en tests).

### Isolation workspace
- Backend gère via `WorkspaceMemberRepository.findByUserAndPrimaryTrue()` (validé en SF-DT-13-01).

## Tables / endpoints / composants impactés

- **Endpoints consommés** : `POST + GET /api/v1/case-files/{caseFileId}/licenciement-economique`
- **Nouveaux fichiers** :
  - `frontend/src/app/core/models/licenciement-economique.model.ts`
  - `frontend/src/app/core/services/licenciement-economique.service.ts`
  - `frontend/src/app/case-files/licenciement-economique-section/licenciement-economique-section.component.{ts,html,scss,spec.ts}`
- **Modifié** :
  - `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — ajout entrée `TOOL_REGISTRY`.

## Hors périmètre

- Backend (déjà mergé en SF-DT-13-01).
- Belgique : Loi Renault (PSE) traitée dans F-DT-14.
- Pré-extraction IA des champs `motifEconomique` / preuves / critères : non implémentée côté backend pipeline (réutilise les champs `TravailExtractedData` existants : `dateLicenciement` et `motifLicenciement` chaîne libre).

## Analyse de cohérence transversale

- **Autres outils décisionnels FR droit travail** : pattern aligné avec F-DT-08 (licenciement-section), F-DT-12 (discrimination-section), F-DT-15 (inaptitude-section). Tous suivent le canon harcelement-licenciement-nul.
- **Outils 2-pays (BE)** : F-DT-14 PSE Loi Renault prévue (pas livrée). Cet outil reste single-FR.
- **Pré-fill IA** : `TravailExtractedData.dateLicenciement` réutilisé (déjà prévu dans `case-analysis.model.ts`).
- **Validation F-IA-03** : utilise le `CoherenceAlertBuilder` partagé (SF-155-05). Pas de nouvelle source / pattern.
- **Composant partagé / nouveau pattern UI** : aucun. Réutilise `<app-coherence-popover-trigger>`, `LegalCitationsPipe`, `CaseDashboardRefreshService` existants.

## Impact par domaine métier

- **Droit du travail FR** : feature de cet outil, conforme L.1233-x.
- **Droit du travail BE** : non applicable (BE Loi Renault → F-DT-14, prévue).
- **Immigration** : non applicable.
- **Famille** : non applicable.

## Parité des domaines métier

L'outil est un scoring (niveau 5), single-country FRANCE. La parité BE est traitée par F-DT-14 (PSE Loi Renault) — feature parente F-DT-14 reste ouverte au backlog. Pas d'asymétrie introduite par la présente SF (la SF est strictement le pendant frontend du backend SF-DT-13-01).

## Préoccupations transversales

- **Auth/Principal** : aucune modification — réutilise `@AuthenticationPrincipal OidcUser` côté backend (déjà testé SF-DT-13-01).
- **Workspace context** : aucun changement — `workspaceCountry` consommé via @Input.
- **Plans/limites** : pas de quota nouveau.
- **Navigation/routing** : pas de nouvelle route.
- **Outil décisionnel métier** : nouvel outil (frontend pur). Single-situation (licenciement économique FR). Conforme à l'invariant "1 outil = 1 situation".
