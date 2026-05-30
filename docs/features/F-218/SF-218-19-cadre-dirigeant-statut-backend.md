# Mini-spec — F-218 / SF-218-19 — Cadre dirigeant : qualification (3 critères cumulatifs) — backend

## Identifiant

`F-218 / SF-218-19`

## Feature parente

`F-218b` — Régimes catégoriels FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-19-cadre-dirigeant-statut-backend`

---

## Objectif

Analyser la qualification de **cadre dirigeant** (art. L.3111-2 CT) en vérifiant les **3 critères cumulatifs** dégagés par la jurisprudence (Cass. soc. : grande indépendance dans l'organisation de l'emploi du temps ; habilitation à prendre des décisions de façon largement autonome ; rémunération parmi les plus élevées de l'entreprise), afin de déterminer si le salarié est **exclu des règles de durée du travail** (durée maximale, heures supplémentaires, repos) — enjeu majeur des litiges en rappel d'heures supplémentaires. Aucun outil existant ne couvre cette qualification (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/cadre-dirigeant-statut-analysis`
- Body :
  - `independanceEmploiDuTemps` (boolean, requis) — critère 1 : grande indépendance dans l'organisation de son emploi du temps
  - `autonomieDecision` (boolean, requis) — critère 2 : habilité à prendre des décisions de façon largement autonome
  - `remunerationParmiPlusElevees` (boolean, requis) — critère 3 : perçoit l'une des rémunérations les plus élevées de l'entreprise
  - `participationDirectionEntreprise` (boolean, défaut false) — indice complémentaire (Cass. soc. 2012 : participation effective à la direction)
  - `niveauRemunerationConstate` (BigDecimal, optionnel) — rémunération mensuelle (élément de preuve, non décisif seul)
- Analyzer `CadreDirigeantStatutAnalyzer` :
  - **Évaluation des 3 critères cumulatifs** : chaque critère produit `{ critere, rempli (boolean), commentaire }`. Champ `criteresRemplis` (int 0..3).
  - **Indice complémentaire** : `participationDirectionEntreprise` renforce la qualification (jurisprudence post-2012 exige une participation effective à la direction), mais l'absence des 3 critères légaux suffit à écarter le statut.
  - **Verdict de qualification** :
    - 3 critères remplis → `qualification = CADRE_DIRIGEANT` (exclusion durée du travail confirmée).
    - 3 critères remplis MAIS `participationDirectionEntreprise=false` → `qualification = CADRE_DIRIGEANT_FRAGILE` + note « risque de requalification : la jurisprudence exige une participation effective à la direction ».
    - < 3 critères → `qualification = NON_CADRE_DIRIGEANT` (soumis aux règles de durée du travail → rappel d'heures supplémentaires possible).
  - **Conséquence** : `exclusionDureeTravail` ∈ { `EXCLU`, `NON_EXCLU` } + `risqueRappelHeuresSupp` ∈ { `ELEVE`, `MODERE`, `FAIBLE` } (ELEVE si NON_CADRE_DIRIGEANT, MODERE si FRAGILE, FAIBLE si CADRE_DIRIGEANT).
  - `baseJuridique` : art. L.3111-2 CT ; Cass. soc. (3 critères cumulatifs + participation à la direction de l'entreprise) — annoté `(à vérifier par avocat)`.
- Output persisté dans `cadre_dirigeant_statut_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/cadre-dirigeant-statut-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des 3 critères booléens requis absent (null) | 400 |
| niveauRemunerationConstate négatif | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.3111-2 CT** — définition du cadre dirigeant : trois critères cumulatifs (responsabilités impliquant une grande indépendance dans l'organisation de l'emploi du temps ; habilitation à prendre des décisions de façon largement autonome ; rémunération se situant dans les niveaux les plus élevés des systèmes de rémunération de l'entreprise).
- **Cass. soc.** — exigence d'une participation effective à la direction de l'entreprise (jurisprudence restrictive post-2012). Le cadre dirigeant est exclu des dispositions sur la durée du travail, le repos et les jours fériés (sauf congés payés).

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `niveauRemunerationConstate` | montant | `salaireMensuelBrut` (existant) | Réutiliser si présent |
| `participationDirectionEntreprise` | booléen | `cadreParticipationDirection` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `statut_cadre_dirigeant_detecte` (niveau 3, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL (LLM contextuel) quand l'IA détecte des signaux de cadre dirigeant ou un litige sur la qualification (mentions « cadre dirigeant », « forfait sans référence horaire », « comité de direction », « COMEX », « rappel d'heures supplémentaires » contre un cadre de haut niveau, « membre du directoire »).

---

## Critères d'acceptation

- [ ] POST 3 critères true + `participationDirectionEntreprise=true` → `qualification=CADRE_DIRIGEANT`, `exclusionDureeTravail=EXCLU`, `risqueRappelHeuresSupp=FAIBLE`
- [ ] POST 3 critères true + `participationDirectionEntreprise=false` → `qualification=CADRE_DIRIGEANT_FRAGILE`, `risqueRappelHeuresSupp=MODERE`
- [ ] POST 2 critères true → `qualification=NON_CADRE_DIRIGEANT`, `exclusionDureeTravail=NON_EXCLU`, `risqueRappelHeuresSupp=ELEVE`
- [ ] POST 0 critère → `NON_CADRE_DIRIGEANT`
- [ ] `criteresRemplis` compte correctement (0..3)
- [ ] POST un critère booléen requis null → 400
- [ ] POST niveauRemunerationConstate négatif → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`statut_cadre_dirigeant_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-107-cadre-dirigeant-statut` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `CadreDirigeantStatutAnalyzerTest` : ≥ 6 cas (3 critères + participation → CADRE_DIRIGEANT, 3 critères sans participation → FRAGILE, 2 critères → NON, 0 critère → NON, comptage criteresRemplis, mapping risqueRappelHeuresSupp)
- **IT** `CadreDirigeantStatutControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `cadre_dirigeant_statut_analyses`
- **Migrations** : `create-cadre-dirigeant-statut-analyses.xml` + `seed-cadre-dirigeant-statut-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `CadreDirigeantStatutController` (POST + GET)
- **Service** `CadreDirigeantStatutService` + **Analyzer** `CadreDirigeantStatutAnalyzer`
- **Extension** `TravailExtractedData` : champ `cadreParticipationDirection` + flag `statutCadreDirigeantDetecte` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-20)
- Chiffrage du rappel d'heures supplémentaires en cas de requalification (calculateur heures supp existant / autre situation)
- Régime du forfait-jours (F-DT-50, situation distincte)
- Cadre intégré / cadre autonome (autres catégories, hors L.3111-2)
