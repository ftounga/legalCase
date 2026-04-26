# Mini-spec — F-IM-19 / SF-IM-19-02 Frontend MNA / enfant né en France / documents mineurs

## Identifiant

`F-IM-19 / SF-IM-19-02`

## Feature parente

`F-IM-19` — MNA / enfant né en France / documents mineurs étrangers (FR)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-19-02-frontend-mineurs-immigration`

---

## Objectif

Exposer côté frontend l'outil décisionnel "Mineurs étrangers — éligibilité"
livré par SF-IM-19-01 (backend mergé PR #642). Permettre à l'avocat de
choisir un dispositif (MNA / L.435-3 / DCEM / TIR), de saisir les inputs
clés (date de naissance, date d'entrée en France, parent régulier,
isolement, motif d'ordre public, nationalité), d'afficher le verdict
ELEVEE / MOYENNE / FAIBLE + documents requis + critères non remplis +
formule + base juridique. Pré-fill IA gracieux + alertes F-IA-03 sur les
champs sources.

Contrat importé de **SF-IM-19-01** (mergé PR #642).

---

## Comportement attendu

### Cas nominal

L'avocat ouvre le panneau outils décisionnels d'un dossier `DROIT_IMMIGRATION`
+ `FRANCE`. La section "MINEURS ÉTRANGERS (FR)" est visible (visibility rule
ALWAYS_ON insérée par migration 172).

1. Au mount : GET `/api/v1/case-files/{id}/mineurs-immigration-analysis`.
   - 200 → hydrate les inputs persistés + verdict.
   - 404 → mode formulaire + pré-fill IA.
2. L'avocat choisit un dispositif (radio 4 valeurs). Champs conditionnels :
   - **MNA** : `isolementAvere` (toggle) + note signalement préfecture.
   - **L.435-3** : `parentRegulier` (toggle) + `dateEntreeFrance` (date
     proche dateNaissance = né en France).
   - **DCEM** : `motifOrdrePublic` (toggle, bloquant si true).
   - **TIR** : note "apatride / asile — preuve OFPRA-CNDA requise" + champ
     `nationalite`.
3. L'avocat saisit `dateNaissance` (obligatoire), éventuellement
   `dateEntreeFrance`, `nationalite`, et les toggles selon dispositif.
4. Bandeau alerte si âge ≥ 18 ans : "Ces 4 dispositifs sont strictement
   réservés aux mineurs. Verdict forcé FAIBLE."
5. Soumission → POST. Affichage du résultat : bandeau verdict
   ELEVEE/MOYENNE/FAIBLE + chips documents + chips critères non remplis +
   délai + base juridique + formule + messages. Bouton "Modifier" pour
   ré-éditer.
6. Erreur backend → snackbar rouge.
7. Refresh dashboard sur succès (CaseDashboardRefreshService).

### Pré-fill IA

- `dateNaissance` ← `aiData.dateNaissance` si extrait par le pipeline
  immigration (champ optionnel non encore présent dans le model — cast
  défensif). Pré-fill gracieux : si absent, no-op.
- `dateEntreeFrance` ← `aiData.dateEntreeFrance` (idem cast défensif).
- `nationalite` ← `aiData.nationalite` (idem).
- Provenance signal `'IA' | null` par champ + badge "Pré-rempli depuis
  l'analyse" effaçable au changement.

### Validation F-IA-03

Champs audités :
- **DATE_NAISSANCE** : divergence si IA détecte une `dateNaissance` ≠
  saisie avocat (chemin générique `aiData.dateNaissance`).
- **DATE_ENTREE** : idem pour `dateEntreeFrance`.

Sources : IA + F96 (`IM19_DATE_NAISSANCE` / `IM19_DATE_ENTREE`) +
QUESTION_IA + PIECE_MANQUANTE. Builder partagé `CoherenceAlertBuilder`.

### Cas BE

Bannière info : "Régime mineurs étrangers propre à la France (CESEDA +
Cciv + CASF). L'équivalent belge (procédure tutelle MENA via DGDE)
relève d'une feature jumelle au backlog (F-IM-19-BE)." Pas d'appel HTTP.

---

## Critères d'acceptation

- [ ] Section "MINEURS ÉTRANGERS (FR)" visible dans le panel pour dossier
      DROIT_IMMIGRATION + FRANCE.
- [ ] Workspace BE → bannière info + pas d'appel HTTP.
- [ ] Radio dispositif avec 4 options et libellés humains.
- [ ] Champs conditionnels visibles selon dispositif sélectionné.
- [ ] Pré-fill IA gracieux pour `dateNaissance`, `dateEntreeFrance`,
      `nationalite` (no-op si absent).
- [ ] Provenance IA signalée par badge "Pré-rempli depuis l'analyse" +
      effacée au changement manuel.
- [ ] Bandeau "Mineur requis" si âge calculé ≥ 18 ans avant POST.
- [ ] POST → bandeau verdict ELEVEE/MOYENNE/FAIBLE + documents + délai +
      base juridique + formule + messages.
- [ ] Snackbar succès + `triggerRefresh()` au succès.
- [ ] Snackbar rouge sur erreur backend.
- [ ] CoherenceAlertBuilder utilisé (pas d'interface CoherenceAlert
      locale).
- [ ] Self-check grep 5/5 (palette, datepicker, gate FR, builder,
      refresh).
- [ ] Jest ≥ 12 tests verts.

---

## Périmètre

### Hors scope

- Belgique (F-IM-19-BE backlog).
- Génération de la requête JE / formulaire CERFA L.435-3.
- Suivi de l'instruction préfectorale (CaseDeadline → F-IM-16).
- Extension du modèle `ImmigrationExtractedData` (les champs
  `dateNaissance` / `dateEntreeFrance` / `nationalite` sont consommés via
  cast défensif `as any` — extension future via SF dédiée pipeline IA).

---

## Contrat API (importé de SF-IM-19-01)

- POST `/api/v1/case-files/{caseFileId}/mineurs-immigration-analysis`
- GET `/api/v1/case-files/{caseFileId}/mineurs-immigration-analysis`
- Enum `dispositifVise` : `MNA_ORDONNANCE_JE`, `TITRE_SEJOUR_L435_3`,
  `DCEM`, `TIR`
- Enum `verdictEligibilite` : `ELEVEE`, `MOYENNE`, `FAIBLE`
- Réponse : voir `MineursImmigrationResponse.java` (caseFileId, country,
  dispositifVise, dispositifRecommande, dateNaissance, dateEntreeFrance,
  parentRegulier, isolementAvere, motifOrdrePublic, nationalite,
  ageAnnees, verdictEligibilite, criteresNonRemplis, documentsRequis,
  delaiInstructionMois, baseJuridique, formule, messages).

---

## Composants impactés

| Fichier | Action |
|---------|--------|
| `frontend/src/app/core/models/mineurs-immigration.model.ts` | Création |
| `frontend/src/app/core/services/mineurs-immigration.service.ts` | Création |
| `frontend/src/app/case-files/mineurs-immigration-section/mineurs-immigration-section.component.ts` | Création |
| `frontend/src/app/case-files/mineurs-immigration-section/mineurs-immigration-section.component.html` | Création |
| `frontend/src/app/case-files/mineurs-immigration-section/mineurs-immigration-section.component.scss` | Création |
| `frontend/src/app/case-files/mineurs-immigration-section/mineurs-immigration-section.component.spec.ts` | Création |
| `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` | Ajout entrée `F-IM-19-mineurs` |

---

## Plan de test (Jest ≥ 12)

1. FRANCE → isFrance() true + GET émis au mount.
2. BELGIQUE → isFrance() false + aucun appel HTTP.
3. GET 200 → hydrate inputs + showForm=false.
4. GET 404 → mode formulaire.
5. Pré-fill IA `dateNaissance` + `dateEntreeFrance` + `nationalite` →
   provenance IA.
6. `onDateNaissanceChange` efface badge IA.
7. formValid false initial, true quand dispositif + dateNaissance
   présents.
8. Calcul âge ≥ 18 ans → flag `isMajeurAlert()` true.
9. calculate() POST + body + snackbar succès + refresh.
10. calculate() ignoré si form invalide.
11. calculate() erreur 400 → snackbar rouge.
12. coherenceAlerts.DATE_NAISSANCE présent si IA diverge.
13. coherenceAlerts vides après calcul (showForm=false).
14. bannerClass / bannerLabel mappent verdict correctement.
15. Champ conditionnel : isolementAvere visible uniquement si MNA.

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Pattern de référence** : `changement-statut-section` (PR #640).
- [x] **Builder partagé** : `CoherenceAlertBuilder` réutilisé (pas de
      définition locale ad hoc d'interface alert).
- [x] **Datepicker** : `<input type="date">` (convention canonique
      `licenciement-nul-detection-section`).
- [x] **Palette** : navy/or/rouge réservé verdict FAIBLE (DESIGN_SYSTEM.md).
- [x] **Refresh dashboard** : `CaseDashboardRefreshService.triggerRefresh()`.
- [x] **Pays** : Gate FR (bannière info BE), pas de masquage silencieux.

### Nouveau pattern UI ou service partagé

Aucun nouveau pattern global — réutilisation directe de
`changement-statut-section` (jumeau direct côté frontend).

---

## Impact par domaine métier

- Droit du travail : Non applicable (gate domaine `DROIT_IMMIGRATION`).
- Droit de l'immigration FR : Cœur de la feature.
- Droit de l'immigration BE : Bannière info (backlog F-IM-19-BE).
- Droit de la famille : Non applicable directement (le JE intervient
  pour MNA mais l'analyse porte sur l'éligibilité au dispositif
  immigration, pas sur la mesure d'AP).

Sensibilité au domaine : **OUI** (gate workspace.legalDomain côté backend).

---

## Parité des domaines métier

Niveau : **5 (scoring / analyse validité)** — verdict ELEVEE/MOYENNE/FAIBLE.
Couvert intégralement côté backend (SF-IM-19-01). Frontend = exposition
1:1.

---

## Self-check grep pré-commit (OBLIGATOIRE)

À exécuter avant push :

1. `grep -nE "MatDatepicker|matDatepicker" mineurs-immigration-section/*.{ts,html}` → vide
2. `grep -n "type=\"date\"" mineurs-immigration-section/*.html` → présent
3. `grep -n "CoherenceAlertBuilder" mineurs-immigration-section/*.ts` → présent
4. `grep -n "triggerRefresh" mineurs-immigration-section/*.ts` → présent
5. `grep -nE "interface .*CoherenceAlert\b" mineurs-immigration-section/*.ts` → vide

Verdict attendu : 5/5 PASS.

---

## Readiness checklist

| Item | Verdict | Note |
|------|---------|------|
| Mini-spec rédigée | PASS | Ce fichier |
| Critères d'acceptation listés | PASS | 13 items |
| Plan de test ≥ 12 | PASS | 15 prévus |
| Pattern de référence identifié | PASS | changement-statut-section |
| Contrat API figé (backend mergé) | PASS | SF-IM-19-01 PR #642 |
| Gate workspace.country | PASS | FR uniquement, bannière BE |
| Builder F-IA-03 partagé | PASS | CoherenceAlertBuilder |
| Refresh dashboard | PASS | triggerRefresh() au POST succès |
| Self-check grep 5/5 | À EXÉCUTER | avant push |

**Verdict global** : PASS — prêt pour dev.
