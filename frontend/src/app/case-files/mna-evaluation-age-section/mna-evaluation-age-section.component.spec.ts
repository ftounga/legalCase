import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { MnaEvaluationAgeSectionComponent } from './mna-evaluation-age-section.component';
import { MnaEvaluationAgeResponse } from '../../core/models/mna-evaluation-age.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('MnaEvaluationAgeSectionComponent', () => {
  let component: MnaEvaluationAgeSectionComponent;
  let fixture: ComponentFixture<MnaEvaluationAgeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/mna-evaluation-age-analysis';

  function frResponse(overrides: Partial<MnaEvaluationAgeResponse> = {}): MnaEvaluationAgeResponse {
    return {
      caseFileId: 'case-1',
      dateNaissanceDeclaree: '2010-06-15',
      evaluationASERefusee: true,
      dateRefusASE: '2026-05-01',
      examenOsseuxOrdonne: false,
      resultatExamenOsseux: null,
      country: 'FRANCE',
      statut: 'RECOURS_JE_URGENT',
      dateEcheanceSaisineJE: '2026-05-31',
      contestationExamenOsseux: [],
      procedureASE: ['Saisir le juge des enfants', 'Demander placement provisoire'],
      droitsAttaches: ['Scolarisation', 'Prise en charge ASE'],
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateNaissanceDeclaree.set('2010-06-15');
    component.evaluationASERefusee.set(true);
    component.dateRefusASE.set('2026-05-01');
    component.examenOsseuxOrdonne.set(false);
    component.resultatExamenOsseux.set(null);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: 'Saisine juge des enfants MNA', dueDate: '2026-05-31' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [MnaEvaluationAgeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MnaEvaluationAgeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(MnaEvaluationAgeSectionComponent.TOOL_LABEL).toContain('MNA');
    expect(MnaEvaluationAgeSectionComponent.TOOL_ICON).toBe('escalator_warning');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(MnaEvaluationAgeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 when mineursDateNaissance present (FRANCE)', () => {
    expect(MnaEvaluationAgeSectionComponent.getPrefillCount({
      aiData: { mineursDateNaissance: '2010-06-15' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(MnaEvaluationAgeSectionComponent.getPrefillCount({
      aiData: { mineursDateNaissance: '2010-06-15' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('FRANCE -> GET called on ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('BELGIQUE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({
      statut: 'EXAMEN_OSSEUX_CONTESTE',
      examenOsseuxOrdonne: true,
      resultatExamenOsseux: 'Âge estimé 18-20 ans',
    }));
    expect(component.result()!.statut).toBe('EXAMEN_OSSEUX_CONTESTE');
    expect(component.showForm()).toBe(false);
    expect(component.dateNaissanceDeclaree()).toBe('2010-06-15');
    expect(component.evaluationASERefusee()).toBe(true);
    expect(component.dateRefusASE()).toBe('2026-05-01');
    expect(component.examenOsseuxOrdonne()).toBe(true);
    expect(component.resultatExamenOsseux()).toBe('Âge estimé 18-20 ans');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires date naissance; refus date required only if evaluation refusee', () => {
    expect(component.formValid()).toBe(false);
    component.dateNaissanceDeclaree.set('2010-06-15');
    expect(component.formValid()).toBe(true);
    component.evaluationASERefusee.set(true);
    component.dateRefusASE.set(null);
    expect(component.formValid()).toBe(false);
    component.dateRefusASE.set('2026-05-01');
    expect(component.formValid()).toBe(true);
  });

  it('onEvaluationRefuseeChange(false) clears dateRefusASE', () => {
    component.evaluationASERefusee.set(true);
    component.dateRefusASE.set('2026-05-01');
    component.onEvaluationRefuseeChange(false);
    expect(component.evaluationASERefusee()).toBe(false);
    expect(component.dateRefusASE()).toBeNull();
  });

  it('onExamenOsseuxOrdonneChange(false) clears resultatExamenOsseux', () => {
    component.examenOsseuxOrdonne.set(true);
    component.resultatExamenOsseux.set('résultat');
    component.onExamenOsseuxOrdonneChange(false);
    expect(component.examenOsseuxOrdonne()).toBe(false);
    expect(component.resultatExamenOsseux()).toBeNull();
  });

  it('analyze() POST nominal -> result + snack + correct body', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNaissanceDeclaree: '2010-06-15',
      evaluationASERefusee: true,
      dateRefusASE: '2026-05-01',
      examenOsseuxOrdonne: false,
      resultatExamenOsseux: null,
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('RECOURS_JE_URGENT');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // --- Bridge échéance F-69 ---

  it('bridge F-69: RECOURS_JE_URGENT -> creates deadline with label + echeance date', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_JE_URGENT', dateEcheanceSaisineJE: '2026-05-31' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Saisine juge des enfants MNA', '2026-05-31');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: PRIS_EN_CHARGE -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'PRIS_EN_CHARGE', dateEcheanceSaisineJE: null }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: RECOURS_JE_URGENT but null echeance -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_JE_URGENT', dateEcheanceSaisineJE: null }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: deadline creation failure does not break the flow', () => {
    deadlineSpy.create.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_JE_URGENT' }));
    expect(deadlineSpy.create).toHaveBeenCalled();
    expect(component.deadlineCreated()).toBe(false);
    expect(component.result()!.statut).toBe('RECOURS_JE_URGENT');
  });

  it('bridge F-69: standaloneMode -> never creates a deadline', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_JE_URGENT' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  // --- contestation examen osseux ---

  it('renders contestation list when EXAMEN_OSSEUX_CONTESTE with arguments', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(frResponse({
      statut: 'EXAMEN_OSSEUX_CONTESTE',
      examenOsseuxOrdonne: true,
      contestationExamenOsseux: ['Marge d’erreur scientifique', 'Doute profite au mineur'],
    }));
    fixture.detectChanges();
    const list = fixture.nativeElement.querySelector('.mna-contestation-list');
    expect(list).not.toBeNull();
    expect(list.querySelectorAll('li').length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Marge d’erreur scientifique');
  });

  it('does not render contestation block when contestation list empty', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(frResponse({ contestationExamenOsseux: [] }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.mna-contestation')).toBeNull();
  });

  it('renders procedureASE stepper steps', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(frResponse({ procedureASE: ['Étape 1', 'Étape 2', 'Étape 3'] }));
    fixture.detectChanges();
    const steps = fixture.nativeElement.querySelectorAll('.mna-step');
    expect(steps.length).toBe(3);
  });

  // --- prefill / labels ---

  it('aiData with mineursDateNaissance -> pre-fills + provenance IA', () => {
    component.aiData = { mineursDateNaissance: '2011-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateNaissanceDeclaree()).toBe('2011-02-20');
    expect(component.provenanceDateNaissance()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { mineursDateNaissance: '2099-01-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateNaissanceDeclaree: '2010-06-15' }));
    expect(component.dateNaissanceDeclaree()).toBe('2010-06-15');
    expect(component.provenanceDateNaissance()).toBeNull();
  });

  it('onDateNaissanceChange clears provenance', () => {
    component.aiData = { mineursDateNaissance: '2011-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateNaissance()).toBe('IA');
    component.onDateNaissanceChange('2012-03-01');
    expect(component.provenanceDateNaissance()).toBeNull();
  });

  it('bannerClass / bannerIcon / statutLabel cover all statuts (rouge recours JE urgent)', () => {
    expect(component.bannerClass('PRIS_EN_CHARGE')).toContain('mna-banner--success');
    expect(component.bannerClass('EXAMEN_OSSEUX_CONTESTE')).toContain('mna-banner--warning');
    expect(component.bannerClass('EN_ATTENTE_EVALUATION')).toContain('mna-banner--warning');
    expect(component.bannerClass('RECOURS_JE_URGENT')).toContain('mna-banner--danger');
    expect(component.bannerIcon('PRIS_EN_CHARGE')).toBe('check_circle');
    expect(component.bannerIcon('RECOURS_JE_URGENT')).toBe('gavel');
    expect(component.statutLabel('RECOURS_JE_URGENT')).toContain('Recours JE');
    expect(component.statutLabel('PRIS_EN_CHARGE')).toContain('Pris en charge');
  });

  it('isEcheanceUrgente true only for RECOURS_JE_URGENT', () => {
    expect(component.isEcheanceUrgente(frResponse({ statut: 'RECOURS_JE_URGENT' }))).toBe(true);
    expect(component.isEcheanceUrgente(frResponse({ statut: 'EXAMEN_OSSEUX_CONTESTE' }))).toBe(false);
    expect(component.isEcheanceUrgente(frResponse({ statut: 'PRIS_EN_CHARGE' }))).toBe(false);
    expect(component.isEcheanceUrgente(null)).toBe(false);
  });

  it('toggleCollapse inverts collapsed state', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode resets showForm to true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.dateNaissanceDeclaree()).toBeNull();
    component.aiData = { mineursDateNaissance: '2012-05-01' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateNaissanceDeclaree()).toBe('2012-05-01');
    expect(component.provenanceDateNaissance()).toBe('IA');
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.mna-banner--info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('française uniquement');
  });

  it('standaloneMode -> no GET, form visible, banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
  });
});
