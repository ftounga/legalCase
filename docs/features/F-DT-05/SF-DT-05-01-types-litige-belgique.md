# Mini-spec — F-DT-05 / SF-DT-05-01 Types de litige belges + prescription

---

## Identifiant

`F-DT-05 / SF-DT-05-01`

## Feature parente

`F-DT-05` — Droit du travail belge — types de litige, prescription et indemnités de préavis

## Statut

`draft`

## Date de création

2026-04-06

## Branche Git

`feat/SF-DT-05-01-litige-belgique`

---

## Objectif

Ajouter les types de litige en droit du travail belge dans le référentiel avec leurs délais de prescription, pour que l'IA puisse détecter et appliquer les prescriptions belges quand le workspace est configuré BELGIQUE.

---

## Comportement attendu

### Cas nominal

1. La migration 054 insère 7 types de litige belges dans `legal_referentials` avec country=BELGIQUE
2. Quand l'IA analyse un dossier d'un workspace BELGIQUE en DROIT_DU_TRAVAIL, elle détecte le type de litige parmi les types belges
3. Le système calcule la prescription belge (principalement 1 an post-contrat, sauf harcèlement/discrimination 5 ans)
4. L'affichage dans le bloc Délais légaux mentionne l'article de loi belge

### Types de litige belges à insérer

| Clé | Libellé | Prescription | Article |
|-----|---------|-------------|---------|
| LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE | Licenciement manifestement déraisonnable | 1 an post-contrat | CCT n° 109 du CNT |
| LICENCIEMENT_MOTIF_GRAVE | Licenciement pour motif grave | 1 an post-contrat | Art. 35, Loi du 3 juillet 1978 |
| RUPTURE_IRREGULIERE | Rupture irrégulière (absence de préavis) | 1 an post-contrat | Art. 15, Loi du 3 juillet 1978 |
| HARCELEMENT_MORAL | Harcèlement moral au travail | 5 ans | Loi du 4 août 1996, art. 32bis |
| HARCELEMENT_SEXUEL | Harcèlement sexuel au travail | 5 ans | Loi du 4 août 1996, art. 32ter |
| DISCRIMINATION | Discrimination | 5 ans | Loi du 10 mai 2007 |
| RAPPEL_SALAIRE | Rappel de salaire et heures supplémentaires | 1 an post-contrat | Art. 15, Loi du 3 juillet 1978 |

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Workspace FRANCE + dossier travail | Les types belges ne sont pas proposés (filtre country) |
| Type de litige non reconnu par l'IA | Fail-open — pas de prescription automatique |

---

## Critères d'acceptation

- [ ] Migration 054 insère 7 types de litige belges dans `legal_referentials` avec `country='BELGIQUE'`
- [ ] Chaque entrée a les bons articles de loi et délais de prescription
- [ ] Le `LegalReferentialService` filtre par country — un workspace BELGIQUE ne voit que les types belges
- [ ] L'IA utilise les types belges dans ses prompts quand country=BELGIQUE
- [ ] Les types belges sont visibles dans l'écran "Guides & barèmes" pour un workspace BELGIQUE
- [ ] Les données sont configurées pour FRANCE et BELGIQUE (critère standard)
- [ ] Tous les tests existants restent verts

---

## Périmètre

### Hors scope

- Calculateur d'indemnités belge (SF-DT-05-02)
- Ajout de nouveaux types de litige français
- Modification du comportement de l'écran Guides & barèmes

---

## Technique

### Migration Liquibase

- [x] Oui — `054-insert-belgian-litigation-types.xml`
- 7 INSERT dans `legal_referentials` avec legal_domain='DROIT_DU_TRAVAIL', referential_type='LITIGATION_TYPE', country='BELGIQUE'

### Fichiers impactés

| Fichier | Modification |
|---------|-------------|
| Migration 054 | 7 INSERT types de litige belges |
| `LegalDomainPromptBuilder` | Vérifier que le prompt inclut les types belges quand country=BELGIQUE (normalement déjà dynamique via le référentiel) |

---

## Plan de test

### Tests

- [ ] La migration s'exécute sans erreur
- [ ] Les 7 types belges sont présents en base après migration
- [ ] Le service filtre correctement par country
- [ ] Tous les tests existants restent verts

### Isolation workspace

- [ ] Non applicable — données système, pas liées à un workspace

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — ajout de données dans un référentiel existant
