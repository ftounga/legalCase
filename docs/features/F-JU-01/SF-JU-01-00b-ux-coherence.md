# F-JU-01 — Cadrage cohérence écran (étape 0 bis)

## Verdict : **GO avec ajustements**

Le placement est trivialement cohérent (bloc « Jurisprudence applicable » dans chaque composant outil au sein du modal F-177 ; dashboard admin sur écran super-admin séparé). Les ajustements concernent surtout la **discipline d'affichage** dans le modal d'outil (top-3 compact, pas de dépliable lourd, lien externe en nouvel onglet) et la **stratégie « 0 ouverture obligatoire »** du dashboard super-admin (l'email mensuel récap remplace l'obligation de visite).

---

## Intention métier + comportement visible attendu (1-2 phrases)

Quand l'avocat ouvre un outil décisionnel et consulte le résultat du calcul, il voit immédiatement, **sous ce résultat et dans le même modal**, 1 à 3 arrêts structurants (chapeau officiel Cassation cité textuellement, lien Légifrance, date de dernière vérification, bouton « Signaler un problème ») qui fondent juridiquement la solution proposée — **sans avoir à quitter LegalCase pour Doctrine**.

Côté super-admin : un écran de pilotage `/super-admin/jurisprudence-watch` donne accès aux flags utilisateurs et à l'audit log des actions du cron mensuel, mais n'est consulté qu'occasionnellement (email mensuel récap suffit en régime stationnaire).

---

## Rappel verdict feature-coherence-challenger

✅ **GO** rendu en étape 0 (`SF-JU-01-00-coherence.md` du 2026-05-21). Toutes les briques amont et aval existent. F-JU-01 comble l'étape « justification jurisprudentielle » entre les outils décisionnels qui calculent (F-DT/F-IM/F-FA) et les features aval (F-242, F-243, F-241).

---

## Parcours écran réel de l'avocat (ouverture du dossier → état terminal)

Source : `docs/business/parcours-ecran-dossier.md` (référentiel établi par 7 passages cadrage écran depuis 2026-05-15), reproduit fidèlement.

1. L'avocat ouvre un dossier → écran **détail du dossier**, 4 onglets (`mat-tab-group` F-244).
2. **En-tête** : titre + actions + `app-case-dashboard-stepper`.
3. Onglet **Dossier** : métadonnées, stade procédural F-243, import / liste des pièces.
4. Onglet **Analyse** : pipeline IA, accès à la synthèse (`SynthesisComponent`, sous-écran avec F-179 / F-241 / F-242 sur le panneau « Points juridiques »).
5. L'avocat renseigne le stade procédural.
6. Onglet **Décision** : remplit les **outils décisionnels** via `app-decisional-tools-panel` — chaque outil ouvre dans un **modal MatDialog 90vw/90vh** (refonte F-177 — `app-decision-tool-modal`).
7. **Dans le modal de l'outil**, l'avocat (a) valide / ajuste les champs pré-remplis IA (F-IA-01), (b) clique « Enregistrer » → le calcul s'affiche, (c) **NOUVEAU : lit les arrêts qui fondent ce calcul (F-JU-01)**, (d) ferme le modal.
8. L'avocat consulte le **tableau de bord décisionnel** (`app-case-dashboard`) — verdicts agrégés.
9. L'avocat **génère le projet de conclusions** (`app-conclusions-section`, F-98) — qui peut réutiliser les arrêts mappés F-JU-01.
10. L'avocat finalise les conclusions dans son traitement de texte.
11. Onglet **Suivi** : échéances, notes, calendrier.
12. **État terminal** : projet de conclusions généré (tranché F-98 SF-98-00b 2026-05-18).

---

## État terminal du processus

✅ Inchangé : **« projet de conclusions généré »** (`app-conclusions-section`, onglet Décision). F-JU-01 enrichit l'étape 7 du parcours (lecture des arrêts dans le modal de l'outil) sans modifier l'état terminal.

