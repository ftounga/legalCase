# F-98 — Style learning (SF-98-46/47/48) — Document de cadrage cohérence (étape 0)

**Date** : 2026-05-18
**Skill appliquée** : `ai-skills/feature-coherence-challenger.md`
**Périmètre** : le **bloc « style learning »** de F-98 — SF-98-46 (ingestion du corpus historique), SF-98-47 (génération adaptée au style appris), SF-98-48 (UI cabinet de gestion du corpus). Cadrage focalisé : l'étape 0 de F-98 (`SF-98-00-coherence.md`) a déjà rendu un verdict GO sur la feature entière ; ce document approfondit le sous-bloc style learning, qui porte des enjeux propres (corpus de documents tiers, secret professionnel, RGPD) non explorés au cadrage initial.

---

## Verdict global

**GO avec ajustements**

Le style learning est fonctionnellement cohérent et fortement demandé (signal terrain Renversez 🔴🔴🔴). Toutes les briques amont existent (génération de conclusions livrée, pipeline d'upload + extraction de documents livré). **Un ajustement structurant et non négociable** : le corpus est constitué de **conclusions de clients passés**, couvertes par le secret professionnel de l'avocat — l'ingestion doit extraire le **style**, pas conserver le **contenu client** en clair. Cet ajustement pilote tout le découpage.

---

## Intention métier (1 phrase)

Permettre à l'avocat de faire générer des conclusions **dans son propre style rédactionnel** — sa structure d'argumentation, ses formules, son ton — en apprenant ce style à partir de ses conclusions antérieures, plutôt que de produire un texte générique.

---

## Workflow métier réel de l'utilisateur cible

Source : pratique avocat contentieux + signal terrain Renversez 13/05 (« apprend la rédaction de l'avocat à partir de conclusions historiques uploadées »).

1. L'avocat a derrière lui un corpus de conclusions déjà rédigées et déposées (sa « patte »).
2. Il perçoit la génération automatique comme utile **mais générique** — « ça ne me ressemble pas, je dois tout réécrire ».
3. Il souhaite fournir à l'outil quelques-unes de ses conclusions passées comme **références de style**.
4. L'outil en extrait les caractéristiques de style (structure, formules de transition, ton, longueur de phrase, vocabulaire procédural récurrent).
5. À la génération suivante (SF-98-01 et cellules suivantes), le projet produit **adopte ce style**.
6. L'avocat gère son corpus dans le temps : ajout, retrait, activation/désactivation.
7. État terminal : un projet de conclusions généré qui « sonne » comme l'avocat → relecture allégée → dépôt.

---

## Cartographie features ↔ workflow

| # | Étape | Feature(s) LegalCase | Statut |
|---|---|---|---|
| 1 | L'avocat possède un corpus | — (hors produit, c'est son historique) | n/a |
| 3 | Upload de documents | F-43 import dossier — pipeline d'upload livré | ✅ Livrée |
| 4a | Extraction du texte Word/PDF | `ExtractionService` (POI / PDFBox), pipeline d'extraction documentaire | ✅ Livrée |
| 4b | Extraction du **style** | — | ❌ **Manquant → SF-98-46/47** |
| 4c | Stockage d'un corpus de style **au niveau workspace** | — (les documents existants sont rattachés à un dossier, pas au cabinet) | ❌ **Manquant → SF-98-46** |
| 5 | Génération adaptée au style | F-98 génération (SF-98-01 livrée) — à étendre | 🟡 à étendre |
| 6 | Gérer le corpus | — | ❌ **Manquant → SF-98-48** |
| 7 | Conclusions générées | SF-98-01 + SF-98-49/50/52 (lot livré 2026-05-18) | ✅ Livrée |

---

## Challenge amont — la séquence tient-elle ?

| Pré-requis | Couvert ? | Analyse |
|---|---|---|
| Upload de documents | ✅ | Pipeline F-43 réutilisable |
| Extraction texte .docx/.pdf | ✅ | `ExtractionService` lit déjà Word et PDF |
| Génération de conclusions (le consommateur du style) | ✅ | SF-98-01 livrée — `CaseConclusionPromptBuilder` est le point d'injection |
| Notion de corpus **rattaché au cabinet/workspace** (et non au dossier) | ❌ | Aucune entité ne porte des documents au niveau workspace. Nouvelle entité requise (SF-98-46) |
| Capacité à dériver un « profil de style » d'un texte | ❌ | Nouveau — c'est le cœur de SF-98-46/47 |

**Verdict amont** : aucun trou *bloquant*. Les briques d'infrastructure (upload, extraction) existent ; ce qui manque est le cœur métier de la feature elle-même (corpus workspace + extraction de style), ce qui est normal. Pas de pré-requis externe à créer.

---

## Challenge aval — la sortie est-elle exploitable ?

| Étape aval | Couvert ? | Analyse |
|---|---|---|
| Injection du style dans la génération | 🟡 | `CaseConclusionPromptBuilder` (SF-98-01) est le point d'injection naturel : SF-98-47 y ajoute le profil de style. Architecture prête, extension propre. |
| Relecture / édition du résultat | ✅ | SF-98-49 (éditeur) livrée |
| Export | ✅ | SF-98-50 (Word) livrée |

**Verdict aval** : pas de trou. Le style learning se branche proprement sur la chaîne déjà livrée (générer → versionner → éditer → exporter).

---

## STOP / ajustement structurant — secret professionnel & RGPD

C'est le point central de ce cadrage, absent du cadrage initial F-98.

**Constat** : les conclusions passées de l'avocat contiennent des **données de clients antérieurs** (identités, faits, montants, éléments de santé/famille selon le domaine). Ces clients **n'ont pas consenti** à un traitement par LegalCase, et ces documents sont couverts par le **secret professionnel** de l'avocat (Art. 66 Code de déontologie FR, Art. 458 CP — les mêmes textes qui fondent F-240).

**Analyse** : ce n'est **pas un STOP**. L'avocat téléverse déjà les pièces de ses dossiers *en cours* dans LegalCase — c'est le produit, encadré par le DPA (F-240). Téléverser des conclusions *passées* est de même nature, sous la même responsabilité de l'avocat responsable de traitement. **Mais** la finalité diffère : pour le style learning, le **contenu client est du bruit** — seul le **style rédactionnel** importe. Conserver durablement des conclusions clients en clair pour en apprendre le style serait disproportionné (principe RGPD de minimisation).

**Ajustement non négociable** : l'ingestion (SF-98-46) doit produire un **profil de style** (caractéristiques rédactionnelles : structure, formules, ton, registre) et **ne pas conserver le texte brut des conclusions clients** au-delà du temps de traitement. Le corpus persistant = le profil de style, pas les documents sources. À défaut d'anonymisation fiable, c'est l'extraction-puis-purge qui garantit la minimisation.

---

## Invariants anti-gadget pour les mini-specs

1. **Minimisation des données** : SF-98-46 extrait un profil de style et **ne persiste pas** le texte intégral des conclusions sources. Le document uploadé est traité puis purgé ; seul le profil (et éventuellement des extraits de style courts et anonymisés) est conservé.
2. **Corpus au niveau workspace** : le profil de style appartient au cabinet (workspace), pas à un dossier. Isolation workspace stricte.
3. **Consentement / responsabilité explicite** : l'UI d'upload (SF-98-48) affiche que l'avocat garantit être en droit de fournir ces documents (cohérent avec le DPA F-240).
4. **Style ≠ contenu** : SF-98-47 injecte le style dans le prompt ; il ne réinjecte jamais les faits, montants ou identités d'un dossier passé dans les conclusions d'un dossier courant.
5. **Activable / désactivable** : l'avocat peut générer avec ou sans style appris (SF-98-48) ; le style est une option, jamais imposé.
6. **Transparence** : un projet généré avec adaptation de style le signale (cohérent avec le bandeau de transparence SF-98-01).
7. **Dégradation propre** : corpus vide ou profil indisponible → la génération retombe sur le comportement générique de SF-98-01, sans erreur.

---

## Découpage en SF (confirmation de la matrice F-98)

| SF | Périmètre | Impact écran |
|---|---|---|
| **SF-98-46** | Ingestion du corpus : upload de conclusions (réutilise F-43/extraction), extraction d'un profil de style, persistance du profil au niveau workspace, **purge du texte source**. Entité corpus/profil de style. | 🟡 (point d'entrée d'upload — à cadrer en 0 bis) |
| **SF-98-47** | Style mimicking : injection du profil de style dans `CaseConclusionPromptBuilder` ; la génération adopte le style ; dégradation propre si pas de profil. | ❌ (backend — pas d'écran nouveau) |
| **SF-98-48** | UI cabinet : écran/section de gestion du corpus (liste, ajout, suppression, activation/désactivation). | 🟡 (nouvel écran ou section — à cadrer en 0 bis) |

---

## Décision finale

**GO avec ajustements.** Le bloc style learning démarre. L'ajustement structurant (extraction de style + minimisation, pas de conservation du contenu client) est intégré aux 7 invariants ci-dessus et devra être respecté par les mini-specs. Prochaine étape : **étape 0 bis** (cadrage cohérence écran) pour SF-98-46 (point d'entrée d'upload) et SF-98-48 (écran de gestion du corpus) — SF-98-47 en est exemptée (backend pur). Puis mini-specs.

---

## Liens

- `docs/features/F-98/SF-98-00-coherence.md` — cadrage F-98 (étape 0 initiale)
- `ai-skills/feature-coherence-challenger.md` — skill appliquée
- [[project_renversez_post_demo_13_05]] — signal terrain (style learning = signal A, 🔴🔴🔴)
- F-240 — conformité contractuelle / DPA (secret professionnel — même cadre juridique)
- F-43 — import de dossier (pipeline d'upload réutilisé)
- SF-98-01 — génération de conclusions (`CaseConclusionPromptBuilder` = point d'injection du style)
