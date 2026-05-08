import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';

import { DecisionToolAlignmentsLoader } from './decision-tool-alignments.loader';
import { RetainedPisteAlignmentService } from '../../core/services/retained-piste-alignment.service';
import { PieceManquanteAlignmentService } from '../../core/services/piece-manquante-alignment.service';
import { RisqueAlignmentService } from '../../core/services/risque-alignment.service';
import { AiQuestionAlignmentService } from '../../core/services/ai-question-alignment.service';

import { RetainedPisteAlignment } from '../../core/models/retained-piste-alignment.model';
import { PieceManquanteAlignment } from '../../core/models/piece-manquante-alignment.model';
import { RisqueAlignment } from '../../core/models/risque-alignment.model';
import { AiQuestionAlignment } from '../../core/models/ai-question-alignment.model';

/**
 * F-228 SF-228-01 — Tests du loader partagé `DecisionToolAlignmentsLoader`.
 *
 * Couverture :
 *  - Cas nominal : les 4 services renvoient → loader produit le bundle complet
 *  - Fail-open par stream : 1 service en erreur → bundle complet, ce stream
 *    seul tombe sur `[]`, les 3 autres remontent normalement (4 tests, 1 par
 *    service)
 */
describe('DecisionToolAlignmentsLoader (F-228 SF-228-01)', () => {
  const samplePiste: RetainedPisteAlignment = {
    pisteId: 'p-1',
    texte: 'Demander un titre VPF',
    conditions: [],
    matchStatus: 'ALIGNED',
    toolIdCible: 'F-IM-05-arbre-decisionnel-titre',
  };
  const samplePiece: PieceManquanteAlignment = {
    pieceLibelle: 'Justificatif de domicile',
    statut: 'OBTENUE',
    toolIdsCibles: ['F-IM-05-arbre-decisionnel-titre'],
  };
  const sampleRisque: RisqueAlignment = {
    risqueLibelle: 'Risque OQTF',
    statut: 'VALIDE',
    toolIdsCibles: ['F-IM-08-oqtf-avec-delai-fr'],
  };
  const sampleQuestion: AiQuestionAlignment = {
    questionId: 'q-1',
    answerText: 'Oui',
    statutDeduction: 'PIECE_OBTENUE',
    pieceLibelleDeduit: 'Justificatif de domicile',
  };

  function configure(opts: {
    pistes?: Observable<RetainedPisteAlignment[]>;
    pieces?: Observable<PieceManquanteAlignment[]>;
    risques?: Observable<RisqueAlignment[]>;
    questions?: Observable<AiQuestionAlignment[]>;
  }): DecisionToolAlignmentsLoader {
    TestBed.configureTestingModule({
      providers: [
        DecisionToolAlignmentsLoader,
        {
          provide: RetainedPisteAlignmentService,
          useValue: {
            getForCaseFile: jest.fn().mockReturnValue(opts.pistes ?? of([samplePiste])),
          },
        },
        {
          provide: PieceManquanteAlignmentService,
          useValue: {
            getForCaseFile: jest.fn().mockReturnValue(opts.pieces ?? of([samplePiece])),
          },
        },
        {
          provide: RisqueAlignmentService,
          useValue: {
            getForCaseFile: jest.fn().mockReturnValue(opts.risques ?? of([sampleRisque])),
          },
        },
        {
          provide: AiQuestionAlignmentService,
          useValue: {
            getForCaseFile: jest.fn().mockReturnValue(opts.questions ?? of([sampleQuestion])),
          },
        },
      ],
    });
    return TestBed.inject(DecisionToolAlignmentsLoader);
  }

  afterEach(() => TestBed.resetTestingModule());

  it('CA-10 nominal : les 4 services OK → bundle complet', (done) => {
    const loader = configure({});
    loader.loadAll('case-1').subscribe((bundle) => {
      expect(bundle.retainedPistes).toEqual([samplePiste]);
      expect(bundle.piecesAlignment).toEqual([samplePiece]);
      expect(bundle.risquesAlignment).toEqual([sampleRisque]);
      expect(bundle.aiQuestionsAlignment).toEqual([sampleQuestion]);
      done();
    });
  });

  it('CA-10 fail-open RetainedPiste : 3 streams OK + 1 erreur → bundle complet, retainedPistes=[]', (done) => {
    const loader = configure({
      pistes: throwError(() => new Error('boom')),
    });
    loader.loadAll('case-1').subscribe((bundle) => {
      expect(bundle.retainedPistes).toEqual([]);
      expect(bundle.piecesAlignment).toEqual([samplePiece]);
      expect(bundle.risquesAlignment).toEqual([sampleRisque]);
      expect(bundle.aiQuestionsAlignment).toEqual([sampleQuestion]);
      done();
    });
  });

  it('CA-10 fail-open PieceManquante : 3 streams OK + 1 erreur → bundle complet, piecesAlignment=[]', (done) => {
    const loader = configure({
      pieces: throwError(() => new Error('boom')),
    });
    loader.loadAll('case-1').subscribe((bundle) => {
      expect(bundle.retainedPistes).toEqual([samplePiste]);
      expect(bundle.piecesAlignment).toEqual([]);
      expect(bundle.risquesAlignment).toEqual([sampleRisque]);
      expect(bundle.aiQuestionsAlignment).toEqual([sampleQuestion]);
      done();
    });
  });

  it('CA-10 fail-open Risque : 3 streams OK + 1 erreur → bundle complet, risquesAlignment=[]', (done) => {
    const loader = configure({
      risques: throwError(() => new Error('boom')),
    });
    loader.loadAll('case-1').subscribe((bundle) => {
      expect(bundle.retainedPistes).toEqual([samplePiste]);
      expect(bundle.piecesAlignment).toEqual([samplePiece]);
      expect(bundle.risquesAlignment).toEqual([]);
      expect(bundle.aiQuestionsAlignment).toEqual([sampleQuestion]);
      done();
    });
  });

  it('CA-10 fail-open AiQuestion : 3 streams OK + 1 erreur → bundle complet, aiQuestionsAlignment=[]', (done) => {
    const loader = configure({
      questions: throwError(() => new Error('boom')),
    });
    loader.loadAll('case-1').subscribe((bundle) => {
      expect(bundle.retainedPistes).toEqual([samplePiste]);
      expect(bundle.piecesAlignment).toEqual([samplePiece]);
      expect(bundle.risquesAlignment).toEqual([sampleRisque]);
      expect(bundle.aiQuestionsAlignment).toEqual([]);
      done();
    });
  });

  it('CA-10 forwarding : caseFileId est passé tel quel à chaque service', () => {
    TestBed.configureTestingModule({
      providers: [
        DecisionToolAlignmentsLoader,
        {
          provide: RetainedPisteAlignmentService,
          useValue: { getForCaseFile: jest.fn().mockReturnValue(of([])) },
        },
        {
          provide: PieceManquanteAlignmentService,
          useValue: { getForCaseFile: jest.fn().mockReturnValue(of([])) },
        },
        {
          provide: RisqueAlignmentService,
          useValue: { getForCaseFile: jest.fn().mockReturnValue(of([])) },
        },
        {
          provide: AiQuestionAlignmentService,
          useValue: { getForCaseFile: jest.fn().mockReturnValue(of([])) },
        },
      ],
    });
    const loader = TestBed.inject(DecisionToolAlignmentsLoader);
    const pistes = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const pieces = TestBed.inject(PieceManquanteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const risques = TestBed.inject(RisqueAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const questions = TestBed.inject(AiQuestionAlignmentService) as unknown as { getForCaseFile: jest.Mock };

    loader.loadAll('case-xyz').subscribe();

    expect(pistes.getForCaseFile).toHaveBeenCalledWith('case-xyz');
    expect(pieces.getForCaseFile).toHaveBeenCalledWith('case-xyz');
    expect(risques.getForCaseFile).toHaveBeenCalledWith('case-xyz');
    expect(questions.getForCaseFile).toHaveBeenCalledWith('case-xyz');
  });
});