---

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone LegalCase | Statut | Impact F-JU-01 |
|---|---|---|---|
| 1-2. Ouverture dossier + en-tête | `case-file-detail.component` + `detail-header` | ✅ existant | aucun |
| 3. Onglet Dossier (métadonnées, pièces) | `mat-tab-group` index 0 | ✅ existant | aucun |
| 4. Onglet Analyse → synthèse | `SynthesisComponent` (route séparée) | ✅ existant | aucun (F-JU-01 ne touche pas la synthèse) |
| 5. Stade procédural | `detail-card` onglet Dossier | ✅ existant | aucun |
| 6. Onglet Décision — panel outils | `app-decisional-tools-panel` (onglet Décision, bloc primaire 1) | ✅ existant | indirect (les outils dans le panel obtiennent un nouveau bloc INTERNE) |
| 7. Modal d'un outil | `app-decision-tool-modal` (F-177, MatDialog 90vw/90vh) | ✅ existant | **direct — ajout du bloc « Jurisprudence applicable » dans chaque composant outil, sous le résultat** |
| 7 bis. Tableau de bord décisionnel | `app-case-dashboard` (onglet Décision, bloc primaire 2) | ✅ existant | aucun en V1 (V2 possible : badge « N arrêts cités » par tile) |
| 8. Conclusions générées | `app-conclusions-section` (onglet Décision, bloc primaire 3, F-98) | ✅ existant | à clarifier en mini-spec : F-98 puise-t-elle dans `tool_jurisprudence_mappings` pour citer les arrêts dans les conclusions générées ? Probablement oui — extension naturelle. |
| 9. Onglet Suivi | `app-case-deadlines-section`, `app-case-notes-section` | ✅ existant | aucun |
| (Admin) Audit + flags utilisateurs | **`/super-admin/jurisprudence-watch` — NOUVEAU** | ❌ à créer en SF-JU-01-05 | **direct — nouvel écran super-admin** |

---

## Position candidate de la feature

### Écran 1 — Modal d'outil décisionnel (`app-decision-tool-modal`)

- **Onglet** : Décision (index 2)
- **Bloc primaire conteneur** : `app-decisional-tools-panel`
- **Zone précise** : à l'intérieur de chaque composant outil (~80-90 composants éligibles sur les 131 du `TOOL_REGISTRY`), **sous le résultat principal du calcul** et avant les actions (Enregistrer / Fermer)
- **Composant introduit** : `<app-tool-jurisprudence-citations [toolId]="..." [branchActive]="...">` standalone, réutilisable
- **Interface exposée par chaque composant outil** : `ToolJurisprudenceCitable { branchActive$: Observable<string> }` — l'outil expose la branche de calcul actuellement active (déterminée par le résultat IA + saisie avocat), le composant citations s'y abonne et fetch les 1-3 arrêts du mapping correspondant via l'endpoint `GET /api/tools/{toolId}/jurisprudence-citations?branch={branchActive}`.
- **Point d'entrée** : automatique — apparaît dès que le résultat du calcul est affiché. Aucune action utilisateur requise pour faire apparaître le bloc.

### Écran 2 — Dashboard super-admin

- **Nouvelle route** : `/super-admin/jurisprudence-watch` (cohérent avec pattern existant `/super-admin/blog`, `/super-admin/traction-onepager`)
- **Bloc primaire** : 1 écran monobloc avec 3 sections
  1. Header : timestamp du dernier run cron + bouton « Relancer maintenant » + statut trust mode
  2. Section principale « Flags à arbitrer » : liste des flags PENDING (du cron mensuel + utilisateurs), 3 boutons inline (remplacer / ajouter / ignorer)
  3. Section secondaire « Audit log » (lazy load) : actions auto Claude du mois (consultation pure)
- **Point d'entrée** : menu super-admin (lien direct depuis `/super-admin` tableau de bord). **Pas obligatoire** — l'email mensuel récap couvre les besoins en régime stationnaire.

---

## Challenge placement

**Question : l'écran / la zone candidate correspond-il à l'étape du parcours où l'avocat a réellement besoin de la feature ?**

### Écran 1 — Citations dans modal d'outil

