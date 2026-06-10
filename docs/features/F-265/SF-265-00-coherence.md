# F-265 — Cadrage cohérence (étape 0)

> Feature : **Co-rédaction au paragraphe (instruction IA inline)** — chaque moyen/section des conclusions devient un bloc régénérable/renforçable, avec instruction IA inline.
> Programme « Conclusions V2 », levier UX n°2 (adoption). Skill : `ai-skills/feature-coherence-challenger.md`. 2026-06-10.

## Verdict : **GO avec ajustements** — périmètre cadré sur la **régénération de section markdown** (pas un block-model persisté)

---

## Intention métier (1 phrase)

Aujourd'hui la génération des conclusions est **one-shot** ; F-265 doit permettre à l'avocat de **co-écrire** : sélectionner une section de l'acte et demander à LegalCase de la **régénérer / renforcer** selon une **instruction libre** (« renforce la prescription », « durcis sur le barème »), sans tout régénérer.

---

## Constat central — architecture actuelle = markdown monolithique

Le `content` d'une `CaseConclusion` est **un seul blob markdown** (généré one-shot par le pipeline async `prepare → AnthropicService → finalize`, puis enrichi du bordereau / jurisprudence / réfutation). **Il n'existe aucun modèle de bloc persisté.** Les « sections » de l'acte sont matérialisées uniquement par les **titres markdown** (`##` / `###`) — c'est ainsi que F-259 (rendu) et F-264 (éditeur/aperçu) les traitent déjà.

Deux options de profondeur :

| Option | Principe | Effort | Risque gadget |
|---|---|---|---|
| **A — Block-model persisté** | Découper l'acte en N blocs en base (table `conclusion_section`), pipeline par bloc | Très lourd (migration, refonte génération, versions) | Faible mais surdimensionné V1 |
| **B — Régénération de section markdown (in-place)** | Une « section » = un bloc délimité par titre markdown, parsé à la volée du `content`. Régénération = appel LLM scopé (section courante + instruction + contexte dossier), **remplacement in-place** du bloc dans le markdown, round-trip markdown-safe | Modéré (réutilise pipeline génération + prompt builder) | Faible — valeur réelle immédiate |

➡️ **Option B retenue (décidée par défaut, réversible — voir arbitrage).** Elle livre la valeur d'adoption (co-rédaction par section) **sans** refondre le stockage ni casser l'export Word/PDF (markdown→markdown). Le block-model (A) reste backlog si signal terrain.

---

## Workflow métier réel de l'avocat

1. L'avocat génère ses conclusions (F-98). Résultat : un acte structuré (faits, discussion par moyen, dispositif).
2. Il **relit moyen par moyen**. Sur un moyen, il juge l'argumentation **trop faible** (ex. prescription survolée, barème Macron pas assez appuyé).
3. Aujourd'hui : soit il **réécrit à la main** (F-98-49 / F-264), soit il **régénère tout** (perd ses retouches sur les autres sections).
4. Besoin : **agir sur CE moyen** — « renforce », « ajoute la jurisprudence sur X », « raccourcis » — sans toucher au reste.

**F-265 couvre l'étape 4 : la retouche ciblée assistée.**

## Cartographie features actuelles ↔ workflow

| Étape | Feature LegalCase | Statut |
|---|---|---|
| 1. Génération one-shot | F-98 (pipeline async + matrice prompts) | ✅ |
| 2. Relecture / rendu acte | F-259 (rendu document) | ✅ |
| 3a. Édition manuelle markdown + aperçu | F-264 (éditeur enrichi + aperçu live) | ✅ **déjà livré** |
| 3b. Régénération **complète** (nouvelle version) | F-98 / SF-98-52 (versions) | ✅ |
| **4. Régénération / renforcement d'UNE section avec instruction IA** | — aucune | ❌ **trou réel = valeur F-265** |

## Position de la nouvelle feature

F-265 s'insère **entre** l'édition manuelle (F-264) et la régénération totale (SF-98-52) : une **régénération scopée à une section** pilotée par une instruction. Elle **réutilise** le pipeline de génération (prompt builder + `AnthropicService`) en le restreignant à la section + l'instruction, et **réutilise** l'éditeur F-264 comme surface d'insertion du résultat (round-trip markdown).

## Challenge amont

