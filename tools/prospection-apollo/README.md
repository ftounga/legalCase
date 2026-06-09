# Sourcing DRH via Apollo — setup « from scratch »

But : récupérer ~10 DRH (2 × 5 secteurs) pour les **entretiens discovery** LegalCase
Employeur. C'est du **sourcing discovery**, pas une campagne d'acquisition de masse
(le goulot diagnostiqué est la PMF, pas le volume de touches — cf. pivot DRH 08/06).
Objectif aval : 5 entretiens → 2 POC payants.

Cibles : DRH / DAS / Resp. RH, ETI **200-1500** salariés, France, 5 secteurs
(sécurité privée, propreté/facility, transport/logistique, restauration de chaîne,
médico-social/EHPAD). Messages d'approche : `../../docs/drh/messages-approche-drh.md`.

## Étape 1 — Compte Apollo (toi, via navigateur)
1. Crée un compte sur https://app.apollo.io (login Google possible).
2. Plan : le **free** suffit pour explorer/exporter à petit volume ;
   ⚠️ **l'accès API exige un plan payant** (Basic). Pour 10 contacts discovery,
   l'export manuel UI (étape 3a) évite tout coût.

## Étape 2 — (option) Extension Chrome
- Installe « Apollo.io » depuis le Chrome Web Store si tu veux enrichir des profils
  directement depuis LinkedIn / Sales Navigator. Facultatif pour ce test.

## Étape 3a — Voie SANS code (recommandée pour 10 contacts, gratuite)
Dans Apollo > **Search > People**, applique les filtres :
- **Job titles** : Directeur des Ressources Humaines, DRH, Responsable RH,
  Directeur des Affaires Sociales (+ variantes EN si besoin).
- **Employees** : 201-500, 501-1000, 1001-2000.
- **Location** : France.
- **Keywords (company)** : un secteur à la fois (ex. « sécurité privée », puis
  « propreté », etc.). Lance 5 recherches, une par secteur.
- Sélectionne 2 contacts par secteur → **Save to list** / **Export CSV**.

## Étape 3b — Voie AVEC code (clé API, répétable)
1. Apollo > Settings > Integrations > **API** → copie ta clé.
2. ```bash
   export APOLLO_API_KEY="xxxxxxxx"
   cd tools/prospection-apollo
   python3 apollo_drh_search.py            # 2 DRH par secteur -> drh-leads.csv
   python3 apollo_drh_search.py --sector securite --per-sector 4
   ```
3. L'email pro est **verrouillé** par défaut (l'API ne le révèle pas sans crédit).
   Pour le contact, privilégie **LinkedIn** (colonne `linkedin` du CSV) — cohérent
   avec la méthode du fichier messages (connexion + note courte).
   Ajoute `--reveal-emails` seulement si tu veux dépenser des crédits.

## Étape 4 — Brancher sur les messages
- Le CSV `drh-leads.csv` donne nom / titre / entreprise / secteur / LinkedIn.
- Pour chaque ligne, prends le message correspondant à la colonne `secteur` dans
  `docs/drh/messages-approche-drh.md`, personnalise `[Prénom]`, envoie via LinkedIn.
- Tracke chaque envoi/réponse (ne pas relancer 2× le même ; 1 relance à J+5).

## Gouvernance (à ne pas zapper)
Le plan marketing officiel **M-79** (`docs/MARKETING_BACKLOG.md`) retient
Lemlist + Sales Navigator + Hunter, et classe **Apollo en option de scaling non
retenue**. Tant qu'on reste sur du **sourcing discovery gratuit** (≤ 10 contacts),
pas de modif backlog nécessaire. **Avant** tout passage à un plan Apollo payant ou
une campagne récurrente : repasser le **contrôle de cohérence marketing en 4 points**
(CLAUDE.md, règle 2) et arbitrer Apollo vs la stack M-79.
