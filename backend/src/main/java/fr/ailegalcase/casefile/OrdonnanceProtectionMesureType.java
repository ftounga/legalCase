package fr.ailegalcase.casefile;

/**
 * SF-FA-14-01 : mesures susceptibles d'être ordonnées par le JAF (art. 515-11
 * Cciv + Loi 30/07/2020 BAR). L'outil filtre les mesures recommandées en
 * intersection avec le contexte (logementCommun pour EVICTION, dangerImmediat
 * pour TGD/BAR/DEC, presenceEnfants pour RESIDENCE_ENFANTS).
 *
 * <p>SF-222-05 : DEC (Dispositif Électronique de Contrôle — suivi électronique
 * anti-rapprochement du contact) ajouté comme branche conditionnelle voisine du
 * BAR. Recommandé uniquement si l'avocat envisage le DEC ({@code decEnvisage})
 * <b>et</b> que le danger immédiat est caractérisé (même condition que le BAR).
 */
public enum OrdonnanceProtectionMesureType {
    EVICTION_CONJOINT,
    INTERDICTION_APPROCHER,
    TGD,
    BAR,
    DEC,
    INTERDICTION_PARAITRE,
    OBLIGATION_SOIN,
    RESIDENCE_ENFANTS
}
