import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SynthesisComponent } from './synthesis.component';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
import { AiQuestionAnswerService } from '../../core/services/ai-question-answer.service';
import { ReAnalysisService } from '../../core/services/re-analysis.service';
import { ChatService } from '../../core/services/chat.service';
import { of, throwError } from 'rxjs';
import { CaseAnalysisVersionSummary } from '../../core/models/case-analysis.model';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

const CASE_FILE_ID = 'cf-1';

const makeSynthesis = (version: number, analysisType: 'STANDARD' | 'ENRICHED') => ({
  id: `analysis-${version}`,
  version,
  analysisType,
  status: 'DONE',
  timeline: [],
  faits: ['fait1'],
  pointsJuridiques: [],
  risques: [],
  questionsOuvertes: [],
  modelUsed: 'claude-sonnet-4-6',
  updatedAt: '2026-03-23T10:00:00Z'
});

const makeVersion = (version: number, analysisType: 'STANDARD' | 'ENRICHED'): CaseAnalysisVersionSummary => ({
  id: `analysis-${version}`,
  version,
  analysisType,
  updatedAt: '2026-03-23T10:00:00Z'
});

describe('SynthesisComponent', () => {
  let fixture: ComponentFixture<SynthesisComponent>;
  let component: SynthesisComponent;
  let caseAnalysisService: jasmine.SpyObj<CaseAnalysisService>;
  let aiQuestionService: jasmine.SpyObj<AiQuestionService>;

  beforeEach(async () => {
    caseAnalysisService = jasmine.createSpyObj('CaseAnalysisService', ['getVersions', 'getByVersion', 'getAnalysis']);
    aiQuestionService = jasmine.createSpyObj('AiQuestionService', ['getQuestions', 'getQuestionsByAnalysisId']);

    const caseFileService = jasmine.createSpyObj('CaseFileService', ['getById']);
    caseFileService.getById.and.returnValue(of({ id: CASE_FILE_ID, title: 'Dossier test' }));

    aiQuestionService.getQuestionsByAnalysisId.and.returnValue(of([]));
    aiQuestionService.getQuestions.and.returnValue(of([]));

    const chatService = jasmine.createSpyObj('ChatService', ['getHistory', 'sendMessage']);
    chatService.getHistory.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [SynthesisComponent, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => CASE_FILE_ID } } } },
        { provide: MatSnackBar, useValue: { open: () => {} } },
        { provide: CaseFileService, useValue: caseFileService },
        { provide: CaseAnalysisService, useValue: caseAnalysisService },
        { provide: AiQuestionService, useValue: aiQuestionService },
        { provide: AiQuestionAnswerService, useValue: jasmine.createSpyObj('AiQuestionAnswerService', ['submitAnswer']) },
        { provide: ReAnalysisService, useValue: jasmine.createSpyObj('ReAnalysisService', ['reAnalyze']) },
        { provide: ChatService, useValue: chatService },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SynthesisComponent);
    component = fixture.componentInstance;
  });

  // T-01 : au chargement → version la plus récente sélectionnée (index 0)
  it('selects most recent version on load', () => {
    const versions = [makeVersion(3, 'ENRICHED'), makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.and.returnValue(of(versions));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(3, 'ENRICHED')));

    fixture.detectChanges();

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 3);
    expect(component.synthesis()?.version).toBe(3);
  });

  // T-02 : changement de version → loadSynthesisForVersion et loadQuestionsForVersion appelés
  it('reloads synthesis and questions on version change', () => {
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.and.returnValue(of(versions));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    component.onVersionChange(1);

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 1);
    expect(aiQuestionService.getQuestionsByAnalysisId).toHaveBeenCalledWith(CASE_FILE_ID, 'analysis-1');
  });

  // T-03 : analysisType ENRICHED → isEnriched() true
  it('returns true for isEnriched when analysisType is ENRICHED', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'ENRICHED')));
    fixture.detectChanges();

    expect(component.isEnriched()).toBeTrue();
  });

  // T-04 : analysisType STANDARD → isEnriched() false
  it('returns false for isEnriched when analysisType is STANDARD', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    expect(component.isEnriched()).toBeFalse();
  });

  // T-05 : une seule version → versions().length === 1
  it('exposes single version without selector interaction', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    expect(component.versions().length).toBe(1);
  });

  // T-06 : versions vides → synthesis() null, loading false
  it('leaves synthesis null when no versions available', () => {
    caseAnalysisService.getVersions.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component.synthesis()).toBeNull();
    expect(component.loading()).toBeFalse();
  });

  // T-07 : changement de version ne recharge pas le chat
  it('does not reload chat on version change', () => {
    const chatService = TestBed.inject(ChatService) as jasmine.SpyObj<ChatService>;
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.and.returnValue(of(versions));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    const callsBefore = chatService.getHistory.calls.count();
    component.onVersionChange(1);

    expect(chatService.getHistory.calls.count()).toBe(callsBefore);
  });

  // T-08 : versionLabel — ENRICHED affiche le badge Enrichie
  it('versionLabel includes Enrichie for ENRICHED type', () => {
    const v = makeVersion(2, 'ENRICHED');
    expect(component.versionLabel(v)).toBe('v2 — Enrichie');
  });

  // T-09 : versionLabel — STANDARD affiche juste le numéro
  it('versionLabel shows only version number for STANDARD type', () => {
    const v = makeVersion(1, 'STANDARD');
    expect(component.versionLabel(v)).toBe('v1');
  });
});
