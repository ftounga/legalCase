# Mini-spec — F-213 / SF-213-04-backend Outil préavis formule Claeys BE — contrats pré-2014

## Identifiant

`F-213 / SF-213-04-backend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-04-backend-licenciement-be-formule-claeys`

---

## Objectif

Calculateur du délai de préavis selon la **formule Claeys** (ancien art. 82 Loi 03/07/1978) pour les **contrats signés ou ancienneté accumulée avant le 01/01/2014**, combinable avec le statut unique post-2014 via la clause de sauvegarde (Loi 26/12/2013 art. 67). **BELGIQUE UNIQUEMENT — BE-only** : pas d'équivalent FR. Le « double préavis » issu de la combinaison Claeys + statut unique est encore fréquemment contesté en contentieux en 2026.

---

## Source juridique BE

- **Loi du 3 juillet 1978** art. 82 (version **avant** la loi du 26/12/2013) : formule de préavis employés à rémunération > seuil (anciennement 16 100 € env., indexé).
- **Loi du 26 décembre 2013** **art. 67** : **clause de sauvegarde des droits acquis** — le salarié conserve le préavis calculé selon les règles applicables au 31/12/2013 pour la partie d'ancienneté antérieure à 2014, plus le préavis statut unique pour la partie postérieure.
- **Formule Claeys** (Jurisprudence Claeys, Cour du travail) :
  - `preavisEnMois = 0.8 × [ancienneté (années) + 0.04 × rémunération annuelle brute (K€)]`
  - Cette formule est une **jurisprudence constante** appliquée aux employés > seuil rémunération (anciennement 32 254 €/an env.) sous l'ancien art. 82.
  - **À vérifier par avocat BE** : la formule exacte et le seuil précis de rémunération — annotés comme tels dans la réponse (`avertissement`).
