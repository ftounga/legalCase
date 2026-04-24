# Mini-spec — F-DT-11 / SF-DT-11-01 Harcèlement moral/sexuel + indemnité licenciement nul — BACKEND

## Identifiant
`F-DT-11 / SF-DT-11-01`

## Feature parente
`F-DT-11` — Harcèlement moral / sexuel — outil décisionnel indemnités + licenciement nul (critique 🔴)

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-DT-11-01-harcelement-licenciement-nul-backend`

---

## Objectif

Fournir le backend d'un outil décisionnel calculant l'indemnité minimum due au salarié victime d'un licenciement nul pour harcèlement moral ou sexuel (art. L.1235-3-1 FR = 6 mois de salaire plancher) ou pour les cas équivalents en Belgique (Loi 4 août 1996 Welzijn + Loi 10 mai 2007 anti-discrimination = 6 mois de rémunération brute plancher).

---

## Comportement attendu

### Cas nominal

**Formule commune FR + BE :** `indemniteMinimumNullite = 6 × salaireMensuelReference`

**Motifs de nullité (FR — art. L.1235-3-1 al. 2 Code du travail) :**
| Code | Libellé |
|---|---|
| `HARCELEMENT_MORAL` | Faits de harcèlement moral (L.1152-3) |
| `HARCELEMENT_SEXUEL` | Faits de harcèlement sexuel (L.1153-4) |
| `DISCRIMINATION` | Licenciement discriminatoire (L.1134-4) |
| `GROSSESSE` | Salariée en état de grossesse (L.1225-71) |
| `SALARIE_PROTEGE` | Salarié protégé (L.2411-1 et s.) |
| `LIBERTE_FONDAMENTALE` | Violation d'une liberté fondamentale |
| `ACTION_JUSTICE` | Licenciement consécutif à une action en justice (L.1132-3-3) |
| `ALERTE_ETHIQUE` | Dénonciation d'alerte éthique (L.1132-3-3) |

**Motifs de nullité (BE — Loi 4 août 1996 + Loi 10 mai 2007) :**
| Code | Libellé |
|---|---|
| `HARCELEMENT_MORAL_BE` | Harcèlement moral au travail (Loi 4/8/1996 art. 32tredecies) |
| `HARCELEMENT_SEXUEL_BE` | Harcèlement sexuel au travail (Loi 4/8/1996 art. 32tredecies) |
| `VIOLENCE_AU_TRAVAIL_BE` | Violence au travail (Loi 4/8/1996 art. 32tredecies) |
| `DISCRIMINATION_BE` | Licenciement discriminatoire (Loi 10/5/2007, 6 mois de rémunération brute) |

Le pays est dérivé de `caseFile.getWorkspace().getCountry()`. Les motifs FR sont acceptés seulement si FR, et inversement.

### Cas d'erreur
| Situation | Comportement | Code HTTP |
|---|---|---|
| `salaireMensuelReference` nul ou négatif | 400 | 400 |
| `motifNullite` absent | 400 "Motif de nullité requis" | 400 |
| `motifNullite` non reconnu | 400 enum error | 400 |
| Motif FR utilisé sur workspace BE (ou inverse) | 400 "Motif non applicable au pays" | 400 |
| Dossier autre que DROIT_DU_TRAVAIL | 400 | 400 |
| Workspace différent | 404 | 404 |

---

## Contrat API (figé pour parallélisation backend/frontend)

### POST `/api/v1/case-files/{caseFileId}/harcelement-licenciement-nul`

**Request body :**
```json
{
  "salaireMensuelReference": 3000.00,
  "motifNullite": "HARCELEMENT_MORAL"
}
```

**Response 200 :**
```json
{
  "caseFileId": "uuid",
  "salaireMensuelReference": 3000.00,
  "motifNullite": "HARCELEMENT_MORAL",
  "country": "FRANCE",
  "indemniteMinimumNullite": 18000.00,
  "formule": "6 mois × 3 000,00 € = 18 000,00 €",
  "baseJuridique": "Art. L.1235-3-1 al. 2 Code du travail (L.1152-3)",
  "messages": [
    "Plancher légal 6 mois — le juge peut allouer davantage selon le préjudice effectif.",
    "Cumul : ..."
  ]
}
```

### GET `/api/v1/case-files/{caseFileId}/harcelement-licenciement-nul`
Même forme de réponse (404 si pas de POST préalable).

---

## Analyse de cohérence transversale

- [x] **Autres outils travail** : pattern aligné F-DT-17 / F-DT-21 (calculator + entity + service + controller + migration).
- [x] **Pays** : FR + BE intégrés ensemble (règle "toute feature métier doit couvrir FR ET BE").
- [x] **Jumeaux** : F-DT-12 (Discrimination) et F-DT-16 (Licenciement nul) couvriront d'autres motifs mais se chevauchent en partie (DISCRIMINATION déjà listée ici). Ces features sont prévues dans le plan — pas de redondance car chacune couvre un angle différent : F-DT-11 est l'outil dédié harcèlement/violence, F-DT-12 l'outil dédié discrimination typologique (17 motifs + guide preuve), F-DT-16 l'outil dédié détection nullité + calcul. L'indemnité minimum 6 mois est la même mais les messages et la détection diffèrent.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| Backend calculator + endpoint | Oui | Cette SF (SF-11-01) |
| Frontend composant | Oui | SF-DT-11-02 (PARALLÈLE — branche séparée) |
| BE jumeau | Oui | Intégré — codes BE distincts |
| F-IA-04 visibility rules | Oui | Migration 111 : ALWAYS_ON × 2 (DROIT_DU_TRAVAIL/FRANCE + DROIT_DU_TRAVAIL/BELGIQUE) |

---

## Impact par domaine métier

DROIT_DU_TRAVAIL, FR + BE. Non applicable immigration/famille.

## Parité des domaines métier

Niveau 3 (calculateur). Règle parité niveau ≥5 non applicable.

---

## Critères d'acceptation

- [ ] **C1** : `HarcelementNulliteCalculator.compute(3000, "HARCELEMENT_MORAL", "FRANCE")` → indemnite 18000, base `L.1235-3-1`, message cumul.
- [ ] **C2** : compute(3000, "HARCELEMENT_MORAL_BE", "BELGIQUE") → indemnite 18000, base `Loi 4 août 1996 art. 32tredecies`.
- [ ] **C3** : motif FR sur BE (ou inverse) → `IllegalArgumentException`.
- [ ] **C4** : chacun des 12 motifs (8 FR + 4 BE) testé.
- [ ] **C5** : salaire ≤ 0 → `IllegalArgumentException`.
- [ ] **C6** : motifNullite null ou inconnu → `IllegalArgumentException`.
- [ ] **C7** : Migration 111 : `create table harcelement_nullite_analyses` + 2 règles visibility ALWAYS_ON.
- [ ] **C8** : POST /api/v1/case-files/{id}/harcelement-licenciement-nul sur workspace FRANCE, motif FR → 200 + persiste.
- [ ] **C9** : POST motif BE sur workspace FRANCE → 400.
- [ ] **C10** : POST workspace étranger → 404.
- [ ] **C11** : POST dossier immigration → 400.
- [ ] **C12** : GET après POST retourne le résultat persisté (idempotent).
- [ ] **C13** : POST upsert remplace l'analyse existante.

---

## Technique

### Endpoints
POST + GET `/api/v1/case-files/{caseFileId}/harcelement-licenciement-nul`

### Tables impactées
- `harcelement_nullite_analyses` (CREATE) : colonnes id, case_file_id UNIQUE, salaire_mensuel_reference numeric(12,2), motif_nullite varchar(50), country varchar(20), result_data text, timestamps.
- `decision_tool_visibility_rules` : 2 INSERT (FRANCE + BELGIQUE, ALWAYS_ON, tool_id `F-DT-11-harcelement-licenciement-nul`).

### Migration Liquibase
- `111-create-harcelement-nullite-analyses.xml`
- UUIDs visibility rules : `f1a04001-0000-0000-0000-ee0000000111` (FR) + `f1a04001-0000-0000-0000-ee0000000112` (BE).

### Composants créés (package `fr.ailegalcase.casefile`)
- `HarcelementNulliteCalculator.java` — logique pure, `compute(salaire, motif, country) → HarcelementNulliteResult`
- `HarcelementNulliteAnalysis.java` — entity 1:1
- `HarcelementNulliteRepository.java`
- `HarcelementNulliteRequest/Response/Result.java`
- `HarcelementNulliteService.java` — isolation workspace + gate DROIT_DU_TRAVAIL + dérivation `country` depuis `caseFile.getWorkspace().getCountry()`
- `HarcelementNulliteController.java`
- Migration `111-create-harcelement-nullite-analyses.xml`

### Tests
- `HarcelementNulliteCalculatorTest.java` — ~14 UT (12 motifs + erreurs + formula)
- `HarcelementNulliteControllerIT.java` — ~10 IT (nominal, upsert, validation, cross-country error, workspace isolation, domain gate)

---

## Périmètre

### Hors scope
- Frontend (SF-DT-11-02 en parallèle).
- Détection IA automatique du motif depuis les pièces (prompt engineering) — pourra être ajoutée en SF ultérieure.
- Calcul du préjudice effectif supérieur au plancher 6 mois — hors scope (le juge l'apprécie).
- Intégration dans la synthèse PDF — hors scope.

---

## Notes

- Pas de détection automatique dans cette SF : l'avocat saisit le motif de nullité qu'il a identifié après analyse du dossier.
- Le plancher 6 mois est un forfait MINIMUM, pas un maximum — le message le rappelle.
- Les motifs FR et BE sont distinctement préfixés pour éviter l'ambiguïté : HARCELEMENT_MORAL (FR) vs HARCELEMENT_MORAL_BE (BE).
