# Mini-spec — F-207 / SF-207-08b-frontend Outil outplacement obligatoire 45+ (UI)

## Identifiant

`F-207 / SF-207-08b-frontend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-08b-frontend-outplacement-be-obligatoire-45`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées #1119. Pattern miroir : `c4-onem-checklist-section` (#1133, verdict + sanction quantifiée + lettre rectificative copiable). Dernière SF F-207.

## Objectif

Section frontend de l'analyseur outplacement 45+ (backend SF-207-08 #1160). Formulaire 9 champs (3 dates + 1 enum + 4 bool + 1 number) avec visibilité conditionnelle ; verdict 5 états + sanction quantifiée (€ employeur ou range semaines salarié). BE-only.

## Contrat API (figé #1160)

`POST` + `GET /api/v1/case-files/{caseFileId}/decision-tools/outplacement-be-obligatoire-45`

Inputs :
```ts
{
  dateLicenciement: string;                      // ISO, requis
  dateNaissanceSalarie: string;                  // ISO, requis
  ancienneteAnnees: number;                       // BigDecimal, requis ≥ 0
  motifLicenciement: 'LICENCIEMENT_ECONOMIQUE' | 'LICENCIEMENT_AUTRE' | 'FAUTE_GRAVE' | 'DEMISSION';
  contratTempsPlein: boolean;
  offreOutplacementRecue: boolean;
  dateOffreOutplacement?: string | null;         // requis si offreRecue=true
  offreConformeCCT82?: boolean | null;            // requis si offreRecue=true
  salarieAcceptantOffre?: boolean | null;
}
```

Réponse 200 — verdict 5 états + `sanctionEmployeurEuros` + `sanctionSalarieRange` + `dateLimiteOffre` + `delaiOffreRespecte` + `etapeSuivante` + `baseJuridique` + `formuleCalcul`.

## Comportement

Section `outplacement-be-obligatoire-45-section.component` — pattern F-IA-04.

### Formulaire à visibilité conditionnelle

- `dateLicenciement` (date, requis).
- `dateNaissanceSalarie` (date, requis).
- `ancienneteAnnees` (number, requis, ≥ 0, hint « décimales possibles »).
- Select `motifLicenciement` (4 options humanisées).
- Checkbox `contratTempsPlein` (hint « CCT 82 bis si décoché »).
- Checkbox `offreOutplacementRecue`.
- **Si `offreOutplacementRecue=true`** : affiche les 3 champs conditionnels :
  - `dateOffreOutplacement` (date, requis).
  - Checkbox `offreConformeCCT82` avec hint « 60 h sur 12 mois ».
  - Radio tri-état `salarieAcceptantOffre` (Oui / Non / Non encore décidé).
- Bouton « Évaluer la conformité ».

### Pré-fill IA

| Champ | Source aiData |
|---|---|
| `dateLicenciement` | `aiData.dateLicenciement` (existant) |
| `dateNaissanceSalarie` | `aiData.dateNaissanceSalarie` (SF-207-06) |
| `ancienneteAnnees` | `aiData.ancienneteSalarie` (SF-207-08 backend) |
| `motifLicenciement` | `aiData.motifLicenciementDetecte` (whitelist 4 enum) |
| `offreOutplacementRecue` | `aiData.offreOutplacementMentionnee` (true uniquement) |

`getPrefillCount` 0-5.

### Verdict UI

Badge coloré :
- Vert : `OUTPLACEMENT_NON_DU` (employeur exempté — info) ou `OFFRE_CONFORME_RESPECTEE`.
- Rouge : `OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR` ou `OFFRE_NON_FAITE_SANCTION_EMPLOYEUR` — affiche `sanctionEmployeurEuros` (1 800,00 €) en évidence + encart « Étape suivante : PLAINTE_INSPECTION_LOIS_SOCIALES ».
- Ambre : `OFFRE_REFUSEE_SANCTION_SALARIE` — affiche `sanctionSalarieRange` (4-52 semaines) + encart « Voir C4 ONEM ».

Si `OUTPLACEMENT_NON_DU` : afficher `raisonNonObligation` humanisée (« âge inférieur à 45 ans », « ancienneté inférieure à 1 an », « faute grave », « démission »).

Si offre reçue : afficher `dateLimiteOffre` + `delaiOffreRespecte` (✓ vert / ✗ rouge).

`baseJuridique` + `formuleCalcul` en `JetBrains Mono`.

### TOOL_REGISTRY

`outplacement-be-obligatoire-45` inséré après `rcc-be-indemnite-complementaire` (séquence métier). Theme `VALIDITE`.

### Visibility seed

Migration `XXX-add-outplacement-be-visibility.xml` (prochain après 266) : ALWAYS_ON BELGIQUE / DROIT_DU_TRAVAIL priority 96. Outplacement = sujet récurrent en consultation BE Travail post-licenciement.

## Critères

- [ ] Section rend formulaire 9 champs conditionnels + verdict + sanction ; gate `BELGIQUE` strict.
- [ ] Checkbox `offreOutplacementRecue` masque/affiche 3 champs conditionnels.
- [ ] Pré-fill 5 champs.
- [ ] `getPrefillCount` 0-5.
- [ ] 5 verdicts colorés (vert/rouge/ambre).
- [ ] `sanctionEmployeurEuros` formatée en euros fr-BE.
- [ ] `sanctionSalarieRange` "4-52 semaines" affichée si pertinent.
- [ ] `raisonNonObligation` humanisée si NON_DU.
- [ ] `delaiOffreRespecte` ✓/✗.
- [ ] Migration visibility ALWAYS_ON priority 96.

## Composants

Standard sous `frontend/src/app/case-files/outplacement-be-obligatoire-45-section/` + model + service + modifs panel + case-analysis.model.ts (3 fields) + migration backend visibility.

## Tests Jest
- prefill-rules : 5+ tests.
- component : 10+ tests (visibilité conditionnelle, 5 verdicts, sanction, raison non-obligation).

## Dépendances
- Backend SF-207-08 (#1160 mergé).
- Pattern frontend `c4-onem-checklist-section` (#1133).
