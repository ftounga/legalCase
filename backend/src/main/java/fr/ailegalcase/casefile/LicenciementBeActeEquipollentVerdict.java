package fr.ailegalcase.casefile;

/**
 * SF-213-09 : verdict de l'outil acte équipollent à rupture BE
 * (Loi 03/07/1978 art. 20 + jurisprudence Cass. BE 23/12/1957).
 *
 * <ul>
 *   <li>{@link #ACTE_EQUIPOLLENT_PROBABLE} — modification unilatérale
 *       substantielle d'un élément essentiel du contrat : la rupture est
 *       très probablement imputable à l'employeur, ouvrant droit à
 *       l'indemnité compensatoire de préavis (ICP).</li>
 *   <li>{@link #PAS_ACTE_EQUIPOLLENT} — modification mineure relevant du
 *       Ius Variandi : pas d'acte équipollent caractérisé.</li>
 *   <li>{@link #RISQUE_ACCEPTATION_TACITE} — modification potentiellement
 *       substantielle mais le salarié n'a pas protesté dans un délai
 *       raisonnable (jurisprudence : ~30 j). Le silence prolongé peut
 *       valoir acceptation tacite — situation à risque pour le salarié.</li>
 *   <li>{@link #A_ANALYSER} — éléments insuffisants pour trancher (ampleur
 *       indéterminée ou modification substantielle mais élément essentiel
 *       contestable) : analyse approfondie de la jurisprudence requise.</li>
 * </ul>
 */
public enum LicenciementBeActeEquipollentVerdict {
    /** Acte équipollent à rupture probable — rupture imputable à l'employeur. */
    ACTE_EQUIPOLLENT_PROBABLE,
    /** Modification mineure — pas d'acte équipollent caractérisé. */
    PAS_ACTE_EQUIPOLLENT,
    /** Risque que le silence du salarié vaille acceptation tacite. */
    RISQUE_ACCEPTATION_TACITE,
    /** Analyse approfondie requise (ampleur indéterminée ou élément essentiel discutable). */
    A_ANALYSER
}
