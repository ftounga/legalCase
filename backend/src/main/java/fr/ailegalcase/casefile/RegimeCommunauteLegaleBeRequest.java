package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-217-01 : requête HTTP de l'endpoint d'analyse du régime de communauté
 * légale belge.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record RegimeCommunauteLegaleBeRequest(
        LocalDate dateMariage,
        Boolean contratMariageSigne,
        List<BienRequest> biens,
        List<DetteRequest> dettes
) {

    /** Bien transmis dans le body de requête. */
    public record BienRequest(
            String libelle,
            BienCategorieBe categorie,
            Boolean acquisAvantMariage,
            Boolean acquisParDonationOuSuccession,
            Boolean financeParFondsPropres,
            Boolean affectationProfessionnelle,
            Boolean outilDeTravailPersonnel,
            String commentaire
    ) {}

    /** Dette transmise dans le body de requête. */
    public record DetteRequest(
            String libelle,
            Boolean anterieureAuMariage,
            Boolean contracteeDansLInteretDuMenage,
            Boolean contracteeParUnSeulEpoux,
            String commentaire
    ) {}

    RegimeCommunauteLegaleBeInput toInput() {
        List<RegimeCommunauteLegaleBeInput.BienItem> biensItems =
                (biens == null ? List.<BienRequest>of() : biens).stream()
                        .map(b -> b == null ? null : new RegimeCommunauteLegaleBeInput.BienItem(
                                b.libelle(),
                                b.categorie(),
                                b.acquisAvantMariage(),
                                b.acquisParDonationOuSuccession(),
                                b.financeParFondsPropres(),
                                b.affectationProfessionnelle(),
                                b.outilDeTravailPersonnel(),
                                b.commentaire()))
                        .toList();
        List<RegimeCommunauteLegaleBeInput.DetteItem> dettesItems =
                (dettes == null ? List.<DetteRequest>of() : dettes).stream()
                        .map(d -> d == null ? null : new RegimeCommunauteLegaleBeInput.DetteItem(
                                d.libelle(),
                                d.anterieureAuMariage(),
                                d.contracteeDansLInteretDuMenage(),
                                d.contracteeParUnSeulEpoux(),
                                d.commentaire()))
                        .toList();
        return new RegimeCommunauteLegaleBeInput(
                dateMariage,
                contratMariageSigne,
                biens == null ? null : biensItems,
                dettes == null ? null : dettesItems
        );
    }
}
