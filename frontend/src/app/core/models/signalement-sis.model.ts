/**
 * SF-220-06 : modèles miroirs du contrat API (backend SF-220-06) pour l'outil
 * décisionnel "contestation / radiation d'un signalement SIS aux fins de
 * non-admission" (F-IM-52-signalement-sis-fr, Règl. UE 2018/1860 / CESEDA
 * L.312-3). FR uniquement.
 *
 * Un signalement aux fins de non-admission dans le SIS bloque l'entrée dans
 * l'espace Schengen même avec un titre de séjour valide. L'outil identifie la
 * voie de contestation / radiation selon l'État signalant. Distinct de F-IM-20
 * (mesures d'éloignement : expulsion / IRTF / IAT) : ici l'objet est le
 * signalement lui-même, pas l'IRTF.
 */

export type EtatSignalant = 'FRANCE' | 'AUTRE_ETAT_MEMBRE' | 'INCONNU';

export type MotifSignalement =
  | 'IRTF'
  | 'MESURE_ELOIGNEMENT_ETRANGERE'
  | 'MENACE_ORDRE_PUBLIC'
  | 'AUTRE';

export type ActionPossibleSis =
  | 'RADIATION_AUTORITE_FR'
  | 'RADIATION_ETAT_SIGNALANT'
  | 'DROIT_ACCES_RECTIFICATION'
  | 'CONSULTATION_ENTRE_ETATS'
  | 'INDETERMINE';

export interface SignalementSisRequest {
  signalementConnu: boolean | null;
  etatSignalant: EtatSignalant | null;
  motifSignalement: MotifSignalement | null;
  titreSejourValide: boolean | null;
  dateSignalement: string | null;
}

export interface SignalementSisResponse {
  caseFileId: string;
  signalementConnu: boolean | null;
  etatSignalant: EtatSignalant | null;
  motifSignalement: MotifSignalement | null;
  titreSejourValide: boolean | null;
  dateSignalement: string | null;
  country: string;
  actionPossible: ActionPossibleSis;
  demarches: string[];
  autoriteCompetente: string;
  basesJuridiques: string[];
  messages: string[];
}
