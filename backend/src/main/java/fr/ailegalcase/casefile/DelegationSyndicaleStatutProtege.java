package fr.ailegalcase.casefile;

/**
 * SF-218-33 : statut de salarié protégé du DS / RSS (art. L.2411-3 CT, F-DT-69).
 * Outil <b>FRANCE UNIQUEMENT</b>. Le délégué syndical et le représentant de
 * section syndicale sont, par construction, des salariés protégés ; ce statut
 * est donc toujours {@code OUI}. L'enum est conservé pour la clarté du contrat
 * de réponse et son éventuelle évolution.
 */
public enum DelegationSyndicaleStatutProtege {
    OUI
}
