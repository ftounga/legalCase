package fr.ailegalcase.casefile;

/**
 * SF-218-25 : secteur d'activité de l'employeur recourant au CDI de chantier /
 * d'opération (art. L.1223-8 et s. CT, F-DT-37). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>BTP : bâtiment et travaux publics — secteur historique du contrat de
 *       chantier (usage constant reconnu).</li>
 *   <li>INGENIERIE : ingénierie / bureaux d'études (convention Syntec) — usage
 *       constant reconnu.</li>
 *   <li>AUTRE : autre secteur recourant au CDI de chantier par accord de branche
 *       étendu (le seul usage constant ne suffit alors pas).</li>
 * </ul>
 */
public enum CdiChantierSecteur {
    BTP,
    INGENIERIE,
    AUTRE
}