- **Employés < seuil** (anciennement ≤ 16 100 € / à confirmer) : préavis légal tabulaire simplifié (non couvert par Claeys, hors scope V1 — avertissement affiché).

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-formule-claeys`

Inputs (body) :
- `ancienneteAnneesPreStatutUnique` (int) — ancienneté complète accumulée avant le 01/01/2014, obligatoire.
- `ancienneteMoisPreStatutUnique` (int 0-11) — mois supplémentaires (optionnel).
- `remunerationAnnuelleBruteEnMilliers` (BigDecimal) — rémunération brute en milliers d'euros (K€) au 31/12/2013.
- `appliquerClauseSauvegarde` (boolean, défaut true) — si true, calculer également le préavis statut unique pour la partie post-2014 et indiquer la somme totale indicative.
- `ancienneteAnneesPostStatutUnique` (int, requis si `appliquerClauseSauvegarde=true`) — ancienneté post-01/01/2014.
- `salaireHebdomadaireBrut` (BigDecimal, requis si `appliquerClauseSauvegarde=true`) — pour calcul indemnité post-2014.

Logique (`LicenciementBeFormuleClaeys Calculator`) :

**Partie Claeys (pré-2014) :**
1. `ancienneteTotaleAnnees = ancienneteAnneesPreStatutUnique + ancienneteMoisPreStatutUnique / 12`
2. `preavisClaeysMois = 0.8 × (ancienneteTotaleAnnees + 0.04 × remunerationAnnuelleBruteEnMilliers)`
3. Arrondi à l'entier supérieur (en semaines : `preavisClaeysMois × 4.333`).

**Partie statut unique (post-2014), si `appliquerClauseSauvegarde=true` :**
4. Résoudre `ancienneteAnneesPostStatutUnique` → `preavisStatutUniquesSemaines` (barème loi 26/12/2013 — même logique SF-213-03).
5. `preavisTotalSemaines = preavisClaeysMois × 4.333 + preavisStatutUniquesSemaines`.
6. `indemniteClaeys = salaireHebdomadaireBrut × (preavisClaeysMois × 4.333)`.
7. `indemniteTotale = salaireHebdomadaireBrut × preavisTotalSemaines`.

Output :
```json
{
  "preavisClaeysMois": 18.5,
  "preavisClaeysSemaines": 80,
  "preavisStatutUniquesSemaines": 27,
  "preavisTotalSemaines": 107,
  "indemniteClaeysBrute": 40000.00,
  "indemniteTotaleBrute": 53500.00,
  "formuleClaeys": "0.8 × (12 ans + 0.04 × 60 K€) = 18.5 mois",
  "baseJuridique": "Loi 03/07/1978 art. 82 (avant loi 26/12/2013) ; Loi 26/12/2013 art. 67",
  "avertissement": "Formule Claeys issue de la jurisprudence — à valider avec un avocat BE pour les seuils exacts applicables."
}
```

Persistance : `licenciement_be_formule_claeys_analyses` — unique sur `case_file_id`.

`GET` → dernière analyse ou 404.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation BE-only |
| `ancienneteAnneesPreStatutUnique` < 0 | 400 | Invalide |
| `remunerationAnnuelleBruteEnMilliers` ≤ 0 | 400 | Invalide |
| `appliquerClauseSauvegarde=true` sans `ancienneteAnneesPostStatutUnique` | 400 | Champ requis |

---

## Champs IA à extraire — BELGIQUE UNIQUEMENT

| Champ | Type | Champ `TravailExtractedData` BE | Notes |
|---|---|---|---|
| `ancienneteAnneesPreStatutUnique` | int | `ancienneteAnneesPreStatutUnique` — **BELGIQUE UNIQUEMENT** | Calculé depuis `dateDebutContrat` si < 2014 |
| `remunerationAnnuelleBruteEnMilliers` | BigDecimal | `salaireBrutAnnuelEnKEur` — **BELGIQUE UNIQUEMENT** | Converti depuis `salaireBrutAnnuel` existant |
| `appliquerClauseSauvegarde` | boolean | dérivé : `dateContrat < 2014-01-01` → true | |

`critereCode` : `BE_CLAEYS_ANCIENNETE_PRE`, `BE_CLAEYS_REMUNERATION`, `BE_CLAEYS_SAUVEGARDE`.

---

## Critères d'acceptation

- [ ] Formule `0.8 × (ancienneté + 0.04 × salaire)` appliquée correctement.
- [ ] Clause sauvegarde optionnelle — si activée, cumul Claeys + statut unique.
- [ ] `avertissement` toujours présent dans la réponse (formule jurisprudentielle).
- [ ] Workspace France → 404.
- [ ] `CritereCodeIntegrityIT` vert.

---

## Périmètre

### Hors scope

- Frontend — SF-213-04b.
- Employés sous seuil (barème tabulaire simplifié pré-2014) — avertissement affiché, calcul hors scope V1.
- Ouvriers pré-2014 (régime différent, hors scope).
- Formule Claeys pour **dépassement du plafond** (jurisprudence divergente) — avertissement affiché.

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-formule-claeys` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-formule-claeys` | OIDC | MEMBER |

### Tables

`licenciement_be_formule_claeys_analyses` — unique `case_file_id`, `result_data` JSON.

### Composants backend

- `LicenciementBeFormuleClaeys{Analysis,Repository,Request,Result,Response,Service,Calculator,Controller}.java`
- Réutilisation du barème statut unique de SF-213-03 via une constante partagée ou un service utilitaire.
- Extension `TravailExtractedData` + `LegalDomainPromptBuilder` BE.
- Migration `XXX-create-licenciement-be-formule-claeys-analyses.xml`.

---

## Plan de test

### Unitaires

- [ ] `0.8 × (12 + 0.04 × 60) = 18.48` → arrondi 19 mois.
- [ ] Clause sauvegarde : cumul Claeys 80 sem + statut unique 27 sem = 107 sem.
- [ ] Rémunération en K€ : vérification conversion (100 000 € → 100 K€).
- [ ] `avertissement` toujours non null dans la réponse.

### Intégration

- [ ] `POST` BE → 200, `POST` FR → 404.
- [ ] `GET` après POST → 200.

---

## Dépendances

- Barème statut unique (SF-213-03) — peut être extrait en constante partagée lors du dev.
