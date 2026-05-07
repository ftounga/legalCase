# Radar stratégique — décision business model (lifestyle vs levée seed)

> **Statut** : registre vivant pour cadrer la décision business model. Décision **différée à T+6 mois minimum** par défaut. Pas une roadmap, pas urgent.

> **Périmètre** : ce fichier trace la décision **structurante** entre 2 trajectoires de business model — solo lifestyle vs boîte avec levée seed. Cette décision est **orthogonale** au choix de produit (LegalCase tel quel, extension corporate-b2b, ou pivot total) — elle peut s'appliquer à n'importe lequel.

> Distinct de `docs/radar-corporate-b2b.md` (extensions produit) et `docs/radar-pivots-totaux.md` (changements de produit). Si une activation est décidée sur un radar produit, ce fichier devra être consulté pour caler le business model du nouveau scope.

## ⚠️ Garde-fou discipline — à relire avant chaque réflexion

3 questions critiques à garder en tête :

1. **Pourquoi décider du business model alors que le produit n'a pas un seul client payant ?** Cette décision est **prématurée** sans traction. Tant que le seuil de 30 K€ MRR n'est pas atteint, l'arbitrage est purement intellectuel — sans matière empirique, n'importe quel choix se défend. La décision se prend **à la lumière des données réelles** de traction.
2. **Lever en l'absence de traction est un piège.** Le 2ᵉ expert (analyse Claude 2026-05-05) le formule clairement : *"Le piège classique : si tu ne sais pas vendre toi-même, aucun VP Sales ne sauvera ton produit. Tu vas brûler 1 M€ en 18 mois et fermer."* Le mode *"j'attends de lever pour vraiment commencer"* est un évitement de la vente, pas une stratégie.
3. **Le seuil non-négociable** : aucune décision business model ne se prend avant **30 K€ MRR atteint en bootstrap**. En dessous, ce radar reste un observatoire passif.

**Décider trop tôt = se contraindre dans une trajectoire que les données réelles auraient mieux éclairée.**

## Pourquoi ce document existe

Au cours d'une analyse stratégique sollicitée le 2026-05-05, le 2ᵉ expert a posé la question directement : *"Veux-tu un business solo lifestyle (300 K€ ARR, vie tranquille) ou une boîte (recrutement commercial, levée seed à 1-2 M€) ?"*

Cette question est probablement **la décision la plus structurante des 12 prochains mois** parce qu'elle conditionne :
- Les prochaines features (lifestyle = produit minimal et stable / levée = produit ambitieux multi-segment)
- Le pricing (lifestyle = self-service 99-429 € / levée = enterprise 1500-5000 €)
- La façon de vendre (lifestyle = solo / levée = équipe commerciale 5+ personnes)
- Le rythme de vie (lifestyle = 35-45 h/semaine / levée = pression VC permanente)

Or cette décision **ne peut pas être prise aujourd'hui de façon éclairée** — elle dépend de signaux qu'on n'a pas encore (capacité personnelle à vendre, vélocité naturelle d'acquisition, adéquation au management). D'où ce radar : tracer les paramètres de la décision **pour le moment où elle deviendra légitimement décidable**.

## Comparatif synthétique des 2 trajectoires

| Critère | Chemin 1 — Lifestyle solo | Chemin 2 — Boîte avec levée seed |
|---|---|---|
| **Structure** | Solo + 1-2 freelances occasionnels max | 8-10 personnes à 18 mois |
| **Cibles ARR** | Y1 0-50 K€, Y3 250-400 K€, plateau 400-700 K€ | Y1 200-500 K€, Y3 2-4 M€, Y5 5-15 M€ |
| **Levée** | 0 € (cash-flow seul) | 1-2 M€ seed (dilution 20-25 % + option pool) |
| **Charge horaire** | 35-45 h/semaine | 60-80 h/semaine (CEO mode) |
| **Rythme journée** | 50 % vente / 30 % code / 20 % admin | 30 % vente top-deals / 30 % management / 20 % stratégie+board / 10 % produit |
| **Revenu net (à plateau)** | 150-200 K€/an net | Salaire CEO 80-150 K€/an post-levée + sortie potentielle |
| **Sortie potentielle** | 1-2 M€ avant impôts (revente 2-4× ARR à acquéreur stratégique) | 5-20 M€ pour la part founder (après dilutions multiples, finit à 15-30 % de la boîte) |
| **Risque binaire** | Faible (modèle soutenable indéfiniment) | Élevé (60 % des seed ne lèvent pas leur Series A et meurent) |
| **Filet équipe** | Vulnérabilité si maladie 3 mois | Continuité si founder absent |
| **Liberté décisionnelle** | 100 % | Réduite (board + investisseurs + votes) |
| **Pression croissance** | Aucune | Constante (VC veulent 3× ARR/an minimum) |

