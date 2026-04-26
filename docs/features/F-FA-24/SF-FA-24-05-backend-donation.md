# Mini-spec — F-FA-24 / SF-FA-24-05 Backend donation entre vifs

## Identifiant

`F-FA-24 / SF-FA-24-05`

## Feature parente

`F-FA-24` — Droit des successions (chantier ~9-11 SF — déjà livrées : SF-01 dévolution légale, SF-02 frontend dévolution, SF-03 testament backend, SF-04 testament frontend).

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-05-backend-donation`

---

## Objectif

5ème SF backend de F-FA-24 — calculator + endpoint d'analyse de la **validité d'une donation entre vifs** (FR — art. 893-958 + 902-906 + 920+ Cciv) — détermine la forme de la donation (notariée / manuelle / indirecte / déguisée), identifie les risques (capacité, formalisme, vices, requalification, quotité disponible, révocation possible) et rend un verdict (VALIDE / CONTESTABLE / NUL).

---

## Comportement attendu

### Cas nominal

L'avocat saisit la forme de la donation + critères de capacité, consentement, formalisme et de quotité disponible → l'outil applique les exigences légales propres à chaque forme + capacité (art. 902, 906) + vices consentement (art. 901+) + révocation (art. 953-958) → renvoie un verdict, la liste des risques de requalification, l'éventuelle action en réduction (art. 920+) et la base juridique.

#### 4 formes de donation FR

1. **DONATION_NOTARIEE** (art. 931) : forme authentique obligatoire devant notaire pour les immeubles et les promesses de donation. Critères : `acteAuthentique`, `acceptationExpresse`.
2. **DONATION_MANUELLE** (art. 894+, jurisprudence constante) : remise effective d'un bien meuble (cash, chèque, virement, bijoux). Pas d'écrit obligatoire mais preuve fragile. Critères : `remiseEffective`, `bienMeuble`.
3. **DON_INDIRECT** : avantage octroyé sans intention apparente de donner (renonciation à un droit, remise de dette, garantie...). Critères : `intentionLiberale`, `actePrincipalNeutre`.
4. **DONATION_DEGUISEE** : sous l'apparence d'un acte onéreux (vente à prix vil, par ex.) — risque de requalification fiscale et civile. Critères : `apparenceOnerueuse`, `prixIncoherent`.

#### Capacité (art. 902-906 Cciv)

- `capaciteDonateur` (art. 902 — sain d'esprit, ≥ 16 ans pour mobilier, ≥ 18 pour reste).
- `capaciteRecipiendaire` (art. 906 — exister à la date, pas être incapable absolu — par ex. médecin du donateur, art. 909).

#### Consentement (art. 901+)

- `consentementLibre` (vices : dol, violence, erreur).

#### Objet (art. 893+)

- `objetDeterminé` (le bien donné existe et appartient au donateur).

#### Formalisme (art. 931+)

- `respectFormalisme` (notarié si art. 931, sinon preuve à charge — pas de formalité pour le don manuel).

#### Quotité disponible / réserve (art. 913-920)

- `respectQuotiteDisponible` (boolean) → si excès, la donation reste valide mais ouvre **action en réduction** (art. 920+) — délai 5 ans à compter du décès du donateur.

#### Révocation (art. 953-958)

- `revocationPossible` (3 motifs limitatifs) :
  - Ingratitude (art. 955-958) — attentat, sévices, refus aliments.
  - Inexécution des charges (art. 953) — la donation est assortie d'une obligation non remplie.
  - Survenance d'enfant (art. 960-961) — si donateur sans enfant à la donation, abrogé en 2007 sauf clause expresse.

#### Verdict

- **VALIDE** : forme respectée + capacité + pas de vices + formalisme respecté.
- **CONTESTABLE** : un ou plusieurs critères douteux mais pas tranché — `risquesRequalification` non vide, le juge tranchera.
- **NUL** : vice rédhibitoire (incapacité absolue, vice de consentement, formalisme manifestement non respecté quand obligatoire, objet inexistant).

#### Délai d'action

`delaiContestationAns` :
- **5 ans** (art. 1304 / 2224 Cciv) — délai de droit commun pour vices de consentement (dol, erreur).
- **30 ans** — nullité absolue (incapacité, défaut de forme art. 931 quand obligatoire) — note : depuis la réforme 2008, ramené à 5 ans pour la plupart des nullités absolues. Conservation du seuil 30 ans à titre informatif pour éviter toute interprétation hâtive — l'avocat tranchera.

L'outil renvoie 5 ans par défaut, et signale 30 ans dans `messages` lorsqu'un vice de capacité absolu ou de défaut de forme est détecté.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body absent | "Corps de requête requis" | 400 |
| `formeDonation` null | "Forme de la donation requise" | 400 |
| `dateDonation` null | "Date de la donation requise" | 400 |
| `ageDonateurAns` null | "Âge du donateur requis" | 400 |
| `ageDonateurAns` < 0 ou > 130 | "Âge du donateur invalide" | 400 |
| Workspace pays ≠ FRANCE | "Outil non disponible pour le pays X — backlog jumeau F-FA-24-BE-donation" | 400 |
| Dossier ≠ DROIT_FAMILLE | "Ce dossier n'est pas un dossier de droit de la famille" | 400 |
| Dossier d'un autre workspace | "Case file not found" | 404 |
| GET sans POST préalable | "Aucune analyse de donation trouvée pour ce dossier" | 404 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils décisionnels famille FR** : F-FA-24 SF-01 (dévolution légale), SF-03 (testament), F-FA-16 (communauté universelle), F-FA-17 (partage judiciaire) — **classement** : déjà séparés. La donation est une situation distincte (acte juridique entre vifs, gratuit, par opposition au testament qui est unilatéral et à effet post-mortem). **Non applicable** : aucun outil existant ne couvre la validité d'une donation.
- [x] **Autres pays** : Belgique → règles différentes (CC BE art. 893+ — formes équivalentes mais quelques exigences notamment sur les donations entre époux). **Backlog jumeau F-FA-24-BE-donation** prévu (mention dans la mini-spec et message d'erreur explicite si workspace BE).
- [x] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_IMMIGRATION → **non applicable** (donation = strictement DROIT_FAMILLE).
- [x] **UI patterns** : section décisionnelle Angular F-IA-04 → SF-FA-24-06 frontend future (séquentiel).
- [x] **Auth / workspace** : pattern `CurrentUserResolver` + `WorkspaceMemberRepository` + gate `legalDomain == "DROIT_FAMILLE"` + gate `country == "FRANCE"` (cf. SF-FA-24-01 dévolution + SF-FA-24-03 testament) → **réutilisé tel quel**.

### Verdict

Pattern aligné F-FA-24 SF-03 (TestamentValidite) et F-FA-24 SF-01 (DevolutionLegale). Aucune duplication créée — outil isolé, single-country, single-domain.

---

## Impact par domaine métier

- **Sensibilité au domaine** : forte — feature 100% droit famille FR (donation entre vifs, succession). Aucun impact DROIT_DU_TRAVAIL ou DROIT_IMMIGRATION.
- **Sensibilité au pays** : forte — règles propres au Code civil français. Belgique = backlog jumeau **F-FA-24-BE-donation** (CC BE art. 893+ avec différences).

---

## Parité des domaines métier (outil de niveau 5 — scoring/analyse de validité)

L'outil est une **analyse de validité** (niveau 5 — verdict VALIDE/CONTESTABLE/NUL). Application des règles de parité :

| Domaine | Équivalent existant | Décision |
|---------|---------------------|----------|
| DROIT_DU_TRAVAIL | N/A — concept inapplicable | Non applicable, justifié |
| DROIT_IMMIGRATION | N/A — concept inapplicable | Non applicable, justifié |
| DROIT_FAMILLE FRANCE | **Cette SF** | En cours |
| DROIT_FAMILLE BELGIQUE | Règles propres CC BE art. 893+ | **Backlog jumeau F-FA-24-BE-donation** à ouvrir |

---

## Critères d'acceptation

1. POST `/api/v1/case-files/{id}/donation-analysis` avec body valide (FR, DROIT_FAMILLE) → 200 + `verdictValidite` + `formeDonation` + `risquesRequalification` + `actionEnReductionPossible` + `delaiContestationAns` + `baseJuridique` + `formule` + `messages`.
2. **Donation notariée** valide (acte authentique + acceptation expresse + capacité OK) → `verdictValidite=VALIDE`.
3. **Donation notariée** sans acte authentique → `verdictValidite=NUL`, code "FORME_NOTARIEE_NON_AUTHENTIQUE".
4. **Donation manuelle** valide (remise effective + bien meuble) → `verdictValidite=VALIDE`.
5. **Donation manuelle** sans remise effective → `verdictValidite=NUL`, code "FORME_MANUELLE_SANS_REMISE".
6. **Donation manuelle** sur bien immeuble → `verdictValidite=NUL`, code "FORME_MANUELLE_BIEN_NON_MEUBLE".
7. **Don indirect** sans intention libérale → `verdictValidite=CONTESTABLE`, code "DON_INDIRECT_INTENTION_LIBERALE".
8. **Donation déguisée** apparence onéreuse + prix incohérent → `verdictValidite=CONTESTABLE` + risque "REQUALIFICATION_DEGUISEMENT".
9. **Mineur < 16 ans donateur** → `verdictValidite=NUL`, code "INCAPACITE_DONATEUR".
10. **Récipiendaire incapable absolu (médecin du donateur, art. 909)** → `verdictValidite=NUL`, code "INCAPACITE_RECIPIENDAIRE".
11. **Vice de consentement (dol)** → `verdictValidite=NUL`, code "VICE_CONSENTEMENT_DOL".
12. **Erreur substantielle** → `verdictValidite=NUL`, code "VICE_CONSENTEMENT_ERREUR".
13. **Objet indéterminé** → `verdictValidite=NUL`, code "OBJET_INDETERMINE".
14. **Donation excédant la quotité disponible** → `verdictValidite=VALIDE` + `actionEnReductionPossible=true`.
15. **Ingratitude (révocation possible)** → `revocationPossible=true` + message + verdict reste `VALIDE` (la révocation est un événement post-donation qui ne nullifie pas la donation à sa formation).
16. POST workspace BE → 400 mentionnant `BELGIQUE` et backlog jumeau.
17. POST dossier DROIT_DU_TRAVAIL FR → 400.
18. POST dossier d'un autre workspace → 404.
19. POST upsert (2ème POST sur même dossier) → remplace l'analyse précédente (1 ligne via UNIQUE).
20. GET après POST → renvoie l'analyse persistée.
21. GET sans POST → 404.
22. `baseJuridique` contient `893`, `902`, `906`, `920`, `931`, `953`.
23. Migration Liquibase 184 crée la table + UNIQUE + insert visibility rule ALWAYS_ON DROIT_FAMILLE FRANCE priority 93 UUID `f1a04001-0000-0000-0000-ee0000000184` tool_id `F-FA-24-donation`.

---

## Plan de test

### Tests unitaires (`DonationCalculatorTest`) — ≥ 18

1. Notariée valide → VALIDE.
2. Notariée sans acte authentique → NUL.
3. Notariée sans acceptation expresse → NUL.
4. Manuelle valide → VALIDE.
5. Manuelle sans remise effective → NUL.
6. Manuelle sur immeuble → NUL.
7. Don indirect avec intention libérale → VALIDE.
8. Don indirect sans intention libérale → CONTESTABLE.
9. Donation déguisée prix incohérent → CONTESTABLE + risque requalification.
10. Donation déguisée apparence cohérente → VALIDE.
11. Mineur < 16 ans donateur → NUL.
12. Donateur insanité d'esprit → NUL.
13. Récipiendaire incapable absolu (médecin) → NUL.
14. Vice consentement dol → NUL.
15. Erreur substantielle → NUL.
16. Objet indéterminé → NUL.
17. Excès quotité disponible → VALIDE + actionEnReductionPossible=true.
18. Ingratitude → VALIDE + revocationPossible=true.
19. Validation : country null → IllegalArgumentException.
20. Validation : country BELGIQUE → IllegalArgumentException mentionnant feature jumelle.
21. `baseJuridique` contient 893, 902, 906, 920, 931, 953.
22. `formule` contient forme + verdict + score.

### Tests intégration (`DonationControllerIT`) — ≥ 7

1. POST FR DROIT_FAMILLE notariée valide → 200 + verdictValidite=VALIDE.
2. POST FR DROIT_FAMILLE manuelle non remise → 200 + verdictValidite=NUL.
3. POST FR DROIT_FAMILLE excès quotité → 200 + actionEnReductionPossible=true.
4. POST workspace BE → 400.
5. POST DROIT_DU_TRAVAIL FR → 400.
6. POST autre workspace → 404.
7. POST formeDonation manquant → 400.
8. POST upsert remplace → 200 + nouveau verdict.
9. GET après POST → 200 + données persistées.
10. GET sans POST → 404.

### Isolation workspace

Test cross-workspace explicite (workspace A POST sur dossier de workspace B → 404).

---

## Tables / endpoints / composants impactés

### Tables
- **Nouvelle** : `donation_analyses` (1:1 case_files via UNIQUE) — créée par migration **184-create-donation-analyses.xml**.
- **Modifiée** : `decision_tool_visibility_rules` — INSERT règle ALWAYS_ON DROIT_FAMILLE FRANCE tool_id `F-FA-24-donation` priority 93 UUID `f1a04001-0000-0000-0000-ee0000000184`.

### Endpoints
- `POST /api/v1/case-files/{caseFileId}/donation-analysis` — upsert
- `GET /api/v1/case-files/{caseFileId}/donation-analysis` — lecture

### Composants Java
- `DonationRequest` (record)
- `DonationResponse` (record)
- `DonationResult` (record)
- `DonationAnalysis` (entity JPA)
- `DonationRepository` (JpaRepository)
- `DonationCalculator` (final class — règles métier pures)
- `DonationService` (Spring service — orchestration + auth)
- `DonationController` (REST endpoint)

---

## Hors périmètre

- **Frontend** : SF-FA-24-06 future.
- **Belgique** : F-FA-24-BE-donation (backlog jumeau).
- **Autres SF de F-FA-24** : réserve héréditaire (913+), action en réduction, partage successoral, indivision successorale, rapport à succession.
- Cas exotiques non couverts en SF-05 :
  - Donation entre époux (révocable spécifiquement art. 1096) — feature distincte.
  - Donation-partage (art. 1075-1078) — feature distincte.
  - Pacte successoral (art. 1075+) — feature distincte.
  - Tontine — non lié à la donation.
  - Présent d'usage (art. 852) — exclu de la matière donation.
  - Calcul fiscal des droits de donation — hors périmètre juridique.

---

## Contrat API

### POST /api/v1/case-files/{caseFileId}/donation-analysis

**Body**
```json
{
  "formeDonation": "DONATION_NOTARIEE",
  "dateDonation": "2024-03-15",
  "ageDonateurAns": 65,
  "saineDEsprit": true,
  "capaciteDonateur": true,
  "capaciteRecipiendaire": true,
  "consentementLibre": true,
  "objetDeterminé": true,
  "respectFormalisme": true,
  "respectQuotiteDisponible": true,
  "acteAuthentique": true,
  "acceptationExpresse": true,
  "remiseEffective": null,
  "bienMeuble": null,
  "intentionLiberale": null,
  "actePrincipalNeutre": null,
  "apparenceOnerueuse": null,
  "prixIncoherent": null,
  "vicesConsentementDol": false,
  "erreurSubstantielle": false,
  "ingratitudeAvere": false,
  "inexecutionCharge": false
}
```

**Réponse 200**
```json
{
  "caseFileId": "...",
  "formeDonation": "DONATION_NOTARIEE",
  "verdictValidite": "VALIDE",
  "risquesRequalification": [],
  "actionEnReductionPossible": false,
  "revocationPossible": false,
  "delaiContestationAns": 5,
  "scoreEligibilite": 100,
  "baseJuridique": "Art. 893-958, 902-906, 920 et s., 931, 953-958 Cciv",
  "formule": "Forme DONATION_NOTARIEE + verdict VALIDE + 0 risque → score 100",
  "messages": ["..."],
  "country": "FRANCE"
}
```

**Codes enum**
- `formeDonation` : `DONATION_NOTARIEE` | `DONATION_MANUELLE` | `DON_INDIRECT` | `DONATION_DEGUISEE`
- `verdictValidite` : `VALIDE` | `CONTESTABLE` | `NUL`
- `risquesRequalification[*].code` : `FORME_NOTARIEE_NON_AUTHENTIQUE`, `FORME_NOTARIEE_SANS_ACCEPTATION`, `FORME_MANUELLE_SANS_REMISE`, `FORME_MANUELLE_BIEN_NON_MEUBLE`, `DON_INDIRECT_INTENTION_LIBERALE`, `REQUALIFICATION_DEGUISEMENT`, `DEGUISEMENT_PRIX_VIL`, `INCAPACITE_DONATEUR`, `INSANITE_ESPRIT`, `INCAPACITE_RECIPIENDAIRE`, `VICE_CONSENTEMENT_DOL`, `VICE_CONSENTEMENT_ERREUR`, `OBJET_INDETERMINE`, `FORMALISME_NON_RESPECTE`, `EXCES_QUOTITE_DISPONIBLE`, `REVOCATION_INGRATITUDE`, `REVOCATION_INEXECUTION_CHARGE`

---

## Préoccupations transversales

- [x] **Outil décisionnel métier** : F-FA-24 SF-05 = nouvel outil dédié à la validité d'une donation entre vifs FR. Scan effectué : aucun outil existant ne le couvre (testament SF-03 = acte unilatéral à effet post-mortem, donation = acte gratuit entre vifs avec effet immédiat). Le périmètre F-FA-24 sera découpé en 9-11 SF, chacune = un outil pour une situation distincte.
- [x] **Auth / Principal** : pattern `OidcUser + Principal` réutilisé tel quel — aucun changement.
- [x] **Workspace context** : pattern `WorkspaceMemberRepository.findByUserAndPrimaryTrue` + gate FRANCE/DROIT_FAMILLE strictement copié de SF-FA-24-03 — aucun changement.

Aucune préoccupation critique modifiée — pas besoin de smoke tests E2E.
