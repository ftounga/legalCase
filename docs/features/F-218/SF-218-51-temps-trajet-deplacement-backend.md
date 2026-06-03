# Mini-spec — F-218 / SF-218-51 — Temps de trajet / déplacements professionnels — backend

## Identifiant

`F-218 / SF-218-51`

## Feature parente

`F-218d` — Temps de travail / congés FR-only (P3 Travail FR — différé signal terrain, réactivé)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-51-temps-trajet-deplacement-backend`

---

## Objectif

Qualifier le **temps de trajet professionnel** et déterminer si une **contrepartie est due** (art. L.3121-4 CT ; CJUE C-266/14 « Tyco » ; jurisprudence Cass. soc.) : le temps de trajet entre le domicile et le lieu habituel de travail n'est pas du temps de travail effectif ; lorsqu'il dépasse le temps normal de trajet, il ouvre droit à une contrepartie (repos ou financière) ; pour un salarié itinérant sans lieu de travail fixe, le temps de déplacement entre le domicile et le premier/dernier client peut être qualifié de temps de travail effectif. **Analyseur de qualification**. Aucun outil existant ne couvre la qualification du temps de trajet (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/temps-trajet-deplacement-analysis`
- Body :
  - `typeTrajet` (enum, requis) ∈ { `DOMICILE_TRAVAIL_HABITUEL`, `DOMICILE_CLIENT_DEPASSEMENT`, `ITINERANT_SANS_LIEU_FIXE` }
  - `tempsTrajetQuotidienMinutes` (int, requis, ≥ 0) — temps de trajet quotidien constaté (minutes)
  - `tempsTrajetNormalMinutes` (int, requis, ≥ 0) — temps de trajet « normal » de référence (minutes)
  - `contrepartiePrevueAccord` (boolean, défaut false) — une contrepartie (repos/financière) est déjà prévue par accord/usage
- Analyzer `TempsTrajetDeplacementAnalyzer` :
  - **`ITINERANT_SANS_LIEU_FIXE`** → `qualification = TEMPS_TRAVAIL`, `contrepartieDue = false` (déjà rémunéré comme temps de travail) + note « salarié itinérant sans lieu de travail fixe : le déplacement domicile–premier/dernier client peut être qualifié de temps de travail effectif (CJUE C-266/14 ; Cass. soc.) ».
  - **`DOMICILE_TRAVAIL_HABITUEL`** :
    - le trajet n'est pas du temps de travail effectif (L.3121-4).
    - si `tempsTrajetQuotidienMinutes > tempsTrajetNormalMinutes` → `qualification = TRAJET_AVEC_CONTREPARTIE`, `contrepartieDue = true` (sauf si `contrepartiePrevueAccord = true`, alors `contrepartieDue = false` + note « contrepartie déjà prévue »).
    - sinon `qualification = TRAJET_SANS_CONTREPARTIE`, `contrepartieDue = false`.
  - **`DOMICILE_CLIENT_DEPASSEMENT`** : trajet domicile–client dépassant le temps normal → même logique de dépassement que ci-dessus (`TRAJET_AVEC_CONTREPARTIE` si dépassement, contrepartie due sauf prévue).
  - **Calcul du dépassement** : `depassementMinutes = max(0, tempsTrajetQuotidienMinutes − tempsTrajetNormalMinutes)`.
  - **Verdict** `qualification` ∈ { `TEMPS_TRAVAIL`, `TRAJET_AVEC_CONTREPARTIE`, `TRAJET_SANS_CONTREPARTIE` } ; `contrepartieDue` (boolean) ; `depassementMinutes` (int).
  - `baseJuridique` : art. L.3121-4 CT ; CJUE 10/09/2015 C-266/14 « Tyco » ; jurisprudence Cass. soc. — annoté `(à vérifier par avocat)`.
