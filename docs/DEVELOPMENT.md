# DEVELOPMENT.md — Commandes de développement local

Référencé depuis `CLAUDE.md`.

## Démarrer le backend

### Profil `dev` (H2 en mémoire — pas besoin de Docker)
```bash
source .env.local
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
- Port : 8080 | Base : H2 en mémoire (données perdues à chaque redémarrage)
- Console H2 : http://localhost:8080/h2-console

### Profil `local` (PostgreSQL + MinIO via docker compose)
```bash
source .env.local
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
- Port : 8080 | Base : PostgreSQL (données persistantes)
- Requiert : `docker compose up -d`

## Démarrer le frontend
```bash
source ~/.nvm/nvm.sh && nvm use 22
cd frontend && npm start
```
- Port : 4200
- Node 22 requis (géré via nvm)

## Démarrer PostgreSQL (prod locale)
```bash
docker compose up -d
```
- Port : 5432
- DB : `legalcasedb` / User : `legalcase` / Password : `legalcase`

## Accès base de données H2 (dev uniquement)
- URL : http://localhost:8080/h2-console
- JDBC URL : `jdbc:h2:mem:legalcasedb`
- Utilisateur : `sa` / Mot de passe : (vide)

## Builder le backend sans tests
```bash
cd backend && ./mvnw clean package -DskipTests
```

## Builder le frontend
```bash
source ~/.nvm/nvm.sh && nvm use 22
cd frontend && npm run build
```
