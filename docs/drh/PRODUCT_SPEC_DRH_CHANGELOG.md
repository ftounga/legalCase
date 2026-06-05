# Changelog — Fiche produit LegalCase DRH (offre employeur)

> Append-only. Nouvelle entrée en tête. Ne jamais réécrire les entrées passées.

## 2026-06-06 — arbitrage PO (hors run de maturation)
- Ajout d'une section "## Décisions PO — arbitrage 2026-06-06" en tête du draft : convertit les proposedDeletions et manques résiduels du run 3 en DÉCISIONS (tracées, non exécutées tant que le verrou d'activation n'est pas franchi — D4).
- A — Périmètre net DÉCIDÉ : A.1 doublons purs à supprimer (réf. survivante confirmée) ; A.2 consolidations (feature de référence fixée). À appliquer mécaniquement (appliedDeletions) au passage backlog.
- B — Principe structurant ADOPTÉ : 1 capacité = 1 implémentation de référence + N déclinaisons (PLATFORM-04, F-DT-07, F-JU-01, F-38/AUDIT-LOG-01, CHIFFRAGE-07, CHIFFRAGE-11, moteur d'arbitrage commun).
- C — Contrôle de cohérence marketing 4 points PASSÉ pour AI-ACT-10 + API-SIRH-10 → verdict NE PAS ajouter à MARKETING_BACKLOG (séquence stratégique KO : DRH non activé). Retirées du périmètre produit, gelées jusqu'à activation DRH.
- D — Sujet UX « scoring/métrage ⊂ outils calculés » : reco PO = « alerte avant génération » ; reste OUVERT (→ OPEN_QUESTIONS), à ratifier avant tout dev de scoring/facturation.
- E — Cohérences éditoriales : AI-ACT-13 = source unique des échéances AI Act ; SCOPE-01/02 = note de scope canonique.
- F — D8 : GO/NO-GO conditionné au test terrain 2-3 avocats ; PREAVOCAT-07/10/12/13 = export borné/révocable + smoke test isolation avant merge.
- G — Pricing/GTM corporate : différé à l'approche du verrou.
- Aucune feature supprimée ni dev déclenché ; livrable reste HYPOTHÈSE hors backlog (D4).

## 2026-06-06 — run de maturation
- overall 90/100 (Δ 5) → continue
- ajoutées: 75 features ; modifiées/justifiées et suppressions: voir détail
- directive PO ce run: {"directivePrincipale":"RUN 3 — viser le seuil d'excellence 90. Combler les 3 trous de contenu identifiés au run 2 ET tenir le dédoublonnage.","trousACombler":{"pricing":"CRÉER les features de PRICING / PACKAGING « compte employeur » (D9) — actuellement AUCUNE feature dédiée. Grille tarifaire corporate 800-3000 €/mois engagement annuel, coexistence avec les 2 grilles avocat (F-123 : 99/219/429), gating/packaging par type d'acteur EMPLOYEUR, métrage usage (s'appuie sur usage_events F-33/F-257 + quota API-SIRH-14). NRR ~130%, acheteur récurrent.","onboarding":"CRÉER les features d'ONBOARDING / ACTIVATION de l'acteur EMPLOYEUR — sélection du type d'acteur à la création du workspace (D7, attribut figé non bloquant), parcours premier dossier centré-salarié, réduction de la friction d'activation (signal terrain récurrent Renversez/Mengue). PLATFORM-01 fixe l'attribut mais ne porte PAS le parcours d'activation.","scopeBE":"STATUER explicitement le périmètre juridiction : V1 = FR seul (CPH, barème Macron, CNIL, France Travail) ; BE = backlog différé (couverture exhaustive du droit social belge attendue, PAS un miroir FR). Énoncer ce verrou dans une note de scope plutôt que laisser une fausse exhaustivité FR-centric."},"uxNonTranche":"CHIFFRAGE-07 et tous les scorings d'exposition : marquer explicitement le sujet UX NON TRANCHÉ « scoring/conclusions ⊂ outils CALCULÉS/persistés uniquement, pas les champs pré-remplis non cliqués » — à arbitrer avant dev (alerte avant génération / pré-calcul auto / laisser tel quel). Ne pas le résoudre en silence.","dedup":"Maintenir le dédoublonnage du run 2 : NE PAS recréer un outil existant (catalogue F-DT/F-JU), réutiliser (plateforme-reutilisee, decisionTool=false côté DRH). Capacités transverses uniques : lecture employeur=PLATFORM-04, CCN-aware=F-DT-07, jurisprudence=F-JU-01, journal contrôle humain=AI-ACT-01, vue portefeuille=DASHBOARD-01, scoring=CHIFFRAGE-07 décliné. Génération d'actes centralisée dans ACTES (F-98+F-DT-04). Fiche provision IAS 37 = CHIFFRAGE-11 référence + N configurations. Pattern contester/transiger = moteur d'arbitrage commun (paramètres procédure 2026 alignés).","gouvernance":"Statuts réels pour features existantes (plateforme=Terminée ; F-22/F-134=V9+ Grands comptes), 'Hypothèse' réservé aux F-DRH-*. AI-ACT-10 + API-SIRH-10 = tâches marketing à sortir du produit. REQUAL-CDD-03 + CSE-CONFORM-11 = ALERTES d'exposition pénale, pas outils pénal (D5, pénal hors V1). PREAVOCAT-07/10 = export borné/révocable, jamais accès croisé inter-workspaces (D8).","prioriteMustHave":"Prioriser les 8 must-have §7 + exploiter les trous concurrents §6 (créneau dossier-centric employeur VIDE)."}

## 2026-06-06 — enrichissement domaine CORP-READY (ISO 27001 / SOC 2 & DPA RGPD self-serve)
- mode document vivant D12 : APPEND ; IDs existants (F-134 + 01→18) préservés, aucune suppression nouvelle
- ajoutées (5) :
  - F-DRH-CORP-READY-19 — feuille de route de certification datée & séquencée (ISO 27001 d'abord, SOC 2 Type II en parallèle, jalons Type I 2-3 mois / Type II observation 3-12 mois) publiée au Trust Center. Comble le trou GTM long-lead-time (certif à LANCER avant le 1er cycle grand compte). Distinct de -01 (rapport SOC 2) et -02 (certificat ISO). decisionTool=false (marche)
  - F-DRH-CORP-READY-20 — matrice de contrôles partagée multi-référentiels (ISO 27001 ↔ SOC 2 TSC ↔ CCM/CAIQ) exposée comme livrable procurement, exploite le recouvrement ~70-80 % ; alimente le pré-remplissage CAIQ (-06)/SIG-VSA (-07). Cadrée PRODUIT (D10) ; Vanta/Drata/Secureframe restent FOURNISSEURS hors produit (D11). decisionTool=false (concurrent-gap)
  - F-DRH-CORP-READY-21 — engagement réponse à incident & notification de violation (RGPD Art. 33/34, 72 h). Mesure de sécurité/DPA attendue, distincte du DPA contrat (-03) et des audit logs (-10). decisionTool=false (marche)
  - F-DRH-CORP-READY-22 — notification & droit d'opposition aux changements de sous-traitants ultérieurs (Art. 28.2), workflow dans le temps distinct de la liste statique -04. Friction DPO réelle sur les sous-processeurs IA (Anthropic/OpenAI/Textract/AWS). decisionTool=false (droit-travail)
  - F-DRH-CORP-READY-23 — support des demandes d'audit client / droit d'audit (Art. 28.3.h), souvent par substitution rapports SOC 2/ISO. Distinct de la clause DPA -03 et des rapports -01/-02. decisionTool=false (marche)
- modifiées : aucune (curation -15→-18 du run 2026-06-05 inchangée)
- suppressions proposées : aucune nouvelle (les doublons transverses inchangés : AI-ACT-07/08/14 ↔ CORP-READY-05/06/07/10/14/15 déjà tracés)
- invariants : corporate-readiness = features PRODUIT (D10), PAS infra ni outillage interne d'obtention de certif (D11 — compliance-automation = fournisseurs hors périmètre) ; D8 (conformité/procurement, jamais arme anti-salarié) ; 1 feature = 1 situation procurement propre (19=roadmap, 20=matrice multi-référentiels, 21=incident/breach, 22=changement sous-traitant, 23=droit d'audit — aucune ne duplique -01/-02/-03/-04/-08/-10) ; toutes decisionTool=false
- note de scope : data residency EU (eu-west-3) déjà acté D1 ; AIPD/FRIA portés par AI-ACT-02 (FRIA = secteur public uniquement) ; kit DPO = CORP-READY-15 (référence) — non re-dupliqués ici
- directive PO ce run : RUN 3 — viser 90, combler les trous marché/concurrents de CORP-READY, tenir le dédoublonnage

## 2026-06-06 — enrichissement domaine SECU-PROC (Sécurisation procédurale des ruptures & actes)
- mode document vivant D12 : APPEND ; IDs existants 01→16 préservés, aucune suppression nouvelle
- ajoutées (2) :
  - F-DRH-SECU-PROC-17 — Garde-fou de la priorité de réembauche post-licenciement économique (L1233-45, 12 mois) : surveille une obligation DURABLE post-rupture (demande du salarié, postes disponibles, traçabilité des propositions) dont la violation expose à des dommages-intérêts. Distinct de SECU-PROC-02 (checklist pré-décision) et de SECU-PROC-14 (calendrier pré-notification). Étend le moteur calendaire SECU-PROC-03/14 à une borne post-rupture. decisionTool=true (concurrent-gap)
  - F-DRH-SECU-PROC-18 — Contrôle de précision/individualisation de l'offre de reclassement (licenciement économique) : vérifie depuis les pièces que l'offre est écrite, précise, personnalisée et sérieuse — l'imprécision prive le licenciement de cause réelle et sérieuse (6-12 mois). Pendant économique de INAPT-12 (refus caractérisé inaptitude, Cass. nov. 2025) ; alimente l'aval éditorial ACTES-02/05. Zone anti-vice vide chez les concurrents. decisionTool=true (concurrent-gap)
- modifiées (1) : F-DRH-SECU-PROC-15 — marqueur de scope juridiction V1=FR ajouté (CPH, barème Macron, France Travail) ; BE différé au backlog. changeReason = directive PO RUN 3 (statuer le périmètre juridiction, pas de fausse exhaustivité FR-centric)
- note de scope (verrou D-juridiction) : V1 du domaine SECU-PROC = FR seul (procédures CPH, délais L1332/L1233, barème Macron, DREETS/France Travail, contribution saisine CPH ~50 € 2026). BE = backlog différé — couverture exhaustive du droit social belge attendue, PAS un miroir FR (cf. feedback_belgique_never_forget)
- suppressions proposées : inchangées (04/05/06/07/08/09/11 déjà listées run 2026-06-05) ; aucune nouvelle
- invariants : D8 strict (conformité/anticipation du risque, jamais armer contre le salarié — SECU-PROC-18 = « mon offre tient-elle ? », SECU-PROC-17 = « ai-je respecté la priorité ? »), 1 outil = 1 situation (17 et 18 = situations procédurales propres non couvertes par 01/02/14/INAPT-12), réutilise le moteur calendaire (17) et les détecteurs F-DT-13/14 lus côté employeur via PLATFORM-04 (18)
- sujet UX non tranché rappelé : scoring/conclusions ⊂ outils CALCULÉS/persistés uniquement (memory project_coherence_conclusions_outils_non_calcules) — à arbitrer avant dev pour tout scoring dérivé
- directive PO ce run : RUN 3 — viser 90, combler les trous, tenir le dédoublonnage

## 2026-06-06 — création domaine PRICING (Pricing & packaging compte employeur, corporate-readiness)
- mode document vivant D12 : APPEND ; comble le MANQUE #1 du run 2 (aucune feature pricing/packaging dédiée)
- domaine NOUVEAU `PRICING` (préfixe F-DRH-PRICING-) ; socles existants rattachés sans réécriture : F-33 (gate ENRICHED_ANALYSIS, plateforme/Terminée) + F-DRH-API-SIRH-14 (métrage/quota API, marche/Hypothèse)
- ajoutées (9) :
  - F-DRH-PRICING-01 — grille corporate 3-4 paliers 800-3000 €/mois engagement annuel (D9), distincte F-123, feature de référence (marche)
  - F-DRH-PRICING-02 — coexistence des 2 grilles (avocat F-123 ↔ employeur) sélectionnée par type d'acteur, réutilise PLATFORM-01/06/07 (marche)
  - F-DRH-PRICING-03 — packaging features par palier (gating EMPLOYEUR T1/T2/T3), réutilise feature-flags via PLATFORM-07 (marche)
  - F-DRH-PRICING-04 — composante variable métrage (dossiers/analyses enrichies/appels API), réutilise F-33 + usage_events F-257 + API-SIRH-14 ; ⚠️ sujet UX non tranché (compter le calculé/persisté) (marche)
  - F-DRH-PRICING-05 — tableau de consommation employeur (usage vs quota + projection palier), réutilise compteurs API-SIRH-14 (corporate-readiness)
  - F-DRH-PRICING-06 — self-serve pricing calculator opposable procurement (coût vs coût évité), decisionTool=true ; recoupe DASHBOARD-06/CHIFFRAGE-09 → pré-vente paramétrique (marche)
  - F-DRH-PRICING-07 — engagement annuel & cycle de renouvellement (NRR ~130 %), contrat de cycle pas billing infra D11 (marche)
  - F-DRH-PRICING-08 — dépassement de quota : alerte douce/soft-cap/upgrade assisté, jamais coupure dure (D8), cohérent F-33/F-34 (corporate-readiness)
  - F-DRH-PRICING-09 — devis/proforma corporate exportable (palier + variable + engagement), réutilise F-DT-04, alimente CORP-READY-14 (corporate-readiness)
- modifiées (2, cross-référence sans réécriture de scope/statut) : F-33 (socle de métrage), F-DRH-API-SIRH-14 (socle métrage API consommé par PRICING-04/05/08)
- suppressions proposées : aucune (domaine nouveau)
- invariants : D9 (800-3000 €/mois annuel, coexistence F-123), D7 (attribut workspace, pas sélecteur bloquant), D8 (aucun outil contre le salarié par palier ; jamais couper une mise en conformité en cours), D11 (contrats produit, pas billing engine), 1 outil = 1 situation (PLATFORM-07 applique / ce domaine fournit la grille ; ROI post-usage = DASHBOARD-06 ; métrage API = API-SIRH-14)
- directive PO ce run : RUN 3 — combler le trou pricing (D9), tenir le dédoublonnage

## 2026-06-05 — run de maturation
- overall 85/100 (Δ 3) → continue
- ajoutées: 64 features ; modifiées/justifiées et suppressions: voir détail
- directive PO ce run: aucune

## 2026-06-05 — enrichissement domaine ACTES (Génération d'actes & courriers RH conformes)
- mode document vivant D12 : APPEND ; aucune feature existante supprimée ni modifiée (IDs 01→10 préservés)
- ajoutées (4) :
  - F-DRH-ACTES-11 — documents de fin de contrat (certificat de travail + solde de tout compte + attestation France Travail), obligation légale à la rupture, non couverte, aucun concurrent dossier-centric (concurrent-gap)
  - F-DRH-ACTES-12 — génération en lot des actes éco collectif avec critères d'ordre par salarié (besoin marché « fort volume », CNB ~65 %), chaîne F-DT-13/14 (marche)
  - F-DRH-ACTES-13 — traçabilité horodatée + versionnage des actes (acte daté/signé opposable → fiche provision IAS 37 + audit logs RGPD), recoupe corporate-readiness/AI-ACT (corporate-readiness)
  - F-DRH-ACTES-14 — cohérence de la liasse procédurale (séquence d'actes anti-contradiction), au-delà du gate par acte ACTES-10 (concurrent-gap)
- modifiées : aucune
- suppressions proposées : aucune (la suppression existante F-DRH-ACTES-09 reste inchangée)
- invariants : D7 (workspace EMPLOYEUR), D8 (conformité/obligation légale, jamais arme anti-salarié), 1 outil = 1 situation (réutilise F-98/F-DT-04/F-DT-13/14/31/10)
- directive PO ce run : aucune

## 2026-06-05 — run de maturation
- overall 82/100 (Δ 82) → continue
- ajoutées: 117 features ; modifiées/justifiées et suppressions: voir détail
- directive PO ce run: aucune
