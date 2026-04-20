# Mini-spec — F-129 / SF-129-04 Ajout de 7 commissions paritaires belges étendues

## Identifiant
`F-129 / SF-129-04`

## Feature parente
`F-129` — Référentiel conventions collectives — couverture étendue

## Statut `draft`  · Date `2026-04-20`  · Branche `feat/SF-129-04-extended-belgian-commissions`

---

## Objectif

Compléter la couverture belge du référentiel `CONVENTION_BAREMES` en ajoutant 7 commissions paritaires (CP) belges à fort volume d'emploi au-delà des 3 déjà seedées (CP 200 Employés, CP 124 Construction, CP 302 Hôtellerie). Couvre les principaux secteurs d'activité concentrant le plus grand nombre de salariés belges, pour rendre l'outil F-DT-07 Ancienneté (Belgique) utile sur une majorité de dossiers.

---

## Comportement attendu

### Cas nominal

Après merge :
- La table `legal_referentials` contient 7 nouvelles entries `referential_type = 'CONVENTION_BAREMES'` / `country = 'BELGIQUE'` / `is_system = true`
- L'endpoint `GET /api/v1/referentials/conventions?country=BELGIQUE` retourne **10 CP belges** (3 existantes + 7 nouvelles) au lieu de 3
- `LegalReferentialService.getConventionBareme("CP111")` (ou toute autre nouvelle CP) retourne un `ConventionBareme` complet avec congés légaux 20j + congés supplémentaires éventuels + primes d'ancienneté éventuelles
- `ConventionCodeNormalizer.normalize("CP111")` retourne `"CP111"` (pattern `CP\d{2,4}` déjà géré, inchangé)
- Dropdown frontend convention collective (cas dossier BE) affiche les 10 CP

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Migration rejouée (idempotence) | Liquibase détecte `changeSet` déjà appliqué → no-op |
| Collision UUID ou clé unique `(workspace_id, referential_type, entry_key, is_system)` | Migration échoue → rollback Liquibase → signalement pipeline CI |
| `getConventionBareme` appelé sur une CP non listée | Retourne null (comportement existant inchangé) |

---

## Analyse de cohérence transversale

### Périmètres scannés

| Cible | Applicable ? | Traitement |
|---|---|---|
| Seeding référentiel FR (49 CCN) | Non touché — cette SF ne modifie que les entries Belgique | N/A |
| Normalizer `ConventionCodeNormalizer` | Pattern `CP\d{2,4}` déjà présent (SF-129-01), couvre les nouveaux codes tel quel | **Vérifié**, aucun changement |
| Fallback statique Java | Déjà supprimé par SF-129-03 (DB only) | N/A |
| Cohérence Admin UI (F-110 édition référentiels) | Les nouvelles entries apparaissent automatiquement dans la page admin "Guides & barèmes" car elles respectent le schéma `CONVENTION_BAREMES` existant | **Vérifié** |
| Cohérence ReferentialCheckService (cron de vérification 6 mois) | Le cron parcourt toutes les entries — les nouvelles y participeront automatiquement | N/A |
| Outil décisionnel métier | Ne crée pas ni ne modifie d'outil décisionnel — uniquement données de référence | N/A |
| Tests backend | `LegalReferentialServiceTest` couvre le lookup DB via mock — aucun test à ajouter sur les entries spécifiques. Le context load Spring ré-exécute la migration sur H2, donc toute corruption XML sera détectée. | Couverture suffisante |

### Décision

- [x] Intégré dans cette SF (ajout de 7 entries)
- [x] Non applicable aux autres domaines / outils (scope pur seeding référentiel)

---

## Liste des 7 CP retenues

Sélection basée sur le volume d'employés couverts en Belgique (sources publiques SPF Emploi + communiqués UCM/FGTB/CSC). Priorité aux secteurs à fort effectif et aux CP les plus citées dans les dossiers prud'homaux.

