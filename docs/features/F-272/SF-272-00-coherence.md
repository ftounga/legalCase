# SF-272-00 — Cadrage cohérence — F-272 Moyens de procédure systématiques & ordre *in limine litis* (art. 74 CPC)

> Étape 0 (CLAUDE.md). Feature transverse de garde de prompt, programme « Conclusions V4 ».
> Verdict : **GO avec ajustements**.

## 1. Workflow métier réel de l'avocat cible

Avocat **du défendeur** (employeur en travail, intimé en appel, défendeur en famille…) qui prépare ses conclusions FR.
En procédure civile française, **l'article 74 CPC** impose que les **exceptions de procédure** (incompétence, nullité de forme/de fond, litispendance, connexité, dilatoires) soient soulevées **in limine litis**, c'est-à-dire **avant toute défense au fond ou fin de non-recevoir**, à peine d'irrecevabilité de l'exception. À l'inverse, les **fins de non-recevoir** (prescription, défaut de qualité/d'intérêt, autorité de chose jugée — art. 122 CPC) peuvent être soulevées en tout état de cause mais se placent, dans l'ossature de l'acte, **avant la défense au fond**.

Ordre canonique d'un jeu de conclusions en défense :
1. **Exceptions de procédure** (art. 73-74 CPC) — *in limine litis*.
2. **Fins de non-recevoir** (art. 122 CPC) — prescription, irrecevabilité.
3. **Défense au fond** — discussion moyen par moyen.
4. **Dispositif** (PAR CES MOTIFS).

Oublier cet ordre = exception de procédure jugée irrecevable = **forclusion = faute professionnelle**. C'est le manque métier #2 de l'audit conclusions 2026-06-12.

## 2. Cartographie des features existantes

| Brique existante | Rôle | Lien F-272 |
|---|---|---|
| `CaseConclusionPromptBuilder.REDACTION_QUALITY_GUARD` (SF-98-55) | garde de qualité commune à TOUTES les cellules (anti-jargon, syllogisme, dispositif, demandes subsidiaires, réfutation adverse, prudence pronostic) | **point d'ancrage** — F-272 ajoute la garde d'ordre *in limine litis* dans ce même mécanisme transverse |
| `JURISPRUDENCE_GUARD` (F-242) | autre garde transverse symétrique appliquée par `buildSystemPrompt` | pattern de référence (garde appliquée par-dessus le prompt de base) |
| `CombinationKey(domain, country, jurisdiction, stage, position)` | clé de cellule passée à `buildSystemPrompt` | porte `position()` (DEFENDEUR/INTIME/DEFENDEUR_POURVOI) et `country()` → **permet de conditionner la garde au défendeur FR** |
| 56 `*PromptProvider` (cellules) | prompt de base par cellule, dont les `*Defendeur*`/`*Intime*` décrivent déjà « réfutation moyen par moyen » | F-272 NE réécrit AUCUN provider — il impose l'ossature **par-dessus**, une fois (directive PO) |
| Outil nullités F-DT-36 (`ProcedureNulliteLicenciementService`, `HarcelementNulliteAnalysis`, `LicenciementNulDetectionResponse`…) | détecte les nullités du licenciement / vices de procédure | « tissage » via le canal EXISTANT : ses verdicts arrivent déjà dans `=== VERDICTS DES OUTILS DÉCISIONNELS REMPLIS ===` du message utilisateur. F-272 demande au prompt de **les positionner au bon endroit** de l'acte (exceptions/FNR), sans nouvel intrant. |

## 3. Challenge de cohérence

**Amont** — les pré-requis existent-ils ?
- La position (défendeur vs demandeur) est connue dès la `CombinationKey`. ✅
- Le pays FR est connu (`country()`). ✅
- Les vices de procédure / nullités sont déjà calculés par F-DT-36 et déjà transmis comme verdicts d'outils. ✅ Aucun nouvel intrant à construire.

**Aval** — la sortie est-elle exploitable ?
- La sortie est le texte des conclusions, déjà éditable (F-264) et récapitulatif (F-271). L'ossature *in limine litis* améliore la conformité de l'acte sans casser le contrat de sortie. ✅

**Anti-gadget / anti-doublon (précédents F-262/F-263 clos à la fondation)** :
- F-272 ≠ nouvel outil décisionnel : un outil = une situation métier (invariant). Ici **aucun outil**, c'est une **garde de prompt**. Pas de collision avec l'invariant.
- F-272 ne duplique pas la garde subsidiaire (point 5 du REDACTION_QUALITY_GUARD) : celle-ci structure le **dispositif** (principal/subsidiaire), F-272 structure la **discussion** (exceptions → FNR → fond). Complémentaires.
- Risque gadget écarté : la garde n'est utile QUE pour une **posture de défense**. La plaquer sur un **demandeur** serait du bruit (un demandeur ne soulève pas d'exception *in limine litis*, il forme une demande). → **Garde conditionnée au défendeur FR**.

## 4. Verdict : GO avec ajustements

**GO.** Saut métier réel (évite une forclusion), coût faible, réutilise un mécanisme transverse éprouvé, zéro nouvel intrant, zéro nouvelle table, zéro endpoint, zéro écran.

**Ajustements imposés à la mini-spec (invariants anti-gadget) :**
1. **Conditionner au défendeur FR** : garde injectée uniquement si `country == FRANCE` ET `position ∈ {DEFENDEUR, INTIME, DEFENDEUR_POURVOI}`. Aucun effet sur les cellules demandeur / BE / titre administratif.
2. **Une seule fois, transverse** : ajout dans `CaseConclusionPromptBuilder` (mécanisme `buildSystemPrompt`), PAS dans les 56 providers. « Construite une fois, déclinée par domaine FR si besoin » (directive PO) — ici un seul texte couvre les 3 domaines FR car l'art. 74 CPC est transverse.
3. **Signalement si non applicable** : la garde dit explicitement à l'IA de **n'ajouter ces sections QUE s'il existe une exception/FNR fondée** par les faits, pièces ou verdicts d'outils ; à défaut, **ne pas créer de rubrique vide** (cohérent avec le style des sections optionnelles existantes — pas de « néant »).
4. **Anti-jargon préservé** (non-régression SF-98-55) : la garde parle de « exceptions de procédure », « fins de non-recevoir », « prescription », jamais d'un code d'outil (« F-DT-36 ») ni d'un score brut.
5. **Tissage F-DT-36 par le canal existant** : aucun nouvel intrant ; la garde demande de **positionner** les vices/nullités fournis au bon rang de l'ossature.

## 5. Pas d'impact écran

Feature purement backend (texte de prompt système). Aucun élément visible nouveau, aucun composant Angular touché, aucune route. → **Étape 0bis (cohérence écran) NON applicable** (exemption CLAUDE.md : feature purement backend sans élément visible nouveau).
