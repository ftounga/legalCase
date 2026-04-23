# Mini-spec — F-IM-10 / SF-IM-10-01 Passeport talent — 9 sous-catégories CESEDA

## Identifiant
`F-IM-10 / SF-IM-10-01`

## Feature parente
`F-IM-10` — Passeport talent — 10 sous-catégories

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IM-10-01-passeport-talent-sous-categories`

---

## Objectif

Remplacer le type unique `PASSEPORT_TALENT` (groupé dans `ImmigrationPieceReferentiel`) par 9 sous-catégories distinctes du CESEDA L.421-9 à L.421-22, chacune avec sa propre liste de pièces et description avocat. Étendre le prompt IA pour orienter la détection vers la sous-catégorie précise. Le type historique `PASSEPORT_TALENT` est **conservé** comme alias de "Passeport Talent générique / sous-catégorie non identifiée" pour rétrocompat.

---

## Comportement attendu

### Cas nominal
1. `ImmigrationPieceReferentiel` étendu avec 9 nouvelles entrées (7 types + 2 alias BE ignorés) : `TALENT_CHERCHEUR`, `TALENT_SALARIE_QUALIFIE`, `TALENT_ENTREPRENEUR`, `TALENT_INNOVANT`, `TALENT_INVESTISSEUR`, `TALENT_PROFESSION_ARTISTIQUE`, `TALENT_RENOMMEE_INTERNATIONALE`, `TALENT_SALARIE_EN_MISSION`, `TALENT_FAMILLE`. Pièces spécifiques par sous-catégorie.
2. Migration Liquibase 107 insère ces 9 entrées dans `legal_referentials` (`referential_type = 'IMMIGRATION_PIECES'`, `country = 'FRANCE'`, `is_system = true`) avec `description` avocat conforme SF-140-03.
3. `LegalDomainPromptBuilder` : le prompt immigration FR guide l'IA vers les 9 nouveaux codes quand le titre détecté est `CARTE_PLURIANNUELLE_PASSEPORT_TALENT`.
4. L'alias `PASSEPORT_TALENT` existant reste valide (rétrocompat V1).

### Cas d'erreur
| Situation | Comportement |
|---|---|
| Titre détecté = `TALENT_CHERCHEUR` mais pas dans référentiel | fallback existant `PASSEPORT_TALENT` générique |
| IA détecte sous-catégorie ambigüe | le prompt demande explicitement de choisir la plus précise ou `PASSEPORT_TALENT` par défaut |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|---|---|---|
| `ImmigrationPieceReferentiel` (Java fallback) | Oui | **Intégré** : 9 entrées ajoutées |
| `legal_referentials` DB (source de vérité) | Oui | **Intégré** : migration 107 (règle CLAUDE.md obligatoire) |
| `LegalDomainPromptBuilder` (prompt IA) | Oui | **Intégré** : guide détection vers les 9 sous-codes |
| `ImmigrationWorkRightReferentiel` (F-IM-07) | Oui — utilise `CARTE_PLURIANNUELLE_PASSEPORT_TALENT` comme code titre | Non applicable ici : F-IM-07 reste sur le code générique car droit au travail identique pour les 9 sous-catégories (OUI avec mention) |
| `decision_tool_visibility_rules` (F-IA-04) | Oui — F-IM-01 ALWAYS_ON transversal déclenche déjà sur tout titre | Non applicable ici — F-IM-01 affiche les pièces quel que soit le type, les 9 sous-catégories sont lues via même mécanisme |
| BE | Oui — la Belgique n'a pas d'équivalent "Passeport talent" précis | `PROFESSION_HAUTEMENT_QUALIFIEE` existe mais couverte par permis unique → **non étendu** dans cette SF (cf. F-IM-14 couverture BE étendue à venir) |
| Frontend | Les composants F-IM-01 (checklist) et F-IM-05 (arbre décisionnel) consomment le référentiel via API | **Non modifié** — affichage dynamique par `entry_key` déjà générique |

### Décision
- [x] Étendu : 9 sous-catégories FR uniquement
- [x] BE : backlog (F-IM-14)
- [x] Aucune incidence frontend visible (pieces et titres dynamiques)

### Nouveau pattern UI ou service partagé
Pas de nouveau pattern — simple extension de référentiel existant.

---

## Impact par domaine métier

**Sensible au domaine et au pays** :
- **Droit du travail** : N/A
- **Immigration FR** : impact direct (9 nouveaux codes)
- **Immigration BE** : non applicable en V1 — le passeport talent est un régime purement français (art. L.421-* CESEDA). BE a son permis unique / carte bleue européenne, traités séparément (F-IM-14)
- **Famille** : N/A

## Parité des domaines métier

Niveau 1 (extension checklist / référentiel). Règle de parité 3 domaines ne s'applique pas — référentiel spécifique à un domaine.

---

## Critères d'acceptation

- [ ] `ImmigrationPieceReferentiel` contient 9 nouvelles entrées `Map.entry(...)` pour les codes cités (avec pièces spécifiques par sous-régime)
- [ ] L'entrée `PASSEPORT_TALENT` historique est conservée (alias générique)
- [ ] Migration 107 `107-seed-passeport-talent-sous-categories.xml` avec 9 INSERT `legal_referentials` (`is_system=true`, `country='FRANCE'`, `referential_type='IMMIGRATION_PIECES'`), chaque entrée a `description` remplie en langage avocat (SF-140-03)
- [ ] UUID plage `f1100001-0000-0000-0000-0000000003XX` libre — pas de collision
- [ ] Rollback Liquibase symétrique (DELETE des 9 entries par entry_key)
- [ ] `LegalDomainPromptBuilder` prompt immigration FR enrichi avec les 9 sous-codes + règle de sélection (le plus précis ou fallback sur `PASSEPORT_TALENT`)
- [ ] Tests unitaires : `ImmigrationPieceReferentielTest` (si existe) vérifie que les 9 codes ont une liste de pièces non vide en FRANCE
- [ ] Test intégration : `LegalReferentialDescriptionIntegrityIT` doit rester vert (9 nouvelles INSERT avec description)
- [ ] Suite Maven complète verte

---

## Périmètre

### Hors scope
- Frontend : aucune modification nécessaire (référentiel consommé dynamiquement)
- BE : pas d'extension BE ici (F-IM-14)
- Découpage dynamique frontend des sous-cartes cliquables par sous-catégorie → V2

### Déjà fait
- Référentiel Java + migration 101 (SF-IM-01-04) existent ; on étend.

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|---|---|---|
| `entry_key` | Oui | uppercase snake, unique par (referential_type, country) |
| `country` | Oui | `'FRANCE'` uniquement en V1 |
| `label` | Oui | libellé court avocat |
| `description` | Oui | description avocat (SF-140-03) |
| `source_ref` | Oui | article CESEDA précis |

---

## Technique

### Les 9 sous-catégories CESEDA

| Code | Article | Libellé |
|---|---|---|
| `TALENT_CHERCHEUR` | L.421-14 | Chercheur |
| `TALENT_SALARIE_QUALIFIE` | L.421-9 | Salarié qualifié (master/cadre ≥ 2× SMIC) |
| `TALENT_ENTREPRENEUR` | L.421-10 | Créateur d'entreprise |
| `TALENT_INNOVANT` | L.421-11 | Projet économique innovant |
| `TALENT_INVESTISSEUR` | L.421-12 | Investisseur |
| `TALENT_PROFESSION_ARTISTIQUE` | L.421-13 | Profession artistique et culturelle |
| `TALENT_RENOMMEE_INTERNATIONALE` | L.421-13 al. 3 | Personne à renommée nationale ou internationale |
| `TALENT_SALARIE_EN_MISSION` | L.421-15 | Salarié en mission intra-groupe |
| `TALENT_FAMILLE` | L.421-22 | Membre de famille du bénéficiaire Passeport Talent |

**Note sur "grands concours"** : `TALENT_RENOMMEE_INTERNATIONALE` couvre les grands concours (JO, Coupe du monde, prix Nobel, médaille Fields) — pas de code séparé.

### Fichiers modifiés

**Backend**
- `backend/src/main/java/fr/ailegalcase/casefile/ImmigrationPieceReferentiel.java` — 9 Map.entry ajoutées
- `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java` — prompt enrichi
- `backend/src/main/resources/db/changelog/migrations/107-seed-passeport-talent-sous-categories.xml` — nouveau

### Aucune route, aucun endpoint modifié.

---

## Plan de test

### Tests unitaires (backend)
- Si `ImmigrationPieceReferentielTest` existe : 9 nouveaux tests paramétrés — chaque code a ≥ 5 pièces FRANCE
- Si pas de test : ajouter un test de complétude

### Tests intégration
- `LegalReferentialDescriptionIntegrityIT` — doit rester vert (toutes les nouvelles lignes ont `description`)

### Tests E2E smoke
Non applicable.

### Isolation workspace
Non applicable — `workspace_id = NULL` pour les entrées système.

---

## Analyse d'impact

### Préoccupations transversales
- [ ] Auth / Principal : N/A
- [ ] Workspace context : N/A
- [ ] Plans / limites : N/A
- [ ] Navigation / routing : N/A
- [ ] Outil décisionnel : oui — étend F-IM-01 (checklist pièces). F-IM-01 ALWAYS_ON transversal migré par SF-IA-04-03 → pas de nouvelle règle `decision_tool_visibility_rules` nécessaire.

### Composants impactés
- `ImmigrationPieceReferentiel` (Java), `LegalDomainPromptBuilder`, migration 107.

### Smoke tests E2E
Non applicable.

---

## Dépendances
- SF-IM-01-04 done ✓
- SF-140-03 done (garde-fou description obligatoire) ✓

---

## Notes et décisions

### Pourquoi conserver `PASSEPORT_TALENT` générique alors qu'on le découpe ?
Rétrocompat : les dossiers historiques peuvent avoir `CARTE_PLURIANNUELLE_PASSEPORT_TALENT` détecté sans sous-catégorie. L'alias reste une option de fallback pour l'IA quand elle ne peut pas trancher.

### Pourquoi ne pas créer 9 codes `CARTE_PLURIANNUELLE_TALENT_*` en face dans `ImmigrationWorkRightReferentiel` (F-IM-07) ?
Le droit au travail est identique pour les 9 sous-catégories : OUI avec restrictions mineures. Dédoubler créerait du bruit sans plus-value. F-IM-07 continue à utiliser `CARTE_PLURIANNUELLE_PASSEPORT_TALENT` comme code parent.