## Signaux d'orientation — à surveiller dans les 6-12 prochains mois

Ce sont les indicateurs qui éclairent **dans quelle direction tu penches naturellement** sur la base des données réelles, pas des intuitions.

### Signaux pro-lifestyle

| # | Signal | Comment le détecter |
|---|---|---|
| L1 | Ton plafond de revenus personnel souhaité = expert salarié senior (150-250 K€/an net) | Réflexion personnelle. Si tu n'as pas envie d'être millionnaire à 8 chiffres, lifestyle suffit. |
| L2 | Tu prends plaisir à coder le week-end | Si oui, levée = jeter ce plaisir (tu deviens 90 % manager) |
| L3 | Tu n'as pas envie de manager 5+ personnes | Tester en se posant honnêtement la question : "passer 30 % de mes journées en 1-1 et performance reviews, ça me parle ?" |
| L4 | Croissance organique linéaire (5-10 clients/mois en croisière) | Si à 6 mois tu signes ~5-10 clients/mois sans canal payant, c'est le signal lifestyle solide |
| L5 | Marge brute > 80 % et churn stable | Économie SaaS qui fonctionne bien sans besoin d'investissement massif |

### Signaux pro-levée seed

| # | Signal | Comment le détecter |
|---|---|---|
| S1 | Cycle de vente reproductible identifié | "1 démo LinkedIn = 30 % closing à 200 €/mois" — formule duplicable avec cash |
| S2 | Queue de prospects qualifiés > 100 et tu refuses des démos | Tu brûles tes weekends à supporter l'existant au lieu de signer du nouveau = signal de plafonnement solo |
| S3 | TAM > 500 M€ pitchable | Avec extension RH/DRH ou multi-pays, tu passes de TAM 43 M€ (avocats FR+BE) à TAM 1-2 Md€ |
| S4 | Premiers signaux grands comptes | 1-2 mid-market signés à 800-1500 €/mois prouvent la capacité enterprise |
| S5 | Concurrence financée arrive sur ta niche | Si Predictice ou un nouvel entrant lève 5 M€ sur ton terrain, le bootstrap devient une course perdue |
| S6 | Envie de manager / construire | Tu passes du temps à imaginer ton organigramme = signal positif |

## Critères de décision — comment trancher quand ce sera légitime

À T+6 mois minimum (= ~2026-11-07 si on compte depuis 2026-05-07), poser les **3 questions de filtre** suivantes :

### Q1 — Mon canal d'acquisition est-il reproductible ?

| Réponse | Signification | Conséquence |
|---|---|---|
| Oui, identifié et mesurable | Lever **accélère** une machine qui marche | Levée envisageable |
| Non, je signe par opportunités | Lever **échoue** (le cash ne crée pas un canal) | Lifestyle ou pause produit |

### Q2 — Est-ce que je veux manager 10 personnes ?

| Réponse | Signification | Conséquence |
|---|---|---|
| Oui, ça me motive | Levée **débloque** ton potentiel | Levée envisageable |
| Non, je préfère builder seul | Lever te rend **malheureux** | Lifestyle |

### Q3 — Est-ce que je vise une sortie 5-20 M€ pour ma part ?

| Réponse | Signification | Conséquence |
|---|---|---|
| Oui, je vise ce niveau | Levée **nécessaire** | Levée envisageable |
| Non, 1-2 M€ + lifestyle me suffit | Levée **inutile et coûteuse** | Lifestyle |

