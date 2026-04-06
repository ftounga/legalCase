# Mini-spec — F-115 / SF-115-01 Persistance sessions HTTP via Spring Session JDBC

---

## Identifiant

`F-115 / SF-115-01`

## Feature parente

`F-115` — Persistance des sessions HTTP

## Statut

`draft`

## Date de création

2026-04-06

## Branche Git

`feat/SF-115-01-spring-session-jdbc`

---

## Objectif

Remplacer le stockage de sessions HTTP en mémoire par Spring Session JDBC (PostgreSQL) pour que les sessions survivent aux redémarrages de pods et aux rolling deployments.

---

## Comportement attendu

### Cas nominal

1. Les sessions HTTP sont stockées dans PostgreSQL (tables `spring_session` et `spring_session_attributes`)
2. Un redémarrage du pod backend ne déconnecte pas les utilisateurs
3. Le flow OAuth2 (Google/Microsoft) fonctionne même si le pod redémarre entre l'authorization request et le callback
4. Le login local fonctionne normalement avec les sessions JDBC
5. Le logout détruit la session en base
6. Le profil `dev` (H2) utilise aussi Spring Session JDBC (H2 compatible)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Base de données inaccessible | Session non créée — l'utilisateur doit se reconnecter |
| Session expirée | Redirect vers /login (comportement inchangé) |

---

## Critères d'acceptation

- [ ] Dépendance `spring-session-jdbc` ajoutée dans pom.xml
- [ ] `spring.session.store-type=jdbc` configuré dans application.yml
- [ ] Migration Liquibase 053 crée les tables `spring_session` et `spring_session_attributes`
- [ ] Les sessions sont stockées en base (vérifiable via H2 console en dev)
- [ ] Le flow OAuth2 fonctionne après redémarrage du backend
- [ ] Le login local fonctionne après redémarrage du backend
- [ ] Le logout détruit la session en base
- [ ] Tous les tests existants restent verts

---

## Périmètre

### Hors scope

- Redis (trop de complexité infra pour le moment)
- Scaling horizontal / sticky sessions (couvert nativement par JDBC sessions)
- Nettoyage automatique des sessions expirées (Spring Session le fait automatiquement)

---

## Technique

### Dépendance ajoutée

| Package | Version |
|---------|---------|
| `org.springframework.session:spring-session-jdbc` | Via spring-boot-starter |

### Fichiers impactés

| Fichier | Modification |
|---------|-------------|
| `pom.xml` | Ajout dépendance spring-session-jdbc |
| `application.yml` | `spring.session.store-type: jdbc`, `spring.session.jdbc.initialize-schema: never` (Liquibase gère) |
| Migration Liquibase 053 | Tables spring_session et spring_session_attributes |

### Migration Liquibase

- [x] Oui — `053-create-spring-session-tables.xml`
- Tables : `spring_session` (primary_id, session_id, creation_time, last_access_time, max_inactive_interval, expiry_time, principal_name) + `spring_session_attributes` (session_primary_id, attribute_name, attribute_bytes)
- Index : session_id (unique), expiry_time, principal_name

---

## Plan de test

### Tests unitaires / intégration

- [ ] Les tests IT existants (qui utilisent MockMvc + sessions) restent verts
- [ ] Vérification manuelle : login → redémarrage → session toujours valide

### Isolation workspace

- [ ] Non applicable — les sessions sont isolées par utilisateur nativement

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Auth / Principal** — les sessions stockent le SecurityContext (OidcUser ou UsernamePasswordAuthenticationToken)
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [ ] Aucune préoccupation transversale

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|-----------|-----------------|----------------------|
| SecurityConfig | Session management inchangé — Spring Session JDBC est transparent | Tests IT existants |
| LocalAuthService | HttpSessionSecurityContextRepository fonctionne identiquement avec JDBC sessions | Tests IT auth locale |
| OAuth2 flow | Authorization request repository bascule automatiquement en JDBC | Test manuel OAuth2 |
| Logout | invalidateHttpSession + deleteCookies fonctionne identiquement | Tests IT logout |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/auth.spec.ts` — login/logout à valider post-staging

---

## Dépendances

- Aucune subfeature bloquante
- Aucune question ouverte impactée

---

## Notes et décisions

- `spring.session.jdbc.initialize-schema: never` car on crée les tables via Liquibase (contrôle du schéma)
- Le schéma des tables Spring Session est standardisé — on utilise le DDL officiel de Spring Session
- Spring Session gère automatiquement le cleanup des sessions expirées (cron interne)
- Pas besoin de modifier SecurityConfig ni LocalAuthService — Spring Session JDBC est un drop-in replacement transparent
