# Mini-spec — F-IM-20 / SF-IM-20-01 Backend mesures d'éloignement avancées (FR)

## Identifiant

`F-IM-20 / SF-IM-20-01`

## Feature parente

`F-IM-20` — Mesures d'éloignement avancées (Expulsion / IRTF / IAT — distinctes de l'OQTF F-IM-08)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-20-01-backend-mesures-eloignement-avancees`

---

## Objectif

Évaluer la légalité d'une mesure d'éloignement administrative française autre que l'OQTF — Expulsion (préfectorale L.631-1, ministérielle L.631-2, sécurité État L.631-3), IRTF (L.612-6+) ou IAT (L.222-1+) — en produisant un verdict (VALIDE / CONTESTABLE / NUL), les risques d'annulation, le délai de recours applicable et les pièces à produire.

---

## Comportement attendu

### Cas nominal

L'avocat saisit le dispositif visé, le motif de la menace, des indicateurs procéduraux (commission d'expulsion respectée, urgence absolue justifiée, durée de présence irrégulière pour IRTF, etc.) et la date du dépôt prévu de recours. Le calculateur applique le switch sur `dispositif` :

1. **EXPULSION_PREFECTORALE** (CESEDA L.631-1) — menace grave à l'ordre public.
   - Verdict VALIDE si commission d'expulsion respectée + motif `ORDRE_PUBLIC` ou `RECIDIVE_GRAVE`.
   - Verdict CONTESTABLE si commission non respectée sans urgence absolue.
   - Verdict NUL si motif uniquement `AUTRE` sans qualification de menace.
   - Délai recours : **1 mois TA** (compétence préfecture, contentieux administratif général).
2. **EXPULSION_MINISTERIELLE** (CESEDA L.631-2) — urgence absolue.
   - Verdict VALIDE si `urgenceAbsolueJustifiee = true` + motif terroriste ou ordre public grave.
   - Verdict CONTESTABLE si urgence absolue non justifiée (la procédure devait passer par commission).
   - Délai recours : **2 mois CE** (Conseil d'État compétence directe acte ministériel).
3. **EXPULSION_SECURITE_ETAT** (CESEDA L.631-3) — nécessité impérieuse pour la sûreté de l'État.
   - Verdict VALIDE si motif `SECURITE_ETAT` ou `TERRORISME` sans procédure commission requise.
   - Verdict CONTESTABLE si motif non qualifié (`AUTRE`).
   - Délai recours : **2 mois CE**.
4. **IRTF** (CESEDA L.612-6 et s.) — sanction associée à OQTF.
   - Verdict VALIDE si critères présence irrégulière > 12 mois OU comportement justifiant + durée ≤ 3 ans.
   - Verdict CONTESTABLE si motif faible (présence courte, pas de comportement aggravant).
   - Verdict NUL si motif `AUTRE` strict + circularité < 3 mois (pas de fondement).
   - Délai recours : **15 jours TA** (recours suspensif via OQTF).
5. **IAT** (CESEDA L.222-1+) — interdiction préventive d'entrée.
   - Verdict VALIDE si motif `TERRORISME` / `SECURITE_ETAT` / `ORDRE_PUBLIC` (menace réelle, actuelle, suffisamment grave).
   - Verdict CONTESTABLE si motif `RECIDIVE_GRAVE` ou `AUTRE`.
   - Délai recours : **2 mois CE**.

Pré-blocages transversaux (avant switch) :

- Si dispositif requérant procédure commission (EXPULSION_PREFECTORALE) et `procedureCommissionRespectee = false` ET `urgenceAbsolueJustifiee != true` → vice de procédure, verdict abaissé à CONTESTABLE et message "Procédure CESEDA L.632-1 (commission expulsion) non respectée".
- Si `recoursDelai` est antérieure à `LocalDate.now().minus(delaiRecoursJours, DAYS)` → message "Délai de recours expiré — risque d'irrecevabilité".

Sortie : `verdictLegalite`, `dispositifRecommande`, `risqueAnnulation` (List<String>), `delaiRecoursJours` (15 / 30 / 60), `juridictionRecours` (TA / CE), `documentsRequis`, `baseJuridique`, `formule`, `messages`.

Persistance 1:1 par `case_file_id` (upsert).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body absent | Message "Corps de requête requis" | 400 |
| `dispositif` null/vide | Message "dispositif est requis" | 400 |
| `dispositif` non supporté | Message "Dispositif non supporté" | 400 |
| `motifMenace` null/vide | Message "motifMenace est requis" | 400 |
| `motifMenace` non supporté | Message "motifMenace non supporté" | 400 |
| `dureeCircularitePrecaire` < 0 | Message "dureeCircularitePrecaire doit être ≥ 0" | 400 |
| `dureePresenceIrreguliereMois` < 0 | Message similaire | 400 |
| `recoursDelai` future > 1 an | Message "recoursDelai trop éloignée" | 400 |
| Workspace BELGIQUE | Message "Régime des mesures d'éloignement propre à la France" | 400 |
| Domaine ≠ DROIT_IMMIGRATION | Message "Ce dossier n'est pas un dossier de droit de l'immigration" | 400 |
| Case file d'un autre workspace | Message "Case file not found" | 404 |
| GET sans POST préalable | Message "Aucune analyse trouvée" | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier immigration** : F-IM-08 OQTF (familier — éloignement aussi, mais OQTF est la mesure de droit commun ; ici on couvre les 3 dispositifs spéciaux), F-IM-19 Mineurs (template récent), F-IM-13 Naturalisation (template procédure single-FR), F-IM-11 Changement statut (template switch enum). Pas de redondance avec OQTF.
- [x] **Pays** : France uniquement (CESEDA art. L.631+, L.612+, L.222+). La Belgique a un régime autonome (Loi 1980 art. 20-21, art. 74/15 et s.) — backlog F-IM-20-BE.
- [x] **Domaine** : DROIT_IMMIGRATION uniquement.
- [x] **UI patterns** : N/A — backend seul. Frontend SF-IM-20-02 dédiée au backlog.
- [x] **Flows transversaux** : Pas d'impact auth / workspace / plans / nav. Endpoint suit le pattern Naturalisation (POST + GET sur `/api/v1/case-files/{id}/mesures-eloignement-analysis`).

### Niveaux de vérification

- [x] **Record / DTO backend** : Request, Response, Result.
- [x] **Service / logique métier** : MesuresEloignementService gates + délégation au calculator.
- [x] **Entité JPA + schéma DB** : `mesures_eloignement_analyses` avec UNIQUE case_file_id + result_data JSON.
- [x] **Tests existants** : pattern Naturalisation/MineursImmigration.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : sera intégrée dans la SF frontend. Champs sources IA potentiels : motif, nationalité (via ImmigrationExtractedData), date de décision (via procedure_checks).
- [x] **Refresh dashboard (F-IA-02)** : SF frontend.
- [x] **Pré-remplissage IA** : SF frontend (ImmigrationExtractedData : `motif_menace_detecte`, `dispositif_eloignement_detecte`, etc. — extension prompt à anticiper en SF frontend).
- [x] **Persistance des inputs** : colonnes dédiées + result_data JSON.
- [x] **Masquage conditionnel** : visibility rule ALWAYS_ON pour DROIT_IMMIGRATION + FRANCE (priority 76).
- [x] **Alertes actives** : SF frontend.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-IM-08 OQTF | Voisin (éloignement) | Conserver distinct — OQTF = mesure de droit commun ; F-IM-20 = 3 dispositifs spéciaux (expulsion, IRTF, IAT) |
| F-IM-19 Mineurs | Pattern de référence | Réutilisé comme template (single-FR, switch enum, gates) |
| F-IM-13 Naturalisation | Pattern de référence | Idem (POST + GET, persistance result_data) |
| Belgique équivalent (Loi 1980 art. 20-21) | Non immédiat | Backlog F-IM-20-BE |
| Famille / Travail | Non applicable | Mesures d'éloignement = administrative pure, pas de croisement |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette SF (FR uniquement par scope CESEDA)
- [x] Subfeature(s) parallèle(s) à planifier : SF-IM-20-02 (frontend) au backlog
- [x] Backlog : F-IM-20-BE (équivalent Belgique — Loi 15/12/1980 art. 20-21, 74/15)
- [ ] Non applicable aux autres cibles

---

## Impact par domaine métier

- **Droit du travail** : Non applicable. Les 3 dispositifs sont de l'immigration administrative.
- **Droit de l'immigration FR** : Cœur de la feature. Enum dispositif fermé (5 valeurs : 3 sous-types EXPULSION + IRTF + IAT).
- **Droit de l'immigration BE** : Non couvert ici — backlog F-IM-20-BE.
- **Droit de la famille** : Non applicable.

Sensibilité au domaine : **OUI** — outil DROIT_IMMIGRATION exclusif (gate workspace.legalDomain).

---

## Parité des domaines métier

Niveau : **5 (scoring / analyse validité)** — verdict VALIDE / CONTESTABLE / NUL.

| Domaine | Outil équivalent ? | Note |
|---------|--------------------|------|
| Droit du travail FR | Non pertinent | Pas d'éloignement administratif en droit du travail (autre logique : licenciement, rupture, etc.) |
| Famille FR | Non pertinent | Pas d'éloignement administratif en famille |
| Immigration BE | À ouvrir au backlog | Loi 15/12/1980 art. 20-21 (ordre de quitter le territoire) + art. 74/15 (interdiction d'entrée) → F-IM-20-BE |
| Famille BE / Travail BE | Non pertinent | Idem FR |

---

## Critères d'acceptation

- [ ] POST `/api/v1/case-files/{id}/mesures-eloignement-analysis` calcule et persiste l'analyse avec verdict + risqueAnnulation + delaiRecoursJours + juridictionRecours + documentsRequis.
- [ ] Switch sur `dispositif` couvre les 5 valeurs (EXPULSION_PREFECTORALE / EXPULSION_MINISTERIELLE / EXPULSION_SECURITE_ETAT / IRTF / IAT).
- [ ] Délai de recours retourné conforme : 15 jours TA (IRTF), 1 mois (30j) TA (EXPULSION_PREFECTORALE), 2 mois (60j) CE (EXPULSION_MINISTERIELLE / EXPULSION_SECURITE_ETAT / IAT).
- [ ] Pré-blocage procédure commission : si EXPULSION_PREFECTORALE + `procedureCommissionRespectee = false` + `urgenceAbsolueJustifiee != true` → CONTESTABLE + message vice de procédure.
- [ ] EXPULSION_MINISTERIELLE sans `urgenceAbsolueJustifiee` → CONTESTABLE.
- [ ] EXPULSION_SECURITE_ETAT avec motif `AUTRE` → CONTESTABLE.
- [ ] IRTF avec présence irrégulière > 12 mois + comportement → VALIDE.
- [ ] IAT avec motif TERRORISME → VALIDE.
- [ ] IAT avec motif AUTRE → CONTESTABLE.
- [ ] Workspace BELGIQUE → 400.
- [ ] Workspace DROIT_DU_TRAVAIL → 400.
- [ ] Case file d'un autre workspace (cross-isolation) → 404.
- [ ] GET retourne l'analyse persistée.
- [ ] Migration 174 crée la table + UNIQUE case_file_id + visibility rule (priority 76, UUID `f1a04001-0000-0000-0000-ee0000000174`, tool_id `F-IM-20-mesures-eloignement`).
- [ ] Tests : ≥ 18 unitaires + ≥ 7 IT.

---

## Périmètre

### Hors scope

- Frontend (SF-IM-20-02 dédiée à planifier au backlog).
- Belgique (F-IM-20-BE backlog).
- Calcul automatique du délai écoulé depuis la décision (l'avocat saisit `recoursDelai` directement).
- Génération automatique du recours (mémoire CE, requête référé suspension).
- Référé liberté art. L.521-2 CJA (non couvert ici — possible extension future).
- Suivi de l'instruction (CaseDeadline traité en F-IM-16).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| country | "FRANCE" | Imposé par gate workspace |
| result_data | "{}" puis JSON sérialisé | rempli au compute |
| created_at / updated_at | now() | @PrePersist |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Notes |
|-------|-------------|------------------|-------|
| `dispositif` | Oui | enum : EXPULSION_PREFECTORALE / EXPULSION_MINISTERIELLE / EXPULSION_SECURITE_ETAT / IRTF / IAT | |
| `motifMenace` | Oui | enum : ORDRE_PUBLIC / SECURITE_ETAT / TERRORISME / RECIDIVE_GRAVE / AUTRE | |
| `procedureCommissionRespectee` | Non | Boolean (default true) | |
| `urgenceAbsolueJustifiee` | Non | Boolean (default false) | dérogation procédure |
| `dureeCircularitePrecaire` | Non | Integer mois (≥ 0) | pour IRTF |
| `dureePresenceIrreguliereMois` | Non | Integer mois (≥ 0) | pour IRTF |
| `recoursDelai` | Non | LocalDate | future ≤ 1 an |
| `comportementAggravant` | Non | Boolean (default false) | IRTF — comportement justifiant |

---

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/mesures-eloignement-analysis`

