# Cadrage stratégique — LegalCase DRH (offre employeur)

> **Statut : HYPOTHÈSE DE CADRAGE — observation passive.**
> Ce document fige les décisions stratégiques *amont* qui servent d'entrée au workflow
> `.claude/workflows/drh-product-spec.js`. Il ne constitue **pas** un engagement de roadmap.
> Verrou d'activation (radar `docs/radar-corporate-b2b.md`) : **30 K€ MRR atteint en bootstrap sur la
> cible avocat actuelle**, OU ≥ 2 prospects DRH demandant un POC payant, OU intro chaude DAF/DJ entreprise > 200p.
> Signal actuel = **2/5** (1 avocat senior + 1 analyse interne). Garde-fou n°2 du radar : *« coder n'est pas
> vendre »* — ce cadrage ne dispense pas de prouver le PMF avocat et de forcer les entretiens prospects.
> Tracer ≠ pivoter ≠ engager.

## 1. Décisions figées (inputs du workflow)

| # | Sujet | Décision | Source / date |
|---|-------|----------|---------------|
| D1 | **Architecture produit** | **Même application, même repo, même infra `eu-west-3`** (réutilisation ~90 % du moteur droit du travail existant). LegalCase DRH = le produit existant + un **type de workspace `EMPLOYEUR`** + packaging/pricing/UI orientés DRH. **Aucune infra séparée** (pas d'enjeu de résidence, FR/EU). La séparation est **commerciale** (buyer, messaging, pricing), pas technique. Un **fork** vers un produit/marque distinct reste **différé et optionnel** (si GTM corporate dédié ou dossier de levée l'exige), non requis au démarrage. | Arbitré 2026-06-04 |
| D2 | **Marque / positionnement** | **Même marque LegalCase, déclinaison corporate/RH** : mêmes fondamentaux, messaging orienté employeur (ROI, productivité, conformité). | Arbitré 2026-06-04 |
| D3 | **Structure de la fiche produit** | **Miroir du pattern dossier-centric** : situation employeur → upload pièces → analyse pipeline IA → outil décisionnel/simulateur → acte/courrier RH. ⚠️ Le miroir porte sur le FORMAT / pattern / barre de qualité — **pas** le contenu cible (cf. D12). | Arbitré 2026-06-04 |
| D4 | **Statut du livrable** | **Fichier de cadrage séparé, hors backlog** : `docs/drh/PRODUCT_SPEC_DRH_DRAFT.md`, marqué HYPOTHÈSE, **exclu** du `PRODUCT_SPEC.md` live et du sync backlog F-178. Finalité = dimensionnement + finançabilité, **pas** un ordre de build. | Arbitré 2026-06-04 |
| D5 | **Scope métier** | **Droit du travail uniquement, côté employeur** — gestion internalisée du risque/contentieux social (prud'hommes). **Hors scope** : la trajectoire « juriste d'entreprise » large (contrats, sociétés, M&A…) = un AUTRE pari (radar corporate, trajectoire ≠ DRH). DRH ne déborde pas du droit social. | Radar corporate 2026-05 |
| D6 | **Buyer** | **DRH / Directeur des Affaires Sociales**, entreprise **200+ salariés** (~10 000 FR + 30 000 ETI EU). ARPU cible **800-3000 €/mois** (vs 99-429 € avocat), cycle de vente 1-3 mois, NRR ~130 %, acheteur récurrent. ROI : « évitez 50 K€/an d'erreur de calcul d'indemnité ». | Radar corporate, signal #2 |
| D7 | **Type d'acteur (rôle)** | **Attribut du WORKSPACE**, fixé **une fois à la création** (comme le domaine juridique), **jamais un sélecteur bloquant** : `AVOCAT` (défaut, existant) vs **`EMPLOYEUR`**. L'employeur **est partie** au litige (≠ l'avocat qui représente un tiers) → le **dossier est centré-salarié**, le multi-utilisateur existant couvre l'équipe RH. **Même moteur d'outils décisionnels**, simplement **lu côté employeur** (« mon risque » au lieu de « le risque de mon client »). | Arbitré 2026-06-04 |
| D8 | **Invariant anti-conflit (déontologie)** | La même plateforme sert avocats ET employeurs : **pas de conflit de données** (multi-tenant isole déjà). Risque = **perception** (ne pas « armer l'adversaire » des avocats, ne pas aliéner le Barreau). **Invariant** : l'offre DRH se cadre strictement **conformité / anticipation du risque social / productivité**, **jamais** « gagner contre vos salariés ». Surfaces de marque déclinées. **À valider terrain** : réaction de 2-3 clients avocats **avant l'engagement**. | Arbitré 2026-06-04 |
| D9 | **Pricing** | Palier **corporate 800-3000 €/mois** (engagement annuel), distinct des paliers avocat (F-123 : 99/219/429). Coexistence des deux grilles dans la même app. À affiner par la fiche produit + le marché. | Radar corporate, signal #2 |
| D10 | **Corporate-readiness = features PRODUIT** | Le procurement entreprise impose : **SSO/OIDC entreprise (F-22)**, **ISO 27001 + DPA (F-134)**, **audit logs avancés**, **API documentée**, **organisation commerciale (F-135)**. Ce sont des **features produit** (repo `legalCase`), **PAS de l'infra**. Bundle déjà flaggé V9+ (`project_enterprise_readiness_v9`). | Radar corporate, mémoire V9 |
| D11 | **Infra** | **Aucune infra séparée** : même `eu-west-3`, même cluster, même RDS/S3. **Pas de `SF-INFRA-DRH`** (contrairement à OHADA). Les besoins corporate sont des features produit (D10), pas du provisioning. | Arbitré 2026-06-04 |
| D12 | **Cible & mode de la fiche produit** | **La cible de contenu est le MARCHÉ DRH/employeur** : besoins réels du DRH, **concurrence** (outils RH / legal-ops / éditeurs droit social employeur), trous à exploiter, normes d'achat corporate. LegalCase avocat = **ancre de cohérence + barre de qualité/format**, jamais le plafond. Fiche produit = **document vivant** (appends justifiés + provenance + changelog append-only, modif sur info marché/directive PO seulement, jamais réécrit de zéro). **Auto-évaluation de maturité** avec **seuil d'excellence** (rendements décroissants). | Arbitré 2026-06-04 |

## 2. Cartographie des situations (ancrage métier réel — droit du travail côté employeur)

Le moteur droit du travail existe déjà (côté avocat). La fiche DRH le **ré-exprime côté employeur**. Domaines = situations employeur réelles :

| Domaine | Situations employeur |
|---------|----------------------|
| Rupture du contrat | licenciement (perso / éco), rupture conventionnelle, fin de CDD/période d'essai, départ négocié |
| Risque & chiffrage prud'homal | simulation indemnités, barème Macron, chiffrage d'une transaction, exposition d'un dossier |
| Sanctions disciplinaires | avertissement, mise à pied, procédure disciplinaire, proportionnalité |
| Inaptitude & santé au travail | inaptitude médicale, obligation de reclassement, AT/MP |
| Temps de travail | heures supplémentaires, forfait jours, RTT, astreintes, litiges durée |
| Égalité & prévention | discrimination, harcèlement, égalité F/H — **prévention employeur** |
| Représentation du personnel | CSE, élections, consultation, délit d'entrave |
| Documents & actes RH | courriers, convocations, notifications, protocoles transactionnels générés |
| Conformité sociale | DUERP, obligations employeur, registres, échéances |

## 3. Préoccupations transversales (corporate-readiness — features PRODUIT, pas infra)

- **SSO/OIDC entreprise** (F-22) — Entra ID / Okta obligatoires en B2B
- **ISO 27001 + DPA** (F-134) — sans certif/DPA, le DPO et le procurement bloquent l'achat
- **Audit logs avancés** — qui a fait quoi sur quel dossier salarié (exigence compliance)
- **API documentée** — intégration SIRH (éventuel critère d'achat)
- **Organisation commerciale** (F-135) — cycle de vente entreprise, account management
- **Type d'acteur `EMPLOYEUR`** (D7) — packaging, visibilité d'outils, pricing
- **Invariant anti-conflit** (D8) — garde-fou messaging conformité/productivité

> Pas d'infra dédiée (D11). Tout ci-dessus vit dans le repo `legalCase` comme features produit.

## 4. Couche plateforme & moteur réutilisés (le ~90 %)

Auth/onboarding (+ type d'acteur), gestion de dossiers, upload & stockage, **pipeline IA chunk→document→dossier**,
**outils décisionnels / simulateurs droit du travail** (déjà construits), **génération d'actes**, Q&A interactive.
Repris tels quels, **re-cadrés côté employeur** (framing, pricing, multi-utilisateurs RH).

## 5. Ce que ce cadrage ne tranche PAS (downstream)

- La **propension à payer** réelle d'un DRH (signal encore 2/5 — à valider par des POC payants).
- La **réaction des avocats** à la coexistence (D8) — à tester avant engagement.
- La **priorisation** des features (la fiche est une hypothèse, pas un ordre de build).
- L'activation : conditionnée au verrou (§ en-tête).
