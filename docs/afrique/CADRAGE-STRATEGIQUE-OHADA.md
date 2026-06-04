# Cadrage stratégique — LegalCase Afrique OHADA

> **Statut : HYPOTHÈSE DE CADRAGE — observation passive.**
> Ce document fige les décisions stratégiques *amont* qui servent d'entrée au workflow
> `.claude/workflows/afrique-product-spec.js`. Il ne constitue **pas** un engagement de roadmap.
> Le verrou d'activation reste celui du radar `docs/radar-cameroun-ohada.md` :
> **30 K€ MRR FR/BE atteint en bootstrap** (réévaluation ≥ 2026-11-28), OU substitution
> par une preuve externe (capital fléché Afrique + ≥ 3 intentions de paiement OHADA).
> Tracer ≠ pivoter ≠ engager.

## 1. Décisions figées (inputs du workflow)

| # | Sujet | Décision | Source / date |
|---|-------|----------|---------------|
| D1 | **Architecture produit** | **Même application, même repo au départ** (codebase partagée, réutilisation ~90 % du moteur dossier-centric). LegalCase Afrique = le produit existant + un **4ᵉ domaine juridique « droit des affaires OHADA »** (comme Travail / Immigration / Famille). La **séparation** porte sur l'**instance déployée**, **pas sur le code** : déploiement régional dédié (af-south-1, voir D9), **données séparées** (résidence), **marque déclinée** (D2), **domaine propre** (D10, `legalcase.africa`), **roadmap & dossier de levée indépendants**. **Pas de scaffold ni de nouveau repo nécessaire pour démarrer le dev.** Un **fork** vers un repo dédié reste **différé et optionnel** — uniquement si un dossier de levée exige un actif autonome ; non requis au démarrage. | Arbitré 2026-06-03, précisé 2026-06-04 |
| D2 | **Marque / charte** | **Même marque LegalCase, charte déclinée** : mêmes fondamentaux visuels, déclinaison régionale (langue, mentions légales, devise XAF, références OHADA, sous-processeurs hébergement Afrique). | Arbitré 2026-06-03 |
| D3 | **Structure de la fiche produit** | **Miroir du pattern dossier-centric** : structure identique à `docs/PRODUCT_SPEC.md` (Blocs → features `| ID | Feature | Description | Statut |` → outils décisionnels/simulateurs → génération d'actes). Conserve le différenciateur dossier-centric (créneau libre vs concurrents recherche-centric). **⚠️ Le miroir porte sur le FORMAT, le pattern et la barre de qualité/granularité — PAS sur le contenu ni la volumétrie cible** (la cible de contenu est le marché africain, cf. **D12**). | Arbitré 2026-06-03, précisé 2026-06-04 |
| D4 | **Statut du livrable** | **Fichier de cadrage séparé, hors backlog** : `docs/afrique/PRODUCT_SPEC_OHADA_DRAFT.md`, marqué HYPOTHÈSE, **exclu** du `PRODUCT_SPEC.md` live et du sync backlog F-178. Finalité = dimensionnement effort + finançabilité, **pas** un ordre de build. | Arbitré 2026-06-03 |
| D5 | **Scope métier** | **OHADA droit des affaires uniquement** — pas d'élargissement aux domaines nationaux (travail/famille/immigration restent l'Europe). | Radar 2026-05-28 |
| D6 | **Géographie** | **OHADA-17**, Cameroun comme tête de pont (TAM 12-18K avocats d'affaires). | Radar 2026-05-28 |
| D7 | **Bijuridisme** | **Option A — francophone uniquement** au démarrage. Common law NWSW reporté V2 conditionnel. Garde-fou messaging : ne jamais sous-entendre couvrir l'anglophone avant implémentation réelle. | Radar 2026-05-28 |
| D8 | **Paiement** | **Bi-rail obligatoire** : CinetPay (XAF — MTN MoMo + Orange Money) + Stripe (cabinets internationaux EUR). Stripe seul ne marche pas au Cameroun. | Radar 2026-05-28 |
| D9 | **Hébergement** | **Résidence des données en Afrique obligatoire** (loi CM n°2024/017 du 23/12/2024, conformité 23/06/2026) — AWS Cape Town ou OVH Africa. | Radar 2026-05-28 |
| D10 | **Nom de domaine** | **`legalcase.africa`** (gTLD panafricain, cohérent OHADA-17, brandable pour un dossier de levée autonome) en prod ; **`staging.legalcase.africa`** en staging. Alternatives écartées : `ohada.legalcase.fr` (couple au produit FR, affaiblit le « produit séparé » D1) ; `legalcase.cm` (trop Cameroun-only vs scope OHADA-17). Reste à faire : vérifier dispo + déposer. | Arbitré 2026-06-03 |
| D11 | **Pays / juridiction** | OHADA = **domaine unique à loi de fond uniforme** (10 Actes uniformes harmonisés, d'application directe dans les 17 pays) — **aucune sélection de législation par pays**. Le **pays est un attribut du DOSSIER** (jamais du workspace ni de l'authentification), **pré-rempli par l'IA** depuis les pièces (siège social, n° RCCM, devise des contrats) avec override avocat. Il ne sert qu'au **contexte opérationnel** : devise (XOF UEMOA / XAF CEMAC / autres), juridiction compétente, RCCM / huissiers, frais d'enregistrement. L'onboarding demande le **pays principal** comme *préférence par défaut*, pas comme gate. **Interdit : sélecteur de pays bloquant à l'authentification.** Justifie le cross-border (un dossier peut mêler plusieurs pays OHADA). | Arbitré 2026-06-04 |
| D12 | **Cible & mode de la fiche produit** | **La cible de contenu est le MARCHÉ AFRICAIN** : son contexte, ses normes OHADA, les besoins réels de l'avocat d'affaires, **ce que fait la concurrence** (Lexbase Afrique, Jurisprudence.cc, Legal Doctrine, Jurisprudence-OHADA.com…) et **les trous à exploiter**. LegalCase FR/BE n'est qu'une **ancre de cohérence + barre de qualité/format** — jamais le plafond de contenu ni la volumétrie cible. La fiche produit est un **document vivant** : enrichi par **appends justifiés** run après run (provenance par feature : acte-uniforme / marché / trou-concurrent / vision-PO / plateforme-réutilisée), **modifié seulement** sur info marché nouvelle ou directive PO, **jamais réécrit de zéro**, avec un **changelog append-only**. Le workflow **auto-évalue sa maturité** (couverture marché, différenciation, ancrage juridique, cohérence, complétude) et **signale un seuil d'excellence** quand les runs n'apportent plus que des gains marginaux (rendements décroissants). | Arbitré 2026-06-04 |

## 2. Cartographie des domaines (ancrage juridique réel)

La spec OHADA s'ancre sur les **10 Actes uniformes** harmonisés (matière réelle, pas inventée).
Chaque Acte = un domaine ; chaque domaine se décline en situations métier → outils décisionnels → actes générés.

| Domaine | Acte uniforme | Exemples de situations métier |
|---------|---------------|-------------------------------|
| Droit commercial général | **AUDCG** | statut commerçant, RCCM, bail à usage professionnel, fonds de commerce, vente commerciale, intermédiaires |
| Sociétés commerciales & GIE | **AUSCGIE** | constitution, gouvernance, AG, pactes, augmentation/réduction capital, transformation, dissolution |
| Sûretés | **AUS** | cautionnement, gage, nantissement, hypothèque, réserve de propriété, classement des sûretés |
| Recouvrement & voies d'exécution | **AUPSRVE** | injonction de payer, injonction de délivrer, saisies (attribution, vente, conservatoire) |
| Procédures collectives | **AUPC** | conciliation, règlement préventif, redressement judiciaire, liquidation des biens |
| Comptabilité & info financière | **AUDCIF / SYSCOHADA** | états financiers, obligations comptables, seuils, système normal/minimal de trésorerie |
| Arbitrage | **AUA** | convention d'arbitrage, constitution du tribunal, sentence, exequatur |
| Médiation | **AUM** | accord de médiation, homologation |
| Transport de marchandises par route | **AUCTMR** | lettre de voiture, responsabilité du transporteur, litiges |
| Sociétés coopératives | **AUSCOOP** | constitution, gouvernance coopérative, registre |

## 3. Préoccupations transversales / infra Afrique (à spécifier comme domaine propre)

- **Multi-pays OHADA-17** : gestion de la juridiction applicable, variations nationales résiduelles (taux, juridictions compétentes).
- **Paiement bi-rail** (D8), **hébergement / résidence données** (D9).
- **i18n / devise XAF / charte déclinée** (D2), peg EUR/XAF fixe (1 EUR = 655,957 XAF).
- **Bijuridisme Option A** (D7) — garde-fou messaging francophone-only.
- **Auth** : réutilisation OIDC ; évaluer pénétration Google/Microsoft vs besoin d'un rail local.

### 3.1 Cible infra Afrique — DOCUMENTÉE, **non provisionnée** tant que le verrou tient

> ⚠️ Spécification de dimensionnement, pas un `terraform apply`. Provisionner = engager → interdit en
> observation passive. Coût réel = **0 $ tant que non provisionné**. Repo concerné : `legalcase-infra`.

- **Région** : `af-south-1` (AWS Cape Town), ou OVH Africa en alternative. **Distincte** d'`eu-west-3` (Europe) — la résidence des données (D9) interdit de router des workspaces OHADA vers l'Europe ; un simple node group régional ne suffit pas légalement.
- **Cluster** : **un seul cluster EKS Afrique** hébergeant **deux environnements en namespaces — `staging` + `production`** (même pattern que l'EKS partagé eu-west-3 actuel). Pas deux clusters.
- **Nouvel environnement Terraform** : `environments/production-afrique/` (staging via namespace sur le même cluster), instanciant les **mêmes modules** existants (`networking`, `eks`, `rds`, `s3`, `cdn`, `monitoring`, `backup`) avec `region = af-south-1`. Effort = **configuration**, pas développement (conséquence directe de D1 : produit séparé, codebase et modules partagés).
- **Données** : RDS + S3 + backups **régionaux Afrique** (exigence de résidence D9). Le staging peut partager la région ; s'il n'héberge que des données synthétiques, la résidence n'est pas un blocage légal, mais la co-localisation simplifie la conformité.
- **Domaines & TLS** : `legalcase.africa` (prod) + `staging.legalcase.africa` (staging) → CloudFront + ACM régionaux (D10).
- **Coût indicatif à régime** (à chiffrer finement) : control plane EKS ~73 $/mois + nodes + RDS + NAT + CDN → ordre de grandeur de l'eu-west-3 actuel (~400 $/mois). À intégrer au dossier de finançabilité.

## 4. Couche plateforme réutilisée (le ~90 % moteur, ré-exprimé pour le produit séparé)

Fondations (auth, onboarding workspace, domaine = droit des affaires OHADA), gestion de dossiers,
upload & stockage, **pipeline IA chunk→document→dossier**, **outils décisionnels / simulateurs**,
**génération d'actes**, Q&A interactive. Ces blocs sont repris du produit Europe mais **re-spécifiés**
dans le contexte produit séparé (marque, devise, juridiction).

## 5. Ce que ce cadrage ne tranche PAS (downstream, hors workflow)

- La **propension à payer** d'un cabinet d'affaires OHADA (donnée manquante critique — radar).
- La **priorisation** réelle des features (la spec générée est une hypothèse exhaustive, pas un ordre de build).
- Le **business model** (lifestyle vs levée) — voir `docs/radar-business-model.md`.
- L'activation : reste conditionnée au verrou (§ en-tête).
