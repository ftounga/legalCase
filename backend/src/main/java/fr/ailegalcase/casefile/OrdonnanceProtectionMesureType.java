package fr.ailegalcase.casefile;

/**
 * SF-FA-14-01 : mesures susceptibles d'être ordonnées par le JAF (art. 515-11
 * Cciv + Loi 30/07/2020 BAR). L'outil filtre les mesures recommandées en
 * intersection avec le contexte (logementCommun pour EVICTION, dangerImmediat
 * pour TGD/BAR, presenceEnfants pour RESIDENCE_ENFANTS).
 */
public enum OrdonnanceProtectionMesureType {
    EVICTION_CONJOINT,
    INTERDICTION_APPROCHER,
    TGD,
    BAR,
    INTERDICTION_PARAITRE,
    OBLIGATION_SOIN,
    RESIDENCE_ENFANTS
}
