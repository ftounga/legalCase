package fr.ailegalcase.email;

/**
 * F-248 SF-248-01 : corps des requêtes {@code POST /unsubscribe} et {@code /resubscribe}.
 *
 * <p>Le format du token (UUID présent / bien formé) est validé dans
 * {@link EmailSubscriptionService} afin de renvoyer les libellés d'erreur
 * exacts du contrat ({@code Token requis} / {@code Token invalide}).
 */
public record EmailSubscriptionRequest(String token) {}