- ✅ Pré-requis présents : pipeline de génération (F-98), prompt builder par combinaison, rendu/édition markdown (F-259/F-264), gestion de version + cycle de vie DRAFT (SF-98-52/49).
- ⚠️ Pré-requis manquant **léger** : un **découpage section** fiable du markdown (parser titres `##`/`###`) — pur frontend, déterministe.
- ⚠️ Pré-requis backend : un **endpoint de régénération de section** qui prend `{ sectionMarkdown, instruction, contexte dossier }` et renvoie le markdown régénéré de la section. Réutilise le contexte dossier (analyse, outils, jurisprudence) déjà assemblé par le pipeline.

## Challenge aval

- ✅ La sortie (markdown de section régénéré) se **réinjecte in-place** dans le `content` markdown → édition existante (`PATCH …/content`) → rendu/export inchangés (markdown-safe). Aval propre, **non-régression export garantie** (markdown→markdown).
- ✅ Le résultat reste éditable manuellement (F-264) avant enregistrement → l'avocat garde la main (pas de boîte noire).

## STOPs / pré-requis

- **Garde markdown-safe** : la régénération de section doit produire du markdown valide et ne JAMAIS injecter le bordereau / la jurisprudence globale (ces sections sont ajoutées par `finalize`, hors périmètre section). La régénération est **scopée au corps argumentatif** d'une section.
- **Pas d'invention** : la régénération consomme le **même contexte dossier** que la génération (faits, pièces, outils calculés, jurisprudence d'appui) — l'instruction oriente le style/l'angle, **pas** les faits/chiffres. Invariant F-98 (silence > erreur, gardes chiffres/jurisprudence SF-98-55) **conservé** sur la section régénérée.
- **Édition DRAFT only** : la régénération de section n'est possible que sur une version `DONE` + `DRAFT` (même garde que `updateContent`).
- **Coût LLM** : un appel LLM par régénération → gaté par le gate Anthropic existant (AiCallContext obligatoire, F-257). JobType dédié (USER) à enregistrer.

## Invariants anti-gadget pour la mini-spec

1. **Réutilise le pipeline existant** : pas de second pipeline de génération ; le prompt de section réutilise le `ConclusionPromptBuilder` / contexte dossier + une consigne « régénère uniquement cette section selon l'instruction ».
2. **Round-trip markdown** : section markdown in → section markdown out → remplacement in-place → `content` reste markdown valide ; export Word/PDF **non régressé** (test).
3. **Avocat garde la main** : le markdown régénéré est inséré dans l'éditeur F-264, **pas** auto-sauvegardé en aveugle — l'avocat relit, ajuste, puis « Enregistrer ».
4. **Gardes F-98 conservées** : chiffres tracés aux outils, jurisprudence vérifiée, anti-jargon (SF-98-55) s'appliquent à la section régénérée (le prompt de section embarque les mêmes gardes).
5. **3 domaines** : le **mécanisme** (parser section + endpoint régénération + UI instruction) se construit **une fois**, uniforme aux 3 domaines (travail/immigration/famille) ; aucun contenu métier spécifique à coder (la régénération réutilise le prompt builder déjà décliné par combinaison).
6. **Gate coût** : appel LLM gaté (F-257), JobType USER enregistré.

## Découpage proposé

- **SF-265-01 (backend)** — endpoint `POST …/versions/{versionId}/sections/regenerate` : entrée `{ sectionMarkdown, instruction }` → contexte dossier réassemblé → appel LLM scopé (gardes F-98) → `{ regeneratedMarkdown }`. **Synchrone** (une section = court, pas besoin d'async/RabbitMQ ; timeout borné). Contrat API figé → parallélisable.
- **SF-265-02 (frontend)** — dans l'éditeur F-264 : parser les sections (titres markdown), sélecteur de section + champ « instruction IA inline » + bouton « Régénérer cette section » → appel endpoint → insertion in-place du markdown dans `draftContent` (éditable avant enregistrement). Étape 0 bis requise (impact écran).

## Décision finale

**GO avec ajustements.** Option B (régénération de section markdown in-place) retenue par défaut (réversible). Le block-model persisté (Option A) reste backlog. F-265 : `Backlog` → `À faire`. Mécanisme uniforme 3 domaines ; aucun contenu métier nouveau.
