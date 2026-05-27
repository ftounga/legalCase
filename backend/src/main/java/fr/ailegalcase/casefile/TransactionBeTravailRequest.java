package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-213-06 : payload de création / mise à jour d'une analyse de validité
 * d'une transaction de fin de contrat en droit belge (art. 2044 Code civil
 * belge + Loi 03/07/1978 art. 6).
 *
 * <p>Outil <b>BE-only</b> — la France a un outil distinct
 * (transaction-fin-contrat FR couvert par F-DT-31) avec un régime
 * juridique différent. En BE, les renonciations doivent être
 * <b>expresses et spécifiques</b> (pas de renonciation globale « à tous
 * droits ») et certains droits sont <b>indisponibles</b> (créances
 * salariales futures, dispositions impératives CCT).</p>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code montantTransactionBrut} : montant total brut proposé en €,
 *       strictement positif.</li>
 *   <li>{@code renonciationsListees} : liste (peut être vide) des
 *       renonciations expresses du salarié. Une liste vide bascule le
 *       verdict en {@link TransactionBeTravailVerdict#A_COMPLETER}.</li>
 * </ul>
 *
 * <h2>Champs optionnels / flags IA</h2>
 * <ul>
 *   <li>{@code indemniteLegaleEtimee} : indemnité légale à laquelle le
 *       salarié aurait droit (ICP + CCT 109 etc.) ; déclenche le calcul du
 *       ratio et l'avertissement de lésion si fournie et &gt; 0.</li>
 *   <li>{@code concessionsEmployeurDescrites} : flag — l'employeur a-t-il
 *       concédé quelque chose ? Défaut {@code false} = verdict
 *       {@link TransactionBeTravailVerdict#INVALIDE}.</li>
 *   <li>{@code renonciationOrdrePublicDetectee} : flag IA — renonciation
 *       à un droit d'ordre public détectée. Défaut {@code false}.
 *       <b>Correctif orthographique</b> : la mini-spec écrivait
 *       « Punlic » (typo) — on utilise la forme correcte
 *       {@code renonciationOrdrePublicDetectee} en code.</li>
 *   <li>{@code mentionContestation} : flag — la transaction mentionne-t-elle
 *       une contestation née ou à naître ? Défaut {@code false} = verdict
 *       {@link TransactionBeTravailVerdict#A_COMPLETER}
 *       (raison {@link TransactionBeTravailRaisonInvalidite#LITIGE_NON_VISE}).</li>
 * </ul>
 *
 * @see TransactionBeTravailValidator
 */
public record TransactionBeTravailRequest(

        @NotNull(message = "montantTransactionBrut est requis")
        @DecimalMin(value = "0.01",
                message = "montantTransactionBrut doit être strictement positif")
        BigDecimal montantTransactionBrut,

        /**
         * Optionnel — indemnité légale estimée (ICP + CCT 109 etc.) en €.
         * Si {@code > 0}, déclenche le calcul du ratio
         * {@code montantTransactionBrut / indemniteLegaleEtimee × 100} et
         * l'avertissement de lésion si {@code ratio < 50 %}.
         */
        BigDecimal indemniteLegaleEtimee,

        /**
         * Défaut {@code false} — bascule le verdict en
         * {@link TransactionBeTravailVerdict#INVALIDE} si non coché
         * (art. 2044 Cciv : pas de transaction sans concessions
         * réciproques).
         */
        boolean concessionsEmployeurDescrites,

        @NotNull(message = "renonciationsListees est requise (peut être vide)")
        List<String> renonciationsListees,

        /**
         * Défaut {@code false} — si {@code true}, bascule le verdict en
         * {@link TransactionBeTravailVerdict#INVALIDE_PARTIELLE} (raison
         * {@link TransactionBeTravailRaisonInvalidite#RENONCIATION_ORDRE_PUBLIC}).
         *
         * <p><b>Correctif orthographique</b> : la mini-spec utilisait
         * « renonciationOrdrePunlicDetectee » (typo « Punlic ») — la
         * forme correcte « Public » est appliquée ici (champ et tests).</p>
         */
        boolean renonciationOrdrePublicDetectee,

        /**
         * Défaut {@code false} — bascule en
         * {@link TransactionBeTravailVerdict#A_COMPLETER} si non coché
         * (la transaction doit viser un litige né ou à naître pour
         * relever de l'art. 2044 Cciv).
         */
        boolean mentionContestation
) {
}
