# SF-211-02 — Divorce pour désunion irrémédiable (DDI, 3 voies) BE — backend

## Objectif (1 phrase)
À partir d'indices factuels (dates de séparation, nature de la demande), déterminer la voie ouverte au titre du divorce pour désunion irrémédiable belge (CC art. 229 §§1, 2, 3) et indiquer le délai restant si le seuil temporel n'est pas encore atteint.

## Comportement nominal
- POST `/api/v1/case-files/{caseFileId}/divorce-ddi-3voies-be-analysis`
- Body : `dateSeparation` (LocalDate, requis), `natureDemande` ∈ {COMMUNE_APRES_SEPARATION, UNILATERALE_APRES_SEPARATION, CONSTATATION_PAR_JUGE} (requis), `preuvesDesunionDisponibles` (boolean, requis)
- Calculator `DivorceDdiBeCalculator` calcule :
  - §1 (constatation directe par juge — preuves) : toujours ouvert si `preuvesDesunionDisponibles=true`
  - §2 (commune après ≥ 6 mois séparation) : ouvert si nature=COMMUNE et joursSeparation ≥ 180
  - §3 (unilatérale après ≥ 1 an séparation) : ouvert si nature=UNILATERALE et joursSeparation ≥ 365
  - `voieRecommandee` = la voie qui s'applique la plus rapidement
  - `joursRestants` jusqu'à éligibilité prochaine voie temporelle
- Persistance 1:1 `divorce_ddi_be_analyses`
- GET → 200 ou 404

## Cas d'erreur
- 400 si dateSeparation futur, natureDemande inconnue, paramètres null
- 400 si workspace.country ≠ BELGIQUE
- 400 si caseFile.legalDomain ≠ DROIT_FAMILLE
- 404 isolation workspace

## Critères d'acceptation vérifiables
- [x] §1 ouvert si preuves dispo (durée séparation indifférente)
- [x] §2 ouvert après 6 mois si nature=COMMUNE
- [x] §3 ouvert après 1 an si nature=UNILATERALE
- [x] joursRestants calculé correctement si pas encore éligible
- [x] POST FR retourne 400
- [x] POST travail retourne 400
- [x] GET sans POST → 404

## Plan de test minimal
- **UT** `DivorceDdiBeCalculatorTest` : 10+ tests (chaque voie ouverte / fermée, joursRestants, nature unilatérale tardive, preuves dispo+récente, dates invalides)

## Tables / endpoints / composants impactés
- **Nouvelle table** `divorce_ddi_be_analyses` (id, case_file_id UNIQUE, date_separation DATE NOT NULL, nature_demande VARCHAR(40) NOT NULL, preuves_desunion_disponibles BOOLEAN NOT NULL, country VARCHAR(20) NOT NULL, result_data TEXT NOT NULL, timestamps)
- **Migration** `225-create-divorce-ddi-be-analyses.xml` (table — pas de seed visibility, voir hors périmètre)
- **Endpoint** `DivorceDdiBeController` (POST, GET)

## Hors périmètre
- Composant Angular (SF F-211 frontend ultérieure)
- Seed `decision_tool_visibility_rules` (différé pour CI verte)

## Impact par domaine métier
**Sensible Famille BE uniquement.** Concept différent du divorce-faute FR (F-FA-11). En BE, le divorce-faute a été aboli en 2007 — seul subsiste DDI 3 voies. Aucun impact Travail / Immigration.

## Parité des domaines métier
Niveau 5 (analyse validité). Aucune parité Travail/Immigration nécessaire. FR a déjà DDI dans F-FA-11 (Divorce pour faute) — concept BE distinct, donc pas de duplication.

## Analyse de cohérence transversale
- **F-FA-11** (Divorce pour faute FR) : concept FR distinct, pas de divorce-faute en BE post-2007. Outils en parallèle, situations différentes.
- **SF-211-01** (DC-BE) : voies parallèles du divorce en BE. Le panel F-IA-04 affichera les 2 outils si workspace BE Famille (modes contextuels distincts).
- **Pattern réutilisé** : copie pattern F-208.

## Audit "Impact F-166 cross-C×D"
- **BE×Famille** : ajout outil contextuel candidat (trigger `divorce_ddi_envisage=true` selon flag F-202) — seed différé.
- **FR×Famille** : non concerné (concept BE-only post-2007).
- Autres : non concernés.

## Audit "exhaustivité droit national BE"
- Source juridique : CC belge art. 229 §1 (constatation), §2 (commune ≥ 6 mois), §3 (unilatérale ≥ 1 an). Loi 27/04/2007.
- 3 voies + procédure unique tribunal de la famille.
- FR équivalent : divorce pour altération du lien conjugal (art. 237-238 CC FR — 1 an séparation), couvert par F-FA distinct.

## Contrat API
**POST** `/api/v1/case-files/{caseFileId}/divorce-ddi-3voies-be-analysis`
```json
{
  "dateSeparation": "2025-08-01",
  "natureDemande": "UNILATERALE_APRES_SEPARATION",
  "preuvesDesunionDisponibles": false
}
```
Réponse :
```json
{
  "caseFileId": "...",
  "country": "BELGIQUE",
  "dateSeparation": "2025-08-01",
  "natureDemande": "UNILATERALE_APRES_SEPARATION",
  "joursSeparation": 283,
  "preuvesDesunionDisponibles": false,
  "voie1Ouverte": false,
  "voie2Ouverte": false,
  "voie3Ouverte": false,
  "voieRecommandee": "AUCUNE",
  "joursRestantsVoie2": 0,
  "joursRestantsVoie3": 82,
  "formule": "Désunion irrémédiable — séparation depuis ...",
  "baseJuridique": "CC art. 229 §§1, 2, 3 ; Loi 27/04/2007",
  "messages": ["..."]
}
```