- Output persisté dans `temps_trajet_deplacement_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/temps-trajet-deplacement-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des champs requis absent (null) | 400 |
| `typeTrajet` valeur inconnue | 400 |
| `tempsTrajetQuotidienMinutes` < 0 ou `tempsTrajetNormalMinutes` < 0 | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.3121-4 CT** — le temps de déplacement professionnel pour se rendre sur le lieu d'exécution du contrat de travail n'est pas un temps de travail effectif ; toutefois, s'il dépasse le temps normal de trajet entre le domicile et le lieu habituel de travail, il fait l'objet d'une contrepartie (repos ou financière).
- **CJUE 10/09/2015, C-266/14 « Tyco »** — pour les travailleurs n'ayant pas de lieu de travail fixe ou habituel, le temps de déplacement quotidien entre le domicile et les sites du premier et du dernier client constitue du temps de travail au sens de la directive 2003/88/CE.
- **Jurisprudence Cass. soc.** — application aux salariés itinérants ; appréciation du dépassement du temps normal de trajet.

(à vérifier par avocat)

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `typeTrajet` | enum (String) | `typeTrajetProfessionnel` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `tempsTrajetQuotidienMinutes` | entier | `tempsTrajetQuotidienMinutes` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Consolidation IA critique** : les nouveaux champs IA de cet outil sont ajoutés au **sous-record consolidé `Sf218dDetail`** (un seul sous-record `@JsonUnwrapped` partagé par les 9 outils de la vague F-218d, dans `TravailExtractedData` du record `CaseAnalysisResponse.java`) — **PAS** un sous-record dédié, afin de ne pas dépasser la limite JVM de 255 paramètres du constructeur canonical. Clés JSON HTTP inchangées (plates).

**Flag CONTEXTUAL pivot** : `temps_trajet_detecte` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux de temps de trajet (mentions « temps de trajet », « temps de déplacement professionnel », « contrepartie au temps de trajet », « salarié itinérant », « déplacement domicile-client », « dépassement du temps normal de trajet »).

---

## Critères d'acceptation

- [ ] POST `typeTrajet=DOMICILE_TRAVAIL_HABITUEL`, quotidien=30, normal=30 → `qualification=TRAJET_SANS_CONTREPARTIE`, `contrepartieDue=false`, `depassementMinutes=0`
- [ ] POST `typeTrajet=DOMICILE_TRAVAIL_HABITUEL`, quotidien=90, normal=30 → `qualification=TRAJET_AVEC_CONTREPARTIE`, `contrepartieDue=true`, `depassementMinutes=60`
- [ ] POST même cas avec `contrepartiePrevueAccord=true` → `contrepartieDue=false` + note prévue
- [ ] POST `typeTrajet=ITINERANT_SANS_LIEU_FIXE` → `qualification=TEMPS_TRAVAIL`, `contrepartieDue=false`
- [ ] POST `typeTrajet=DOMICILE_CLIENT_DEPASSEMENT`, quotidien=120, normal=40 → `TRAJET_AVEC_CONTREPARTIE`, `depassementMinutes=80`
- [ ] POST champ requis null → 400 ; minutes < 0 → 400 ; `typeTrajet` inconnu → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`temps_trajet_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL, priority 98
- [ ] `F-DT-81-temps-trajet-deplacement` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `TempsTrajetDeplacementAnalyzerTest` : ≥ 6 cas (trajet habituel sans dépassement → sans contrepartie, trajet habituel avec dépassement → contrepartie due, dépassement mais contrepartie déjà prévue → non due, itinérant → temps de travail, domicile-client dépassement → contrepartie due, calcul depassementMinutes)
- **IT** `TempsTrajetDeplacementControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `temps_trajet_deplacement_analyses`
- **Migrations** : `540-create-temps-trajet-deplacement-analyses.xml` (create) + `541-seed-temps-trajet-deplacement-visibility.xml` (seed visibility, priority 98)
- **Endpoint** `TempsTrajetDeplacementController` (POST + GET)
- **Service** `TempsTrajetDeplacementService` + **Analyzer** `TempsTrajetDeplacementAnalyzer`
- **Extension** `TravailExtractedData` : champs `typeTrajetProfessionnel` + `tempsTrajetQuotidienMinutes` ajoutés au sous-record consolidé `Sf218dDetail` + flag `tempsTrajetDetecte` + instruction `TRAVAIL_INSTRUCTION_PART43` dans `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-52)
- Frais de déplacement / indemnités kilométriques (régime de remboursement de frais, situation distincte)
- Temps d'astreinte / temps d'habillage et déshabillage (situations distinctes)
- Montant chiffré de la contrepartie financière (renvoi à l'accord, non recalculé)
- Forfait jours / cadres autonomes (situation distincte)
