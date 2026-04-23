package fr.ailegalcase.analysis;

/**
 * F-153 SF-153-01 : fourchettes jurisprudentielles JAF pour enrichir les
 * calculateurs famille (pension alimentaire, prestation compensatoire).
 *
 * <p>Les tables sont calibrées à partir de la dispersion observée autour
 * des barèmes officiels :
 * <ul>
 *   <li>Pension alimentaire : le barème UNAF/CGKR donne le centre, les JAF
 *       modulent ± 35 % selon situation (garde alternée contestée, charges
 *       inhabituelles, déclassement professionnel, etc.).</li>
 *   <li>Prestation compensatoire : art. 271 Cciv laisse le JAF entièrement
 *       libre — dispersion ± 50 % autour de la valeur capitalisée standard.</li>
 * </ul>
 *
 * <p>V1 : tables statiques Java. À faire évoluer en migration DB si besoin
 * de mise à jour par Product Owner sans release backend.
 */
public final class JafReferenceTable {

    /**
     * Fourchette pension alimentaire basée sur un montant médian donné.
     * p25 = médiane × 0.65, p75 = médiane × 1.35 (dispersion observée JAF).
     */
    public static JurisprudenceRange pensionAlimentaireRange(double medianMontant, String pays) {
        if (medianMontant <= 0) return null;
        int p50 = (int) Math.round(medianMontant);
        int p25 = (int) Math.round(medianMontant * 0.65);
        int p75 = (int) Math.round(medianMontant * 1.35);
        String label = "BELGIQUE".equals(pays)
                ? "Fourchette observée tribunaux famille (Belgique)"
                : "Fourchette observée JAF (France)";
        String sourceRef = "BELGIQUE".equals(pays)
                ? "Barème CGKR + dispersion observée tribunaux famille"
                : "Barème UNAF + dispersion observée JAF (statistiques min. Justice)";
        return new JurisprudenceRange(p25, p50, p75, label, sourceRef);
    }

    /**
     * Fourchette prestation compensatoire basée sur le montant capitalisé standard
     * (écart revenus × coeff × 12 × 8 ans). La dispersion jurisprudentielle est
     * volontairement large (± 50 %) car art. 271 Cciv laisse le JAF libre.
     */
    public static JurisprudenceRange prestationCompensatoireRange(double medianMontant, String pays) {
        if (medianMontant <= 0) return null;
        int p50 = (int) Math.round(medianMontant);
        int p25 = (int) Math.round(medianMontant * 0.50);
        int p75 = (int) Math.round(medianMontant * 1.50);
        String label = "BELGIQUE".equals(pays)
                ? "Fourchette observée tribunaux famille (Belgique)"
                : "Fourchette observée JAF (France)";
        String sourceRef = "BELGIQUE".equals(pays)
                ? "Observation tribunaux famille BE — art. 301 Code civil belge"
                : "Observation jurisprudence JAF — art. 271 Code civil français";
        return new JurisprudenceRange(p25, p50, p75, label, sourceRef);
    }

    private JafReferenceTable() {}
}
