import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { AjCndaSectionComponent } from './aj-cnda-section.component';
import { AjCndaResponse } from '../../core/models/aj-cnda.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('AjCndaSectionComponent', () => {
  let component: AjCndaSectionComponent;
  let fixture: ComponentFixture<AjCndaSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/aj-cnda-analysis';

  function frResponse(overrides: Partial<AjCndaResponse> = {}): AjCndaResponse {
    return {
      caseFileId: 'case-1',
      dateDecisionOFPRA: '2026-01-15',
      ressourcesMensuellesNettes: 800,
      procedureAcceleree: false,
      demandeAJDeposee: false,
      dateDepotAJ: null,
      country: 'FRANCE',
      statut: 'AJ_A_DEMANDER',
      eligibleAJ: true,
      dateEcheanceRecoursCNDA: '2026-02-15',
      dateEcheanceDemandeAJ: '2026-02-15',
      procedureAccelereeDureeReduite: false,
      piecesAJ: ['Décision OFPRA', 'Justificatifs de ressources'],
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateDecisionOFPRA.set('2026-01-15');
    component.ressourcesMensuellesNettes.set(800);
    component.procedureAcceleree.set(false);
    component.demandeAJDeposee.set(false);
    component.dateDepotAJ.set(null);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: 'Demande AJ CNDA', dueDate: '2026-02-15' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [AjCndaSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AjCndaSectionComponent);
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
    expect(AjCndaSectionComponent.TOOL_LABEL).toContain('CNDA');
    expect(AjCndaSectionComponent.TOOL_ICON).toBe('balance');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(AjCndaSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 when asileDateDecisionAnterieure present (FRANCE)', () => {
    expect(AjCndaSectionComponent.getPrefillCount({
      aiData: { asileDateDecisionAnterieure: '2026-01-15' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(AjCndaSectionComponent.getPrefillCount({
      aiData: { asileDateDecisionAnterieure: '2026-01-15' },
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
    httpMock.expectOne(BASE_URL).flush(frResponse({ statut: 'AJ_DEPOSEE', demandeAJDeposee: true, dateDepotAJ: '2026-01-20' }));
    expect(component.result()!.statut).toBe('AJ_DEPOSEE');
    expect(component.showForm()).toBe(false);
    expect(component.dateDecisionOFPRA()).toBe('2026-01-15');
    expect(component.ressourcesMensuellesNettes()).toBe(800);
    expect(component.demandeAJDeposee()).toBe(true);
    expect(component.dateDepotAJ()).toBe('2026-01-20');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires date + ressources; depot date required only if demande deposee', () => {
    expect(component.formValid()).toBe(false);
    fillValidForm();
    expect(component.formValid()).toBe(true);
    component.demandeAJDeposee.set(true);
    component.dateDepotAJ.set(null);
    expect(component.formValid()).toBe(false);
    component.dateDepotAJ.set('2026-01-20');
    expect(component.formValid()).toBe(true);
  });

  it('onRessourcesChange parses string/number and handles empty', () => {
    component.onRessourcesChange('950');
    expect(component.ressourcesMensuellesNettes()).toBe(950);
    component.onRessourcesChange('');
    expect(component.ressourcesMensuellesNettes()).toBeNull();
    component.onRessourcesChange(1200);
    expect(component.ressourcesMensuellesNettes()).toBe(1200);
  });

  it('onDemandeDeposeeChange(false) clears dateDepotAJ', () => {
    component.demandeAJDeposee.set(true);
    component.dateDepotAJ.set('2026-01-20');
    component.onDemandeDeposeeChange(false);
    expect(component.demandeAJDeposee()).toBe(false);
    expect(component.dateDepotAJ()).toBeNull();
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateDecisionOFPRA: '2026-01-15',
      ressourcesMensuellesNettes: 800,
      procedureAcceleree: false,
      demandeAJDeposee: false,
      dateDepotAJ: null,
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('AJ_A_DEMANDER');
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

  it('bridge F-69: AJ_A_DEMANDER -> creates deadline with label + echeance date', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'AJ_A_DEMANDER', dateEcheanceDemandeAJ: '2026-02-15' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Demande AJ CNDA', '2026-02-15');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: AJ_DEPOSEE -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'AJ_DEPOSEE' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: NON_ELIGIBLE_RESSOURCES -> no deadline + chip rouge danger', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'NON_ELIGIBLE_RESSOURCES', eligibleAJ: false }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
    expect(component.bannerClass('NON_ELIGIBLE_RESSOURCES')).toContain('aj-banner--danger');
  });

  it('bridge F-69: HORS_DELAI_AJ -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'HORS_DELAI_AJ' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: deadline creation failure does not break the flow', () => {
    deadlineSpy.create.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'AJ_A_DEMANDER' }));
    expect(deadlineSpy.create).toHaveBeenCalled();
    expect(component.deadlineCreated()).toBe(false);
    expect(component.result()!.statut).toBe('AJ_A_DEMANDER');
  });

  it('bridge F-69: standaloneMode -> never creates a deadline', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'AJ_A_DEMANDER' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  // --- prefill / labels ---

  it('aiData with asileDateDecisionAnterieure -> pre-fills + provenance IA', () => {
    component.aiData = { asileDateDecisionAnterieure: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateDecisionOFPRA()).toBe('2026-02-20');
    expect(component.provenanceDateDecision()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { asileDateDecisionAnterieure: '2099-01-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateDecisionOFPRA: '2026-01-15' }));
    expect(component.dateDecisionOFPRA()).toBe('2026-01-15');
    expect(component.provenanceDateDecision()).toBeNull();
  });

  it('onDateDecisionChange clears provenance', () => {
    component.aiData = { asileDateDecisionAnterieure: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateDecision()).toBe('IA');
    component.onDateDecisionChange('2026-03-01');
    expect(component.provenanceDateDecision()).toBeNull();
  });

  it('bannerClass / bannerIcon / statutLabel cover all statuts (rouge non éligible + hors délai)', () => {
    expect(component.bannerClass('AJ_DEPOSEE')).toContain('aj-banner--success');
    expect(component.bannerClass('AJ_A_DEMANDER')).toContain('aj-banner--warning');
    expect(component.bannerClass('HORS_DELAI_AJ')).toContain('aj-banner--danger');
    expect(component.bannerClass('NON_ELIGIBLE_RESSOURCES')).toContain('aj-banner--danger');
    expect(component.bannerIcon('AJ_DEPOSEE')).toBe('check_circle');
    expect(component.bannerIcon('NON_ELIGIBLE_RESSOURCES')).toBe('block');
    expect(component.bannerIcon('HORS_DELAI_AJ')).toBe('error');
    expect(component.statutLabel('NON_ELIGIBLE_RESSOURCES')).toContain('Non éligible');
    expect(component.statutLabel('AJ_DEPOSEE')).toContain('déposée');
  });

  it('isEcheanceUrgente true only for AJ_A_DEMANDER and HORS_DELAI_AJ', () => {
    expect(component.isEcheanceUrgente(frResponse({ statut: 'AJ_A_DEMANDER' }))).toBe(true);
    expect(component.isEcheanceUrgente(frResponse({ statut: 'HORS_DELAI_AJ' }))).toBe(true);
    expect(component.isEcheanceUrgente(frResponse({ statut: 'AJ_DEPOSEE' }))).toBe(false);
    expect(component.isEcheanceUrgente(frResponse({ statut: 'NON_ELIGIBLE_RESSOURCES' }))).toBe(false);
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
    expect(component.dateDecisionOFPRA()).toBeNull();
    component.aiData = { asileDateDecisionAnterieure: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateDecisionOFPRA()).toBe('2026-05-01');
    expect(component.provenanceDateDecision()).toBe('IA');
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.aj-banner--info');
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
