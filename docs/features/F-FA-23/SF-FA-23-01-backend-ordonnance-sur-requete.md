# Mini-spec — F-FA-23 / SF-FA-23-01 Backend ordonnance sur requête (mesures urgentes familiales)

## Identifiant

`F-FA-23 / SF-FA-23-01`

## Feature parente

`F-FA-23` — Mesures urgentes familiales (ordonnance sur requête + IST + saisies conservatoires)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-23-01-backend-ordonnance-sur-requete`

---

## Objectif

Évaluer la probabilité que le juge délivre une **ordonnance sur requête** (procédure non-contradictoire art. 493 CPC) pour des mesures urgentes en droit de la famille (consultation dossier bancaire, IST avec enfant, saisies conservatoires, constat d'huissier, production forcée de pièces) à partir de critères structurants, et exposer le résultat via un endpoint REST persisté en base.

---

## Comportement attendu

### Cas nominal

L'avocat saisit le motif de la requête (DETOURNEMENT_PATRIMOINE / ENLEVEMENT_INTERNATIONAL / ORGANISATION_INSOLVABILITE / PREUVE_ABANDON / ACCES_PIECES), confirme les critères de recevabilité (urgenceJustifiee, derogationContradictoireJustifiee, pieceJustificativeFournie) et indique si des enfants sont concernés. Le calculateur retourne un **verdict de probabilité d'acceptation** (ELEVEE / MOYENNE / FAIBLE), un **délai typique** (24-72 h pour IST avec enfants, 5-15 jours pour les autres motifs), la base juridique applicable (art. 493 + 497 CPC + 373-2-6 Cciv si IST), une **formule** explicative et un message décrivant le **recours adverse** (référé-rétractation art. 497 CPC dans 15 jours). Le résultat est persisté 1:1 par `case_file_id`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Body absent / null | Message "Corps de requête requis" | 400 |
| Champ obligatoire absent (`motifRequete`, `urgenceJustifiee`, `derogationContradictoireJustifiee`, `pieceJustificativeFournie`) | Message d'erreur explicite par champ | 400 |
| Workspace ≠ DROIT_FAMILLE | Message "Ce dossier n'est pas un dossier de droit de la famille" | 400 |
| Utilisateur hors workspace du dossier | Case file not found | 404 |
| GET sans POST préalable | "Aucune analyse Ordonnance sur requête trouvée" | 404 |
| `caseFileId` inconnu | Case file not found | 404 |

---

## Analyse de cohérence transversale

### Périmètres scannés

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Autres outils famille FR (F-FA-12 mesures provisoires, F-FA-14 ordonnance protection, F-FA-13 révisions post-divorce, F-FA-25 majeurs protégés) | Oui — outils famille jumelés | Cette SF s'aligne sur leur pattern (record + entity + service + calculator + controller + 1 table dédiée). Pas de modification |
| Belgique (équivalent : requête unilatérale art. 1025 CJ devant juge des référés famille) | Oui — applicable FR + BE | Périmètre **inclus** dans la SF : la procédure existe à l'identique (procédure non-contradictoire urgente). 2 INSERT visibility (FRANCE + BELGIQUE) avec même tool_id. Le service ne gate **pas** sur le pays (info uniquement). Mention dans la base juridique selon `country` |
| Domaine DROIT_DU_TRAVAIL | Non | La requête unilatérale en travail (art. 145 CPC) est un mécanisme distinct — feature séparée si besoin, pas dans le périmètre F-FA |
| Domaine DROIT_IMMIGRATION | Non | Le contentieux administratif n'utilise pas la requête art. 493 CPC |
| Pré-remplissage IA | Non — SF backend pure | Sera implémenté dans la SF frontend correspondante (SF-FA-23-02) — `aiData` côté composant Angular |
| Cohérence F-IA-03 | Non — SF backend pure | Idem — couvert par la SF frontend |
| Refresh dashboard F-IA-02 | Non — SF backend pure | Idem |
| Persistance des inputs | Oui | Tous les inputs sont persistés en colonnes dédiées + `result_data` JSON pour survie au reload |

### Décision

- [x] Étendu à toutes les cibles applicables backend dans cette SF (FR + BE via 2 visibility rules, même endpoint, `country` traité comme info)
- [x] SF parallèle/suivante : SF-FA-23-02 frontend (composant Angular, pré-remplissage IA, F-IA-03)
- [x] Backlog : autres motifs/saisies conservatoires patrimoniales = SF-FA-23-03 (extension calculator)

---

## Impact par domaine métier

Cette feature est **sensible au domaine** : exclusivement DROIT_FAMILLE. Comportement :

- **Droit du travail** : non applicable (mécanisme distinct art. 145 CPC, hors périmètre F-FA)
- **Droit de la famille (FR)** : applicable — art. 493 + 497 CPC + 373-2-6 Cciv (IST)
- **Droit de la famille (BE)** : applicable — art. 1025 et s. CJ (requête unilatérale devant juge famille)
- **Immigration** : non applicable

Le service gate sur `legalDomain == DROIT_FAMILLE` (400 sinon). Le `country` est récupéré du workspace mais **ne gate pas** : la procédure est applicable FR + BE avec base juridique adaptée dans le `formule`.

---

## Parité des domaines métier (niveau ≥ 5)

L'outil livré est de **niveau 5 (scoring / analyse de validité)**.

| Domaine | Équivalent existant ? | Action |
|---------|----------------------|--------|
| DROIT_DU_TRAVAIL | Non — référé prud'homal R.1454-1 (F-DT-34) déjà couvert pour mesures urgentes travail | Pas d'équivalent direct nécessaire (la requête art. 145 CPC est un mécanisme transversal pas spécifique à un domaine — backlog si besoin) |
| DROIT_FAMILLE | **CETTE SF** | Livrée FR + BE simultanément |
| DROIT_IMMIGRATION | Non — le référé liberté/suspension administratif (F-IM-XX) couvre les mesures urgentes immigration | Pas d'équivalent — la requête est une procédure judiciaire non administrative |

Conclusion : pas d'asymétrie créée — la SF complète le maillage des mesures urgentes par domaine sans laisser de domaine à la traîne.

---

## Critères d'acceptation

- [x] **C1** : POST avec critères tous OK + motif IST + enfants → verdict `ELEVEE` + `delaiTypiqueJoursMin = 1` + `delaiTypiqueJoursMax = 3`
- [x] **C2** : POST avec motif autre que IST + critères OK → verdict `ELEVEE` + `delaiTypiqueJoursMin = 5` + `delaiTypiqueJoursMax = 15`
- [x] **C3** : POST sans `urgenceJustifiee` → verdict `MOYENNE` ou `FAIBLE` selon dérogation
- [x] **C4** : POST sans `derogationContradictoireJustifiee` → verdict `FAIBLE` + message recours référé contradictoire
- [x] **C5** : POST sans `pieceJustificativeFournie` → verdict dégradé (au max MOYENNE)
- [x] **C6** : Réponse contient `recoursAdverseDelaiJours = 15` (référé-rétractation art. 497 CPC)
- [x] **C7** : `baseJuridique` contient "493" et "497" CPC ; "373-2-6" Cciv si motif IST
- [x] **C8** : Workspace travail/immigration → 400
- [x] **C9** : Utilisateur d'un autre workspace → 404
- [x] **C10** : Upsert : 2e POST remplace l'analyse, pas de doublon (UNIQUE case_file_id)
- [x] **C11** : GET après POST renvoie l'analyse ; sans POST → 404
- [x] **C12** : Applicable FRANCE et BELGIQUE (visibility rules + service ne gate pas country)

---

## Périmètre

### Hors scope (explicite)

- Frontend Angular (SF-FA-23-02 suivante)
- Pré-remplissage IA, F-IA-03 (couvert frontend)
- Génération du document de requête à imprimer (potentielle SF future, pattern F-FA-09 acte huissier)
- Calculs de saisies conservatoires patrimoniales détaillées (montants) — backlog SF-FA-23-03

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `id` | UUID auto | `@GeneratedValue(strategy = UUID)` |
| `case_file_id` | issu du path | UNIQUE |
| `result_data` | "{}" | écrasé à chaque POST |
| `created_at` / `updated_at` | now() | `@PrePersist` / `@PreUpdate` |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées |
|-------|-------------|----------------------------|
| `motifRequete` | Oui | Enum {`DETOURNEMENT_PATRIMOINE`, `ENLEVEMENT_INTERNATIONAL`, `ORGANISATION_INSOLVABILITE`, `PREUVE_ABANDON`, `ACCES_PIECES`} |
| `urgenceJustifiee` | Oui | boolean |
| `derogationContradictoireJustifiee` | Oui | boolean |
| `pieceJustificativeFournie` | Oui | boolean |
| `presenceEnfants` | Non | boolean (par défaut false) |
| `commentaireUrgence` | Non | text libre, max 2000 caractères |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/ordonnance-requete-analysis` | Oui (OIDC) | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/ordonnance-requete-analysis` | Oui (OIDC) | MEMBER |

### Schema body POST

```json
{
  "motifRequete": "ENLEVEMENT_INTERNATIONAL",
  "urgenceJustifiee": true,
  "derogationContradictoireJustifiee": true,
  "pieceJustificativeFournie": true,
  "presenceEnfants": true,
  "commentaireUrgence": "Risque imminent de départ à l'étranger sans accord du parent gardien"
}
```

### Schema response

```json
{
  "caseFileId": "uuid",
  "motifRequete": "ENLEVEMENT_INTERNATIONAL",
  "scoreEligibilite": 100,
  "verdictAccordeProbabilite": "ELEVEE",
  "delaiTypiqueJoursMin": 1,
  "delaiTypiqueJoursMax": 3,
  "recoursAdverseDelaiJours": 15,
  "criteresRemplis": ["URGENCE_JUSTIFIEE", "DEROGATION_CONTRADICTOIRE_JUSTIFIEE", "PIECE_JUSTIFICATIVE_FOURNIE"],
  "criteresManquants": [],
  "baseJuridique": "Art. 493 + 497 CPC + 373-2-6 Cciv (mesures concernant l'enfant)",
  "formule": "Motif ENLEVEMENT_INTERNATIONAL + 3 critères remplis / 0 manquants → score 100 → verdict ELEVEE → délai 1-3 jours",
  "messages": ["...", "..."],
  "country": "FRANCE"
}
```

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `ordonnance_requete_analyses` | CREATE | Nouvelle table 1:1 par dossier (UNIQUE case_file_id) |
| `decision_tool_visibility_rules` | INSERT × 2 | F-IA-04 — `F-FA-23-ordonnance-requete` ALWAYS_ON DROIT_FAMILLE FRANCE + BELGIQUE priority 70 |

### Migration Liquibase

`168-create-ordonnance-requete-analyses.xml`

---

## Plan de test

### Tests unitaires `OrdonnanceRequeteCalculatorTest` (≥ 18)

1. Motif DETOURNEMENT + tous critères OK → ELEVEE
2. Motif ENLEVEMENT_INTERNATIONAL + enfants + tous critères OK → ELEVEE + délai 1-3 jours
3. Motif ORGANISATION_INSOLVABILITE + tous critères OK → ELEVEE + délai 5-15 jours
4. Motif PREUVE_ABANDON + tous critères OK → ELEVEE
5. Motif ACCES_PIECES + tous critères OK → ELEVEE
6. Critère urgenceJustifiee=false → verdict dégradé (MOYENNE)
7. Critère derogationContradictoireJustifiee=false → verdict FAIBLE + message renvoi référé contradictoire
8. Critère pieceJustificativeFournie=false → verdict dégradé
9. Tous critères false → verdict FAIBLE + score 0
10. 2 critères sur 3 manquants → FAIBLE
11. 1 critère sur 3 manquant → MOYENNE
12. 0 critère manquant → ELEVEE
13. Délai IST 1-3 jours quand motif=ENLEVEMENT_INTERNATIONAL OU presenceEnfants=true
14. Délai 5-15 jours pour motifs non-IST
15. recoursAdverseDelaiJours = 15 toujours
16. baseJuridique contient "373-2-6 Cciv" pour motif IST
17. baseJuridique ne contient PAS "373-2-6 Cciv" pour motif non-IST
18. NPE/IllegalArgument si motifRequete null
19. NPE/IllegalArgument si country null
20. country BELGIQUE → baseJuridique mentionne art. 1025 CJ

### Tests d'intégration `OrdonnanceRequeteControllerIT` (≥ 7)

1. POST tout OK → 200 + verdict ELEVEE
2. POST motif ENLEVEMENT_INTERNATIONAL + enfants → délai 1-3 jours
3. POST sans urgenceJustifiee → verdict dégradé
4. POST sans derogationContradictoire → verdict FAIBLE
5. POST workspace travail → 400
6. POST autre workspace (isolation) → 404
7. POST upsert → 1 seule ligne en base
8. GET après POST → 200 + données persistées
9. GET sans POST → 404
10. POST motifRequete absent → 400
11. POST workspace BELGIQUE → 200 + country BELGIQUE

### Isolation workspace

- [x] Test C9 ci-dessus : utilisateur du workspace A ne peut pas accéder au case-file du workspace B (404)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Outil décisionnel métier** — création d'un nouveau scoring de niveau 5

### Composants existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `decision_tool_visibility_rules` | INSERT × 2 (UUID dédiés) | `LegalReferentialDescriptionIntegrityIT` (vérifie cohérence DB) |
| `CaseFileRepository` | Lecture seulement (find) | Existants — pas de changement |
| `ordonnance_protection_analyses`, `mesures_provisoires_analyses` | Indépendants — table dédiée | Pas d'impact croisé |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (SF backend isolée — UI couverte par SF-FA-23-02 future)

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- **Décision visibility FR + BE** : la procédure de requête unilatérale existe à l'identique dans les 2 pays (CPC art. 493 FR / CJ art. 1025 BE) — un seul outil avec 2 visibility rules, sans gate country.
- **Décision délai IST** : déclenché soit par motif=ENLEVEMENT_INTERNATIONAL, soit par presenceEnfants=true (en pratique l'IST est demandée presque toujours quand des enfants sont concernés, même sur d'autres motifs).
- **Décision verdict** : scoring sur 3 critères = 0/1/2/3 critères remplis → mapping FAIBLE / FAIBLE / MOYENNE / ELEVEE. Si la dérogation au contradictoire est manquante → forcer FAIBLE (le juge renverra en référé contradictoire).
- **Recours adverse** : 15 jours pour le référé-rétractation art. 497 CPC (constant FR ; en BE c'est aussi un délai d'opposition court — on exposera 15 jours uniformément, mention dans le message).
