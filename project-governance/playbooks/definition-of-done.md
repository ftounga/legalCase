# Definition of Done — AI LegalCase

## Principe

Une subfeature est **Done** quand tous les critères ci-dessous sont verts.
Aucun critère n'est optionnel.
Un seul critère rouge = la subfeature n'est pas Done.

---

## Critères — Code

- [ ] Le code implémente exactement ce qui est décrit dans la mini-spec, ni plus ni moins
- [ ] Aucune logique métier dans les controllers ou entités JPA
- [ ] Toute requête sur des données filtre par `workspace_id`
- [ ] Les cas d'erreur sont gérés (validation, 403, 404, 500)
- [ ] Aucune stacktrace exposée dans les réponses API
- [ ] Les DTOs de requête et de réponse sont distincts
- [ ] Les traitements IA sont asynchrones avec un enregistrement `analysis_jobs`

---

## Critères — Tests

- [ ] Tests unitaires présents sur la logique métier du service
- [ ] Tests d'intégration présents sur les endpoints exposés
- [ ] Les cas d'erreur sont testés (données invalides, accès interdit, ressource absente)
- [ ] L'isolation workspace est testée (un workspace ne peut pas voir les données d'un autre)
- [ ] Tous les tests passent (aucun test ignoré ou commenté)
- [ ] Le plan de test de la mini-spec est couvert

---

## Critères — Base de données

- [ ] La migration Liquibase est présente si un changement de schéma est requis
- [ ] La migration suit le format de nommage `{NNN}-{description}.xml`
- [ ] Les FK et index sont en place sur les colonnes critiques
- [ ] La migration est réversible ou une stratégie de rollback est documentée

---

## Critères — Review

- [ ] La PR respecte le template `project-governance/templates/pr-template.md`
- [ ] La review a été faite selon `project-governance/playbooks/review-rules.md`
- [ ] Aucun critère bloquant de la review-checklist n'est ouvert
- [ ] La review est approuvée

---

## Critères — CI / Build

- [ ] Le build passe sans erreur
- [ ] Tous les tests de la CI sont verts
- [ ] Aucun warning de compilation non traité (sauf exception documentée)

---

## Critères — Documentation

- [ ] Si un endpoint est ajouté ou modifié : le contrat API est mis à jour
- [ ] Si une décision d'architecture est prise : un ADR est créé (`project-governance/templates/adr-template.md`)
- [ ] Si une question ouverte est tranchée : `docs/OPEN_QUESTIONS.md` est mis à jour
- [ ] Si une nouvelle table est ajoutée : `docs/ARCHITECTURE_CANONIQUE.md` est mis à jour

---

## Critères — Sécurité

- [ ] Aucune donnée sensible (token, mot de passe, clé API) dans le code ou les logs
- [ ] Les endpoints sont protégés par les bons rôles (`OWNER`, `ADMIN`, `LAWYER`, `MEMBER`)
- [ ] L'isolation workspace est vérifiée pour tout accès aux ressources

---

## Feature parente — Done

Une feature parente est Done quand :
- Toutes ses subfeatures sont Done
- Le comportement de bout en bout est validé manuellement ou via un test E2E
- Le résultat est conforme à l'objectif fonctionnel décrit dans le `feature-template.md`