| Code | Label | Secteur | Justification |
|---|---|---|---|
| `CP100` | CP 100 — Commission paritaire auxiliaire pour ouvriers | Toutes industries ouvrières non spécifiquement couvertes | CP résiduelle très large, couvre des centaines de milliers d'ouvriers |
| `CP111` | CP 111 — Industries des métaux, des machines et matériel électrique (ouvriers) | Métallurgie, construction mécanique | Secteur industriel majeur |
| `CP118` | CP 118 — Industries alimentaires (ouvriers) | Agro-alimentaire ouvriers | Secteur à fort effectif |
| `CP209` | CP 209 — Employés des fabrications métalliques | Métallurgie, industrie mécanique (employés) | Pendant employés de CP 111 |
| `CP220` | CP 220 — Industrie alimentaire (employés) | Agro-alimentaire employés | Pendant employés de CP 118 |
| `CP311` | CP 311 — Grandes entreprises de vente au détail | Grande distribution | Secteur à très fort effectif |
| `CP330` | CP 330 — Secteur des soins de santé | Hôpitaux, maisons de repos, services à domicile | Plus grand employeur BE (~400k employés) |

**Valeurs** : congés légaux 20 jours (loi du 28/06/1971 — base commune). Congés supplémentaires d'ancienneté et primes d'ancienneté saisis **uniquement quand documentés par la CCT sectorielle** ; sinon valeurs vides (`congesSupp: []`, `primes: []`). La plupart des CP belges n'ont pas de prime d'ancienneté standardisée, contrairement à la France — rester conservateur et ne pas inventer de chiffres.

**Valeurs indicatives retenues** (basées sur les CCT sectorielles connues — à réviser si meilleure source identifiée) :
- CP 100 : 20j, pas de congés supp, pas de prime (CP auxiliaire générique)
- CP 111 : 20j, +1j après 10 ans, +2j après 20 ans (CCT métal)
- CP 118 : 20j, pas de congés supp documenté, pas de prime standardisée
- CP 209 : 20j, +1j après 5 ans, +2j après 15 ans (CCT employés métal)
- CP 220 : 20j, pas de congés supp documenté, pas de prime standardisée
- CP 311 : 20j, +1j après 10 ans (CCT commerce)
- CP 330 : 20j, +1j après 10 ans, +2j après 20 ans (CCT santé — variations secteurs)

Ces valeurs sont **indicatives** et l'admin (super-admin + OWNER/ADMIN workspace via F-110) peut override par workspace si nécessaire. `source_ref` indique explicitement le caractère indicatif.

---

## Critères d'acceptation

- [ ] Nouvelle migration Liquibase `089-seed-extended-belgian-commissions.xml`
- [ ] Les 7 nouvelles entries sont insérées avec `workspace_id = NULL`, `is_system = true`, `is_active = true`, `country = 'BELGIQUE'`, `legal_domain = 'DROIT_DU_TRAVAIL'`, `referential_type = 'CONVENTION_BAREMES'`
- [ ] Chaque `entry_key` est au format `CP{numero}` (exact, cohérent avec les 3 existantes CP200/CP124/CP302 et avec le pattern `ConventionCodeNormalizer`)
- [ ] `source_ref` mentionne explicitement "valeurs indicatives — vérification juridique recommandée" pour les CP dont les valeurs viennent de sources sectorielles non primaires
- [ ] `value_json` respecte le schéma existant : `{congesLegauxJours, congesSupp: [{min, jours}], primes: [{min, pct}]}`
- [ ] Migration idempotente (pattern Liquibase standard — vérification via `preConditions` si rejouable)
- [ ] `LegalcaseBackendApplicationTests.contextLoads` PASS (la migration est rejouée sur H2 en test)
- [ ] Tous les tests existants restent verts (981 → 981)
- [ ] `GET /api/v1/referentials/conventions?country=BELGIQUE` retourne désormais 10 entries (vérifiable manuellement sur staging)

---

## Périmètre

### Hors scope

- Ajout de CP belges supplémentaires au-delà des 7 listées (backlog si retour terrain)
- Enrichissement des CCT françaises
- Modification des 3 CP belges existantes (CP200, CP124, CP302) — inchangées
- Ajout de tests unitaires spécifiques par CP (déjà couvert par pattern `getConventionBareme` + context load)
- Frontend : aucun changement nécessaire (dropdown dynamique via endpoint existant)
- Documentation juridique détaillée des CCT sources (renvoi `source_ref` suffit)

