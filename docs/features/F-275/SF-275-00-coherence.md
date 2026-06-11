# SF-275-00 — Cadrage cohérence : auto-remplissage des identités des parties (POUR / CONTRE) depuis le dossier

> Skill : `ai-skills/feature-coherence-challenger.md` — Étape 0 de la séquence CLAUDE.md.
> Feature : **F-275** (Conclusions V4 ⑤, audit conclusions 2026-06-12, manque métier #5).

## 1. Workflow métier réel de l'avocat cible

En procédure civile française, l'en-tête des conclusions désigne nominativement les parties : « **POUR** : [identité + adresse du client] » contre « **CONTRE** : [identité + adresse de l'adversaire] ». L'avocat connaît ces identités par le dossier (contrat, lettre de licenciement, requête, pièces). Aujourd'hui, quand le générateur ne dispose pas des identités, il laisse `[à compléter]`. Or **le dossier connaît déjà les parties** (analyse IA) **et la position du client** (`procedurePosition` du dossier / `positionCode` de la combinaison procédurale). Laisser `[à compléter]` sur ce qui est en réalité connu est une friction (re-saisie manuelle de l'en-tête à chaque génération).

## 2. Cartographie des features existantes sur ce workflow

| Étape workflow | Feature produit | État |
|---|---|---|
| Extraction des identités/adresses (travail) | **SF-98-61** — `appendPartiesIdentity` lit `travail_extracted_data` (nom/prénom/adresse salarié + nom/adresse employeur) → section « IDENTITÉ DES PARTIES » | ✅ livré |
| Consigne de reprise des identités dans l'acte | **SF-98-55** — REDACTION_QUALITY_GUARD **point 6** (« reprends les identités fournies ; à défaut `[à compléter]` ; n'invente jamais d'adresse ») | ✅ livré |
| Ancrage de la partie représentée (client) | **F-269** — `buildPartieRepresenteeContext` (synthèse) : le client = la partie de `procedurePosition` (salarié = demandeur, employeur = défendeur) | ✅ livré (côté **synthèse**, pas conclusions) |
| **Orientation POUR/CONTRE de l'en-tête des conclusions** depuis la position | **F-275 (cette feature)** | ❌ manquant |

**Pré-requis amont — présent ?** OUI. Les identités sont déjà extraites (SF-98-61) et la position procédurale est déjà portée par la combinaison (`positionCode` → `positionLabel`, déjà dans `ConclusionPromptInput`). F-269 a déjà tranché la correspondance position↔rôle (salarié=demandeur / employeur=défendeur) côté synthèse.

**Sortie exploitable en aval — oui ?** OUI. La sortie est du texte de conclusions (markdown) consommé identiquement par l'éditeur (F-264), l'export (F-281) et les versions. Aucun nouvel artefact.

## 3. Challenge de cohérence — RISQUE DOUBLON (directive : vigilance gadget)

⚠️ **F-275 frôle le doublon avec SF-98-61.** SF-98-61 injecte déjà les identités ; le point 6 de SF-98-55 dit déjà de les reprendre et de mettre `[à compléter]` à défaut. **Que reste-t-il de réel à livrer ?**

Le **trou résiduel** : la section « IDENTITÉ DES PARTIES » et le point 6 décrivent les parties **par rôle métier** (« Salarié », « Employeur ») **sans dire laquelle est le client** (POUR) et laquelle est l'adversaire (CONTRE). Le modèle doit *deviner* l'orientation de l'en-tête. C'est précisément le manque que pointe l'audit (« le dossier connaît les parties ET la position »). Le saut F-275 = **rendre l'orientation POUR/CONTRE déterministe** dans le prompt, à partir de la position procédurale déjà disponible — pas ré-extraire des identités (déjà fait).

- **Doublon / gadget ?** NON après recentrage : SF-98-61 fournit les *données*, F-275 fournit l'*orientation POUR/CONTRE* (qui est le client). Sans F-275, l'avocat re-corrige régulièrement l'en-tête quand le modèle inverse demandeur/défendeur.
- **Invariant « un outil = une situation » ?** OUI — F-275 n'est PAS un outil décisionnel : **garde de prompt** (comme SF-98-55 / F-272 / F-273 / F-274). Aucun calculateur, aucun `decision_tool_visibility_rules`.
- **Risque d'invention ?** Maîtrisé : on ne crée aucune donnée. On annote la section existante avec la position (déjà connue) et on renforce le point 6 pour structurer en POUR/CONTRE **sans inventer** d'identité ni d'adresse — `[à compléter]` reste la valeur honnête pour tout champ réellement absent (immigration/famille, dont l'extraction d'identités n'existe pas → no-op naturel, l'en-tête reste `[à compléter]`).
- **Portée.** « Uniforme » (PRODUCT_SPEC) : l'orientation POUR/CONTRE est une consigne générique de l'en-tête, transverse aux 3 domaines FR. La **donnée** d'identité reste, elle, fournie là où elle existe (travail FR/BE via SF-98-61). Aucun élargissement BE requis : la garde ajoute seulement l'orientation au niveau du message utilisateur (annotation de la section IDENTITÉ) et du point 6, déjà transverses.

## 4. Verdict

**GO avec ajustements (recentré).**

Ajustements retenus :
1. **Ne pas ré-implémenter** l'extraction d'identités : SF-98-61 reste la source. F-275 = **orientation POUR/CONTRE** uniquement.
2. Annoter la section « IDENTITÉ DES PARTIES » du message utilisateur avec la **position procédurale** du dossier (POUR = le client = la partie de `positionCode` ; CONTRE = l'adversaire), en réutilisant la correspondance position↔rôle déjà tranchée par F-269 pour le travail.
3. Renforcer le **point 6 de REDACTION_QUALITY_GUARD** : exiger un en-tête structuré « POUR … / CONTRE … » repris des identités fournies, `[à compléter]` seulement pour les champs réellement absents, **zéro invention** d'adresse ou d'identité — non-régression de l'anti-jargon SF-98-55.
4. **No-op naturel** hors travail (immigration/famille) : aucune identité extraite → en-tête `[à compléter]`, aucune rubrique vide, aucune invention.
5. **Non-régression** : SF-98-61 (données), F-269 (ancrage synthèse), gardes F-272/F-273/F-274 inchangés.

## 5. Invariants anti-gadget pour la mini-spec

- I1 — **Aucune nouvelle extraction**, aucune nouvelle clé JSON : on réutilise `travail_extracted_data` (SF-98-61) et `positionLabel`/`positionCode` (déjà dans l'input).
- I2 — Garde de prompt transverse construite **une fois**, jamais dupliquée provider par provider.
- I3 — **Zéro invention** : `[à compléter]` reste la valeur honnête pour tout champ absent ; aucune adresse/identité inventée (non-régression SF-98-55 point 6).
- I4 — **No-op silencieux** hors travail (pas de section vide ; en-tête `[à compléter]`).
- I5 — Aucune table, aucun endpoint, aucun écran, aucune migration. Backend-only.
- I6 — Pas d'impact écran ⇒ **étape 0 bis non requise** (cohérent avec F-272/F-273/F-274).

## 6. Impact PRODUCT_SPEC

Verdict GO ⇒ statut F-275 passe **À faire → en cours de livraison** (mise à jour docs groupée par l'orchestrateur en fin de vague).
