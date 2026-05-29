import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { RetraitTitreFraudeSectionComponent } from './retrait-titre-fraude-section.component';
import { RetraitTitreFraudeResponse } from '../../core/models/retrait-titre-fraude.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('RetraitTitreFraudeSectionComponent', () => {
  let component: RetraitTitreFraudeSectionComponent;
  let fixture: ComponentFixture<RetraitTitreFraudeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/retrait-titre-fraude-analysis';

  function frResponse(overrides: Partial<RetraitTitreFraudeResponse> = {}): RetraitTitreFraudeResponse {
    return {
      caseFileId: 'case-1',
      dateRetrait: '2026-01-15',
      motifRetrait: 'FAUSSES_DECLARATIONS',
      miseEnDemeurePrealable: false,
      dateMiseEnDemeure: null,
      country: 'FRANCE',
      statut: 'RECOURS_POSSIBLE',
      vicesDeProcedure: ['Défaut de procédure contradictoire préalable'],
      motifsContestation: ['Absence de fraude caractérisée', 'Atteinte à la vie privée'],
      delaiRecoursTA: '2026-03-16',
      baseJuridique: 'CESEDA, art. L. 432-4 et s.',
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateRetrait.set('2026-01-15');
    component.motifRetrait.set('FAUSSES_DECLARATIONS');
    component.miseEnDemeurePrealable.set(false);
    component.dateMiseEnDemeure.set(null);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: 'Recours TA retrait titre', dueDate: '2026-03-16' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [RetraitTitreFraudeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RetraitTitreFraudeSectionComponent);
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
    expect(RetraitTitreFraudeSectionComponent.TOOL_LABEL).toContain('RETRAIT TITRE');
    expect(RetraitTitreFraudeSectionComponent.TOOL_ICON).toBe('gpp_bad');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(RetraitTitreFraudeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 when date + motif present (FRANCE)', () => {
    expect(RetraitTitreFraudeSectionComponent.getPrefillCount({
      aiData: { retraitTitreDateRetrait: '2026-01-15', retraitTitreMotif: 'MARIAGE_GRIS' },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(RetraitTitreFraudeSectionComponent.getPrefillCount({
      aiData: { retraitTitreDateRetrait: '2026-01-15', retraitTitreMotif: 'MARIAGE_GRIS' },
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
    httpMock.expectOne(BASE_URL).flush(frResponse({ statut: 'URGENT', motifRetrait: 'FRAUDE_DOCUMENTAIRE', miseEnDemeurePrealable: true, dateMiseEnDemeure: '2026-01-02' }));
    expect(component.result()!.statut).toBe('URGENT');
    expect(component.showForm()).toBe(false);
    expect(component.dateRetrait()).toBe('2026-01-15');
    expect(component.motifRetrait()).toBe('FRAUDE_DOCUMENTAIRE');
    expect(component.miseEnDemeurePrealable()).toBe(true);
    expect(component.dateMiseEnDemeure()).toBe('2026-01-02');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires dateRetrait only', () => {
    component.dateRetrait.set(null);
    expect(component.formValid()).toBe(false);
    component.dateRetrait.set('2026-01-15');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + body shape', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateRetrait: '2026-01-15',
      motifRetrait: 'FAUSSES_DECLARATIONS',
      miseEnDemeurePrealable: false,
      dateMiseEnDemeure: null,
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('RECOURS_POSSIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.dateRetrait.set(null);
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

  // --- section orange vices de procédure ---

  it('vicesDeProcedure non vide -> renders the orange vices block', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      vicesDeProcedure: ['Défaut de procédure contradictoire préalable', 'Motivation insuffisante'],
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="vices-procedure-block"]');
    expect(block).not.toBeNull();
    const items = block.querySelectorAll('.acc-vices-list li');
    expect(items.length).toBe(2);
    expect(items[0].textContent).toContain('Défaut de procédure contradictoire');
  });

  it('vicesDeProcedure vide -> no vices block', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ vicesDeProcedure: [] }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="vices-procedure-block"]');
    expect(block).toBeNull();
  });

  it('PRESCRIT -> danger chip rendered', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'PRESCRIT' }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acc-chip--danger');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('prescrit');
  });

  it('renders delaiRecoursTA and baseJuridique in result', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const delai = fixture.nativeElement.querySelector('.acc-bd-value--delai');
    expect(delai).not.toBeNull();
    expect(delai.textContent).toContain('2026-03-16');
    const base = fixture.nativeElement.querySelector('.acc-meta-base');
    expect(base.textContent).toContain('CESEDA');
  });

  it('renders motifsContestation list in result', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.acc-motifs-list li');
    expect(items.length).toBe(2);
    expect(items[0].textContent).toContain('Absence de fraude');
  });

  // --- Bridge échéance F-69 ---

  it('bridge F-69: RECOURS_POSSIBLE -> creates deadline with label + delaiRecoursTA', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_POSSIBLE', delaiRecoursTA: '2026-03-16' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Recours TA retrait titre', '2026-03-16');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: URGENT -> creates deadline', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'URGENT', delaiRecoursTA: '2026-02-01' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Recours TA retrait titre', '2026-02-01');
  });

  it('bridge F-69: PRESCRIT -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'PRESCRIT' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: deadline creation failure does not break the flow', () => {
    deadlineSpy.create.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_POSSIBLE' }));
    expect(deadlineSpy.create).toHaveBeenCalled();
    expect(component.deadlineCreated()).toBe(false);
    expect(component.result()!.statut).toBe('RECOURS_POSSIBLE');
  });

  it('bridge F-69: standaloneMode -> never creates a deadline', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_POSSIBLE' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  // --- prefill / labels ---

  it('aiData with date + motif -> pre-fills + provenance IA', () => {
    component.aiData = { retraitTitreDateRetrait: '2026-02-20', retraitTitreMotif: 'MARIAGE_GRIS' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateRetrait()).toBe('2026-02-20');
    expect(component.provenanceDateRetrait()).toBe('IA');
    expect(component.motifRetrait()).toBe('MARIAGE_GRIS');
    expect(component.provenanceMotifRetrait()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { retraitTitreDateRetrait: '2099-01-01', retraitTitreMotif: 'MARIAGE_GRIS' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateRetrait: '2026-01-15', motifRetrait: 'FAUSSES_DECLARATIONS' }));
    expect(component.dateRetrait()).toBe('2026-01-15');
    expect(component.motifRetrait()).toBe('FAUSSES_DECLARATIONS');
    expect(component.provenanceDateRetrait()).toBeNull();
    expect(component.provenanceMotifRetrait()).toBeNull();
  });

  it('onDateRetraitChange clears provenance', () => {
    component.aiData = { retraitTitreDateRetrait: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateRetrait()).toBe('IA');
    component.onDateRetraitChange('2026-03-01');
    expect(component.provenanceDateRetrait()).toBeNull();
  });

  it('onMiseEnDemeureChange false -> clears dateMiseEnDemeure', () => {
    component.dateMiseEnDemeure.set('2026-01-05');
    component.onMiseEnDemeureChange(false);
    expect(component.miseEnDemeurePrealable()).toBe(false);
    expect(component.dateMiseEnDemeure()).toBeNull();
    component.onMiseEnDemeureChange(true);
    expect(component.miseEnDemeurePrealable()).toBe(true);
  });

  it('bannerClass / bannerIcon / statutLabel cover all statuts', () => {
    expect(component.bannerClass('RECOURS_POSSIBLE')).toContain('acc-banner--success');
    expect(component.bannerClass('URGENT')).toContain('acc-banner--warning');
    expect(component.bannerClass('PRESCRIT')).toContain('acc-banner--danger');
    expect(component.bannerIcon('RECOURS_POSSIBLE')).toBe('check_circle');
    expect(component.bannerIcon('URGENT')).toBe('warning');
    expect(component.bannerIcon('PRESCRIT')).toBe('error');
    expect(component.statutLabel('PRESCRIT')).toContain('prescrit');
    expect(component.statutLabel('RECOURS_POSSIBLE')).toContain('possible');
  });

  it('motifLabel covers all motifs', () => {
    expect(component.motifLabel('MARIAGE_GRIS')).toContain('Mariage gris');
    expect(component.motifLabel('FAUSSES_DECLARATIONS')).toContain('Fausses');
    expect(component.motifLabel('FRAUDE_DOCUMENTAIRE')).toContain('documentaire');
    expect(component.motifLabel('PERTE_CONDITIONS')).toContain('conditions');
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
    expect(component.dateRetrait()).toBeNull();
    component.aiData = { retraitTitreDateRetrait: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateRetrait()).toBe('2026-05-01');
    expect(component.provenanceDateRetrait()).toBe('IA');
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.acc-banner--info');
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