---

## Contraintes de validation

| Champ | Règle |
|---|---|
| `entry_key` | Format `CP\d{2,4}`, unique parmi les entries `CONVENTION_BAREMES` système |
| `congesLegauxJours` | Toujours 20 (loi BE) |
| `value_json` | JSON valide, respect du schéma existant |
| UUID des entries | Format standard, unique — suivre la convention des entries 067 (`f1100001-0000-0000-0000-0000000000NN`) |

---

## Technique

### Endpoints touchés

- `GET /api/v1/referentials/conventions?country=BELGIQUE` (déjà existant, comportement enrichi automatiquement)
- `LegalReferentialService.getConventionBareme(code)` (déjà existant, lookup DB)

### Tables impactées

- `legal_referentials` : INSERT de 7 lignes

### Migration Liquibase

- [x] `089-seed-extended-belgian-commissions.xml`

### Composants Angular

- [ ] Aucun

---

## Plan de test

### Tests backend

- **Context load** : `LegalcaseBackendApplicationTests.contextLoads` doit passer (rejeu de la migration sur H2, détecte XML cassé ou contrainte violée)
- **Tests existants** : les 981 tests backend doivent rester verts (la migration ajoute des données, elle ne casse aucun comportement)
- **Test manuel staging post-merge** : `curl https://staging.../api/v1/referentials/conventions?country=BELGIQUE | jq '. | length'` doit renvoyer 10 (vs 3 avant)

### Isolation workspace

- N/A — entries système (`workspace_id = NULL`)

---

## Analyse d'impact

### Préoccupations transversales

- [ ] Auth / Principal : non
- [ ] Workspace context : non (entries système)
- [ ] Plans / limites : non
- [ ] Navigation / routing : non
- [ ] Outil décisionnel : non (ajout de données uniquement)

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `LegalReferentialService.getConventionBareme` | Nouveaux codes résolus par DB lookup | Tests existants verts (pattern identique aux CP200/124/302) |
| `ReferentialCheckService` (cron 6 mois) | Parcourt désormais 10 CP BE au lieu de 3 | N/A (comportement cron inchangé, volume +7) |
| Dropdown frontend convention collective | Affiche 7 options supplémentaires pour les dossiers BE | Test manuel staging |

### Smoke tests E2E

- Aucun — pas de flux utilisateur modifié côté code. Test manuel staging suffisant.

---

## Dépendances

### Subfeatures bloquantes

- SF-129-01 mergée ✅ (infrastructure DB-first)
- SF-129-03 mergée ✅ (DB only, plus de fallback statique)

### Questions ouvertes

- Aucune

---

## Notes et décisions

- **Pourquoi ces 7 CP et pas d'autres** : choix basé sur le volume d'emploi sectoriel. Alternatives envisagées et rejetées pour cette SF : CP 201 (Commerce de détail indépendant — plus petit que CP 311), CP 207 (Industrie chimique employés — moins d'effectif que CP 209), CP 306 (Assurances — niche), CP 322 (Intérim — statut spécifique). Peuvent être ajoutées en SF follow-up si retour terrain.
- **Pourquoi valeurs indicatives et pas valeurs vérifiées en cabinet** : les CCT belges sont fragmentées (accords sectoriels + accords d'entreprise), parfois à jour, parfois obsolètes. Plutôt que de figer des valeurs certifiées dans la migration, on fournit des valeurs indicatives documentées et on laisse l'admin workspace override si nécessaire via F-110. Approche "fail-open" cohérente avec la philosophie du référentiel.
- **Pourquoi pas de congés supplémentaires sur CP 100/118/220** : ces CP "auxiliaires" ou "alimentaire" n'ont pas de CCT centralisée standardisant les congés supplémentaires d'ancienneté — les accords d'entreprise locaux varient trop pour une valeur indicative fiable. Laisser vide est plus honnête.
- **Pourquoi pas de prime d'ancienneté** : contrairement à la France, la prime d'ancienneté n'est pas standardisée en Belgique. Les 3 CP existantes en ont une à titre historique (CP 200/124/302) mais ajouter des primes sur les 7 nouvelles CP sans source primaire serait de l'invention.
