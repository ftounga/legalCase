# Mini-spec — F-218 / SF-218-07 — Saisie sur rémunération (quotité saisissable) — backend

## Identifiant

`F-218 / SF-218-07`

## Feature parente

`F-218a` — Procédure CPH avancée (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-07-saisie-remuneration-backend`

---

## Objectif

Calculer la quotité saisissable d'une rémunération selon le barème annuel par tranches (R. 3252-2 Code travail) avec correction pour personnes à charge (R. 3252-3) et fraction absolument insaisissable (montant RSA), pour accompagner l'avocat côté créancier ou côté salarié saisi après jugement CPH.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/saisie-remuneration-analysis`
- Body :
  - `remunerationNetteMensuelle` (Double > 0, requis) — rémunération nette mensuelle servant d'assiette
  - `nombrePersonnesACharge` (Integer ≥ 0, défaut 0)
  - `creanceTotale` (Double > 0, requis) — montant de la créance à recouvrer
  - `creanceAlimentaire` (boolean, défaut false) — si la créance est alimentaire (paiement direct prioritaire, hors plafond classique)
- Analyzer/Calculator `SaisieRemunerationCalculator` :
  - **Barème par tranches** : applique le barème annuel de `R. 3252-2` (5 tranches + fraction au-delà, quotités progressives 1/20, 1/10, 1/5, 1/4, 1/3, puis totalité au-delà). Les bornes des tranches sont annualisées puis ramenées au mois. **Constante `BAREME_SAISIE_REMUNERATION_2026` documentée « à actualiser annuellement » (décret annuel).**
  - **Correction personnes à charge** : majoration de chaque borne de tranche d'un montant par personne à charge (R. 3252-3) — **constante `MAJORATION_PAR_PERSONNE_A_CHARGE_2026`**.
  - **Fraction insaisissable** : la part inférieure au montant forfaitaire RSA (1 personne) est absolument insaisissable (L. 3252-3) — **constante `FRACTION_INSAISISSABLE_RSA_2026`**.
  - **Calcul** : `quotiteSaisissableMensuelle` (somme par tranche), `montantLaisseAuSalarie` (remunérationNette − quotité, jamais < fraction insaisissable), `nombreMoisRecouvrement` (creanceTotale / quotité, arrondi sup).
  - **Créance alimentaire** : si `creanceAlimentaire=true` → mention paiement direct (loi 1973) hors barème, `quotiteSaisissable` = fraction insaisissable RSA mise à part uniquement.
  - **Verdict** : `SAISISSABLE` (quotité > 0), `INSAISISSABLE` (rémunération ≤ fraction insaisissable), `ALIMENTAIRE_PAIEMENT_DIRECT`.
  - `baseJuridique` : R. 3252-1 à R. 3252-5 Code travail ; L. 3252-2 et L. 3252-3.
- Output persisté dans `saisie_remuneration_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/saisie-remuneration-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| remunerationNetteMensuelle ≤ 0 | 400 |
| creanceTotale ≤ 0 | 400 |
| nombrePersonnesACharge < 0 | 400 |
| caseFile inaccessible | 404 |

---

## Source juridique

- **R. 3252-2 Code travail** — barème annuel des quotités saisissables par tranches (révisé chaque année par décret).
- **R. 3252-3 Code travail** — majoration des seuils par personne à charge.
- **L. 3252-3 Code travail** — fraction absolument insaisissable (montant forfaitaire RSA).
- **R. 3252-1 et s.** — procédure de saisie des rémunérations.
- **Constantes à actualiser annuellement** : `BAREME_SAISIE_REMUNERATION_2026` (bornes de tranches), `MAJORATION_PAR_PERSONNE_A_CHARGE_2026`, `FRACTION_INSAISISSABLE_RSA_2026`. Le décret annuel publie les nouveaux seuils chaque décembre — une note de maintenance documente la mise à jour annuelle.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `remunerationNetteMensuelle` | nombre | dérivé de `salaireBrutMensuel` (proxy, conversion brut→net non garantie) | Réutiliser brut + note provenance ; sinon saisie manuelle |
| `nombrePersonnesACharge` | nombre | `nombrePersonnesACharge` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `saisie_remuneration_detectee` (niveau 2, FR-only, default false) — nouveau flag. Bascule CONTEXTUAL quand l'IA détecte une procédure de saisie sur rémunération (mention « saisie sur salaire », « quotité saisissable », « titre exécutoire », « commissaire de justice »).

---

## Critères d'acceptation

- [ ] POST `remunerationNetteMensuelle=2000`, `nombrePersonnesACharge=0` → `quotiteSaisissableMensuelle` conforme au barème par tranches (valeur déterministe testée)
- [ ] POST `nombrePersonnesACharge=3` → quotité réduite vs 0 personne (majoration des seuils appliquée)
- [ ] POST `remunerationNetteMensuelle` ≤ fraction insaisissable → verdict `INSAISISSABLE`, quotité 0
- [ ] POST `creanceAlimentaire=true` → verdict `ALIMENTAIRE_PAIEMENT_DIRECT`
- [ ] POST `nombreMoisRecouvrement` = ceil(creance / quotité)
- [ ] POST `remunerationNetteMensuelle=0` ou `creanceTotale=0` → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_FAMILLE → 400
- [ ] GET sans POST → 404 ; upsert sur double POST
- [ ] Isolation workspace
- [ ] Constantes barème documentées « à actualiser annuellement »
- [ ] Seed `decision_tool_visibility_rules` : CONTEXTUAL, trigger_field=`saisie_remuneration_detectee`, trigger_value=`true`
- [ ] `F-DT-89-saisie-arret-remuneration` dans `KNOWN_FRONTEND_TOOL_IDS`

## Plan de test minimal

- **UT** `SaisieRemunerationCalculatorTest` : ≥ 6 cas (chaque tranche, majoration personnes à charge, insaisissable, alimentaire, nombre de mois recouvrement, borne haute totalité)
- **IT** `SaisieRemunerationControllerIT` : ≥ 5 cas (200 nominal, 400 country, 400 rémunération nulle, 404 isolation, upsert)

## Tables / endpoints / composants impactés

- **Nouvelle table** `saisie_remuneration_analyses`
- **Migration Liquibase** + seed visibility rules
- **Endpoint** `SaisieRemunerationController`
- **Service** `SaisieRemunerationService` + **Calculator** `SaisieRemunerationCalculator`
- **Constantes** `BaremeSaisieRemuneration` (BAREME_*_2026, MAJORATION_*, FRACTION_INSAISISSABLE_*) à actualiser annuellement
- **Extension** `TravailExtractedData` : `nombrePersonnesACharge`, flag `saisieRemunerationDetectee` + prompt
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-08)
- Conversion automatique brut→net certifiée (proxy via salaireBrutMensuel)
- Pluralité de saisies concurrentes (répartition entre créanciers)
- Génération de l'acte de saisie