**Request body** :
```json
{
  "dispositif": "EXPULSION_PREFECTORALE",
  "motifMenace": "ORDRE_PUBLIC",
  "procedureCommissionRespectee": true,
  "urgenceAbsolueJustifiee": false,
  "dureeCircularitePrecaire": null,
  "dureePresenceIrreguliereMois": null,
  "comportementAggravant": false,
  "recoursDelai": "2026-05-15"
}
```

**Response 200** :
```json
{
  "caseFileId": "uuid",
  "country": "FRANCE",
  "dispositif": "EXPULSION_PREFECTORALE",
  "dispositifRecommande": "EXPULSION_PREFECTORALE",
  "motifMenace": "ORDRE_PUBLIC",
  "procedureCommissionRespectee": true,
  "urgenceAbsolueJustifiee": false,
  "comportementAggravant": false,
  "verdictLegalite": "VALIDE",
  "risqueAnnulation": [],
  "delaiRecoursJours": 30,
  "juridictionRecours": "TA",
  "documentsRequis": ["Décision préfectorale notifiée", "Avis commission expulsion (CESEDA L.632-1)", "..."],
  "baseJuridique": "CESEDA art. L.631-1 (expulsion préfectorale)",
  "formule": "Expulsion préfectorale — VALIDE — recours TA dans 30 jours.",
  "messages": ["Procédure commission expulsion respectée."]
}
```

