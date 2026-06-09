# Recette Apollo — acquisition avocat, 3 domaines (test PMF)

But : sourcer des **cabinets d'avocats multi-avocats** (pas de solos) spécialisés sur
les 3 domaines que LegalCase sert réellement, pour un test discovery comparant
**où est le PMF côté avocat**. Méthode = Apollo (voie A, export CSV) → tri/dédoublonnage
→ recherche par contact → mail personnalisé → envoi (même pipeline que les 23 DRH).

Cible aval : décrocher des démos / essais, comparer le taux de réponse entre domaines.
Volume conseillé : **~6 contacts par domaine = ~18** (comparabilité > volume).

## Filtres communs (à appliquer dans Apollo > Search > People)
- **Location** : France.
- **Industry** : Law Practice / Legal Services (cabinets d'avocats — PAS les juristes d'entreprise).
- **Job titles** : `Avocat associé`, `Associé`, `Avocat` (cibler les **décideurs** : associés/fondateurs/gérants ;
  éviter « collaborateur » qui ne décide pas de l'achat).
- **Employees** : `11-20`, `21-50`, `51-200` (= cabinets à plusieurs avocats ; on saute les solos,
  cf. logique volume). Si trop peu de résultats sur une niche, relâcher à `1-10` mais garder les
  structures affichant **plusieurs avocats**.

## Mots-clés société (company keywords) — UNE recherche par domaine
Lancer 3 recherches distinctes, exporter ~6 contacts chacune :

1. **Droit du travail** → `droit du travail`, `droit social`
2. **Immigration / droit des étrangers** → `droit des étrangers`, `droit de l'immigration`, `droit de la nationalité`
3. **Droit de la famille** → `droit de la famille`, `divorce`, `droit patrimonial de la famille`

## Export
- 2 contacts mini / 6 conseillés par domaine → **Save to list** / **Export CSV**.
- Peu importe le nom du fichier (`apollo-contacts-export (2).csv`, etc.) : le pipeline détecte
  tout `.csv` récent dans `~/Downloads`.

## Pipeline aval (côté Claude, identique aux DRH)
1. Fusion + dédoublonnage + **tag domaine** + contrôle adéquation (cabinet privé, multi-avocats,
   bonne spécialité) → `drh-leads`-équivalent `avocat-leads.csv`.
2. Recherche web par contact (1 fait public vérifié : actualité cabinet, spécialité, publication…),
   **zéro invention**, fallback générique si rien.
3. Mail personnalisé par domaine (angle = le pipeline LegalCase sur SON domaine :
   travail = exposition prud'homale ; immigration = délais/recours/validité dossier ;
   famille = liquidation/pension/prestation compensatoire).
4. Envoi étalé + tableau de suivi + relance unique J+5.

## Garde-fous (leçons des DRH)
- Écarter les **juristes d'entreprise** (on veut des cabinets, pas des directions juridiques).
- Vérifier que le cabinet est **réellement spécialisé** sur le domaine (un généraliste avec un
  vague pôle social = signal faible).
- Surveiller les emails `catch-all`/`invalid` → fallback LinkedIn.
