# F-158 V4 — Cadrage cohérence (étape 0)

> Refonte du messaging de la landing page V4 — repositionnement « de vos pièces jusqu'aux conclusions déposables » + actualisation du périmètre réel du produit.
> Rattaché à **F-158** (Refonte landing page, statut V3 Terminée → réactivée en vague V4).
> Skill : `ai-skills/feature-coherence-challenger.md`. Date : 2026-06-03.

## Verdict : **GO avec ajustements**

La refonte est fonctionnellement cohérente : **toutes** les capacités qu'on veut mettre en avant correspondent à des features **Terminées** au `PRODUCT_SPEC.md` (pas d'overclaim possible si on s'y tient). L'enjeu n'est pas amont (les briques existent), il est **aval inversé** : le messaging actuel s'arrête à mi-parcours du workflow métier alors que le produit le couvre désormais jusqu'à l'état terminal (rédaction des conclusions). Les ajustements sont des **invariants anti-overclaim / anti-undersell**.

## Intention métier (1 phrase)

Faire que la landing reflète le produit réel de juin 2026 — qui ne se contente plus d'analyser et de pré-remplir des outils, mais accompagne l'avocat **jusqu'au projet de conclusions déposable, dans son style, avec la jurisprudence de Cassation citée** — au lieu du discours « 92 outils décisionnels pré-remplis » figé au 04/05.

## Workflow métier réel de l'utilisateur cible (avocat)

Source : cadrages existants `docs/features/F-98/SF-98-00-coherence.md` + `docs/features/F-243/SF-243-00-coherence.md` (workflow cabinet déjà validé), signaux terrain Renversez (13/05) et Mengue (11/05).

1. L'avocat reçoit un dossier + les pièces du client (PDF, scans, photos, fax administratifs).
2. Il lit et numérise les pièces — y compris des scans dégradés / manuscrits / captures.
3. Il qualifie juridiquement le litige (type de rupture, fondement, partie défendue).
4. Il fait des calculs et porte des décisions chiffrées (indemnités, validité, délais, nullités…).
5. Il cherche la **jurisprudence d'appui** pertinente pour étayer sa position.
6. Il fixe le **stade procédural** (juridiction, stade, position demandeur/défendeur).
7. Il **rédige les conclusions / l'acte** dans son style rédactionnel habituel.
8. Il met en forme, exporte (Word/PDF), relit, fait valider, puis dépose / envoie.
9. Il suit l'évolution procédurale du dossier.

> **État terminal du métier** : un projet de conclusions/acte relu et exportable, prêt à déposer. L'analyse n'est qu'une étape intermédiaire.

## Cartographie features actuelles ↔ workflow

| Étape métier | Feature(s) LegalCase | Statut | Vendu sur landing V3 ? |
|---|---|---|---|
| 1. Réception dossier + pièces | F-43 import dossier | ✅ Livrée | Oui |
| 2. Lecture / numérisation (scans, photos, fax) | F-122 OCR (Legal OCR) + F-148 Vision (Legal Vision) | ✅ Livrée | ✅ Oui (section dédiée) |
| 3. Qualification + synthèse | F-3/4/5 analyse, F-12/13/14 synthèse & questions | ✅ Livrée | ✅ Oui |
| 4. Calculs & décisions chiffrées | ~150-240 outils décisionnels (registre réel ; F-DT/F-FA/F-IM, F-218…) | ✅ Livrée | ⚠️ Oui **mais sous-évalué (« 92 »)** |
| 5. Jurisprudence d'appui | F-JU-01 (citations Cassation/JUDILIBRE) + F-JU-02 (injection auto dans conclusions) | ✅ Livrée | ❌ **Absent** (1 mention indirecte) |
| 6. Stade procédural | F-243 (juridiction + stade + position) | ✅ Livrée | ❌ Absent (implicite) |
| 7. Rédaction conclusions / acte | **F-98 (53 cellules, 3 domaines × 2 pays × juridictions × stades × positions) + style learning** | ✅ Livrée | ❌ **Absent** |
| 8. Export Word/PDF + relecture + versions | F-98 (export .docx/PDF, éditeur, versioning) | ✅ Livrée | ❌ **Absent** |
| 9. Suivi procédural | F-244 (onglet Suivi) | ✅ Livrée | Partiel |

## Position de la nouvelle « feature »

La refonte messaging V4 est **transversale** : elle ne crée pas de brique métier, elle réaligne le **discours** sur les étapes 4 à 8 que le produit couvre désormais et que la landing tait.

## Challenge amont

Pour vendre la rédaction de conclusions (étape 7), il faut que les étapes amont existent : pièces (✅ F-43), lecture (✅ OCR/Vision), analyse (✅), outils décisionnels (✅), stade procédural (✅ F-243), jurisprudence (✅ F-JU-01). **Tous amont livrés.** Aucun trou ❌ amont → aucun pré-requis à créer.

## Challenge aval

L'« output » de la refonte est du discours marketing. Son aval est le parcours de conversion (essai gratuit / démo) — existant (CTA, Calendly, plans Stripe). Aucun trou aval bloquant.

**Risque inverse identifié (cœur du cadrage)** : le messaging V3 s'arrête fonctionnellement à l'étape 4. Il **sous-vend** l'état terminal réel du produit (conclusions + jurisprudence), qui est précisément l'argument à plus forte valeur pour un avocat (temps facturable récupéré, pas seulement de l'analyse).

## STOPs / pré-requis à ajouter au backlog

Aucun STOP. Aucun pré-requis fonctionnel manquant — toutes les features à mettre en avant sont Terminées.

## Invariants anti-gadget pour la mini-spec

1. **Anti-overclaim — règle d'existence** : chaque capacité affichée doit pointer vers une feature **Terminée** au `PRODUCT_SPEC.md`. Interdiction de vendre le pénal (non livré, signal Mengue seulement) ou l'export Word « natif V2 » comme acquis.
2. **Chiffre d'outils sourcé, jamais figé en dur** : le nombre affiché doit être re-dérivé de la source de vérité (registre / `landing-tools-catalog.ts` **régénéré**, périmé depuis le 04/05). Afficher le chiffre réel (~150-240, à figer en mini-spec) ou une formulation arrondie prudente (« 150+ »). Jamais réafficher « 92 » en dur.
3. **Conclusions = « projet à relire », pas « acte parfait en 1 clic »** : reprendre la transparence déjà présente dans le produit (bandeau F-98). Mentionner le périmètre réel : 3 domaines × 2 pays × juridictions × stades × positions, style learning, export Word/PDF, versioning.
4. **Jurisprudence = uniquement le périmètre sourcé** : ne vendre « citations avec autorité » que sur JUDILIBRE/Cassation FR (F-JU-01/02). Ne **pas** suggérer une couverture BE (parkée — cf. `reference_be_jurisprudence_sources`).
5. **Couverture BE honnête** : ne pas suggérer une parité FR/BE non tenue (Famille BE partielle). Formuler la couverture telle qu'elle est.
6. **Claims chiffrés sourcés** : « 10× plus rapide » et « fax 200 dpi » doivent être sourcés (étude/test interne référencé) ou retirés/nuancés.
7. **Pricing inchangé** : SOLO 99 / TEAM 219 / PRO 429 (F-123 V7) — aligné, ne pas y toucher.

## Décision finale

**GO avec ajustements.** La refonte ne crée aucun risque de gadget (zéro brique inexistante mise en avant) ; elle corrige un déficit de représentation. Les 7 invariants ci-dessus encadrent la mini-spec. Passage à l'étape 0 bis (cohérence écran) requis (feature à impact écran).