**Oui, parfaitement.** L'avocat a besoin de la jurisprudence **au moment où il regarde le résultat du calcul**, pour :
1. Comprendre **pourquoi** ce résultat (ex. plafond barème Macron 20 mois = quelle est la jurisprudence applicable ?)
2. Pouvoir **citer** l'arrêt dans ses conclusions (F-98 réutilisera ce mapping)
3. Vérifier que la jurisprudence est **récente** (date de dernière vérification visible)

Tout autre placement serait incohérent :
- ❌ Bloc séparé sur l'onglet Décision → coupé du contexte du calcul, déconnecté
- ❌ Sur l'écran synthèse (`SynthesisComponent`) → la synthèse couvre les points juridiques généraux du dossier ; F-JU-01 est lié à un calcul spécifique d'outil
- ❌ Onglet dédié « Jurisprudence » → fragmente l'information, casse le flow « je calcule → je vois le résultat → je vois la juris »

**Verdict placement écran 1 : ✅ GO** — modal d'outil sous le résultat.

### Écran 2 — Dashboard super-admin

**Oui, cohérent avec le pattern.** Les 4 écrans super-admin existants (`/super-admin`, `/super-admin/backlog`, `/super-admin/blog`, `/super-admin/traction-onepager`) sont des écrans **monobloc consultatifs** non destinés à l'avocat. F-JU-01 ajoute le 5ème sur le même pattern.

Alternative écartée : onglet « Veille juris » dans `/super-admin/backlog` (à côté de Produit / Marketing / Audit dashboard). Rejet : la veille jurisprudentielle n'a aucun lien sémantique avec le pilotage backlog produit/marketing. Mauvais regroupement.

**Verdict placement écran 2 : ✅ GO** — écran dédié `/super-admin/jurisprudence-watch`.

---

## Challenge lisibilité de la séquence

**Question : l'UI rend-elle visible l'ordre des étapes ?**

### Écran 1 — Modal d'outil

La séquence visuelle dans le modal devient :

```
┌─ MatDialog 90vw/90vh ──────────────────────────────────────┐
│ [Titre de l'outil] (header)                                 │
│                                                              │
│ [Formulaire — champs pré-remplis IA, badges F-155/F-IA-03]  │
│                                                              │
│ [Résultat du calcul] ← sortie du calculator                  │
│   ┌──────────────────────────────────────────────┐         │
│   │ Indemnité licenciement : 75 600 €             │         │
│   │ Méthode : barème Macron, plafond 20 mois      │         │
│   └──────────────────────────────────────────────┘         │
│                                                              │
│ ⬇ NOUVEAU F-JU-01 — sous le résultat                        │
│   ┌──────────────────────────────────────────────┐         │
│   │ Jurisprudence applicable                      │         │
│   │ ─────────────────────────────────────         │         │
│   │ • Cass. soc. 8 janv. 2025, n°23-12345         │         │
│   │   « [chapeau officiel Cassation cité          │         │
│   │      textuellement, 1-2 phrases] »            │         │
│   │   🔗 Légifrance  ⚠ Signaler                   │         │
│   │                                                │         │
│   │ • Cass. soc. 12 mars 2024, n°22-XXX           │         │
│   │   « [chapeau] »                                │         │
│   │   🔗 Légifrance  ⚠ Signaler                   │         │
│   │                                                │         │
│   │ • Cass. soc. 5 sept. 2023, n°21-YYY           │         │
│   │   « [chapeau] »                                │         │
│   │   🔗 Légifrance  ⚠ Signaler                   │         │
│   │                                                │         │
│   │ Citation indicative — dernière vérification : │         │
│   │ 15/05/2026. L'avocat reste seul juge.         │         │
│   └──────────────────────────────────────────────┘         │
│                                                              │
│ [Footer : Annuler] [Enregistrer]                             │
└─────────────────────────────────────────────────────────────┘
```

L'ordre visuel **formulaire → résultat → jurisprudence** est sémantiquement clair : on calcule, on lit le résultat, on voit la juris qui fonde ce résultat. Pas d'effort cognitif d'orientation.

