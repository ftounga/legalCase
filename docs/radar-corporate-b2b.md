# Radar stratégique — trajectoire corporate / B2B juristes d'entreprise

> **Statut** : registre vivant des signaux faibles. Pas une roadmap, pas une feature au PRODUCT_SPEC.md.
> Hors V1. Ne devient un sujet d'engagement de roadmap que **si les critères d'activation sont réunis**.

## Pourquoi ce document existe

LegalCase V1 est défini : **outil pour avocats** (cabinet individuel ou petite structure), centré sur l'**analyse de dossier sur pièces** dans les domaines droit du travail, droit des étrangers, droit de la famille (FR + BE en extension).

Or, depuis le démarrage de la prospection BE en mai 2026, des signaux convergents pointent vers une **trajectoire alternative** : développer une couverture **corporate / B2B** ciblant les **juristes d'entreprise** (corporate counsel, in-house legal teams). Ce document trace ces signaux et formalise les conditions pour engager — ou non — ce pivot dans la roadmap.

L'objectif est d'**éviter deux pièges symétriques** : (1) ignorer un signal stratégique majeur qui s'accumule discrètement ; (2) céder au premier retour qui suggère un pivot et brûler de la roadmap V1 pour rien.

## Trajectoire candidate — résumé

| Aujourd'hui (V1) | Trajectoire candidate |
|---|---|
| Buyer = **avocat** (cabinet individuel / petit) | Buyer = **juriste d'entreprise interne** (corporate counsel, DAF/DPO/Achats triangulés) |
| Use case = **analyse de dossier contentieux** sur pièces | Use case = **revue de contrats, due diligence, secrétariat juridique, compliance** |
| Cycle de vente = **SaaS solo** (Calendly + carte bleue) | Cycle de vente = **procurement entreprise** (3 à 9 mois — IT, Achats, DPO, légal) |
| Concurrence = Predictice, Doctrine, Lexbase | Concurrence = Harvey ($300M levés), Spellbook, Robin AI, Luminance |
| Pricing F-123 = 99-429 €/mois | Pricing cible = 5-50 K€/an avec engagement annuel |
| ARR/client = 2 300 € | ARR/client cible = 20-100 K€ |

## Domaines de droit couverts par "matières commerciales et financières"

| Sous-domaine | Cas d'usage typique |
|---|---|
| Droit des contrats commerciaux | revue CGV, contrats fournisseurs, distribution, baux commerciaux |
| Droit des sociétés | gouvernance, AG, pactes d'associés, fusions-acquisitions, restructurations |
| Droit bancaire et du crédit | conventions de crédit, sûretés, financement |
| Droit financier / marchés | valeurs mobilières, prospectus, transparence (AMF en FR / FSMA en BE) |
| Droit de la concurrence | ententes, abus de position dominante, contrôle des concentrations |
| Droit fiscal | TVA, IS, prix de transfert |
| Droit des entreprises en difficulté | sauvegarde, redressement, procédure collective |
| Compliance / RGPD entreprise | DPO, audits conformité, registre des traitements |

## Implications produit — pré-requis durs

Engager cette trajectoire suppose au minimum :

- **F-22 SSO / OIDC entreprise** — Microsoft Entra ID, Okta, Azure AD obligatoires en B2B
- **F-134 Certification ISO 27001 + DPA contractuel** — Sans certif, le DPO bloque l'achat. Sans DPA, le procurement bloque.
- **F-135 Organisation commerciale** — Sales pipeline, contracts, ARR tracking, account management
- **Architecture multi-tenant SSO** — séparation stricte entre corporate workspaces
- **Audit logs avancés** — qui a fait quoi sur quel contrat, exigence compliance corporate
- **API publique documentée** — l'intégration aux outils existants (ContractWorks, Ironclad, DocuSign) devient un critère d'achat

Mémoire associée : `project_enterprise_readiness_v9.md` — bundle V9+ déjà flaggé pour les grands comptes.

## Pourquoi V8+ minimum, pas avant

