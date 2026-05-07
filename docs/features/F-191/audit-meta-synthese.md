# Méta-audit — Couverture exhaustive des 6 country×domain

**Date** : 2026-05-06
**Périmètre** : 3 domaines (Travail / Immigration / Famille) × 2 pays (FR / BE) = 6 country×domain
**Méthode** : pour chaque C×D, audit juridique exhaustif depuis les sources nationales (pas miroir FR↔BE), inventaire DB + frontend, audit F-166 (outils ALWAYS_ON candidats CONTEXTUAL + flags IA manquants).
**Sources individuelles** :
- `docs/features/F-191/audit-be-travail-exhaustif.md` (Travail BE)
- `docs/features/F-191/audit-travail-fr-exhaustif.md` (Travail FR)
- `docs/features/F-191/audit-immigration-fr-exhaustif.md` (Immigration FR)
- `docs/features/F-191/audit-immigration-be-exhaustif.md` (Immigration BE)
- `docs/features/F-191/audit-famille-fr-exhaustif.md` (Famille FR)
- `docs/features/F-191/audit-famille-be-exhaustif.md` (Famille BE)

> **⚠️ Note de renumérotation post-audit (2026-05-07)** : ce document parle d'un plan « F-192 à F-200 » à valeur pédagogique (Section 6). Au moment du commit définitif dans `docs/PRODUCT_SPEC.md`, les numéros F-192 à F-197 étaient déjà utilisés par 6 features Terminées le 2026-05-06 (F-192 propagation pistes stratégiques, F-193 matérialisation F-96, F-194 pièces markables, F-195 risques markables, F-196 questions complémentaires, F-197 override type_litige). Le plan définitif a donc été décalé de +6 et utilise **F-198 à F-225** (28 features). Voir la section dédiée `### Couverture juridique exhaustive — F-198 à F-225` dans `PRODUCT_SPEC.md`. Les références « F-192/F-193/... » ci-dessous restent en l'état pour préserver le caractère historique de l'audit.

---

## 1. Tableau global — couverture juridique réelle

| Country×Domain | Outils existants | Situations identifiées | Manquants | Couverture |
|---|---|---|---|---|
| **Travail FR** | 28 | 97 | 75 | **23 %** |
| **Travail BE** | 12 | 70 | ~60 | **17 %** |
| **Immigration FR** | 17 | ~110 | ~95 | **15 %** |
| **Immigration BE** | 9 | ~75 | ~66 | **12 %** |
| **Famille FR** | 35 | ~90 | 55 | **39 %** |
| **Famille BE** | 5 | ~93 | ~88 | **5 %** |
| **TOTAL** | **106** | **~535** | **~440** | **~20 %** |

### Lecture

- **L'app couvre aujourd'hui 1/5 des outils décisionnels nécessaires** sur l'ensemble des 6 country×domain.
- **Famille BE est le pire** : 5 % de couverture (5 outils sur ~93 situations), alors qu'on a déjà des avocats BE qui pourraient consommer Famille BE.
- **Famille FR semble la moins mauvaise** (39 %) mais c'est un trompe-l'œil — voir section 2 (gap F-166).
- Tous les domaines ont une couverture < 40 % — ce n'est plus une dette ponctuelle, c'est un **déficit produit transversal**.

---

## 2. Audit F-166 — le problème était systémique, pas Travail-FR-only

F-166 (livrée 2026-05-06) a basculé 8 outils Travail FR ALWAYS_ON → CONTEXTUAL et ajouté 8 flags IA pour driver la contextualisation. **Le même problème existe sur les 5 autres C×D**.

| Country×Domain | Outils ALWAYS_ON candidats CONTEXTUAL | Flags IA à introduire | Réduction du bruit panel F-IA-04 |
|---|---|---|---|
| Travail FR | 0 (F-166 a déjà tranché) | +24 flags pour P1+P2 manquants | déjà fait |
| Travail BE | TBD (audit BE focalisé exhaustivité — pas section F-166 dédiée) | TBD | TBD |
| Immigration FR | **10** | 9 nouveaux | **−71 %** (14 → 4 cards) |
| Immigration BE | **5** | 5 nouveaux + 15 pour MANQUE futurs | ~−56 % (9 → 4) |
| Famille FR | **31** | ~30 nouveaux | **−91 %** (34 → 3 cards) |
| Famille BE | toutes les ALWAYS_ON BE (les 5) | 21 nouveaux | drastique |

### Lecture

