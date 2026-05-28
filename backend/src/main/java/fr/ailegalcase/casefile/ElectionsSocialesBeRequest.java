package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * SF-219-09 : payload de création / mise à jour de l'analyse des
 * élections sociales BE (Loi du 04/12/2007 + AR du 25/05/2012).
 *
 * <p>Outil <b>BE-only</b> — les élections CSE françaises (Code du
 * travail art. L. 2311-1 et s.) ont un calendrier libre déclenché
 * par l'employeur à tout moment dans le cycle 4 ans, des seuils
 * différents (11 / 50 / 300), pas de jour Y national imposé. Aucun
 * mapping possible.</p>
 *
 * <h2>Logique métier (calculateur + checklist)</h2>
 * <ol>
 *   <li><b>Détermination de l'obligation</b> selon l'effectif moyen
 *       calculé sur les 4 trimestres précédant le 1er trimestre de
 *       l'année des élections — seuils : ≥ 50 (CPPT), ≥ 100 (CE +
 *       CPPT).</li>
 *   <li><b>Détermination du jour Y</b> dans la fenêtre nationale 14 j
 *       imposée par AR.</li>
 *   <li><b>Calendrier complet</b> calculé à partir de Y :
 *     <ul>
 *       <li>Jour X = Y - 90 : affichage de l'avis des élections.</li>
 *       <li>Jour X - 60 : début de la procédure d'unité technique
 *           d'exploitation (UTE).</li>
 *       <li>Jour X - 35 : transmission des listes électorales.</li>
 *       <li>Jour X + 35 : dépôt des listes de candidats.</li>
 *       <li>Jour X + 40 : affichage des candidats — déclenche la
 *           période protégée pour licenciement (Loi 19/03/1991).</li>
 *       <li>Jour Y : scrutin.</li>
 *       <li>Jour Y + 6 : proclamation des résultats.</li>
 *       <li>Jour Y + 45 : première réunion CE / CPPT installé.</li>
 *     </ul>
 *   </li>
 *   <li><b>Fenêtre de protection des candidats</b> contre le
 *       licenciement (Loi 19/03/1991 art. 2 §1) : 30 jours avant
 *       l'affichage candidats (occulte rétroactive jour X+10) jusqu'à
 *       l'installation des élus du cycle suivant (élus) ou 2 ans
 *       après la date d'affichage (non-élus présentés pour la 1re
 *       fois).</li>
 * </ol>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code cycleElectoral} — cycle 4 ans : 2024 / 2028 / 2032.</li>
 *   <li>{@code dateJourY} — date du scrutin choisie par l'employeur
 *       dans la fenêtre AR (validation : doit être dans la fenêtre
 *       du cycle déclaré).</li>
 *   <li>{@code effectifMoyenEtp} — effectif moyen en ETP calculé sur
 *       les 4 trimestres précédents.</li>
 *   <li>{@code uniteTechniqueExploitationConfirmee} — l'UTE retenue
 *       a-t-elle déjà été confirmée par les organes paritaires /
 *       décision juridictionnelle (sinon procédure UTE à initier
 *       X-60).</li>
 * </ul>
 */
public record ElectionsSocialesBeRequest(

        @NotNull(message = "cycleElectoral est requis")
        ElectionsSocialesBeCycle cycleElectoral,

        @NotNull(message = "dateJourY est requise")
        LocalDate dateJourY,

        @NotNull(message = "effectifMoyenEtp est requis")
        @PositiveOrZero(message = "effectifMoyenEtp doit être ≥ 0")
        Integer effectifMoyenEtp,

        @NotNull(message = "uniteTechniqueExploitationConfirmee est requis")
        Boolean uniteTechniqueExploitationConfirmee
) {
}
