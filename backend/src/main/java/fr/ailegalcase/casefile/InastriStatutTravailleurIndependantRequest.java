package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * SF-219-27 : payload de création / mise à jour de l'analyse <i>INASTI
 * — statut travailleur indépendant et qualification salarié /
 * indépendant</i> (Loi du 27/06/1969 + AR n° 38 du 27/07/1967 +
 * Loi-programme I du 27/12/2006 art. 328 à 333 + art. 337/1 à 337/3
 * critères sectoriels).
 *
 * <h2>Logique métier — critères cumulatifs</h2>
 *
 * <h3>4 critères généraux art. 333 Loi-programme I 27/12/2006</h3>
 * <ol>
 *   <li><b>Volonté des parties</b> ({@code volonteParties}) — telle
 *       qu'exprimée dans la convention initiale (indice non
 *       décisif : primauté de l'exécution réelle).</li>
 *   <li><b>Liberté d'organisation du temps de travail</b>
 *       ({@code liberteOrganisationTemps}) — la personne fixe-t-elle
 *       elle-même ses horaires, congés, jours non travaillés ?</li>
 *   <li><b>Liberté d'organisation du travail</b>
 *       ({@code liberteOrganisationTravail}) — la personne choisit-elle
 *       ses méthodes, ses outils, ses sous-traitants éventuels ?</li>
 *   <li><b>Exercice d'un contrôle hiérarchique</b>
 *       ({@code controleHierarchique}) — l'employeur exerce-t-il un
 *       pouvoir de direction et de sanction analogue à celui d'un
 *       chef d'entreprise vis-à-vis d'un salarié ?</li>
 * </ol>
 *
 * <h3>Critères sectoriels art. 337/2 § 1 (si secteur visé)</h3>
 * <ol>
 *   <li><b>Secteur</b> ({@code secteur}) — construction, transport
 *       de choses, gardiennage, nettoyage, agriculture/horticulture
 *       ou AUTRE.</li>
 *   <li><b>Nombre de critères sectoriels salarié</b>
 *       ({@code nbCriteresSectorielsSalarie}) — sur les 9 critères
 *       sectoriels de l'art. 337/2 § 1, combien indiquent une
 *       relation salariée ? Si secteur AUTRE, ce champ est ignoré.</li>
 * </ol>
 *
 * <h3>Contexte</h3>
 * <ol>
 *   <li><b>Statut déclaré</b> ({@code statutDeclare}) — INDEPENDANT_INASTI
 *       / SALARIE_DIMONA / SANS_DECLARATION.</li>
 *   <li><b>Présence d'une DIMONA</b> ({@code dimonaPresente}) —
 *       déclenche-t-elle la présomption simple de salariat art. 328
 *       § 1 C. pén. soc. en cas d'absence ?</li>
 *   <li><b>Présence d'un autre client</b>
 *       ({@code autreClientPresent}) — indice d'autonomie
 *       économique. L'absence d'autre client (mono-client) est un
 *       indice de salariat déguisé (Cass. BE 23/12/2002).</li>
 * </ol>
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 27/06/1969</b> révisant l'arrêté-loi du 28/12/1944
 *       concernant la sécurité sociale des travailleurs salariés.</li>
 *   <li><b>AR n° 38 du 27/07/1967</b> organisant le statut social
 *       des travailleurs indépendants — affiliation caisse
 *       d'assurances sociales et cotisations.</li>
 *   <li><b>AR du 19/12/1967</b> portant règlement général en
 *       exécution de l'AR n° 38 — modalités pratiques.</li>
 *   <li><b>Loi-programme I du 27/12/2006</b> art. 328 à 333 « doctrine
 *       Bart Buysse » — 4 critères généraux de qualification +
 *       présomption simple de salariat art. 328 § 1.</li>
 *   <li><b>Loi-programme I du 27/12/2006</b> art. 337/1 à 337/3 —
 *       critères sectoriels (introduits par Loi du 25/08/2012,
 *       extension agriculture/horticulture AR du 29/10/2013).</li>
 *   <li><b>Cass. BE 23/12/2002 S.02.0021.F</b> — primauté de
 *       l'exécution réelle sur la qualification conventionnelle.</li>
 *   <li><b>Cass. BE 06/12/2010 S.10.0067.F</b> — caractère simple
 *       (renversable) de la présomption art. 328 § 1.</li>
 *   <li><b>Cour const. arrêt 167/2013 du 19/12/2013</b> —
 *       conformité de la liste sectorielle et de la présomption
 *       sectorielle art. 337/2 aux principes du procès équitable.</li>
 * </ul>
 */
public record InastriStatutTravailleurIndependantRequest(

        @NotNull(message = "volonteParties est requis")
        InastriStatutTravailleurIndependantVolonte volonteParties,

        @NotNull(message = "liberteOrganisationTemps est requis")
        InastriStatutTravailleurIndependantLiberte liberteOrganisationTemps,

        @NotNull(message = "liberteOrganisationTravail est requis")
        InastriStatutTravailleurIndependantLiberte liberteOrganisationTravail,

        @NotNull(message = "controleHierarchique est requis")
        InastriStatutTravailleurIndependantLiberte controleHierarchique,

        @NotNull(message = "secteur est requis")
        InastriStatutTravailleurIndependantSecteur secteur,

        @NotNull(message = "nbCriteresSectorielsSalarie est requis")
        @Min(value = 0,
                message = "nbCriteresSectorielsSalarie doit être entre 0 et 9")
        @Max(value = 9,
                message = "nbCriteresSectorielsSalarie doit être entre 0 et 9")
        Integer nbCriteresSectorielsSalarie,

        @NotNull(message = "statutDeclare est requis")
        InastriStatutTravailleurIndependantStatutDeclare statutDeclare,

        @NotNull(message = "dimonaPresente est requis")
        Boolean dimonaPresente,

        @NotNull(message = "autreClientPresent est requis")
        Boolean autreClientPresent
) {
}