**Codes d'erreur** : 400 (body invalide / dispositif inconnu / motif inconnu / workspace BE / domaine non immigration), 404 (case file inconnu / cross-workspace).

### GET `/api/v1/case-files/{caseFileId}/mesures-eloignement-analysis`

Retourne le dernier état persisté. 404 si jamais POSTé.

### Codes enum

- `dispositif` : `EXPULSION_PREFECTORALE`, `EXPULSION_MINISTERIELLE`, `EXPULSION_SECURITE_ETAT`, `IRTF`, `IAT`
- `motifMenace` : `ORDRE_PUBLIC`, `SECURITE_ETAT`, `TERRORISME`, `RECIDIVE_GRAVE`, `AUTRE`
- `verdictLegalite` : `VALIDE`, `CONTESTABLE`, `NUL`
- `juridictionRecours` : `TA`, `CE`

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/mesures-eloignement-analysis` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/mesures-eloignement-analysis` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `mesures_eloignement_analyses` | createTable + UNIQUE case_file_id | nouvelle |
| `decision_tool_visibility_rules` | INSERT | F-IM-20-mesures-eloignement, ALWAYS_ON, DROIT_IMMIGRATION, FRANCE, priority 76, UUID `f1a04001-0000-0000-0000-ee0000000174` |

