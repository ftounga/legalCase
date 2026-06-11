# VIDÉO 7 — 1 minute 25 secondes — "La jurisprudence ne s'invente pas. Elle se vérifie."

(Angle : fonctionnalité phare **vérification de jurisprudence** — à l'ère des arrêts hallucinés par les IA généralistes, LegalCase fait l'inverse : il **passe au crible les citations adverses** contre la base officielle de la Cour de cassation, **ne cite jamais un arrêt qu'il n'a pas vérifié**, et **renvoie l'avocat vers ses propres outils de recherche** — sans prétendre les remplacer. Mécaniques : (1) audit des conclusions adverses sur la page Synthèse [F-179], (2) pont vers les éditeurs juridiques depuis les points juridiques [F-241], (3) jurisprudence applicable curatée et intégrée aux conclusions [F-JU-01/02], le tout sous l'invariant « silence plutôt qu'erreur » [F-JU-06]. **Positionnement clé : LegalCase n'est PAS un moteur de recherche jurisprudentielle.** Dossier de simulation : LEMAIRE — licenciement, convention SYNTEC, droit du travail FR. Même série visuelle que les vidéos 1 à 6.)

---

## [0s–11s] — ACCROCHE

▎ Voix off :
▎ "En 2023, un avocat dépose six arrêts à l'appui de ses conclusions."
▎ "Six arrêts inventés de toutes pièces par une IA. Sanction."
▎ "LegalCase prend le problème par l'autre bout."

Visuel : fond sombre. Une page de conclusions s'affiche, six références jurisprudentielles surlignées une à une. Sur "inventés de toutes pièces" → chaque référence se fissure et tombe en poussière, un tampon rouge "HALLUCINATION" s'abat. Sur "l'autre bout" → bascule franche sur l'interface LegalCase, dossier LEMAIRE ouvert. **Aucun logo ni nom d'outil tiers** — l'écran fautif est une UI neutre générique.

---

## [11s–25s] — SUR LA SYNTHÈSE : L'ADVERSAIRE CITE, LEGALCASE CONTRÔLE

▎ Voix off :
▎ "Ouvrez la synthèse du dossier. Tout en bas, une section : « Jurisprudences citées »."
▎ "Les conclusions adverses citent cinq arrêts. LegalCase les vérifie un par un — existence réelle, et fidélité de la position qu'on leur prête."

Visuel : on arrive sur la **page Synthèse** du dossier LEMAIRE (URL discrète `/synthesis` visible un instant). Scroll fluide jusqu'à la section **« Jurisprudences citées »** (icône marteau) — le titre de section est mis en évidence pour bien ancrer *où* ça se passe. Les cinq références détectées dans `04-conclusions-salarié.pdf` s'alignent, chacune recevant un badge en cascade :

▎ Cass. soc. 11 mai 2022, n° 21-14.490 — ✅ **Vérifiée**
▎ Cass. soc. 12 déc. 2000, n° 98-41.609 — ✅ **Vérifiée**
▎ Cass. soc. 30 nov. 1990, n° 88-44.308 — ✅ **Vérifiée**
▎ Cass. **civ. 2e** 11 mai 2022, n° 21-14.490 — ⚠️ **Suspecte**
▎ Cass. soc. **30 février 2021**, n° 99-99.999 — ❌ **Non trouvée**

Compteur de section : "5 citations · 3 vérifiées · 1 suspecte · 1 introuvable".

---

## [25s–37s] — LES DEUX FAILLES (la munition de l'avocat)

▎ Voix off :
▎ "Deux d'entre elles ne tiennent pas. Et c'est précisément là qu'est votre argument."

Visuel : zoom sur les deux références problématiques, chacune dépliée en carte explicative.

**Carte ❌ — l'arrêt qui n'existe pas** (`99-99.999`)
▎ "Cour de cassation, chambre sociale, **30 février 2021**."
▎ Verdict : référence introuvable — **le 30 février n'existe pas**.
▎ Mention : *probable hallucination — à soulever en défense.*

**Carte ⚠️ — l'arrêt détourné** (`civ. 2e 21-14.490`)
▎ "L'arrêt existe — mais pas dans cette chambre, et il ne dit pas ce que l'adversaire lui fait dire."
▎ Verdict : **position alléguée incohérente** avec le contenu réel.

