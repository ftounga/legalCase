# F-253 — Cadrage cohérence écran (étape 0 bis)

## Verdict : GO avec ajustements

## Intention métier + comportement visible attendu

Donner un consommateur visuel au statut `À_CREUSER` des risques hors écran synthèse : (a) tile dashboard du dossier qui compte les risques non encore arbitrés et amène vers la section risques de la synthèse, (b) pill grise « 🔍 N à creuser » sur chaque card d'outil décisionnel concernée par un risque à creuser, (c) section dédiée dans l'export PDF.

## Rappel verdict feature-coherence-challenger

**GO** (cf. `SF-253-00-coherence.md`) — workflow métier complet, brique amont F-195 livrée, brique aval (tile / pill / PDF) cohérente avec les patterns existants.

## Parcours écran réel de l'avocat (ouverture du dossier → état terminal)

**Source** : `docs/business/parcours-ecran-*.md` (référentiel constitué incrémentalement) + composants codés `case-file-detail.component.html`, `case-dashboard.component.ts`, `decisional-tools-panel.component.ts`, `synthesis.component.html`.

1. Avocat ouvre `/case-files/:id` → écran **détail dossier** (`case-file-detail.component`).
2. **Onglet « Dashboard »** par défaut : grille de tiles (`case-dashboard.component`) — diagnostic, documents, indemnités, risques (F-195), pistes (F-192), pièces manquantes (F-194), procédure (F-193), riskScore (F-IA-02), etc.
3. Clic sur tile « Risques » (F-195-risques-summary) → navigation `/synthesis#section-risques`.
4. Écran **synthèse** (`synthesis.component`) : section « Risques » avec 3 boutons par risque (🔍 À creuser / ✅ Validé / ❌ Écarté + raison).
5. L'avocat **curate** ses risques, optimistic update + snackbar (UX SF-195-02).
6. Retour à l'écran détail dossier via le breadcrumb / le lien « Voir le dashboard ».
7. Bascule sur l'**onglet « Décision »** (SF-244-02) : panel `<app-decisional-tools-panel>` listant les cards d'outils — chaque card porte les badges F-194 (📎 Pièces V/O/N) + F-195 (⚠️ Risques V/E) + pistes (F-192).
8. Clic sur une card outil → écran outil (calculator/analyzer/generator) avec pré-fill F-246 + flags F-194 / F-195.
9. Retour au dossier, export PDF via bouton « Exporter » de l'écran synthèse — PDF contient sections faits / pistes (F-192) / procédure (F-193) / pièces à demander (F-194) / **risques retenus** (F-195) / chronologie.
10. **État terminal** : export PDF transmis au client, ou génération conclusions (F-243), ou dossier mis en pause / clôturé. Le dossier sort du flux actif.

**État terminal explicite** : l'export PDF est l'état terminal le plus fréquent du parcours « consultation analyse → décision avocat → output exploitable ». Les conclusions F-243 sont l'état terminal supérieur quand le domaine et le type de procédure le permettent.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours écran | Écran / zone LegalCase | Statut |
|---|---|---|
| 1. Ouverture dossier | `case-file-detail.component` | ✅ existant |
| 2. Onglet Dashboard | `case-dashboard.component` grille tiles | ✅ existant |
| 3. Clic tile risques | tile `F-195-risques-summary` → `/synthesis#section-risques` | ✅ existant |
| 4. Section risques synthèse | `synthesis.component` `section-risques` + `<app-synthesis-risques>` | ✅ existant |
| 5. Curation risques | boutons par risque, optimistic update | ✅ existant (SF-195-02) |
| 6. Retour dashboard | onglet Dashboard | ✅ existant |
| 7. Onglet Décision | `<app-decisional-tools-panel>` cards outils | ✅ existant (F-244) |
| 8. Card outil → outil | sections outils Calculator/Analyzer | ✅ existant |
| 9. Export PDF | bouton + `PdfExportService` | ✅ existant |
| 10. État terminal | export / conclusions F-243 | ✅ existant ou en cours |

## Position candidate de la feature

