# Critères Apollo utilisés — export DRH du 2026-06-09

Filtres exacts appliqués dans Apollo People Search pour produire la liste DRH
(2 exports → 30 contacts, cf. `drh-leads.csv` / `suivi-prospection-drh.csv`).

## Filtres
**# Employees** (3 tranches)
- 201-500
- 501-1000
- 1001-2000

**Job Titles** (include)
- Directeur des Ressources Humaines
- DRH
- Directeur des Affaires Sociales
- Responsable RH

**Person Locations**
- France

**Industry** (4)
- facilities services
- logistics & supply chain
- restaurants
- hospital & health care

---

## Enseignements (à réutiliser pour les prochains sourcing)
1. **Le filtre `hospital & health care` a fait remonter des hôpitaux PUBLICS** (fonction
   publique hospitalière → contentieux au tribunal administratif, hors valeur prud'homale).
   9 contacts écartés sur le 1er export. ➜ Pour cibler le médico-social **privé**, ajouter
   un *Exclude keywords* (`centre hospitalier`, `CHU`, `CHS`, `hôpital`) ou viser des
   groupes privés (Korian/Clariane, DomusVi, Emeis, LNA Santé, Colisée, polycliniques, ESPIC).
2. **`sécurité privée / surveillance` ABSENT de la liste Industry** ➜ c'est pourquoi le
   secteur « sécurité privée » est ressorti à **0 contact**. Apollo n'a pas d'industrie
   « sécurité privée » directe : pour ce secteur, filtrer par mots-clés société
   (`sécurité privée`, `gardiennage`, `surveillance humaine`) ou viser des entreprises
   nommées (Securitas, Seris, Fiducial Sécurité, Goron, Brink's France).
3. Les 4 industries couvrent bien : propreté/FM (`facilities services`),
   transport/logistique (`logistics & supply chain`), restauration (`restaurants`),
   médico-social (`hospital & health care`, mais à filtrer privé).