Bandeau bas : **"2 citations adverses fragiles, identifiées avant l'audience."**

---

## [37s–46s] — LA SOURCE FAIT AUTORITÉ

▎ Voix off :
▎ "Une citation n'est confirmée que si elle existe vraiment — dans la base officielle de la Cour de cassation. Pas une recherche web approximative."

Visuel : sur les trois badges « Vérifiée », un petit sceau apparaît : **"Confirmé — source officielle Cour de cassation"**. L'avocat (curseur) clique sur `n° 98-41.609` → un nouvel onglet s'ouvre sur la décision réelle. Retour à LegalCase en 1 seconde. Aucun lien mort, aucune page d'erreur.

---

## [46s–59s] — LE PONT VERS VOS OUTILS DE RECHERCHE (et ce que LegalCase n'est PAS)

▎ Voix off :
▎ "Soyons clairs : LegalCase n'est pas un moteur de recherche jurisprudentielle. Il ne remplace ni vos bases, ni vos éditeurs."
▎ "Il vérifie ce qui est cité dans votre dossier — et, sur chaque point juridique, il vous ouvre vos outils, la recherche déjà saisie."

Visuel : page **Points juridiques** (`/synthesis/points-juridiques`). Sous un point juridique, la ligne **« Rechercher la jurisprudence : »** apparaît avec trois boutons (icône loupe) : **« Doctrine »**, **« Lexis+ »**, **« Lextenso »**. Le curseur clique « Doctrine » → un nouvel onglet s'ouvre avec la requête **pré-remplie** (mots-clés du point juridique). Retour à LegalCase. Bandeau bas, en ton sobre : **"LegalCase vérifie et vous connecte. La recherche reste la vôtre."**

---

## [59s–71s] — VOTRE JURISPRUDENCE, ELLE AUSSI VÉRIFIÉE

▎ Voix off :
▎ "Et quand LegalCase outille votre dossier, chaque outil décisionnel cite la jurisprudence qui le fonde — vérifiée, rattachée, reprise dans vos conclusions."

Visuel : onglet « Décision », dossier LEMAIRE. La tuile **« Ancienneté et congés »** est calculée. Sous le résultat, le bloc **« Jurisprudence applicable »** se déploie : un arrêt de la chambre sociale, chapeau officiel cité textuellement, lien source. Puis la **section conclusions** se génère : la rubrique « Jurisprudence applicable » reprend automatiquement ces arrêts, au bon point juridique. Mention discrète conservée : *« citation indicative — l'avocat reste seul juge »*.

Bandeau bas : **"Des outils au projet de conclusions — la jurisprudence suit, sans copier-coller."**

---

## [71s–80s] — L'INVARIANT : LE SILENCE PLUTÔT QUE L'ERREUR

▎ Voix off :
▎ "Et quand LegalCase n'est pas certain, il ne meuble pas. Il se tait. Mieux vaut aucune citation qu'une mauvaise."

Visuel : split rapide. À gauche, un outil affiche un arrêt pertinent. À droite, un autre outil affiche un encart sobre : **"Aucune jurisprudence vérifiée pour cet outil"** — pas d'arrêt plaqué. Une référence hors-sujet tente d'apparaître puis est barrée par un filet : **"écartée — sans rapport avec la situation"**.

---

## [80s–85s] — SIGNATURE

▎ Voix off :
▎ "La jurisprudence ne s'invente pas. Elle se vérifie."

Visuel : fade vers fond noir. La phrase apparaît en deux temps : "**La jurisprudence ne s'invente pas.**" puis "**Elle se vérifie.**" Logo LegalCase en bas. Tagline secondaire en petits caractères : "Citations adverses contrôlées · Jurisprudence applicable vérifiée · Vos outils de recherche en un clic".

---

## Notes de réalisation