**Risque** : si le modal devient trop long verticalement, les arrêts en bas pourraient nécessiter un scroll. **Mitigation** : le modal F-177 fait déjà 90vh, l'ascenseur est natif et accepté par l'UX du modal. Pas d'ajustement requis.

**Verdict lisibilité écran 1 : ✅ GO** — séquence évidente.

### Écran 2 — Dashboard super-admin

Pas de séquence multi-étapes à rendre visible (écran de consultation/arbitrage, pas de parcours). Header → flags → audit log = ordre de priorité décroissante (urgent → informatif), même logique que F-180 Audit dashboard.

**Verdict lisibilité écran 2 : ✅ GO**.

---

## Challenge charge écran

**Question : quelle est la densité TOTALE de l'écran cible APRÈS ajout ?**

### Écran 1 — Onglet Décision (détail du dossier)

| Bloc primaire | État | F-JU-01 ? |
|---|---|---|
| 1. `app-decisional-tools-panel` | Existant | F-JU-01 ajoute un bloc INTERNE dans chaque composant outil ouvert en modal — **pas un nouveau bloc primaire de l'onglet** |
| 2. `app-case-dashboard` | Existant | aucun changement V1 |
| 3. `app-conclusions-section` (F-98) | Existant | aucun changement direct V1 — peut puiser dans les mappings F-JU-01 pour les citations |

→ **L'onglet Décision reste à 3 blocs primaires.** F-JU-01 enrichit l'**intérieur** d'un bloc existant (`app-decisional-tools-panel` via les composants outils dans le modal), pas la liste des blocs primaires. **Conforme à l'invariant « 3 blocs primaires max par onglet »** établi par F-244 et confirmé F-206, F-214.

### Charge interne du modal d'outil

Le modal F-177 contient aujourd'hui : header (titre) + formulaire + résultat + footer actions. F-JU-01 ajoute un bloc « Jurisprudence applicable » entre résultat et footer.

| Section modal | Hauteur indicative |
|---|---|
| Header titre | ~50 px |
| Formulaire | variable selon outil (~100-500 px) |
| Résultat calcul | ~100-200 px |
| **Bloc F-JU-01 (NOUVEAU)** | **~250-350 px (3 arrêts en liste compacte)** |
| Footer actions | ~60 px |

Total post-F-JU-01 : ~560-1160 px. Modal disponible : 90vh ≈ 800 px sur un écran 900 px. **Probable nécessité d'un scroll vertical** pour les outils avec gros formulaires. **Accepté** : le scroll natif du modal est déjà admis par F-177 sur les outils complexes (ex. F-DT-30 indemnité licenciement avec 12 champs).

### Écran 2 — Dashboard super-admin

Écran neuf, 3 sections compactes. Aucune surcharge. Conforme au pattern existant (`/super-admin/blog`, `/super-admin/traction-onepager`).

**Verdict charge écran : ✅ GO** — pas de nouveau bloc primaire ailleurs, scroll modal accepté, écran admin compact.

---

## Challenge état final / continuité

**Question : après l'output de la feature, que fait l'avocat ?**

### Côté avocat (écran 1)

Après lecture des 3 arrêts dans le modal, l'avocat dispose de **5 options de continuité explicites** :

1. **Lire un arrêt en détail** → clic sur « 🔗 Légifrance » → ouvre Légifrance dans un nouvel onglet du navigateur (pas de navigation interne perdante) → l'avocat revient au modal LegalCase qui reste ouvert
2. **Approfondir via Doctrine / Lexis Plus / Lextenso** → bouton F-241 (déjà présent sur la synthèse) — extension possible V2 : ajouter le bouton F-241 dans le bloc citations F-JU-01 pour ouvrir une recherche éditeur sur le même arrêt
3. **Signaler un problème** → bouton « ⚠ Signaler » inline (pas de modal lourde, juste un petit prompt « pourquoi ce signalement ? » optionnel + envoi) → toast de confirmation + flag remonté au dashboard admin
4. **Fermer le modal** → retour à l'onglet Décision, l'avocat continue son traitement (autre outil, dashboard, conclusions)
5. **Générer les conclusions** (F-98, étape 8 du parcours) → les arrêts mappés F-JU-01 peuvent être réutilisés (à clarifier en mini-spec : F-98 puise-t-elle dans `tool_jurisprudence_mappings` ?)

