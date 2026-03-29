# Captures d'écran marketing — AI LegalCase

## Pré-requis

1. Backend en cours d'exécution (local ou staging)
2. Frontend en cours d'exécution
3. Un compte de test valide (`E2E_LOCAL_EMAIL` / `E2E_LOCAL_PASSWORD`)
4. Idéalement : un dossier avec une **synthèse complète** existante

## Renseigner le dossier de démo

Pour les captures synthèse (07 et 08), définir l'ID du dossier à utiliser :

```bash
export DEMO_CASE_ID="votre-uuid-de-dossier"
```

Si absent, le script prend le premier dossier de la liste et tente de trouver une synthèse.

## Lancer les captures

```bash
# Depuis la racine du projet
source .env.local
source ~/.nvm/nvm.sh && nvm use 22

# Démarrer le backend (terminal 1)
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Démarrer le frontend (terminal 2)
cd frontend && npm start

# Lancer les captures (terminal 3)
cd e2e && npx playwright test --config=screenshots/playwright.screenshots.config.ts
```

## Captures produites

Les fichiers PNG sont générés dans `e2e/screenshots/output/` :

| Fichier | Écran | Acte vidéo |
|---------|-------|------------|
| `01-liste-dossiers.png` | Liste des dossiers (tableau) | Acte 2 |
| `02-tour-bienvenue.png` | Carte tour flottante — étape 0 | Acte 2 |
| `03-tour-nouveau-dossier.png` | Bouton "Nouveau dossier" surligné | Acte 3 |
| `04-detail-dossier.png` | Page dossier avec documents | Acte 3 |
| `05-upload-zone.png` | Zone upload — bouton "Ajouter des documents" | Acte 3 |
| `06-analyse-ia.png` | Section Analyse IA (barres progression) | Acte 4 |
| `07-synthese-haut.png` | Synthèse — haut de page (timeline + faits) | Acte 5 |
| `08-synthese-risques.png` | Synthèse — section risques | Acte 5 |

Résolution : **1920×1080 @2x (retina)** → fichiers PNG 3840×2160px, prêts pour print et vidéo.

## Sur staging

```bash
export E2E_BASE_URL=https://staging.legalcase.ng-itconsulting.com
export E2E_LOCAL_EMAIL=votre@email.com
export E2E_LOCAL_PASSWORD=votreMotDePasse
export DEMO_CASE_ID=uuid-du-dossier-demo

cd e2e && npx playwright test --config=screenshots/playwright.screenshots.config.ts
```
