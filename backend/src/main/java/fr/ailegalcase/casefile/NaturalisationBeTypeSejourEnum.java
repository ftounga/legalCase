package fr.ailegalcase.casefile;

/**
 * SF-215-07 : type de séjour du demandeur de la déclaration de nationalité belge
 * art. 12bis CNB (loi 28/06/1984 consolidée).
 *
 * <p>La déclaration de nationalité par voie 12bis exige un séjour ILLIMITÉ
 * (carte B, C ou K) — un séjour limité (carte A) bloque les deux voies
 * (5 ans / 10 ans).
 *
 * <ul>
 *   <li>LIMITE : titre de séjour à durée limitée (carte A — typiquement
 *       autorisation de séjour provisoire d'1 an renouvelable, AR 08/10/1981
 *       art. 25, modifié AR 22/07/2008). Bloque la voie 12bis.</li>
 *   <li>ILLIMITE : titre de séjour à durée illimitée (carte B = certificat
 *       d'inscription au registre des étrangers à durée illimitée,
 *       carte C = carte d'identité d'étranger à durée illimitée,
 *       carte K = carte d'identité d'étranger UE à durée illimitée — art. 14
 *       Loi 15/12/1980). Ouvre la voie 12bis.</li>
 * </ul>
 *
 * <p>Enum partageable avec SF-215-09 (naturalisation conjoint Belge art. 16 CNB)
 * — la condition de séjour illimité est commune.
 */
public enum NaturalisationBeTypeSejourEnum {
    LIMITE,
    ILLIMITE
}
