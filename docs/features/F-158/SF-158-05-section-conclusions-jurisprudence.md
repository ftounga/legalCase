# Mini-spec — F-158 / SF-158-05 — Section « Rédaction de conclusions » + jurisprudence vérifiable

## Identifiant

`F-158 / SF-158-05`

## Feature parente

`F-158` — Refonte landing page (vague V4).

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-158-05-section-conclusions`

---

## Objectif

Ajouter sur la landing la section qui vend l'argument à plus forte valeur — **LegalCase rédige le projet de conclusions dans le style du cabinet, avec la jurisprudence de Cassation citée automatiquement** — et matérialiser l'état terminal du parcours (conclusions) dans le pipeline et la différenciation.

---

## Comportement attendu

### Cas nominal

La landing (`/`) affiche, **après le catalogue d'outils (l.478) et avant la section Pipeline (l.481)**, une nouvelle section `#conclusions` :
1. Titre + sous-titre annonçant la génération du **projet de conclusions** (vocabulaire « projet à relire », pas « acte définitif »).
2. Mise en avant du périmètre réel **F-98 Terminée** : 3 domaines (travail / immigration / famille) × 2 pays (FR/BE) × juridictions × stades procéduraux (fond / référé / appel / cassation) × positions (demandeur / défendeur).
3. **Style learning** (F-98 SF-98-46/47/48) : « rédige dans le style rédactionnel de votre cabinet » (avec mention RGPD : profil de style, pas de conservation du contenu client source).
4. **Export Word/PDF + relecture/versions** (F-98 SF-98-49/50/51/52).
5. **Jurisprudence vérifiable intégrée** (F-JU-02) : « les arrêts de Cassation correspondant à vos outils décisionnels sont cités automatiquement dans les conclusions, sans ressaisie » — **FR / Cour de cassation uniquement** (ne pas suggérer de couverture BE).

Le **Pipeline IA** (l.481) gagne une **6ᵉ étape « Conclusions »** et son titre passe de « De l'upload à la synthèse » à « De l'upload aux conclusions ».

La section **Différenciation** (l.575) gagne une **5ᵉ carte « Jurisprudence vérifiable »** (citations Cassation tracées, vs paraphrase d'un chatbot).

### Cas d'erreur

Page statique. Couverts par tests de rendu :

| Situation | Comportement attendu |
|-----------|---------------------|
| `prefers-reduced-motion` actif | Pas d'animation nouvelle non désactivable (réutiliser le pattern `fade-in` existant) |
| Ancre `#conclusions` ajoutée à la nav header | Le lien scrolle vers la section (si ajout nav) |

---

## Analyse de cohérence transversale

### Périmètres scannés
- **Anti-overclaim (invariant étape 0)** : chaque affirmation pointe vers une feature **Terminée** — F-98 (53/53 SF), F-JU-01/02 (Terminées). Aucune capacité non livrée annoncée. Vérifié sur `docs/PRODUCT_SPEC.md`.
- **Jurisprudence** : périmètre = JUDILIBRE / Cour de cassation FR (F-JU-01/02). **BE parké** (cf. `reference_be_jurisprudence_sources`) → le copy ne mentionne pas de jurisprudence BE.
- **Conclusions transparence** : reprendre le registre « projet de conclusions à relire » déjà présent dans le produit (bandeau F-98) — pas de promesse « acte prêt à déposer sans relecture ».
- **Charge écran (invariant étape 0 bis)** : +1 section primaire nette (`#conclusions`). La carte jurisprudence s'intègre dans Différenciation (non autonome). L'allègement compensatoire d'une section (Fonctionnalités ↔ Différenciation) est porté par **SF-158-06**, le plafond est tenu à l'échelle de la vague.

### Préoccupation transversale cochée
- **Navigation / routing** : si un lien `#conclusions` est ajouté au `<nav>` header (l.4-12) → simple ancre, aucun guard/route Angular. Sinon aucun impact. Pas d'auth/workspace/plan.
- Aucun outil décisionnel, aucune règle de visibilité, aucun backend touché.

---

## Critères d'acceptation vérifiables

1. ✅ Une section `#conclusions` existe entre le catalogue et le pipeline.
2. ✅ Le copy mentionne : projet de conclusions, 3 domaines × 2 pays × juridictions × stades × positions, style du cabinet, export Word/PDF.
3. ✅ La jurisprudence est présentée comme **Cassation FR** (aucune mention de jurisprudence BE).
4. ✅ Le copy emploie « projet » / « à relire » (pas « acte définitif sans relecture »).
5. ✅ Le pipeline a 6 étapes, la 6ᵉ = « Conclusions » ; le titre ne dit plus « à la synthèse » comme terminus.
6. ✅ Différenciation a une carte « Jurisprudence vérifiable ».
7. ✅ Aucune capacité non Terminée au PRODUCT_SPEC n'est affirmée.
8. ✅ Style visuel : réutilise classes/typo/couleurs existantes (DESIGN_SYSTEM), aucune nouvelle palette.

---

## Plan de test minimal

- **Jest `landing.component.spec.ts`** : (a) la section `#conclusions` est rendue ; (b) elle contient « conclusions », « style », « Word » (ou « .docx ») ; (c) le pipeline rend 6 étapes ; (d) une carte « Jurisprudence » existe dans `why-us` ; (e) garde-fou anti-overclaim : la section ne contient pas « jurisprudence belge » / « Cassation belge ».
- **Smoke E2E `e2e/smoke/landing.spec.ts`** : assertion présence section conclusions + 6ᵉ étape pipeline (non lancé — dette host staging).
- **Isolation workspace** : N/A (page publique).

---

## Tables / endpoints / composants impactés

- `frontend/src/app/landing/landing.component.html` — nouvelle section `#conclusions` (~après l.478), pipeline (l.481-528), différenciation (l.585-606), éventuel lien nav (l.4-12).
- `frontend/src/app/landing/landing.component.scss` — styles de la section (réutilisation maximale des patterns existants).
- `frontend/src/app/landing/landing.component.spec.ts` — tests.
- `e2e/smoke/landing.spec.ts` — assertions.
- **Aucun** backend, table, endpoint, migration, outil décisionnel.

---

## Hors périmètre

- Nettoyage claims « 10× » / « fax 200 dpi » + SEO/OG/JSON-LD (« 92 » résiduel) + **allègement compensatoire d'une section** → **SF-158-06**.
- Refonte visuelle / charte — exclu.
- Toute modification backend / outil décisionnel / jurisprudence — exclu (lecture du périmètre existant uniquement).
