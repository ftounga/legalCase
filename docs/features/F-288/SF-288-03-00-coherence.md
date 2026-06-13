# F-288 / Vague 3 (moyens adverses) — Cadrage cohérence (étape 0)

> Réexamen de la dimension « moyens adverses » de l'écran de composition, à la lumière du code. 2026-06-13.
> Parent : `SF-288-00-coherence.md` (qui listait les moyens adverses comme 3ᵉ trou partiel, « gain marginal »).

## Verdict : **STOP avec pré-requis** — bloqué sur la persistance des moyens adverses

---

## Constat code (vérifié 2026-06-13)

1. **Les moyens adverses SONT un ingrédient réel du prompt** : `CaseConclusionService.loadAdverseMoyens` → section « MOYENS ADVERSES À RÉFUTER » (F-261/SF-261-03). Contrairement à la vague 2, il y aurait **quelque chose à filtrer**.

2. **MAIS `AdverseMoyen` est un record ÉPHÉMÈRE** : `record AdverseMoyen(these, fondements, piecesInvoquees)` — **aucune `@Entity`, aucune table**. Les moyens sont **ré-extraits par LLM à CHAQUE génération** (`AdverseMoyensExtractor`, extraction paresseuse dans `prepare()`). Il n'existe **aucun identifiant stable** d'un moyen.

## Pourquoi c'est bloqué (pas un gadget, mais infaisable proprement en l'état)

La vague 1 repose sur un **`item_key` stable** (le `toolId`). Pour les moyens, il n'y a pas d'équivalent :
- Keyer l'exclusion sur le **texte** (ou un hash) de la thèse → le texte **varie d'une extraction LLM à l'autre** (reformulations) → l'exclusion durable **cesse silencieusement de s'appliquer** à la régénération. C'est un faux-positif d'isolation = pire qu'un gadget (l'avocat croit un moyen écarté alors qu'il revient).
- Présenter les moyens dans le modal suppose de les **extraire AVANT** le modal (un appel LLM ajouté au clic « Générer », latence) **ou** de les avoir **persistés**.

➡️ La curation durable des moyens **suppose la persistance/affichage des moyens extraits** — qui est **déjà un item de backlog F-261** (« persistance/affichage des moyens — aujourd'hui re-extrait à chaque génération »).

## Challenge amont
- **Trou amont bloquant** : pas de persistance des moyens (pas d'entité, pas de table, pas d'ID stable). La vague 3 **ne peut pas** s'appuyer sur l'existant.
- **Pré-requis nommé** : **persistance des moyens adverses extraits** (entité + table + ID stable + ré-extraction idempotente/réconciliée). C'est une feature backend à part entière (déjà tracée côté F-261 backlog), pas une simple « dimension de plus ».

## Décision finale

**STOP avec pré-requis. Zéro code en l'état.** La vague 3 n'est **pas** un gadget (il y a une vraie valeur : ne pas réfuter un moyen qu'on concède), mais elle est **infaisable proprement** sans persistance stable des moyens.

**Deux chemins pour le PO** :
- **A — Différer** la vague 3 jusqu'à ce que la **persistance des moyens adverses** (backlog F-261) soit livrée ; la curation deviendra alors une vraie « dimension » de l'écran de composition (réutilise la table générique + le modal).
- **B — Construire d'abord le pré-requis** (persistance des moyens) en SF dédiée, puis la curation par-dessus.

**Recommandation : A (différer)** — la valeur est marginale (étape 0 parent) et le pré-requis est lourd ; à reprendre sur signal terrain avocat.