| Sortie F-253 | Écran | Zone | Point d'entrée |
|---|---|---|---|
| Tile dashboard `F-253-risques-a-creuser` | `case-dashboard.component` | grille de tiles, cohabite avec `F-195-risques-summary` | la tile (clic) → `/synthesis#section-risques` |
| Pill grise « 🔍 N à creuser » | `<app-decision-tool-card>` (panel décisionnel) | zone badges existante (à côté de la pill VALIDÉ/ÉCARTÉ F-195) | non cliquable directement (la card l'est déjà) |
| Section « Risques à creuser » | export PDF | bloc dédié | non interactif |

## Challenge placement

**Question : l'emplacement correspond-il à l'étape du parcours où l'avocat a besoin de la feature ?**

- Tile dashboard ✅ : l'avocat arrive sur le dashboard à chaque ouverture de dossier — c'est le bon moment pour lui rappeler qu'il y a des risques non arbitrés.
- Pill cards outils ✅ : l'avocat consulte le panel décisionnel après la synthèse — la pill apparaît quand un outil est concerné par un risque à creuser, c'est cohérent.
- Section PDF ✅ : la zone export PDF agrège déjà les blocs synthèse (F-192/193/194/195) — ajout de « Risques à creuser » avant « Risques retenus » est cohérent.

## Challenge lisibilité de la séquence

**Question : l'UI rend-elle visible l'ordre des étapes ?**

- Ordre temporel attendu par l'avocat : risque détecté IA → curation (À_CREUSER → VALIDÉ / ÉCARTÉ).
- Tile F-253 (À_CREUSER) cohabite avec tile `F-195-risques-summary` (VALIDÉ/ÉCARTÉ). Pour ne pas créer de confusion, leurs **libellés / sous-titres** doivent expliciter la complémentarité : F-253 = « risques à arbitrer », F-195 = « risques arbitrés ». L'avocat doit comprendre en lisant les 2 tiles ensemble.
- Pill grise À_CREUSER vs pill VALIDÉ/ÉCARTÉ sur card : la pill À_CREUSER doit être **visuellement secondaire** (gris navy ≤ 60 % opacité, picto 🔍) face aux pills primaires VALIDÉ/ÉCARTÉ. Pas de hiérarchie inversée.
- Ajustement à intégrer en mini-spec : titre tile = « Risques à arbitrer » et **sous-titre** = « N risques à arbitrer · X validés · Y écartés » pour faire le lien explicite.

## Challenge charge écran

**Question : quelle est la densité TOTALE de l'écran cible APRÈS ajout ?**

### Dashboard (`case-dashboard.component`)

État actuel : grille de tiles dynamiques par dossier (tiles s'affichent si pertinentes). Tiles existantes pertinentes pour les risques :
- `F-IA-02-risk-score` (score risque IA brut + score avocat dual)
- `F-195-risques-summary` (statut VALIDÉ/ÉCARTÉ, alertLevel selon keyword critique)

Ajout F-253 : 1 tile supplémentaire **MAIS** masquée si compteur = 0. En pratique, sur un dossier en cours d'arbitrage, l'avocat a déjà tagué quelques risques → mix VALIDÉ/ÉCARTÉ/À_CREUSER. La tile F-253 sera visible mais cohabite avec F-195-risques-summary (qui n'apparaît que quand au moins un VALIDÉ/ÉCARTÉ existe). La situation extrême « tous À_CREUSER » est aussi la situation initiale post-analyse, parfaitement légitime.

**Évaluation** : charge supportable. L'ajout est dynamique (tile masquée si non pertinente). Pas de saturation.

### Panel décisionnel (`<app-decisional-tools-panel>`)

État actuel : grid de cards `<app-decision-tool-card>`. Chaque card porte déjà :
- Titre + description outil
- Badge F-192 pistes (si pertinent)
- Badge F-194 pièces (V/O/N + chip détaillée)
- Badge F-195 risques (V/E + variantes 5 kinds)

Ajout F-253 : 1 pill supplémentaire grise « 🔍 N à creuser » entre le badge F-194 et le badge F-195. **À surveiller** : si une card affiche 4 badges simultanés (pistes + pièces + risques validés + risques à creuser), risque de surcharge visuelle. Mitigation : pill À_CREUSER en variante **subtile** (gris ≤ 60 % opacité, plus petite que VALIDÉ).

**Ajustement à intégrer en mini-spec** : layout des badges sur la card (taille, ordre, espacement) à vérifier visuellement avant merge frontend. Tester avec card qui porte les 4 badges simultanés.

### Export PDF

État actuel : sections faits → pistes → procédure → pièces à demander → risques retenus → chronologie. Ajout d'une section « Risques à creuser » entre « pièces à demander » et « risques retenus » — bloc dédié, pas d'impact charge écran (PDF a pas de contrainte d'espace équivalente).

## Challenge état final / continuité

**Question : après l'output de la feature, que fait l'avocat ?**

- Clic tile F-253 → écran synthèse section risques → l'avocat arbitre (À_CREUSER → VALIDÉ/ÉCARTÉ) → re-run synthèse enrichie → tile F-253 décrément, tile F-195-risques-summary incrément.
- Pill sur card outil → l'avocat clique sur la card → écran outil → utilise l'outil (calculator/analyzer/generator). La pill l'informe qu'il y a un risque potentiellement pertinent à creuser pour cet outil.
- Section PDF → transmise au client ou utilisée pour préparer les conclusions F-243.

**Continuité OK** : chaque output mène à une action concrète, pas de dead-end ni de ping-pong subi. L'état terminal du parcours reste l'export PDF / les conclusions.

## Ajustements IA requis

1. **Libellé tile F-253 et tile F-195-risques-summary** : harmoniser pour montrer la complémentarité. Proposition :
   - F-253 → titre « Risques à arbitrer » · sous-titre « N risques à creuser ».
   - F-195-risques-summary → titre conservé « Risques » · sous-titre « X validés · Y écartés ».
2. **Visibilité tile F-253** : masquée si compteur À_CREUSER = 0. Pas de tile « tous arbitrés ✅ » (anti-bruit — F-195-risques-summary suffit pour montrer l'arbitrage final).
3. **Layout pill sur card outil** : pill À_CREUSER en variante subtile (gris navy ≤ 60 % opacité, picto 🔍), placée entre les pills F-194 et F-195. Tester visuellement avec 4 badges simultanés.
4. **Ordre PDF** : section « Risques à creuser » insérée AVANT « Risques retenus » (logique : indécision avant décision).
5. **Pas de couleur rouge** : palette gris navy uniquement pour la pill et la tile (le rouge `#C0392B` reste réservé à `validated_critical` F-195).

## Invariants anti-surcharge pour la mini-spec

1. **Tile masquée si compteur = 0** : aucune apparition « tous arbitrés ✅ », F-195-risques-summary couvre déjà l'état post-arbitrage.
2. **Pill subtile** : gris navy ≤ 60 % opacité, picto 🔍 (loupe = recherche/exploration), pas de rouge ni d'or.
3. **Pas de doublon dans les compteurs** : F-253 = `À_CREUSER` uniquement, F-195 = `VALIDÉ`/`ÉCARTÉ` uniquement. Vérifier en test que la somme F-253 + F-195 = total risques.
4. **Pas de cliquabilité concurrente sur la card** : la pill ne déclenche pas d'action distincte (la card mène à l'outil, c'est le seul comportement clic).
5. **Layout testé avec 4 badges** : avant merge frontend, capture d'écran d'une card avec pistes + pièces (V/O/N) + risques validés (V/E) + risques à creuser. Si surcharge visuelle, repli sur affichage des badges les plus prioritaires (ex : masquer À_CREUSER si VALIDÉ critique présent).
6. **Section PDF non bloquante** : si compteur À_CREUSER = 0, la section PDF n'apparaît pas (cohérent avec la tile dashboard).

## Décision finale

**GO avec ajustements** — placement, séquence, charge, continuité tous cohérents, avec 5 ajustements à intégrer en mini-spec (libellés tiles, masquage si compteur=0, layout pill, ordre PDF, palette).

## MAJ apportée au parcours écran de référence

À enrichir dans `docs/business/parcours-ecran-decision-risques.md` (à créer si absent) : étape 5 du parcours (curation risques) génère désormais trois sorties parallèles : (a) re-run synthèse enrichie (déjà F-195), (b) tile dashboard À_CREUSER (F-253), (c) pill cards outils À_CREUSER (F-253), (d) section PDF À_CREUSER (F-253). L'avocat dispose d'un rappel persistent du travail de curation restant, sans être obligé de revenir sur l'écran synthèse.
