# SF-235-02 — Migration F-IM-17 ALWAYS_ON → CONTEXTUAL

## Objectif

Convertir l'outil F-IM-17 (régime franco-algérien) de `ALWAYS_ON` vers `CONTEXTUAL` avec `trigger_field='nationalite'` `trigger_value='Algérienne'`, en s'appuyant sur le mécanisme livré par SF-235-01.

## Contexte

F-IM-17 est aujourd'hui ALWAYS_ON Immigration FR (migration 176, UUID `f1a04001-0000-0000-0000-ee0000000176`). La spec F-201 indiquait qu'il devrait être CONTEXTUAL conditionné à la nationalité algérienne, mais le mécanisme de matching CONTEXTUAL ne supportait que des booleans (cf. commentaire dans migration 213). SF-235-01 lève ce blocage en propageant `nationalite` (texte normalisé titlecase) dans la map `detected` du service.

## Comportement nominal

1. La migration 222 supprime l'entrée ALWAYS_ON existante de F-IM-17.
2. Elle insère une entrée CONTEXTUAL avec :
   - `legal_domain='DROIT_IMMIGRATION'`
   - `country='FRANCE'`
   - `tool_id='F-IM-17-regime-algerien'`
   - `layer='CONTEXTUAL'`
   - `trigger_field='nationalite'`
   - `trigger_value='Algérienne'`
   - `priority=77` (préservé de l'ALWAYS_ON original)
3. Sur un dossier Immigration FR avec analyse contenant `nationalite='Algérienne'` (extrait par SF-235-01) :
   - F-IM-17 apparaît dans la liste `contextual` du panel F-IA-04.
4. Sur un dossier Immigration FR sans nationalité algérienne (ou sans nationalité du tout) :
   - F-IM-17 est dans `catalog` (non visible par défaut, accessible via "Voir tous les outils").
5. Sur un dossier Immigration BE : F-IM-17 reste invisible (filtré par country='FRANCE' dans la requête `findForDomainAndCountry`).

## Cas d'erreur

| Cas | Comportement attendu |
|-----|----------------------|
| Migration appliquée 2 fois | Idempotente via UUID unique (la suppression DELETE WHERE id=... + INSERT avec nouveau UUID). |
| Rollback (Liquibase) | Restaure l'entrée ALWAYS_ON originale avec UUID `f1a04001-0000-0000-0000-ee0000000176`. |
| Analyse IA antérieure à SF-235-01 (sans champ `nationalite`) | F-IM-17 reste dans `catalog` (pas de trigger satisfait) — comportement gracieux, pas d'erreur. |
| Avocat surcharge type_litige_avocat_override | Aucun impact (override Travail FR uniquement, pas Immigration). |

## Critères d'acceptation

- [x] Migration Liquibase nommée `222-shift-fim17-regime-algerien-contextual.xml`.
- [x] L'entrée ALWAYS_ON UUID `f1a04001-0000-0000-0000-ee0000000176` est supprimée.
- [x] Une entrée CONTEXTUAL avec UUID `f1a04001-0000-0000-0000-eeee20100170` (parallèle au namespace F-201 `eeee20100`) est insérée avec `trigger_field='nationalite'` `trigger_value='Algérienne'` `priority=77`.
- [x] Le test d'intégration `DecisionToolVisibilityIntegrityIT` passe (F-IM-17 reste présent dans `TOOL_REGISTRY` frontend, donc pas d'orphelin).
- [x] Le test unitaire `DecisionToolVisibilityServiceTest` ajoute un cas Immigration FR avec `nationalite='Algérienne'` → F-IM-17 dans `contextual`.
- [x] Le test unitaire ajoute un cas Immigration FR sans nationalité algérienne → F-IM-17 dans `catalog`.
- [x] Rollback Liquibase opérationnel (restitue l'ALWAYS_ON original).
- [x] `./mvnw test -Dtest='DecisionToolVisibilityServiceTest,DecisionToolVisibilityIntegrityIT'` passe.

## Plan de test minimal

**Unitaires (`DecisionToolVisibilityServiceTest`)** — 2 nouveaux tests dans une section `F-235 — F-IM-17 CONTEXTUAL` :
- `immigrationFr_nationaliteAlgerienne_active_F_IM_17` : analyse JSON `immigration_extracted_data.nationalite='Algérienne'` → `r.contextual()` contient `'F-IM-17-regime-algerien'`.
- `immigrationFr_nationaliteAutre_F_IM_17_dans_catalog` : analyse JSON `immigration_extracted_data.nationalite='Tunisienne'` → `r.contextual()` ne contient PAS F-IM-17, `r.catalog()` le contient.

**Intégration (`DecisionToolVisibilityIntegrityIT`)** — déjà couvert par le test existant qui vérifie que tous les `tool_id` seedés en DB ont une entrée TOOL_REGISTRY frontend. F-IM-17 est déjà dans le registry — aucun ajout requis.

**Isolation workspace** : non affecté (la migration touche uniquement la table de visibilité, pas de données workspace-scoped).

## Tables / endpoints / composants impactés

- `backend/src/main/resources/db/changelog/migrations/222-shift-fim17-regime-algerien-contextual.xml` (nouveau fichier).
- Table `decision_tool_visibility_rules` (DELETE + INSERT 1 ligne).
- `backend/src/test/java/fr/ailegalcase/casefile/DecisionToolVisibilityServiceTest.java` (2 nouveaux tests + helper rules Immigration FR avec F-IM-17 CONTEXTUAL).

## Hors périmètre

- Étendre à d'autres nationalités (Tunisienne, Marocaine, Sénégalaise) — backlog F-220 (accords bilatéraux 1988 / 1983 / 2006).
- Modifier le composant frontend `regime-algerien-section` — pas nécessaire, son comportement de saisie manuelle de la nationalité reste inchangé.
- Modifier la TOOL_REGISTRY frontend — F-IM-17 y figure déjà.

## Analyse de cohérence transversale

| Cible | Statut | Justification |
|-------|--------|---------------|
| Travail FR / BE | Non applicable | F-IM-17 est Immigration FR exclusivement. |
| Famille FR / BE | Non applicable | F-IM-17 est Immigration FR exclusivement. |
| Immigration FR | Intégrée | Cible directe. |
| Immigration BE | Non applicable | Régime algérien = accord bilatéral FR-Algérie 1968. La Belgique a un cadre distinct (loi 15/12/1980). |
| UI panel F-IA-04 | Régression contrôlée | F-IM-17 n'apparaît plus par défaut sur les dossiers Immigration FR — comportement attendu par F-201. Toujours accessible via "Voir tous les outils" / catalog. |
| Pré-fill IA `nationalite` | Non impacté | Le composant `regime-algerien-section` continue d'utiliser sa logique interne (saisie manuelle + pré-fill via `aiData`). |

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern. Réutilise strictement le mécanisme `extractDetectedSituations` étendu par SF-235-01.

## Impact par domaine métier

- **Travail FR/BE** : aucun.
- **Immigration FR** : impact direct — F-IM-17 quitte l'écran par défaut, n'apparaît qu'avec un client de nationalité algérienne. Réduit le bruit visuel pour les ~95 % de dossiers Immigration FR non concernés.
- **Immigration BE** : aucun.
- **Famille FR/BE** : aucun.

## Parité des domaines métier

F-IM-17 est un outil de **niveau ≥ 5** (analyse de validité du régime spécial — scoring + arbre décisionnel par sous-régime).

| Domaine | Équivalent existant | Statut |
|---------|---------------------|--------|
| Famille FR | F-152 (scoring divorce CM) — sans lien | Non applicable, concept différent. |
| Famille BE | Non applicable | Pas de régime bilatéral équivalent. |
| Travail FR | F-DT-08 (validité licenciement), etc. | Non applicable. |
| Travail BE | Non applicable | Pas de régime bilatéral équivalent. |
| Immigration BE | Non applicable | La Belgique applique des accords bilatéraux différents (Maroc 1964, Tunisie 1969). Si pertinents → backlog F-220 BE. |

## Audit "Impact F-166 cross-C×D"

| Combinaison C×D | Impact | Détail |
|-----------------|--------|--------|
| FR × Travail | Aucun | F-IM-17 hors scope. |
| FR × Immigration | **Direct** | ALWAYS_ON → CONTEXTUAL. F-IM-17 quitte l'affichage par défaut. Apparaît si `nationalite='Algérienne'` extrait. |
| FR × Famille | Aucun | F-IM-17 hors scope. |
| BE × Travail | Aucun | F-IM-17 hors scope (country='FRANCE' filtre). |
| BE × Immigration | Aucun | F-IM-17 hors scope (country='FRANCE' filtre). |
| BE × Famille | Aucun | F-IM-17 hors scope. |

Effet d'accumulation : la cellule FR × Immigration passe de 4 ALWAYS_ON (post-F-201) à 4 ALWAYS_ON (inchangée — F-IM-17 était à part, marqué "reste ALWAYS_ON faute de flag dédié" dans migration 213). Ce n'est donc PAS un nouvel ajout dans la cellule, mais la finalisation de la conversion prévue par F-201.

## Audit "exhaustivité droit national FR" (et BE)

**Source juridique FR** : Accord franco-algérien du 27/12/1968 + 3 avenants (1985, 1994, 2001). Régime spécial dérogatoire au CESEDA pour les ressortissants algériens.

**Équivalent BE** : la Belgique n'a pas d'accord bilatéral spécifique avec l'Algérie sur le séjour. Pas d'entrée jumelle requise pour cette SF.

**Ouvertures futures (backlog F-220)** :
- Tunisie : accord bilatéral du 17/03/1988 (FR uniquement)
- Maroc : accord bilatéral du 09/10/1983 (FR uniquement)
- Sénégal : accord-cadre du 23/09/2006 (FR uniquement)

Ces 3 régimes pourraient suivre le même pattern que F-IM-17 (CONTEXTUAL sur `nationalite='Tunisienne' / 'Marocaine' / 'Sénégalaise'`) — ouvertures listées explicitement, non implémentées dans cette SF.

**Cohérence terminologique** : la valeur `'Algérienne'` est l'adjectif féminin titlecase, cohérent avec les conventions françaises de désignation de nationalité dans les actes officiels et dans le composant `regime-algerien-section` (`isAlgerienNationality()` côté front).