### Migration Liquibase

- [x] Oui — `174-create-mesures-eloignement-analyses.xml` (173 réservé à un éventuel ajustement).

---

## Plan de test

### Tests unitaires (≥ 18 — `MesuresEloignementCalculatorTest`)

1. EXPULSION_PREFECTORALE — commission respectée + ORDRE_PUBLIC → VALIDE
2. EXPULSION_PREFECTORALE — commission non respectée + pas d'urgence absolue → CONTESTABLE (vice procédure)
3. EXPULSION_PREFECTORALE — commission non respectée + urgence absolue justifiée → VALIDE
4. EXPULSION_PREFECTORALE — motif AUTRE seul → NUL
5. EXPULSION_MINISTERIELLE — urgence absolue + TERRORISME → VALIDE
6. EXPULSION_MINISTERIELLE — sans urgence absolue → CONTESTABLE
7. EXPULSION_SECURITE_ETAT — motif SECURITE_ETAT → VALIDE
8. EXPULSION_SECURITE_ETAT — motif AUTRE → CONTESTABLE
9. IRTF — présence irrégulière 24 mois + comportement aggravant → VALIDE
10. IRTF — présence irrégulière 2 mois + circularité 1 mois → NUL
11. IRTF — présence courte sans comportement → CONTESTABLE
12. IAT — motif TERRORISME → VALIDE
13. IAT — motif AUTRE → CONTESTABLE
14. Délai de recours par dispositif (15j IRTF, 30j EXPULSION_PREFECTORALE, 60j EXPULSION_MINISTERIELLE / SECURITE_ETAT / IAT)
15. Juridiction recours (TA pour IRTF + EXPULSION_PREFECTORALE, CE pour les autres)
16. baseJuridique contient les bonnes références (L.631-1 / L.631-2 / L.631-3 / L.612-6 / L.222-1)
17. Dispositif non supporté → IllegalArgumentException
18. motifMenace non supporté → IllegalArgumentException
19. dureeCircularitePrecaire négatif → IllegalArgumentException
20. recoursDelai > 1 an dans le futur → IllegalArgumentException
21. recoursDelai expiré → message dans `messages`

