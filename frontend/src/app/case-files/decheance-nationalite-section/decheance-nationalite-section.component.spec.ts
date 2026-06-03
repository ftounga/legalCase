import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { DecheanceNationaliteSectionComponent } from './decheance-nationalite-section.component';
import { DecheanceNationaliteResponse } from '../../core/models/decheance-nationalite.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('DecheanceNationaliteSectionComponent', () => {
  let component: DecheanceNationaliteSectionComponent;
  let fixture: ComponentFixture<DecheanceNationaliteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/decheance-nationalite-analysis';

  function frResponse(overrides: Partial<DecheanceNationaliteResponse> = {}): DecheanceNationaliteResponse {
    return {
      caseFileId: 'case-1',
      motif: 'TERRORISME',
      binational: true,
      dateAcquisitionNationalite: '2010-01-01',
      dateFaits: '2015-06-01',
      mesurePrononcee: false,
      dateDecret: null,
      country: 'FRANCE',
      validite: 'CONDITIONS_REUNIES',
      conditionsManquantes: [],
      voiesRecours: ['Observations en défense'],
      delaiRecoursJours: null,
      basesJuridiques: ['Code civil art. 25'],
      messages: [],
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [DecheanceNationaliteSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(DecheanceNationaliteSectionComponent);
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
    expect(DecheanceNationaliteSectionComponent.TOOL_LABEL).toContain('DÉCHÉANCE');
    expect(DecheanceNationaliteSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(DecheanceNationaliteSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 4 when all 4 IA signals present', () => {
    expect(DecheanceNationaliteSectionComponent.getPrefillCount({
      aiData: {
        decheanceMotif: 'TERRORISME',
        decheanceBinational: true,
        decheanceMesurePrononcee: true,
        decheanceDateDecret: '2024-01-15',
      },
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(DecheanceNationaliteSectionComponent.getPrefillCount({
      aiData: { decheanceMotif: 'TERRORISME' },
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
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expect(component.result()!.validite).toBe('CONDITIONS_REUNIES');
    expect(component.showForm()).toBe(false);
    expect(component.motif()).toBe('TERRORISME');
    expect(component.binational()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires a motif', () => {
    expect(component.formValid()).toBe(false);
    component.motif.set('TERRORISME');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.motif.set('TERRORISME');
    component.binational.set(true);
    component.dateAcquisitionNationalite.set('2010-01-01');
    component.dateFaits.set('2015-06-01');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.motif).toBe('TERRORISME');
    expect(req.request.body.binational).toBe(true);
    req.flush(frResponse());
    expect(component.result()!.validite).toBe('CONDITIONS_REUNIES');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when motif missing (form invalid)', () => {
    component.ngOnInit();
    flush404();
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.motif.set('TERRORISME');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  it('aiData with all 4 IA signals -> pre-fills + provenance IA', () => {
    component.aiData = {
      decheanceMotif: 'FRAUDE_ACQUISITION',
      decheanceBinational: true,
      decheanceMesurePrononcee: true,
      decheanceDateDecret: '2024-03-10',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.motif()).toBe('FRAUDE_ACQUISITION');
    expect(component.provenanceMotif()).toBe('IA');
    expect(component.binational()).toBe(true);
    expect(component.provenanceBinational()).toBe('IA');
    expect(component.mesurePrononcee()).toBe(true);
    expect(component.provenanceMesurePrononcee()).toBe('IA');
    expect(component.dateDecret()).toBe('2024-03-10');
    expect(component.provenanceDateDecret()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = {
      decheanceMotif: 'AUTRE',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ motif: 'TERRORISME' }));
    expect(component.motif()).toBe('TERRORISME');
    expect(component.provenanceMotif()).toBeNull();
  });

  it('onMotifChange / onBinationalChange / onMesurePrononceeChange clear provenance', () => {
    component.aiData = {
      decheanceMotif: 'TERRORISME',
      decheanceBinational: true,
      decheanceMesurePrononcee: true,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceMotif()).toBe('IA');
    component.onMotifChange('AUTRE');
    expect(component.provenanceMotif()).toBeNull();
    component.onBinationalChange(false);
    expect(component.provenanceBinational()).toBeNull();
    component.onMesurePrononceeChange(false);
    expect(component.provenanceMesurePrononcee()).toBeNull();
  });

  it('bannerClass / bannerIcon cover the four verdict states', () => {
    expect(component.bannerClass(frResponse({ validite: 'CONDITIONS_REUNIES' }))).toContain('dech-banner--success');
    expect(component.bannerClass(frResponse({ validite: 'MESURE_CONTESTABLE' }))).toContain('dech-banner--warning');
    expect(component.bannerClass(frResponse({ validite: 'MESURE_IRREGULIERE' }))).toContain('dech-banner--danger');
    expect(component.bannerClass(frResponse({ validite: 'INDETERMINE' }))).toContain('dech-banner--neutral');
    expect(component.bannerIcon(frResponse({ validite: 'CONDITIONS_REUNIES' }))).toBe('verified');
    expect(component.bannerIcon(frResponse({ validite: 'MESURE_CONTESTABLE' }))).toBe('report_problem');
    expect(component.bannerIcon(frResponse({ validite: 'MESURE_IRREGULIERE' }))).toBe('cancel');
  });

  it('validiteLabel maps the four codes to FR labels', () => {
    expect(component.validiteLabel('CONDITIONS_REUNIES')).toContain('réunies');
    expect(component.validiteLabel('MESURE_CONTESTABLE')).toContain('contestable');
    expect(component.validiteLabel('MESURE_IRREGULIERE')).toContain('irrégulière');
    expect(component.validiteLabel('INDETERMINE')).toContain('indéterminée');
    expect(component.validiteLabel(null)).toBe('');
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
    expect(component.motif()).toBeNull();
    component.aiData = {
      decheanceMotif: 'ATTEINTE_INTERETS_NATION',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.motif()).toBe('ATTEINTE_INTERETS_NATION');
    expect(component.provenanceMotif()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ motif: 'TERRORISME' }));
    component.aiData = {
      decheanceMotif: 'AUTRE',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.motif()).toBe('TERRORISME');
    expect(component.provenanceMotif()).toBeNull();
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.dech-banner--info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('française uniquement');
  });

  it('standaloneMode -> no GET, form visible', () => {
    component.standaloneMode = true;
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true);
    expect(component.collapsed()).toBe(false);
  });
});
