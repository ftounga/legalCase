package fr.ailegalcase.casefile;

/**
 * SF-215-19 : titre de séjour actuel du bénéficiaire de la protection temporaire
 * Ukraine (directive 2001/55/CE, décision UE 2022/382, Loi 15/12/1980 art. 57/29+).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> (droit des étrangers belge).
 */
public enum ProtectionTemporaireUkraineBeTitreSejourEnum {
    /** Aucun titre — présentation à l'Office des étrangers encore à effectuer. */
    AUCUN,
    /** Attestation d'immatriculation (annexe 35) — séjour provisoire en attente du titre A. */
    ATTESTATION_IMMATRICULATION,
    /** Titre de séjour A (carte A) — séjour limité bénéficiaire PT. */
    TITRE_A,
    /** Titre de séjour B (carte B) — séjour limité. */
    TITRE_B,
    /** Autre titre de séjour. */
    TITRE_AUTRE
}
