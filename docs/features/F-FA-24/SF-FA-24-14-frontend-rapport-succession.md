# Mini-spec — F-FA-24 / SF-FA-24-14 Frontend rapport à succession (art. 843-863 + 919 Cciv)

## Identifiant

`F-FA-24 / SF-FA-24-14`

## Feature parente

`F-FA-24` — Droit des successions (chantier).

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-14-frontend-rapport-succession`

## SF jumelle

`SF-FA-24-13` (backend mergé PR #679) — contrat API figé importé.

---

## Objectif

Composant Angular `<app-rapport-succession-section>` consommant
`/api/v1/case-files/{id}/rapport-succession-analysis` (POST + GET) pour
qualifier une donation reçue par un cohéritier comme rapportable / exempte /
dispensée et afficher le mode de rapport recommandé + le montant rapportable.

---

## Contrat API (importé de SF-FA-24-13)

- `POST /api/v1/case-files/{id}/rapport-succession-analysis` body :
  - `donationsRecuesEur` (number > 0)
  - `dateDonation` (ISO YYYY-MM-DD ≤ today)
  - `valeurAuJourPartage` (number > 0)
  - `donationDispenseDeRapport` (boolean)
  - `naturePresumeeNonRapportable` (boolean)
  - `qualiteHeritier` enum (`DESCENDANT` | `CONJOINT_SURVIVANT`)
- Réponse :
  - `caseFileId`, `donationsRecuesEur`, `dateDonation`,
    `valeurAuJourPartage`, `donationDispenseDeRapport`,
    `naturePresumeeNonRapportable`, `qualiteHeritier`,
    `verdictObligation` (`RAPPORTABLE` | `EXEMPT` | `DISPENSÉ` | `NON_OBLIGÉ`),
    `modeRapportRecommande` (`RAPPORT_EN_NATURE` | `RAPPORT_EN_VALEUR` |
    `RAPPORT_EN_MOINS_PRENANT` | `NON_APPLICABLE`),
    `montantRapportable` (number, scale 2 — 0 si EXEMPT/DISPENSÉ),
    `delaiPrescriptionAns` (= 5),
    `scoreEligibilite`, `baseJuridique`, `formule`, `messages[]`, `country`.
- Erreurs : 400 (validations / pays ≠ FR / domaine ≠ DROIT_FAMILLE), 404
  (case file autre workspace).

---

## Comportement attendu

- Composant collapsable, intégré au panel décisionnel F-IA-04 via
  `TOOL_REGISTRY['F-FA-24-rapport-succession']`.
- Gate FR : `workspaceCountry === 'FRANCE'` → outil actif. BE → bannière info
  "Outil français uniquement (équivalent BE traité dans la feature jumelle
  F-FA-24-BE backlog)" — pas de masquage silencieux.
- `ngOnInit` : si FR, GET de l'analyse existante. 404 → mode formulaire +
  `prefillFromAi()`. 200 → mode résultat hydraté.
- Form (signals) : qualité héritier (radio DESCENDANT/CONJOINT_SURVIVANT) +
  `donationsRecuesEur` (number) + `dateDonation` (input type="date") +
  `valeurAuJourPartage` (number) + `donationDispenseDeRapport` (toggle/radio
  booléen) + `naturePresumeeNonRapportable` (toggle/radio booléen).
- Submit POST → résultat → snack succès → `dashboardRefresh.triggerRefresh()`.
- Erreur HTTP → snack rouge `panelClass: 'snack-error'`.
- Affichage résultat :
  - Chip verdict (RAPPORTABLE = critique rouge ; EXEMPT/DISPENSÉ/NON_OBLIGÉ =
    info navy) + label humain.
  - `montantRapportable` en JetBrains Mono.
  - Mode de rapport recommandé + libellé pédagogique (uniquement si verdict
    RAPPORTABLE — sinon NON_APPLICABLE non affiché).
  - Délai prescription 5 ans.
  - `formule` en JetBrains Mono.
  - `baseJuridique` rendu via `LegalCitationsPipe` (citations legales en
    JetBrains Mono).
  - `messages[]` rendus via `LegalCitationsPipe`.
- Bouton "Modifier" → revient au mode formulaire.

---

## Pré-fill IA + F-IA-03 (RÈGLE FONDAMENTALE)

### Pré-fill `prefillFromAi()`

Champs alimentés depuis `aiData: FamilleExtractedData` :
- `donationsRecuesEur` ← `aiData.montantDonationsRecuesEurDetecte` (à ajouter)
- `dateDonation` ← `aiData.dateDonationDetectee` (existe — SF-FA-24-06)
- `valeurAuJourPartage` ← `aiData.valeurDonationAuJourPartageEurDetectee`
  (à ajouter)
- `qualiteHeritier` ← `aiData.qualiteHeritierRapportDetectee` (à ajouter,
  string parmi `DESCENDANT` / `CONJOINT_SURVIVANT`)
- `donationDispenseDeRapport` ← `aiData.donationDispenseDeRapportDetected`
  (à ajouter)
- `naturePresumeeNonRapportable` ←
  `aiData.naturePresumeeNonRapportableDetected` (à ajouter)

Pour chaque champ pré-rempli : signal `provenance<Field>` = `'IA'`, badge
`auto_awesome` "Pré-rempli depuis l'analyse" en UI, handler
`on<Field>Change()` qui remet `provenance<Field>` à `null` au 1er changement
manuel.

### F-IA-03 alertes au changement

`coherenceAlerts` computed produit jusqu'à 6 alertes (CONJOINT_SURVIVANT
n'est pas applicable ici — la qualité d'héritier est plus large) sur les
fields :
- `QUALITE_HERITIER` (IA + PIECE_MANQUANTE)
- `DONATIONS_RECUES_EUR` (IA tolérance ±1 % + PIECE_MANQUANTE)
- `VALEUR_AU_JOUR_PARTAGE` (IA tolérance ±1 % + PIECE_MANQUANTE)
- `DATE_DONATION` (IA + PIECE_MANQUANTE)
- `DONATION_DISPENSE` (IA bool + PIECE_MANQUANTE)
- `NATURE_NON_RAPPORTABLE` (IA bool + PIECE_MANQUANTE)

Builder : `CoherenceAlertBuilder.forField<RapportSuccessionAlertField>(...)`.
Hiérarchie F-96 > Question IA > IA > PIECE_MANQUANTE — V1 utilise
principalement IA + PIECE_MANQUANTE (les codes F-96/Question IA dédiés sont
backloggés).

---

## Critères d'acceptation

- [ ] Modèle TS `rapport-succession.model.ts` aligné contrat API
      (3 modes + 4 verdicts + 2 qualités héritier).
- [ ] Service `rapport-succession.service.ts` (`calculate` POST + `get` GET).
- [ ] Composant standalone `RapportSuccessionSectionComponent` :
  - `@Input() caseFileId`, `workspaceCountry`, `aiData`,
    `procedureChecks`, `aiQuestions`, `piecesManquantes`.
  - Signals `result`, `loading`, `calculating`, `showForm`, `collapsed`,
    `provenance<Field>` × 6.
  - Computed `isFrance`, `coherenceAlerts`, `alertsSummary`.
  - Méthode `prefillFromAi()` invoquée dans `ngOnInit` ET `ngOnChanges`.
  - Handlers `on<Field>Change()` qui resettent provenance.
- [ ] Gate FR + bannière info BE (pas de masquage silencieux).
- [ ] Pré-fill IA fonctionnel + badges `auto_awesome`.
- [ ] F-IA-03 alertes câblées via `[appCoherencePopover]` directive et
      builder partagé `CoherenceAlertBuilder` (pas d'interface ad-hoc).
- [ ] `dashboardRefresh.triggerRefresh()` appelé après POST succès.
- [ ] Erreurs HTTP → `MatSnackBar` rouge `panelClass: 'snack-error'`.
- [ ] `baseJuridique` + `formule` en `JetBrains Mono`, citations rendues
      via `LegalCitationsPipe`.
- [ ] Entrée TOOL_REGISTRY `'F-FA-24-rapport-succession'` symétrique aux
      autres outils famille.
- [ ] Champs `aiData` ajoutés à `FamilleExtractedData` (frontend-only V1).
- [ ] ≥ 12 tests Jest passants.

---

## Plan de test (Jest ≥ 12)

1. Gate FR : `isFrance()` true, GET appelé au `ngOnInit`.
2. Gate BE : `isFrance()` false, aucun appel HTTP.
3. GET 200 → mode résultat hydraté, `showForm()` false.
4. GET 404 → mode formulaire, `showForm()` true.
5. Pré-fill IA complet (6 champs) → valeurs + provenance `IA` × 6.
6. Pré-fill sans `aiData` → aucun champ rempli, provenance null.
7. `onQualiteHeritierChange` efface badge IA.
8. `formValid()` initialement false.
9. `formValid()` true après remplissage complet.
10. `formValid()` false si `donationsRecuesEur` ≤ 0.
11. `calculate()` POST envoie le body attendu, hydrate résultat, snack succès.
12. `calculate()` ignoré si form invalide (aucun appel HTTP).
13. `calculate()` erreur backend → snack rouge.
14. `coherenceAlerts.QUALITE_HERITIER` divergent IA → alerte source IA.
15. `coherenceAlerts.MONTANT_RAPPORTABLE` divergent IA tolérance ±1 % → alerte.
16. `coherenceAlerts` vides après calcul (`showForm` false).
17. `ngOnChanges(aiData)` post-mount rafraîchit pré-fill si form vide.
18. `ngOnChanges(aiData)` post-saisie ne réécrase pas saisie avocat.
19. `verdictLabel` couvre les 4 valeurs.
20. `verdictChipClass` discrimine RAPPORTABLE (critique) vs autres (info).
21. `editMode()` ré-affiche le form.
22. `toggleCollapse()` fonctionne.

Self-check pré-commit (5/5) :
- [ ] grep `RapportSuccessionSectionComponent` → ≥ 1 entrée TOOL_REGISTRY.
- [ ] grep `prefillFromAi` dans `.component.ts` → présent.
- [ ] grep `coherenceAlerts` dans `.component.ts` → présent.
- [ ] grep `CoherenceAlertBuilder` dans `.component.ts` → présent.
- [ ] grep `dashboardRefresh.triggerRefresh` dans `.component.ts` → présent.

---

## Tables / endpoints / composants impactés

- **Composant** : `frontend/src/app/case-files/rapport-succession-section/`
  (4 fichiers : ts/html/scss/spec).
- **Modèle** : `frontend/src/app/core/models/rapport-succession.model.ts`.
- **Service** : `frontend/src/app/core/services/rapport-succession.service.ts`.
- **Modifications** :
  - `frontend/src/app/core/models/divorce-accepte.model.ts` (+ 6 champs
    `FamilleExtractedData` pour pré-fill IA).
  - `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`
    (+ import + entrée TOOL_REGISTRY).

## Hors périmètre

- Backend (SF-FA-24-13 — mergé PR #679).
- Régime BE (CC BE art. 843+ — feature jumelle backlog F-FA-24-BE).
- Calcul global de la masse à partager — V1 traite donation par donation.
- Codes F-96 / Question IA dédiés (backlog — V1 utilise IA +
  PIECE_MANQUANTE).
- Population réelle des champs `aiData` côté backend (frontend-only V1, comme
  F-FA-10/11/14/etc.) — l'IA enrichira progressivement.

---

## Impact par domaine métier

Cette SF est **strictement Droit de la famille FR** — outil dédié successions
FR, single-country.
- **Droit du travail** : non applicable (gate côté backend, retour 400).
- **Immigration** : non applicable.
- **Famille FR** : ce qu'on livre.
- **Famille BE** : non couvert ici, feature jumelle dédiée backlog
  (F-FA-24-BE), barème CC BE différent.

## Parité des domaines métier

Outil de niveau 5 (scoring d'obligation). Pas d'équivalent dans
Travail/Immigration (concept successoral propre au droit civil patrimonial).
Pas de feature jumelle requise hors du chantier successions.

## Analyse de cohérence transversale

| Cible | Statut |
|-------|--------|
| Outils décisionnels FR famille (dévolution F-FA-24-02, testament F-FA-24-04, donation F-FA-24-06, réserve F-FA-24-08) | Pattern cohérent (signals + pré-fill IA + F-IA-03 builder partagé + gate FR + bannière info BE) — **intégré** |
| Outils BE famille | Hors scope — backlog F-FA-24-BE dédié |
| Outils Travail/Immigration | Non applicable |
| Préoccupation transversale "Outil décisionnel métier" | Outil isolé : un outil = une situation (rapport à succession = qualification d'une donation comme rapportable/exempte/dispensée), distinct de la réserve héréditaire (action en réduction = excédent global libs > QD) — pattern F-DT-08/F-DT-10 respecté |
| Préoccupation transversale "Auth / Principal" | Pas de modification du Principal — `@Input()` standard, ne touche pas à l'auth |
| Préoccupation transversale "Workspace context" | Lecture seule de `workspaceCountry` — gate FR |
| Préoccupation transversale "Plans / limites" | Non applicable — outil gratuit dans l'offre `decisional` |
| Préoccupation transversale "Navigation / routing" | Pas de nouvelle route — composant intégré au panel F-IA-04 existant |

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern partagé — réutilise strictement le pattern
`reserve-heriditaire-section` (PR #675) avec :
- Signals + computed standardisés.
- `CoherenceAlertBuilder` pour F-IA-03 (pas d'interface locale ad-hoc).
- `LegalCitationsPipe` pour citations.
- `[appCoherencePopover]` directive partagée.
- Palette navy/or, rouge réservé à RAPPORTABLE/erreur HTTP.