Toutes les sorties sont **explicites**, **non-perdantes** (lien externe en nouvel onglet, signalement asynchrone). Pas de ping-pong subi entre écrans.

**Verdict continuité écran 1 : ✅ GO**.

### Côté super-admin (écran 2)

Après arbitrage d'un flag (clic « remplacer » / « ajouter » / « ignorer ») : le flag disparaît de la liste, l'admin reste sur l'écran pour traiter d'autres flags. Audit log mis à jour. Pas de navigation forcée.

Si pas de flag à traiter : écran vide → email mensuel récap → admin peut quitter sans aucune action.

**Verdict continuité écran 2 : ✅ GO**.

---

## Ajustements IA requis

Pas de blocage. Ajustements à formaliser dans la mini-spec SF-JU-01-04 (frontend) :

1. **Bloc citations compact** dans le modal d'outil : titre court (« Cass. soc. JJ/MM/AAAA, n° X »), chapeau officiel 1-2 phrases visibles (pas dépliable verticalement de 500 px), liens et bouton signaler inline.
2. **Lien Légifrance** : `target="_blank" rel="noopener noreferrer"` obligatoire — pas de navigation interne LegalCase vers Légifrance.
3. **Bouton « Signaler »** : prompt minimaliste (1 champ texte optionnel + bouton envoyer), pas de MatDialog imbriqué dans le modal d'outil (un modal dans un modal serait surcharge).
4. **Affichage conditionnel** : si le mapping est vide (Claude < 60 % confiance OU outil non éligible), le bloc « Jurisprudence applicable » est **absent** du modal (pas affiché avec un message « pas de citation disponible »). Silence > erreur > placeholder.
5. **Réactif aux changements de branche** : si l'avocat modifie une saisie qui change la branche de calcul active (ex. ancienneté passe de 8 à 15 ans → bascule barème), le bloc citations se met à jour automatiquement (Observable `branchActive$`).
6. **Dashboard super-admin** : écran 1-page, pas de sous-pages ou drill-down enchaîné. Tout est visible à plat. Actions inline (3 boutons par flag), audit log en bas en lazy load.
7. **Email mensuel récap** : remplace l'obligation d'ouvrir le dashboard. Contenu : compteurs (arrêts traités, confirmations, ajouts, remplacements, contradictions non résolues) + lien direct vers `/super-admin/jurisprudence-watch` si besoin de creuser.
8. **Lien F-241 vers Doctrine sur chaque arrêt cité** : V2, à clarifier post-livraison F-JU-01 V1 selon signal terrain (« je voudrais creuser cet arrêt précis dans Doctrine »).

---

## Invariants anti-surcharge pour la mini-spec

1. **Pas de nouveau bloc primaire sur l'onglet Décision** — F-JU-01 enrichit l'intérieur de `app-decisional-tools-panel` via les composants outils. L'onglet Décision reste à 3 blocs primaires (`app-decisional-tools-panel`, `app-case-dashboard`, `app-conclusions-section`). Toute tentative de créer un bloc primaire « Jurisprudence dossier » au niveau de l'onglet → REFUS.

2. **Pas de bloc primaire ailleurs** — F-JU-01 ne pose **rien** sur l'onglet Dossier, l'onglet Analyse (écran synthèse), l'onglet Suivi, ni l'en-tête. Le bloc citations est strictement confiné au modal d'outil.

3. **Bloc citations compact ≤ 350 px** dans le modal — top-3 en liste, chapeaux 1-2 phrases visibles sans dépliage. Pas de section « historique des arrêts archivés », pas de timeline, pas de graphe.

