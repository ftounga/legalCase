# Mini-spec — F-01 / SF-01-05 Redirect frontend post-login

---

## Identifiant

`F-01 / SF-01-05`

## Feature parente

`F-01` — Authentification OAuth2

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-01-05-frontend-redirect`

---

## Objectif

Après un login OAuth2 réussi, rediriger l'utilisateur vers le frontend Angular (port 4200 en dev, même origine en prod) plutôt que vers la page Spring Security par défaut.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur complète le flow OAuth2 (callback `/login/oauth2/code/{registrationId}`).
2. Spring Security appelle `CustomOidcUserService.loadUser()` — user persisté (SF-01-02).
3. Spring Security exécute le `AuthenticationSuccessHandler`.
4. Le handler redirige vers `${app.frontend-url}` (ex: `http://localhost:4200` en dev).
5. HTTP 302.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Échec OAuth2 (token invalide, accès refusé) | Spring Security redirige vers `/login?error` | 302 |

---

## Critères d'acceptation

- [ ] Après login OAuth2 réussi, le navigateur est redirigé vers `${app.frontend-url}`
- [ ] L'URL de redirect est configurable via propriété `app.frontend-url` (pas hardcodée)
- [ ] En dev : `app.frontend-url=http://localhost:4200`
- [ ] La valeur par défaut de `app.frontend-url` est `http://localhost:4200`

---

## Périmètre

### Hors scope (explicite)

- Gestion de l'URL de retour (deep link) — non requis en V1
- Page de login custom Angular — UI non concernée par cette subfeature
- Gestion des erreurs OAuth2 côté frontend — feature ultérieure

---

## Technique

### Implémentation

- Configurer `.oauth2Login().defaultSuccessUrl()` ou un `AuthenticationSuccessHandler` dans `SecurityConfig`
- Lire `app.frontend-url` depuis `application.yml`
- Injecter la valeur dans `SecurityConfig` via `@Value`

### Propriété de configuration

```yaml
app:
  frontend-url: http://localhost:4200
```

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires

Aucun — comportement couvert par les tests d'intégration.

### Tests d'intégration

- [ ] Après login simulé → réponse 302 avec `Location` contenant `app.frontend-url`

### Isolation workspace

- [x] Non applicable

---

## Dépendances

### Subfeatures bloquantes

- SF-01-01 — statut : done
- SF-01-02 — statut : done

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- `app.frontend-url` permet de changer la cible sans recompilation (dev vs prod).
- En prod, le frontend sera servi sur la même origine — la valeur sera mise à jour dans la config de déploiement.
- Le `defaultSuccessUrl` de Spring Security est suffisant pour V1 (pas de logique de redirect dynamique).