- **Le gap F-166 le plus grave est sur Famille FR** (31 outils mal classés sur 34 ALWAYS_ON, soit ~91 % du panneau). Un avocat Famille FR voit aujourd'hui 34 outils par défaut sur n'importe quel dossier — la majorité non pertinents.
- **Total estimé : ~67 outils à recontextualiser** sur les 5 autres C×D (hors Travail FR).
- **Total estimé : ~85 nouveaux flags IA** à introduire dans `TravailExtractedData`, `ImmigrationExtractedData`, `FamilleExtractedData` (et leurs prompts Sonnet niveau 3).
- **Conclusion honnête** : F-166 n'a corrigé qu'**1/6 du problème de bruit visuel**. Le reste est latent, invisible parce qu'il n'y a pas eu de plainte utilisateur (peu d'avocats Famille/Immigration en prod).

---

## 3. Outils domaine-only sans équivalent transverse (preuve d'indépendance)

Synthèse des spécificités nationales identifiées par les audits — **invalide définitivement le frame "miroir FR↔BE"**.

| Domaine | FR-only | BE-only |
|---|---|---|
| Travail | Conseil de prud'hommes, barème Macron, RTT, forfait jours, France Travail (ARE/CSP), CARSAT, AT/MP code SS, abandon poste présumé démission (loi 2022) | RCC ex-prépension, crédit-temps, outplacement obligatoire 45+, formule Claeys, ONEM/C4, Fedris, FFE, statut unique 2014, CCT 32bis, élections sociales, code pénal social, auditorat, deal pour l'emploi 2022 |
| Immigration | OFPRA/CNDA, AES métiers tension (loi 2024), ANEF, JLD, CRA, ADA, MNA ASE, CRRV, accords bilatéraux Algérie/Tunisie/Maroc/Sénégal, Mayotte, étranger malade L. 425-9, victime traite L. 425-1 | OE/CCE/CGRA, annexes administratives 8/9bis/9ter/40bis/40ter/13/14/26/35, AESM tutelle, Single permit, Carte H Brexit, Carte F UE, instruction 2009 régularisation |
| Famille | JAF, ARIPA, BAR bracelet anti-rapprochement, prestation compensatoire, convention parentale notariée 2017, tribunal pour enfants/AED/AEMO/OPP, ASF CAF, TGD, divorce CM notarié | Tribunal famille (TF) unique, DDI 3 voies, pacte successoral admis 2018, cohabitation légale, méthode Renard, notaire commis, régime kafala, vide juridique GPA, mandat extra-judiciaire (loi 2013) |

**Total : 60+ concepts spécifiques à un seul pays** sur les 6 C×D — preuve qu'on ne peut pas faire l'économie d'un audit juridique national, le miroir mécanique ne fonctionne pas.

---

## 4. Top 30 outils manquants prioritaires (synthèse cross-domain)

Sélection multicritère P1 (urgence procédurale) + P2 (fréquence haute) + spécificité nationale forte. Hiérarchisée par impact métier immédiat.

| Rang | Outil | C×D | Priorité | Justification |
|---|---|---|---|---|
| 1 | `prescription-be-litige-travail` | Travail BE | P1 transversal | Forclusion 1 an post-rupture, irréversible |
| 2 | `c4-onem-checklist` + `contestation-c4-onem` | Travail BE | P1 BE-only | Exclusion ONEM 4-52 sem si C4 mal rempli |
| 3 | `at-fedris-declaration` | Travail BE | P1 BE-only | 8 jours pour déclarer, sinon préjudice salarié |
| 4 | F-DT-42 abandon-poste-detecte | Travail FR | P1 | Loi 21/12/2022 — présomption démission |
| 5 | F-DT-75 CP pendant arrêts maladie | Travail FR | P1 | Loi 22/04/2024 — acquisition CP |
| 6 | `pension-alimentaire-fr` | Famille FR | P1 (DELETE 191) | Régression connue à rattraper |
| 7 | `prestation-compensatoire-fr` | Famille FR | P1 (DELETE 191) | Régression connue |
| 8 | `liquidation-communaute-fr` | Famille FR | P1 (DELETE 191) | Régression connue |
| 9 | `JLD-retention-administrative` | Immigration FR | P1 | Délai 24-48h, irréversible |
| 10 | `Dublin-recours-7j` | Immigration FR | P1 | Délai 7 jours suspensif |
| 11 | `oqt-annexe13-be` | Immigration BE | P1 BE-only | Annexe 13 = OQT BE |
| 12 | `9bis-procedure-be` | Immigration BE | P1 P3 BE-only | Régularisation humanitaire BE |
| 13 | `9ter-medicale-be` | Immigration BE | P1 BE-only | Séjour médical BE |
| 14 | `refere-tribunal-travail-be` | Travail BE | P1 | Mesures provisoires |
| 15 | `rcc-be-conditions` + `rcc-be-indemnite` | Travail BE | P1 BE-only | RCC ex-prépension |
| 16 | `outplacement-be-obligatoire-45` | Travail BE | P1 BE-only | Sanction 1 800 € |
| 17 | F-DT-39 prise-acte-rupture | Travail FR | P2 | CPH fréquent |
| 18 | F-DT-40 résiliation-judiciaire | Travail FR | P2 | CPH fréquent |
| 19 | `divorce-dc-be` | Famille BE | P1 BE-only | DC consentement mutuel BE |
| 20 | `divorce-ddi-3voies-be` | Famille BE | P1 BE-only | 3 voies DDI |
| 21 | `pacte-successoral-be-2018` | Famille BE | P3 BE-only | Admis 2018, demandé |
| 22 | `tribunal-famille-be-mesures-provisoires` | Famille BE | P1 BE-only | TF référé urgence |
| 23 | `CRRV-refus-visa` | Immigration FR | P2 | Recours visa FR |
| 24 | `victime-violences-L425-6` | Immigration FR | P2 | Titre violences conjugales |
| 25 | `regroupement-40ter-detecte` | Immigration BE | P2 BE-only | Regroupement enfant Belge |
| 26 | F-DT-50 forfait-jours-validite | Travail FR | P2 | Cas fréquent cadres |
| 27 | F-DT-72 transfert-entreprise-L1224-1 | Travail FR | P2 | Transfert FR |
| 28 | `mediation-familiale-obligatoire` | Famille FR | P2 | Loi 18/11/2016 |
| 29 | `acceptation-succession` | Famille FR | P2 | Manquant majeur |
| 30 | `clause-non-concurrence-be` | Travail BE | P2 BE-only | Régime CCT 13 |

---

## 5. Estimation effort total — recadrage F-191

### Échelle réaliste

- **~440 outils manquants** sur les 6 C×D
- **~67 outils ALWAYS_ON à recontextualiser** + ~85 flags IA à introduire (extension F-166 multi-domaines)
- **5 outils Famille FR DELETE 191 à rattraper en urgence** (régression silencieuse)
- **Rythme observé** : ~1 outil = 2 SF (back+front parallèles) = 1-2 jours de dev

### Implication

| Scope | Périmètre | SF estimées | Calendrier réaliste | Couverture finale |
|---|---|---|---|---|
| **Z0 — DELETE 191 rattrapage seul** | 5 outils Famille FR | 5 SF | 1 semaine | reste 20 % |
| **A — Top 10 (urgences procédurales)** | 10 outils transverses | 20 SF | 2-3 semaines | ~22 % |
| **B — Top 30 + 5 DELETE 191** | 35 outils + 5 rattrapages | 70 SF | 6-8 semaines | ~28 % |
| **C — F-166 systémique 5 C×D + Top 30** | + recontextualisation 67 outils + 85 flags + 35 outils Top 30 | ~110 SF | 3-4 mois | ~28 % couverture **mais** −85 % bruit visuel partout |
| **D — Exhaustivité totale** | 440 outils + F-166 systémique | ~900 SF | 9-12 mois | ~95 % |

### Observations

- **Z0 + A est non négociable** (urgences procédurales + régressions DELETE 191). 25 SF, 3-4 semaines.
- **C est le meilleur ratio valeur/effort** : −85 % de bruit visuel sur l'app + Top 30 outils prioritaires = **app utilisable et propre sur 6 C×D** en 3-4 mois.
- **D (exhaustivité totale) bloque le reste du backlog** pour 9-12 mois — peu réaliste tant qu'il n'y a pas signal commercial fort sur tous les C×D.

---

## 6. Recommandation

### Étape 1 — Rattrapage immédiat (~1 semaine)
**F-192 — DELETE 191 Famille FR** : restaurer les 5 outils supprimés (`pension-alimentaire-fr`, `prestation-compensatoire-fr`, `liquidation-communaute-fr`, `divorce-CM-scoring`, `fourchettes-jaf`). Rattrapage de régression silencieuse.

### Étape 2 — F-166 systémique (~6-8 semaines)
**F-193 — F-166 généralisée 5 C×D** : un sous-feature par C×D :
- SF-193-01 — F-166 Famille FR (31 outils + 30 flags) — **le plus gros gain ergonomique**
- SF-193-02 — F-166 Immigration FR (10 outils + 9 flags)
- SF-193-03 — F-166 Famille BE (5 outils + 21 flags)
- SF-193-04 — F-166 Immigration BE (5 outils + 5 flags + 15 pour futurs MANQUE)
- SF-193-05 — F-166 Travail BE (audit dédié à compléter, ~5 outils + 8-10 flags estimés)
- SF-193-06 — F-166 Travail FR extension (24 flags additionnels pour P1+P2)

Total : ~85 nouveaux flags IA + ~67 outils recontextualisés. Effet attendu : panneau F-IA-04 lisible et pertinent sur les 6 C×D.

### Étape 3 — Top 30 outils prioritaires (~6-8 semaines en parallèle de l'étape 2)
**F-194 à F-200 — Top 30 (P1 + P2 cross-domain)** par paquets cohérents :
- F-194 Travail FR P1 : abandon poste, CP arrêts maladie, prise d'acte, résiliation judiciaire (8 SF)
- F-195 Travail BE P1 : prescription, C4, contestation C4, AT Fedris, RCC, outplacement, référé TT (14 SF)
- F-196 Immigration FR P1 : JLD rétention, Dublin, CRRV, victime violences L. 425-6 (8 SF)
- F-197 Immigration BE P1 : annexe 13, 9bis, 9ter, regroupement 40ter (8 SF)
- F-198 Famille FR P1 : médiation obligatoire, acceptation succession (4 SF)
- F-199 Famille BE P1 : DC, DDI 3 voies, TF référé, pacte successoral (8 SF)

Total : ~50 SF, parallélisable par paquets domain-pays.

### Étape 4 — Garde-fou CLAUDE.md
Ajouter une règle de blocage automatique :

```
| Modification de `decision_tool_visibility_rules` (INSERT/UPDATE) sans
audit "Impact F-166 cross-C×D" rempli dans la mini-spec | REFUS — chaque
modification de la table de visibilité doit (1) lister les outils impactés
sur les 6 country×domain, (2) confirmer pour chacun layer ALWAYS_ON ou
CONTEXTUAL, (3) lister les flags IA nécessaires si CONTEXTUAL, (4) confirmer
que les flags sont alimentés par le prompt LLM correspondant. Évite
l'accumulation silencieuse de bruit visuel observée sur 5/6 C×D
(découverte audit 2026-05-06). |
```

Et :

```
| Livraison feature qui ajoute un outil décisionnel `legal_domain=X`
`country='FR'` (ou BE) sans audit explicite "exhaustivité du droit
national X-FR/BE" rempli dans la mini-spec | REFUS — chaque feature qui
seede un nouveau tool_id doit citer la source juridique exhaustive (loi,
article) ET confirmer si un outil jumeau dans l'autre pays existe ou doit
être ouvert au backlog. Évite la dette d'asymétrie observée sur les 60+
concepts domaine-only (audit 2026-05-06). |
```

---

## 7. Sujets ouverts / décisions à prendre

1. **Validation Top 30** : ordre OK, ou tu réordonnes en fonction d'avocats actifs sur certains C×D ?
2. **Validation références "à vérifier"** : chaque audit individuel cite des lois/articles à valider par avocat national. Tu veux relire les 6 audits, ou validation au fil de l'eau par vague ?
3. **Choix scope** : Z0+A (1-3 semaines) / C (3-4 mois) / D (9-12 mois) ?
4. **Découpages à éclater** : audit Famille FR identifie 8 outils à scinder (adoption en 4, divorce CM, etc.). Audit Famille BE recommande DDI BE en 3 voies. À inclure dès Top 30 ou différer ?
5. **Garde-fou CLAUDE.md** : OK pour ajouter les 2 règles de blocage proposées section 6 ?
6. **F-191 vs F-192/193/194+** : F-191 actuellement = "Travail BE jumeau F-166". Vu l'élargissement, faut-il :
   - (a) Renommer F-191 en F-191 = "Audit cross-C×D + plan de rattrapage" (méta-feature) ?
   - (b) Garder F-191 = Travail BE et créer F-192-200 selon plan section 6 ?

---

## 8. Conclusion honnête

L'audit révèle **3 trous structurels masqués** :

1. **Couverture juridique** : ~20 % seulement, et pas par paresse — par sous-cadrage systématique des features (anchoring sur le symptôme observé, pas sur l'exhaustivité juridique).
2. **F-166** : était présentée comme "fix Travail FR" alors qu'**elle aurait dû être systémique**. Le panel F-IA-04 est saturé sur Famille FR (34 cards) et Immigration FR (14 cards) — utilisateurs probablement gênés sans le verbaliser.
3. **Asymétrie FR↔BE** : 60+ concepts spécifiques à un seul pays jamais reconnus comme tels — la règle "un outil = une situation" doit s'appliquer aussi à "une situation peut être nationale, donc pas d'équivalent miroir".

**Cause racine commune** : ancrage sur le périmètre observé (un domaine, un pays, une feature) au lieu d'un audit transverse systématique avant cadrage. C'est exactement le pattern des feedback `feedback_belgique_never_forget`, `feedback_audit_obligatoire_avant_vague`, `feedback_pas_de_reduction_scope_silencieuse` — tous violés ici.

L'utilisateur a maintenant le choix entre rattraper massivement (option C, 3-4 mois pour app propre et lisible) ou continuer en mode tactique (option Z0+A, 1-3 semaines pour les urgences). La règle CLAUDE.md proposée à la section 6 garantit que ce trou ne se ré-accumule pas après le rattrapage.
