# SF-FA-18-06 — Frontend action en recherche de paternité (art. 327 + 340 Cciv)

> **SF-06 du chantier F-FA-18 (Filiation)** — 5/8 SF déjà livrées
> (SF-01/02/03/04/05). Cette SF ferme le BLOCAGE backend SF-FA-18-05 (PR #664)
> en exposant l'outil décisionnel correspondant côté frontend, intégré au
> panel F-IA-04.

## Objectif

Implémenter le composant Angular `recherche-paternite-section` (FR
uniquement — bannière info BE) consommant l'API SF-FA-18-05, avec
pré-remplissage IA et validation F-IA-03 RÈGLE FONDAMENTALE.

## Contrat API (importé de SF-FA-18-05 backend, mergé PR #664)

### POST/GET `/api/v1/case-files/{caseFileId}/recherche-paternite-analysis`

Body :
```json
{
  "qualiteDuDemandeur": "ENFANT_MAJEUR" | "REPRESENTANT_LEGAL_MINEUR" | "MERE",
  "dateNaissanceEnfant": "2001-04-15",
  "presomptionPossessionEtat": true,
  "expertiseAdnDemandee": true,
  "pereDesigneRefuseADN": false,
  "motifsSerieux": true
}
```

Response 200 :
```json
{
  "caseFileId": "uuid",
  "qualiteDuDemandeur": "ENFANT_MAJEUR",
  "verdictRecevabilite": "ELEVEE" | "MOYENNE" | "FAIBLE",
  "scoreRecevabilite": 88,
  "delaiPrescriptionAns": 10,
  "delaiPrescriptionRestantMois": 96,
  "expertiseAdnRecommandee": true,
  "presomptionRefusADN": false,
  "risquesRefus": ["..."],
  "documentsRequis": ["..."],
  "baseJuridique": "Art. 327 + 340 + 16-11 + 321 Cciv",
  "formule": "...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

## Comportement nominal

- En FRANCE → GET au mount, mode résultat si 200, mode formulaire si 404.
- En BELGIQUE → bannière info "outil français uniquement, équivalent CC art. 322 et s. au backlog jumeau".
- POST avec form valide → snackbar succès + dashboard refresh + bascule mode résultat.
- Erreur backend → snackbar rouge.

## Cas d'erreur

- Form invalide → bouton désactivé, pas d'appel HTTP.
- 4xx/5xx → snackbar `snack-error` avec message backend.

## Critères d'acceptation vérifiables

1. FR : isFrance() true, GET au ngOnInit.
2. BE : isFrance() false, aucun appel HTTP, bannière info.
3. GET 200 → mode résultat hydraté.
4. GET 404 → mode formulaire.
5. Pré-fill IA : qualité + date naissance + presomptionPossessionEtat + expertiseAdnDemandee + pereDesigneRefuseADN + motifsSerieux.
6. Pas d'aiData → aucun pré-remplissage.
7. Handler onChange efface provenance IA.
8. Form valide ssi qualiteDuDemandeur + dateNaissanceEnfant.
9. POST envoie tous les champs et reçoit Response.
10. Snackbar succès + dashboardRefresh.triggerRefresh() après POST.
11. Erreur backend → snackbar rouge, calculating remis à false.
12. coherenceAlerts F-IA-03 sur QUALITE / POSSESSION_ETAT / EXPERTISE_ADN / MOTIFS_SERIEUX / REFUS_ADN.
13. Chip alerte délai prescription : critical si ≤ 6, warning si > 6 et < 12, null si ≥ 12.
14. ngOnChanges(aiData) post-mount rafraîchit pré-fill si form vide.
15. ngOnChanges(aiData) après saisie manuelle ne réécrase pas.

## Plan de test (Jest ≥ 12)

- Gate FR/BE + GET au mount.
- GET 200 hydrate / GET 404 reste form.
- Pré-fill complet + sans aiData.
- Handler onChange efface provenance.
- formValid critères obligatoires.
- POST nominal + snackbar succès + dashboardRefresh.
- POST erreur backend → snackbar rouge.
- coherenceAlerts QUALITE / MOTIFS_SERIEUX / EXPERTISE_ADN / POSSESSION_ETAT.
- coherenceAlerts vides après calcul (showForm=false).
- Chip délai : null > 12 / warning entre 6 et 12 / critical ≤ 6 / critical si négatif.
- ngOnChanges post-mount rafraîchit / ne réécrase pas saisie.
- toggleCollapse + editMode + bannerClass + qualiteLabel.

## Tables / endpoints / composants impactés

- **Frontend** : `recherche-paternite-section/` (4 fichiers — TS, HTML, SCSS, spec).
- **Modèle** : `core/models/recherche-paternite.model.ts`.
- **Service** : `core/services/recherche-paternite.service.ts`.
- **`FamilleExtractedData`** : ajout des champs `qualiteDuDemandeurRechercheDetected`, `dateNaissanceEnfantRechercheDetectee`, `presomptionPossessionEtatRechercheDetected`, `expertiseAdnDemandeeRechercheDetected`, `pereDesigneRefuseADNDetected`, `motifsSerieuxRechercheDetected`.
- **TOOL_REGISTRY** : entrée `'F-FA-18-recherche-paternite'` dans `decisional-tools-panel.component.ts`.

## Hors périmètre

- Modification du backend SF-FA-18-05 (mergé).
- Belgique (feature jumelle au backlog).
- Possession d'état (action 317 Cciv) — SF future.
- Adoption simple/plénière — SF future.

## Impact par domaine métier

Feature **sensible au domaine** :
- **Droit du travail / Immigration** : non applicable (gate panel F-IA-04).
- **Droit famille FR** : couvert par cette SF.
- **Droit famille BE** : bannière info (feature jumelle au backlog — CC art. 322, 332ter, 332-1).

## Préoccupations transversales

- **Auth / Principal** : aucun changement.
- **Workspace context** : gate `workspaceCountry === 'FRANCE'` (mode formulaire) ou bannière info.
- **Plans / limites** : non concerné.
- **Navigation / routing** : aucune nouvelle route.
- **Outil décisionnel métier** : nouveau outil (1 outil = 1 situation : recherche 327/340 distincte de contestation 332-335 / reconnaissance 316).

## Analyse de cohérence transversale

- **Outils décisionnels existants** : SF-06 vient compléter le bloc filiation aux côtés de SF-02 (reconnaissance volontaire), SF-04 (contestation). Pattern de référence retenu : `contestation-paternite-section` (PR #663) — choix justifié par la similarité jurisprudentielle (expertise ADN + délais + qualités à agir).
- **Pas de nouveau composant partagé** — réutilisation stricte de `CoherencePopoverTriggerDirective`, `CoherenceAlertBuilder`, `LegalCitationsPipe`, `CaseDashboardRefreshService`, palette navy/or/rouge canonique.

## Self-check pré-commit (5/5)

- [x] Pré-fill IA présent (méthode `prefillFromAi()` invoquée dans ngOnInit + ngOnChanges).
- [x] Validation F-IA-03 présente (`coherenceAlerts` computed + `<app-coherence-popover-trigger>` câblé).
- [x] Gate `workspaceCountry` (bannière info BE, pas masquage).
- [x] `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [x] `MatSnackBar` pour erreurs (pas alert/confirm) + JetBrains Mono baseJuridique/formule.
