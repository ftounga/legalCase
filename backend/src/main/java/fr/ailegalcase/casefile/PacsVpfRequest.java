package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-220-04 : requête POST pour l'outil décisionnel « VPF au titre d'un PACS »
 * (F-IM-50-pacs-vpf-fr, CESEDA L.423-23). Outil single-country FR.
 *
 * <p>Le PACS est apprécié comme <b>faisceau d'indices</b> de vie privée et
 * familiale, pas comme droit automatique au séjour (distinct du conjoint marié
 * F-IM-21). {@code partenaireStatut} et {@code intensiteCommunauteVie} sont des
 * codes enum validés en amont.</p>
 */
public record PacsVpfRequest(
        Boolean pacsConclu,
        LocalDate datePacs,
        String partenaireStatut,
        Integer dureeVieCommuneMois,
        String intensiteCommunauteVie,
        Boolean autresLiensPrivesFamiliaux
) {}