- **Pacing** : dense mais narratif (on attaque l'adversaire, on cadre ce que LegalCase n'est pas, puis on sécurise son propre dossier). Respiration ≥ 0,5 s entre blocs.
- **Cohérence visuelle série** : header LegalCase, code couleur des badges, polices Inter / JetBrains Mono **strictement identiques** aux vidéos 1 à 6. Mêmes nappes orchestrales sobres. La vidéo 7 doit être immédiatement reconnaissable comme appartenant à la série.
- **Ancrage écran** : on montre explicitement *où* vivent les fonctionnalités — la section « Jurisprudences citées » **sur la page Synthèse** (bloc 11–25s), et les boutons éditeurs **sur la page Points juridiques** (bloc 46–59s). L'avocat doit pouvoir retrouver chaque écran après la vidéo.
- **Code couleur des badges (réutiliser l'existant)** : vert ✅ « Vérifiée », orange ⚠️ « Suspecte », rouge ❌ « Non trouvée » (rouge réservé à l'échec dur), gris « Incertaine » (non montré ici pour garder le message net).
- **Moments forts à préserver** :
  1. Les six arrêts qui tombent en poussière (0–11s) — accroche choc.
  2. Le « 30 février 2021 » mis en évidence comme hallucination flagrante (25–37s).
  3. Le sceau « source officielle Cour de cassation » + le clic qui ouvre la vraie décision (37–46s).
  4. Le cadrage « LegalCase n'est pas un moteur de recherche » + le bouton éditeur pré-rempli (46–59s) — **rassure le Barreau, désamorce le « il veut nous remplacer »**.
  5. L'encart « il se tait » (71–80s) — contre-pied anti-hallucination, signature de marque.
- **Positionnement (capital — ne pas aliéner le Barreau)** : la vidéo dit **explicitement** que LegalCase n'est pas un outil de recherche jurisprudentielle et ne remplace pas Doctrine / Lexis / Lextenso. LegalCase **vérifie** (citations adverses), **rattache** (jurisprudence d'appui aux outils) et **connecte** (vers les éditeurs). L'avocat reste **seul juge** et garde **ses** outils de recherche. La voix off ne dit jamais « LegalCase plaide » ni « remplace l'avocat ».
- **Différenciation concurrentielle** : l'accroche évoque l'hallucination IA (cas réel type Mata v. Avianca, **jamais nommé**), une seule fois. Le reste est démonstratif, pas comparatif.
- **Conformité copy — point à valider PO** : la feature F-241 affiche réellement les noms **« Doctrine » / « Lexis+ » / « Lextenso »** (éditeurs juridiques) dans l'UI. Les montrer ici = revendiquer une **intégration / un renvoi** (destinations vers lesquelles on dirige l'avocat), pas une comparaison ni un sous-traitant caché — usage jugé légitime, mais **à confirmer par le PO** (différent de la règle « pas de marque tierce IA » type Anthropic/OpenAI). En revanche **« base officielle de la Cour de cassation »** est citable sans réserve (institution publique, source de droit). Ne PAS dire « JUDILIBRE » dans la voix off (jargon) ; usage interne uniquement.
- **Fidélité produit** : tous les statuts (Vérifiée / Suspecte / Non trouvée) et les 5 références sont **réels**, issus du dossier de test LEMAIRE validé en recette le 2026-06-09. Ne pas inventer de chiffre de performance non mesuré.

---

## Annexe — correspondance produit (interne, ne pas afficher à l'écran)

| Bloc vidéo | Feature | Statut |
|------------|---------|--------|
| Section « Jurisprudences citées » sur la Synthèse (badges) | **F-179** (+ SF-179-05 confirmation via JUDILIBRE) | Livrée |
| Sceau « source officielle Cour de cassation » + lien valide | SF-179-05 + PR #1603 (liens courdecassation) | Livrée |
| Boutons « Doctrine / Lexis+ / Lextenso » sur les points juridiques | **F-241** (deeplinks éditeurs, conditionnés au pays FR) | Livrée |
| Jurisprudence applicable sous l'outil | **F-JU-01** | Livrée |
| Jurisprudence dans les conclusions générées | **F-JU-02** | Livrée |
| « il se tait » / « écartée — sans rapport » | **F-JU-06** (qualité, anti hors-sujet, silence > erreur) | Livrée (4/4 SF) |

> **Nom produit éventuel** : si l'on veut nommer la capacité dans la lignée « Legal OCR / Legal Vision », une piste est **« Legal Check »** (vérification jurisprudentielle) — **à valider par le PO** avant tout affichage à l'écran ou usage dans le copy. Tant que non validé, rester descriptif (« vérification de jurisprudence »).
