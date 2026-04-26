# SF-FA-18-10 — Frontend adoption (art. 343-370-2 Cciv)

## Objectif

Exposer dans l'UI avocat un outil décisionnel d'analyse de **recevabilité d'une
adoption** (plénière / simple, FR uniquement), consommant l'API
`/api/v1/case-files/{id}/adoption-analysis` figée par SF-FA-18-09 (mergé PR
#677), avec pré-remplissage IA et validation F-IA-03 au changement.

## Contrat API (importé de SF-FA-18-09 mergé PR #677)

- `POST /api/v1/case-files/{caseFileId}/adoption-analysis` — body :
  ```json
  {
    "formeAdoption": "PLENIERE | SIMPLE",
    "ageAdoptant": 35,
    "ageAdopte": 4,
    "consentementParents": true,
    "consentementAdopte": false,
    "consentementConjointAdoptant": true,
    "enquetes": true,
    "placement6mois": true,
    "pupilleEtat": false,
    "adoptantMarie": true
  }
  ```
- `GET /api/v1/case-files/{caseFileId}/adoption-analysis` — réponse persistée.
- Réponse :
  ```json
  {
    "caseFileId": "uuid",
    "formeAdoption": "PLENIERE",
    "formeRecommandee": "PLENIERE | SIMPLE | AUCUNE",
    "verdictRecevabilite": "ELEVEE | MOYENNE | FAIBLE",
    "ageAdoptant": 35,
    "ageAdopte": 4,
    "differenceAgeAns": 31,
    "criteresNonRemplis": [],
    "delaiInstructionMois": 6,
    "documentsRequis": ["..."],
    "risqueRefus": ["..."],
    "baseJuridique": "Art. 343-370-2 Cciv",
    "formule": "...",
    "messages": ["..."],
    "country": "FRANCE"
  }
  ```
- Codes : 200 succès, 400 (forme manquante / pays BE / domaine ≠ DROIT_FAMILLE),
  404 (GET avant POST ou autre workspace).

## Comportement nominal

1. Composant collapsible `<app-adoption-section>` affiché par le panel F-IA-04
   pour les dossiers FR domaine DROIT_FAMILLE (tool_id `F-FA-18-adoption`).
2. Au mount FRANCE : GET de l'analyse persistée, sinon mode formulaire.
3. Au mount BELGIQUE : bannière info "Outil français — équivalent CC belge au
   backlog" (gate pays informative, pas masquage silencieux).
4. Form : radio forme (PLENIERE / SIMPLE) + champs adoptant (âge, marié) +
   adopté (âge, pupille) + consentements (parents, adopté, conjoint) +
   enquêtes + placement 6 mois.
5. Pré-fill IA depuis 2 signaux `pupilleEtatDetected` (boolean) et
   `formeAdoptionDemandeeDetected` (string `PLENIERE`/`SIMPLE`) si présents
   dans `aiData` (fallback gracieux si absents).
6. Validation F-IA-03 : `coherenceAlerts` produit une alerte par field clé
   quand la valeur affichée diverge des sources IA.
7. POST → bandeau verdict (ELEVEE/MOYENNE/FAIBLE) + chip forme recommandée
   (peut basculer plénière → simple) + listes critères non remplis +
   documents requis + risque refus + délai instruction.

## Cas d'erreur

| Cas | UI |
|---|---|
| Form invalide (forme ou âges manquants) | Bouton "Analyser" disabled |
| 400 backend | MatSnackBar rouge avec message backend |
| 404 GET | Mode formulaire (silencieux) |

## Critères d'acceptation

- Composant `AdoptionSectionComponent` standalone créé.
- Modèle `adoption.model.ts` aligné contrat backend.
- Service `adoption.service.ts` exposant `calculate(id, req)` + `get(id)`.
- Entrée TOOL_REGISTRY `F-FA-18-adoption` ajoutée.
- Pré-fill IA implémenté avec signal provenance + handler reset.
- Validation F-IA-03 via `CoherenceAlertBuilder` (helper partagé).
- Palette navy/or, rouge **uniquement** verdict FAIBLE / forme AUCUNE.
- Datepicker non requis (champs numériques + booléens uniquement).
- Inter pour le texte, JetBrains Mono pour `baseJuridique` + `formule`.
- `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- ≥ 12 tests Jest unitaires (gate FR/BE, pré-fill, alertes, calculate,
  bannerClass, formValid).

## Plan de test (Jest ≥ 12)

1. FRANCE → GET appelé au ngOnInit
2. BELGIQUE → aucun GET (gate pays)
3. GET 200 → mode résultat
4. GET 404 → reste en formulaire
5. Pré-fill IA : `formeAdoptionDemandeeDetected=PLENIERE` + `pupilleEtatDetected=true`
   → forme + pupilleEtat coché + provenances 'IA'
6. Pré-fill sans aiData → aucun pré-rempli
7. onPupilleEtatChange efface badge IA
8. formValid false initialement, true si forme + ages renseignés
9. calculate() POST + résultat + snackBar succès + dashboardRefresh
10. calculate() ignoré si form invalide
11. calculate() erreur backend → snackBar rouge
12. coherenceAlerts.PUPILLE_ETAT présent si IA diverge
13. coherenceAlerts vides après calcul (showForm=false)
14. bannerClass mappe ELEVEE/MOYENNE/FAIBLE → classes attendues
15. ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide

## Tables / endpoints / composants

- Aucune nouvelle table (backend SF-FA-18-09 fournit la persistance).
- Endpoint consommé : `/api/v1/case-files/{id}/adoption-analysis` POST/GET.
- Nouveau composant : `frontend/src/app/case-files/adoption-section/`
  (4 fichiers : .ts / .html / .scss / .spec.ts).
- Nouveau modèle : `frontend/src/app/core/models/adoption.model.ts`.
- Nouveau service : `frontend/src/app/core/services/adoption.service.ts`.
- Modification : `decisional-tools-panel.component.ts` (TOOL_REGISTRY +
  import).
- Modification : `divorce-accepte.model.ts` `FamilleExtractedData` (ajout 2
  champs IA optionnels `pupilleEtatDetected`, `formeAdoptionDemandeeDetected`).

## Hors périmètre

- Belgique (régime CC belge — feature jumelle au backlog).
- Adoption internationale (Conv. La Haye 1993).
- Génération automatique de la requête en adoption.

## Analyse de cohérence transversale

- **Autres outils F-FA-18** : 4 SF frontend déjà livrées (reconnaissance
  paternelle, contestation paternité, recherche paternité, possession
  d'état). Cet outil traite l'établissement adoptif — pas d'overlap.
- **Autres pays** : ouverture future BE (feature jumelle au backlog
  F-FA-18-11/12).
- **Autres domaines** : non applicable (concept civil filiation/adoption).
- **UI patterns** : harmonisé avec `possession-etat-section` (template
  canonique frontend décisionnel).
- **TOOL_REGISTRY** : entrée symétrique aux autres F-FA-18.

## Impact par domaine métier

Sensible au domaine **droit de la famille FR** uniquement. Non applicable
travail / immigration. Pour BE : feature jumelle au backlog.

## Parité des domaines métier

Outil de niveau 5 (scoring / analyse de validité) — uniquement DF FR.
- DF BE : feature jumelle au backlog (régime CC belge distinct).
- DT / Immigration : non pertinent.

## Référence

- Pattern : `possession-etat-section` (PR #676 — chantier F-FA-18 jumeau).
- Backend contrat : `SF-FA-18-09-backend-adoption.md` (PR #677).