4. **Lien externe = nouvel onglet** systématique. Aucune navigation interne LegalCase → Légifrance (perd l'avocat).

5. **Pas de modal imbriqué dans le modal d'outil**. Le bouton « Signaler » utilise un prompt inline ou un toast d'action, pas un `MatDialog` qui s'ouvre par-dessus le `MatDialog` de l'outil.

6. **Affichage conditionnel strict** — si mapping vide, bloc absent du DOM. Pas de placeholder « Aucune citation disponible » qui pollue l'écran sur les outils non éligibles (~40-50 outils non éligibles sur 131).

7. **Réactivité branche active** — le bloc se met à jour automatiquement quand la saisie avocat change la branche de calcul. Pas de bouton « Rafraîchir la jurisprudence » manuel.

8. **Dashboard super-admin 1-page** — pas de sous-pages, pas de breadcrumbs, pas de drill-down. Tout est visible en une seule vue avec actions inline.

9. **Email mensuel récap obligatoire** — pas un nice-to-have. C'est lui qui permet à l'admin de ne **pas** ouvrir le dashboard pendant 6 mois sans rater un signal critique.

10. **Mention de prudence visible** dans le bloc citations — « Citation indicative — date de dernière vérification : XX/XX/XXXX. L'avocat reste seul juge. » Non négociable (responsabilité juridique).

11. **Continuité F-98 conclusions** — invariant à clarifier en mini-spec : `app-conclusions-section` (F-98) doit pouvoir puiser dans `tool_jurisprudence_mappings` pour citer les arrêts du dossier dans les conclusions générées. Sinon F-JU-01 reste isolé du flux principal vers l'état terminal du parcours.

12. **Continuité F-241 (V2)** — possibilité d'ajouter un bouton « Ouvrir dans Doctrine » à côté de chaque arrêt cité, pour ouvrir une recherche éditeur sur le même arrêt. À implémenter en V2 selon signal terrain.

---

## Décision finale

**GO avec ajustements** (8 ajustements IA, 12 invariants anti-surcharge).

L'étape 1 mini-spec (SF-JU-01-01 backend) peut démarrer. Les ajustements écran sont à reprendre dans la mini-spec SF-JU-01-04 (frontend composant citations + bouton signaler) et SF-JU-01-05 (dashboard super-admin).

**Prochaine étape** : étape 1 mini-spec — `SF-JU-01-01-backend-infrastructure.md` peut être rédigée, avec les invariants ci-dessus repris en critères d'acceptation des SF frontend.

---

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-dossier.md` reçoit son **8ᵉ passage** :
- Ajout d'une **étape 7 bis** au parcours (lecture des arrêts dans le modal d'outil)
- Mention du **bloc « Jurisprudence applicable »** dans le modal F-177 (interne aux composants outils, pas un nouveau bloc primaire)
- Invariant **« pas de bloc primaire jurisprudence sur l'onglet Décision »** ajouté
- Renforcement de la chaîne jurisprudence : F-179 (vérification arrêts adverses, écran synthèse) + F-241 (deeplinks, écran synthèse + V2 modal outil) + F-242 (citation manuelle, écran synthèse) + **F-JU-01 (citations proactives, modal outil)** = 4 briques distinctes, à libellés non confondables.

`docs/business/parcours-ecran-super-admin.md` reçoit l'ajout du **5ᵉ écran super-admin** :
- `/super-admin/jurisprudence-watch` — pilotage de la veille jurisprudentielle (flags utilisateurs + audit log cron mensuel + bouton « Relancer maintenant »)
- Pattern : monobloc consultatif comme `/super-admin/blog` et `/super-admin/traction-onepager`
- Note : « 0 ouverture obligatoire » en régime stationnaire grâce à l'email mensuel récap

---

## Sources

- `docs/business/parcours-ecran-dossier.md` — référentiel parcours détail dossier (7 passages au 2026-05-20)
- `docs/business/parcours-ecran-super-admin.md` — référentiel parcours super-admin
- `docs/features/F-JU-01/SF-JU-01-00-coherence.md` — verdict étape 0 GO
- `docs/PRODUCT_SPEC.md` entrée F-JU-01 (ligne 264)
- F-177 — refonte modal outils décisionnels (90vw/90vh, MatDialog)
- F-244 — structure 4 onglets `case-file-detail.component`
- F-98 — conclusions générées (état terminal du parcours)