1. **Buyer entièrement différent** — un juriste d'entreprise n'achète pas comme un avocat indépendant. Le go-to-market complet doit être redéfini (canaux LinkedIn legal-ops, salons HR/CFO/Legal Ops, présence média corporate, certifications obligatoires).
2. **Concurrence très financée** — Harvey vient de lever $300M. Spellbook $20M. Démarrer dans cette ligue avec zéro traction = brûler du runway sans bénéfice mesurable.
3. **Risque de dilution V1** — chaque feature corporate reportée du domaine pris en V1 (avocat indépendant droit du travail) est du retard pris sur le marché qu'on vise actuellement et où on est en mesure de gagner.

## Registre des signaux observés

Chaque signal documenté avec : date, source, formulation, interprétation, poids estimé (faible / moyen / fort).

| # | Date | Source | Contenu | Interprétation | Poids |
|---|------|--------|---------|----------------|-------|
| 1 | 2026-05-07 | Daniel GASPARD (avocat Charleroi, 1 an avant retraite, retour mail M-64 vague 2/3) | *"Vous devriez aussi sérieusement développer l'outil dans les matières commerciales et financières car les entreprises seraient susceptibles d'être intéressées, en tout cas les juristes d'entreprise."* | Signal direct sur la cible juriste d'entreprise. Argument retenu : volume + capacité financière entreprises. Vient d'un avocat senior expérimenté qui a en plus fourni une intro nominale (fs@centrius.be). | **Moyen** (signal direct mais 1 source) |

> Pour ajouter un nouveau signal : copier la ligne ci-dessus, incrémenter le numéro, dater, citer la source verbatim.

## Critères d'activation

Cette trajectoire **devient un sujet de roadmap** quand l'**un** des critères suivants est atteint :

| Critère | Seuil | Signification |
|---|---|---|
| **Volume de signaux** | ≥ 5 signaux indépendants de poids ≥ moyen | Tendance lourde, pas un avis isolé |
| **Demande payante explicite** | ≥ 2 prospects juristes d'entreprise demandant un POC payant | Validation marché tangible |
| **Intro qualifiée chaude** | ≥ 1 contact direct DAF/DJ d'une entreprise > 200 personnes proposant un pilote | Porte d'entrée concrète sur le segment |
| **Saturation V1** | V1 stabilisée + ARR > 100 K€ + churn < 5 % | On peut financer la R&D sur le pivot sans risquer V1 |

Tant qu'aucun de ces seuils n'est franchi : **pas d'engagement roadmap**, pas de feature en mini-spec, pas de dépense marketing dédiée.

## Décisions différées (à NE PAS faire maintenant)

- **Ne pas** ajouter de feature F-XXX corporate au PRODUCT_SPEC.md
- **Ne pas** rédiger de pitch deck B2B juriste d'entreprise (M-40 reste cabinets avocats)
- **Ne pas** ouvrir de campagne marketing dédiée juristes d'entreprise (LinkedIn legal-ops, salons CFO, etc.)
- **Ne pas** modifier le pricing F-123 pour ajouter un palier "Enterprise"
- **Ne pas** présenter la roadmap publiquement (pitch / talks) avec une trajectoire corporate annoncée

## Réévaluation

Ce document est revu **automatiquement** :

- À chaque nouveau signal observé (ajout d'une ligne au registre + relecture des critères d'activation)
- À chaque audit de couverture des domaines métier (`feedback_coverage_audit_every_10_features.md`)
- Au moment de la révision du cadrage budget marketing (`docs/marketing/m71-budget-cadrage-2026h2.md`) — typiquement T+3 mois après une vague d'acquisition

## Lien avec les documents existants

- **`docs/PRODUCT_SPEC.md`** — la trajectoire corporate **n'y figure PAS** par construction (V1 + V2-V8 sont alignés cabinets avocats). Si activation, créer F-XXX corporate.
- **`docs/MARKETING_BACKLOG.md`** — tâche M-76 (Partenariats éditeurs logiciels juridiques) reste un canal **distinct** : elle vise la distribution via éditeurs cabinets (Secib, Kleos), pas le pivot vers la cible juriste d'entreprise.
- **Mémoire `project_enterprise_readiness_v9.md`** — pré-requis techniques bundle V9+ (F-22 SSO + F-134 ISO/SOC 2 + DPA + F-135 organisation commerciale).