**Règle de décision** : levée recommandée **uniquement si les 3 réponses sont OUI**. Une seule réponse "non" → lifestyle.

## Ce qu'il faut faire en attendant la décision

Pendant les 6 mois qui précèdent la décision (mai-novembre 2026), exécuter le **plan 30 K€ MRR** (cible 75 clients à 6 mois, ~10 K€ MRR plus probable) :

- Mois 1-2 : 30 entretiens découverte + 5 premiers clients early adopter
- Mois 3-4 : test 3 canaux d'acquisition + scale du gagnant + remontée pricing
- Mois 5-6 : parrainage + upsell + premier témoignage vidéo + optimisation onboarding

C'est **ce plan** qui produira les données empiriques nécessaires pour répondre aux 3 questions filtre. Avant ce plan exécuté, la décision business model est **abstraite et donc invalide**.

## Pré-requis si activation chemin 2 (levée)

Si à T+6 mois la décision est levée, vérifier que les pré-requis sont remplis avant de pitcher :

- [ ] **30 K€ MRR atteint** en bootstrap (ARR 360 K€) — le seuil PMF crédible aux yeux des VC
- [ ] **Croissance > 15 % MRR/mois sur 3 mois** consécutifs — preuve de momentum
- [ ] **Canal d'acquisition reproductible identifié** (CAC mesuré, LTV > 3× CAC)
- [ ] **Cohorts mensuelles documentées** (rétention M1, M3, M6, NRR, churn)
- [ ] **TAM ≥ 500 M€** pitchable — extension scope (RH/DRH ou multi-pays) ajoutée à la thèse
- [ ] **Co-founder identifié OU profil solo dérisqué** — solo founder = red flag VC à 90 %, à compenser par track record ou expertise vertical forte
- [ ] **Data room construite** (cohorts, NRR, CAC, LTV, attribution canaux)

Sans ces pré-requis : ne pas pitcher (= griller des cartouches inutilement avec les VC du marché).

## Décisions différées (à NE PAS faire maintenant)

- **Ne pas** annoncer publiquement (LinkedIn / Village / pitch deck) une thèse "leader européen" tant que la décision n'est pas prise
- **Ne pas** modifier le pricing F-123 pour ajouter un palier "Enterprise" sans signaux S3+S4 réunis
- **Ne pas** créer de page /entreprise ni de fonctionnalités enterprise (SSO, ISO 27001) tant que les pré-requis levée ne sont pas remplis
- **Ne pas** négliger les domaines V1 (immigration, famille) sous prétexte que "le pitch sera focus travail" — le code reste, c'est un atout, pas un handicap (cf. analyse 2ᵉ expert)
- **Ne pas** prendre la décision avant T+6 mois (= ~2026-11-07 minimum)

## Réévaluation

- **Reévaluation programmée** : 2026-11-07 (T+6 mois) au plus tôt, avec les données réelles du plan 30 K€ MRR
- **Réévaluation anticipée si** : signal très fort entrant (1 grand compte spontané, 1 fonds VC qui contacte non sollicité, 1 compétiteur financé qui arrive)
- **Réévaluation reportée si** : à T+6 mois la traction est insuffisante (< 5 K€ MRR) — auquel cas la question devient *"continue, pivot ou stop ?"* sur les radars produits, pas business model

## Lien avec les documents existants

- **`docs/radar-corporate-b2b.md`** — extension produit (cible adjacente, scope élargi) ; un choix d'extension peut s'exécuter en lifestyle ou avec levée
- **`docs/radar-pivots-totaux.md`** — changement de produit complet ; un pivot total **réinitialise** ce radar business model (les seuils s'appliquent au nouveau produit)
- **`docs/marketing/m71-budget-cadrage-2026h2.md`** — le cadrage budget marketing reste compatible avec lifestyle (Tranche 1 = 13 K€ déployés sans levée). La décision levée modifierait drastiquement la stratégie marketing (équipe + canaux payants).
- **Mémoire `project_enterprise_readiness_v9.md`** — bundle V9+ pour grands comptes (F-22 SSO + F-134 ISO/SOC 2 + DPA + F-135 commercial) : prérequis si chemin 2 + segment grands comptes
