# Mini-spec — F-129 / SF-129-02 Enrichissement barèmes top 30 CCN

## Identifiant
`F-129 / SF-129-02`

## Feature parente
`F-129` — Référentiel conventions collectives — couverture étendue

## Statut `draft`  · Date `2026-04-20`  · Branche `feat/SF-129-02-enrich-top30-baremes`

---

## Objectif

Remplacer les minimums légaux seedés par SF-129-01 par les **vraies tranches de congés supplémentaires et primes d'ancienneté** pour les 30 CCN françaises les plus fréquentes. Sur E28 (Propreté), permettra que le pré-remplissage anciennete-section affiche des congés et une prime d'ancienneté cohérentes avec la CCN.

---

## Comportement

### Données enrichies

Pour chaque CCN du top 30, renseigner :
- `congesLegauxJours` (généralement 25 France, 20 Belgique)
- `congesSupp` : tableau `[{min: ancienneté, jours: jours supplémentaires}]`
- `primes` : tableau `[{min: ancienneté, pct: pourcentage}]`

**Sources de référence** — pour chaque CCN, citer dans `source_ref` :
- N° IDCC + titre court
- Article ou texte de référence quand disponible (ex. "IDCC 3043 art. 6.3 — Congés supplémentaires")

**Valeurs issues des connaissances Claude + vérification croisée** :
- Métallurgie (3248) : déjà enrichie SF-129-01
- Commerce (2216) : déjà enrichie
- BTP (1596) : déjà enrichie
- HCR (1979) : déjà enrichie
- Syntec (1486) : déjà enrichie
- Propreté (3043) : **à enrichir** (+1 jour à 20 ans ; prime 2%/4ans, 4%/7ans, 6%/10ans, 8%/15ans, 10%/20ans)
- Transport routier (16) : à enrichir
- Animation (1518) : à enrichir
- Banques (2120), Assurances (1672), etc.

### Prompt IA — pas de changement

Les 49 CCN sont déjà dans le référentiel et le prompt demande déjà `IDCC_XXXX`. Aucune modification prompt nécessaire.

### Fallback préservé

`LegalReferentialService.getConventionBareme` lit DB d'abord. Les valeurs enrichies en DB remplacent les minimums légaux précédents. L'ancien `ConventionBaremeReferentiel.java` statique reste intact comme fallback.

### Cas d'erreur

- Aucun (migration de données pure, idempotente via `UPDATE ... WHERE entry_key = ...`)

---

## Critères d'acceptation

- [ ] Migration Liquibase 087 met à jour les `value_json` de 25 nouvelles CCN (30 top — 5 déjà enrichies)
- [ ] Chaque CCN enrichie a au moins 1 tranche `congesSupp` OU 1 tranche `primes` (sinon inutile de la toucher)
- [ ] `source_ref` mise à jour avec référence article officiel quand connue
- [ ] Sur E28 après enrichissement Propreté : ouvrir anciennete-section → pré-remplissage congés et prime reflète le barème Propreté
- [ ] Aucune régression sur les 5 CCN préexistantes
- [ ] Tests unitaires vérifient que `LegalReferentialService.getConventionBareme("IDCC_3043")` renvoie un barème non vide (≥ 1 prime tranche)

---

## Plan de test

### Unitaires backend
- `LegalReferentialServiceTest` — nouveau test : `getConventionBareme("IDCC_3043")` retourne un `ConventionBareme` avec au moins 1 `primes` tranche
- Test existant : `getConventionBareme("METALLURGIE")` inchangé (régression)

### Intégration manuelle staging
- Dossier E28 : relancer analyse depuis zéro → vérifier dans anciennete-section que la prime ancienneté affiche la bonne valeur pour l'ancienneté calculée

### Isolation workspace
- N/A (données system-wide)

---

## Tables / endpoints / composants impactés

### Backend
- `db/changelog/migrations/087-enrich-conventions-baremes.xml` — NOUVELLE migration (UPDATE statements)
- Tests unitaires

### Frontend
- Aucun changement — les barèmes sont consommés par le backend via `LegalReferentialService.getConventionBareme`

---

## Hors périmètre

- Conversion salaire net → brut : renvoyé à SF-130-01 (feature distincte)
- CCN en dehors du top 30 : restent à minimums légaux jusqu'à une SF-129-03 éventuelle
- UI admin pour éditer les barèmes : déjà disponible via l'endpoint PUT existant
- Barèmes belges (CP) : restent inchangés (déjà basiques)

---

## Analyse de cohérence transversale

| Cible | Applicable | Classement |
|---|---|---|
| Belgique | Non applicable pour cette SF (hors scope — couvert par SF-129-03 backlog) |
| Autres domaines | Non applicable (CCN spécifique droit du travail) |
| Autres consumers de `getConventionBareme` | Composants frontend lisant via `BaremeService.get(code)` — inchangés (contrat API identique) |

**Analyse d'impact cross-cutting** :
- [ ] Auth — non touché
- [ ] Workspace — non touché
- [x] **Plans / limites** — les calculs de congés et primes changent pour certains dossiers → pas d'impact quota mais potentiellement des alertes de cohérence (F-IA-03) qui étaient silencieuses deviennent actives. C'est le comportement attendu.
- [ ] Navigation — non touché

Aucun smoke E2E concerné.

---

## Nouveau pattern UI ou service partagé

- [x] Pas de nouveau pattern — pure modification de données référentielles existantes
