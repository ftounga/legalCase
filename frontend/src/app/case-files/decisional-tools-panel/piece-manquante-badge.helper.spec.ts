import {
  computePiecesBadge,
  getPiecesBadgeFor,
  piecesObtenuesFor,
} from './piece-manquante-badge.helper';
import { PieceManquanteAlignment } from '../../core/models/piece-manquante-alignment.model';
import { PieceManquanteStatutValue } from '../../core/models/piece-manquante-status.model';

const TOOL_ID = 'F-DT-04-fiche-prudhomale';

function piece(
  statut: PieceManquanteStatutValue,
  libelle: string,
  toolIdsCibles: string[] = [TOOL_ID],
): PieceManquanteAlignment {
  return {
    pieceLibelle: libelle,
    statut,
    toolIdsCibles,
    destinataire: statut === 'A_DEMANDER' ? 'Client' : null,
    raisonNonApp: statut === 'NON_APPLICABLE' ? 'Non pertinent' : null,
  };
}

describe('piece-manquante-badge.helper — F-194 SF-194-02', () => {
  describe('computePiecesBadge', () => {
    it('liste vide → kind=none, counts à 0', () => {
      expect(computePiecesBadge([])).toEqual({
        kind: 'none',
        counts: { aDemander: 0, obtenues: 0, nonApplicable: 0 },
      });
    });

    it('uniquement A_DEMANDER → kind=missing (priorité absolue)', () => {
      expect(computePiecesBadge([
        piece('A_DEMANDER', 'Contrat de travail'),
        piece('A_DEMANDER', 'Bulletins de salaire'),
      ])).toEqual({
        kind: 'missing',
        counts: { aDemander: 2, obtenues: 0, nonApplicable: 0 },
      });
    });

    it('A_DEMANDER + OBTENUE → missing (l\'avocat doit voir qu\'il en manque encore)', () => {
      expect(computePiecesBadge([
        piece('A_DEMANDER', 'Contrat de travail'),
        piece('OBTENUE', 'Bulletins de salaire'),
      ]).kind).toBe('missing');
    });

    it('uniquement OBTENUE → kind=obtained', () => {
      expect(computePiecesBadge([
        piece('OBTENUE', 'Contrat de travail'),
      ])).toEqual({
        kind: 'obtained',
        counts: { aDemander: 0, obtenues: 1, nonApplicable: 0 },
      });
    });

    it('OBTENUE + NON_APPLICABLE → kind=partial', () => {
      expect(computePiecesBadge([
        piece('OBTENUE', 'Contrat de travail'),
        piece('NON_APPLICABLE', 'Acte de mariage'),
      ])).toEqual({
        kind: 'partial',
        counts: { aDemander: 0, obtenues: 1, nonApplicable: 1 },
      });
    });

    it('uniquement NON_APPLICABLE → kind=not_applicable', () => {
      expect(computePiecesBadge([
        piece('NON_APPLICABLE', 'Acte de mariage'),
      ])).toEqual({
        kind: 'not_applicable',
        counts: { aDemander: 0, obtenues: 0, nonApplicable: 1 },
      });
    });
  });

  describe('getPiecesBadgeFor — filtre par toolId', () => {
    it('filtre les pièces d\'autres outils', () => {
      const list = [
        piece('OBTENUE', 'Contrat de travail', [TOOL_ID]),
        piece('A_DEMANDER', 'Acte de mariage', ['F-FA-07-checklist-divorce']),
      ];
      expect(getPiecesBadgeFor({ piecesAlignment: list }, TOOL_ID)).toEqual({
        kind: 'obtained',
        counts: { aDemander: 0, obtenues: 1, nonApplicable: 0 },
      });
    });

    it('input null/undefined → kind=none', () => {
      expect(getPiecesBadgeFor({}, TOOL_ID)).toEqual({
        kind: 'none',
        counts: { aDemander: 0, obtenues: 0, nonApplicable: 0 },
      });
      expect(getPiecesBadgeFor({ piecesAlignment: null }, TOOL_ID)).toEqual({
        kind: 'none',
        counts: { aDemander: 0, obtenues: 0, nonApplicable: 0 },
      });
    });

    it('pièce avec toolIdsCibles vide → ignorée', () => {
      const list = [piece('OBTENUE', 'Pièce orpheline', [])];
      expect(getPiecesBadgeFor({ piecesAlignment: list }, TOOL_ID).kind).toBe('none');
    });
  });

  describe('piecesObtenuesFor — extraction libellés OBTENUE', () => {
    it('retourne les libellés des pièces OBTENUE pour un outil', () => {
      const list = [
        piece('OBTENUE', 'Contrat de travail', [TOOL_ID]),
        piece('OBTENUE', 'Fiches de paie', [TOOL_ID]),
        piece('A_DEMANDER', 'Lettre de licenciement', [TOOL_ID]),
        piece('OBTENUE', 'Acte de mariage', ['F-FA-07-checklist-divorce']),
      ];
      expect(piecesObtenuesFor(list, TOOL_ID)).toEqual([
        'Contrat de travail',
        'Fiches de paie',
      ]);
    });

    it('null → []', () => {
      expect(piecesObtenuesFor(null, TOOL_ID)).toEqual([]);
      expect(piecesObtenuesFor(undefined, TOOL_ID)).toEqual([]);
    });

    it('liste vide → []', () => {
      expect(piecesObtenuesFor([], TOOL_ID)).toEqual([]);
    });
  });
});
