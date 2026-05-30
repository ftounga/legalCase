# Mini-spec — F-218 / SF-218-01 — Appel CPH devant la Cour d'appel — backend

## Identifiant

`F-218 / SF-218-01`

## Feature parente

`F-218a` — Procédure CPH avancée (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-01-appel-cph-cour-appel-backend`

---

## Objectif

Calculer le délai d'appel d'un jugement CPH devant la chambre sociale de la Cour d'appel (1 mois) et produire la checklist des formalités spécifiques à l'appel social (procédure orale, représentation obligatoire avocat / défenseur syndical, déclaration d'appel RPVA), car aucun outil existant ne couvre la voie d'appel post-jugement prud'homal.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/appel-cph-analysis`
- Body :
  - `dateNotificationJugement` (LocalDate, requis) — date de notification du jugement CPH
  - `partieAppelante` (enum `SALARIE` | `EMPLOYEUR`, requis)
  - `modeNotification` (enum `SIGNIFICATION` | `LRAR`, défaut `SIGNIFICATION`)
  - `representationConstituee` (enum `AVOCAT` | `DEFENSEUR_SYNDICAL` | `AUCUNE`, défaut `AUCUNE`)
  - `jugementEnDernierRessort` (boolean, défaut false) — si le taux de compétence en dernier ressort est atteint (pas d'appel possible, pourvoi direct)
- Analyzer `AppelCphAnalyzer` :
  - **Calcul délai** : délai d'appel = 1 mois à compter de la notification (art. 538 CPC ; R. 1461-1 CPC), augmenté des délais de distance si applicable (hors V1). Calcule `dateLimiteAppel` (notification + 1 mois) et `joursRestants`.
  - **Verdict recevabilité** : si `jugementEnDernierRessort` = true → `VOIE_FERMEE` (renvoi vers pourvoi cassation F-DT-87) ; sinon selon `joursRestants` : `DELAI_OUVERT` (> 7 j), `DELAI_URGENT` (1–7 j), `DELAI_EXPIRE` (< 0).
  - **Checklist formalités** : déclaration d'appel via RPVA, mention des chefs de jugement critiqués (art. 901 CPC), représentation obligatoire (avocat ou défenseur syndical — R. 1461-2), procédure orale (art. 946 CPC), constitution intimé. Chaque item = `{ libelle, obligatoire, baseJuridique }`.
  - **Alerte représentation** : si `representationConstituee` = `AUCUNE` → item bloquant « représentation obligatoire en appel social ».
  - `baseJuridique` : R. 1461-1 et s. CPC ; art. 538 CPC ; art. 901 CPC.
- Output persisté dans `appel_cph_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/appel-cph-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| dateNotificationJugement absente | 400 |
| dateNotificationJugement future | 400 |
| partieAppelante inconnue | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **R. 1461-1 et s. CPC** — appel des décisions prud'homales, procédure avec représentation obligatoire.
- **Art. 538 CPC** — délai d'appel de droit commun : 1 mois.
- **Art. 901 CPC** — déclaration d'appel et chefs de jugement critiqués.
- **Art. 946 CPC** — procédure orale en appel social.
- **R. 1461-2 CPC** — représentation obligatoire : avocat ou défenseur syndical.
- **R. 1462-1 CPC** — taux de compétence en dernier ressort (jugement non susceptible d'appel → pourvoi).

**Relation F-DT-87** : si `jugementEnDernierRessort` = true, l'appel est fermé et l'outil renvoie vers le pourvoi en cassation (F-DT-87). Outils complémentaires, non redondants.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `dateNotificationJugement` | date | `dateNotificationJugement` (nouveau) | [x] record + [x] prompt `LegalDomainPromptBuilder` + [x] extracteur + [x] DTO frontend |
| `partieAppelante` | enum | dérivé de `roleClientDetecte` (proxy) | Réutiliser si présent, sinon défaut `SALARIE` |

**Flag CONTEXTUAL pivot** : `appel_cph_envisage` (niveau 3, FR-only, default false) — nouveau flag à ajouter à `TravailExtractedData`. L'outil bascule CONTEXTUAL quand l'IA détecte un jugement CPH rendu + intention de faire appel (mention « jugement », « notification », « interjeter appel », date de jugement présente).

---

## Critères d'acceptation

- [ ] POST nominal `partieAppelante=SALARIE`, notification J-10 → `dateLimiteAppel` = notification + 1 mois, `joursRestants` ≈ 20, verdict `DELAI_OUVERT`
- [ ] POST notification J-29 → verdict `DELAI_URGENT`
- [ ] POST notification J-40 → verdict `DELAI_EXPIRE`
- [ ] POST `jugementEnDernierRessort=true` → verdict `VOIE_FERMEE` + renvoi F-DT-87
- [ ] POST `representationConstituee=AUCUNE` → item checklist bloquant représentation
- [ ] POST dateNotificationJugement future → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert (remplacement)
- [ ] Isolation workspace (A ne lit pas l'analyse de B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`appel_cph_envisage`, trigger_value=`true`
- [ ] `F-DT-86-appel-cph-cour-appel` présent dans `KNOWN_FRONTEND_TOOL_IDS`

## Plan de test minimal

- **UT** `AppelCphAnalyzerTest` : ≥ 6 cas (délai ouvert / urgent / expiré, dernier ressort, représentation absente, calcul date limite mois)
- **IT** `AppelCphControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `appel_cph_analyses`
- **Migration Liquibase** + seed `decision_tool_visibility_rules` (`appel_cph_envisage`)
- **Endpoint** `AppelCphController` (POST + GET)
- **Service** `AppelCphService` + **Analyzer** `AppelCphAnalyzer`
- **Extension** `TravailExtractedData` : champ `dateNotificationJugement` + flag `appelCphEnvisage` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS` (pas de tuile dashboard)

## Hors périmètre

- Composant Angular (SF-218-02)
- Délais de distance (DOM-TOM / étranger) — V2
- Génération de la déclaration d'appel RPVA (générateur futur)
- Calcul du taux de compétence en dernier ressort (saisie manuelle `jugementEnDernierRessort`)