### Tests d'intégration (≥ 7 — `MesuresEloignementControllerIT`)

1. POST FR EXPULSION_PREFECTORALE conforme → 200 verdict VALIDE
2. POST FR EXPULSION_MINISTERIELLE sans urgence → 200 verdict CONTESTABLE
3. POST FR IRTF nominal → 200 verdict VALIDE + delai 15j TA
4. POST FR IAT TERRORISME → 200 verdict VALIDE + juridiction CE
5. POST workspace BELGIQUE → 400
6. POST domaine DROIT_DU_TRAVAIL → 400
7. POST cross-workspace → 404
8. POST upsert (2 fois) → remplace
9. GET après POST → 200
10. GET sans POST → 404

### Isolation workspace

- [x] Applicable — un user FR ne peut accéder à un dossier d'un autre workspace → 404.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Outil décisionnel métier** — création nouvel outil

### Liste des outils décisionnels scannés

| Outil | Pattern | Décision |
|-------|---------|----------|
| F-IM-08 OQTF | sous-types FR + BE | déjà séparé, distinct (OQTF = droit commun, F-IM-20 = 3 dispositifs spéciaux) |
| F-IM-13 Naturalisation | single-FR + switch enum | template de référence |
| F-IM-19 Mineurs | single-FR + switch enum | template de référence |
| F-IM-11 Changement statut | single-FR + switch enum | template référence |

Verdict : F-IM-20 = **une situation = un outil**. 5 dispositifs distincts mais tous "mesure d'éloignement administrative spéciale FR" — switch interne au calculator. Conforme à l'invariant F-DT-08/F-DT-10.

### Smoke tests E2E

- [x] Aucun (pas de modif auth/workspace/nav)

---

## Dépendances

### Subfeatures bloquantes

- F-IA-04 SF-01/02 (visibility rules) : Done — pattern réutilisé
- Aucune autre.

### Questions ouvertes

- Aucune impactée.

---

## Notes et décisions

- **Délai de recours par dispositif** : EXPULSION_PREFECTORALE = 30j TA (acte préfectoral, contentieux administratif général art. R.421-1 CJA), EXPULSION_MINISTERIELLE / EXPULSION_SECURITE_ETAT = 60j CE (acte ministériel, compétence directe CE art. R.311-1 CJA), IRTF = 15j TA (recours suspensif via OQTF, art. L.614-4 CESEDA), IAT = 60j CE (acte ministériel).
- **Visibility rule UUID** : `f1a04001-0000-0000-0000-ee0000000174` (numérotation alignée sur la migration).
- **Tool ID** : `F-IM-20-mesures-eloignement`.
- **Numérotation migration** : 174 (172 = mineurs F-IM-19, 173 réservé pour ajustement intermédiaire éventuel).
- **Description visibility rule** : la migration utilise un INSERT direct dans `decision_tool_visibility_rules` (pas dans `legal_referentials`) — pas de description riche à fournir (la description est dans le commentaire XML Liquibase).
- **OQTF F-IM-08 vs F-IM-20** : OQTF = mesure de droit commun (irrégularité de séjour) ; F-IM-20 = mesures spéciales pour menace grave (sécurité, ordre public, terrorisme) ou sanction associée (IRTF) ou préventive (IAT). Pas de chevauchement fonctionnel.

---

## Readiness checklist

| Item | Verdict | Note |
|------|---------|------|
| Mini-spec rédigée | PASS | Ce fichier |
| Critères d'acceptation listés | PASS | 15 items |
| Plan de test ≥ 18 unit / ≥ 7 IT | PASS | 21 unit / 10 IT prévus |
| Pattern de référence identifié | PASS | F-IM-13 Naturalisation + F-IM-19 Mineurs |
| Migration numérotée libre | PASS | 174 disponible |
| UUID visibility rule libre | PASS | f1a04001-...-ee0000000174 |
| Référence PRODUCT_SPEC | PASS | F-IM-20 ligne 212 backlog V8 |
| Gates workspace + domaine | PASS | DROIT_IMMIGRATION + FRANCE |
| Isolation workspace | PASS | Test cross-workspace prévu |

**Verdict global** : PASS.
