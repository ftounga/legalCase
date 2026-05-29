import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { AssignationResidenceSectionComponent } from './assignation-residence-section.component';
import { AssignationResidenceResponse } from '../../core/models/assignation-residence.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('AssignationResidenceSectionComponent', () => {
  let component: AssignationResidenceSectionComponent;
  let fixture: ComponentFixture<AssignationResidenceSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/assignation-residence-analysis';

  function frResponse(overrides: Partial<AssignationResidenceResponse> = {}): AssignationResidenceResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationAssignation: '2026-01-15',
      dureeAssignationJours: 45,
      motifAssignation: 'EXECUTION_OQTF',
      obligationsPresentation: 'Présentation hebdomadaire',
      country: 'FRANCE',
      statut: 'EN_COURS',
      dateEcheanceAssignation: '2026-03-01',
      dureeTotaleAutoriseeJours: 45,
      renouvellementPossible: true,
      motifsContestation: ['Disproportion de la mesure', 'Vie privée et familiale'],
      recoursPossible: true,
      delaiRecoursTaHeures: 48,
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateNotificationAssignation.set('2026-01-15');
    component.dureeAssignationJours.set(45);
    component.motifAssignation.set('EXECUTION_OQTF');
    component.obligationsPresentation.set('Présentation hebdomadaire');
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: 'Échéance assignation à résidence', dueDate: '2026-03-01' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [AssignationResidenceSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AssignationResidenceSectionComponent);
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
    expect(AssignationResidenceSectionComponent.TOOL_LABEL).toContain('ASSIGNATION');
    expect(AssignationResidenceSectionComponent.TOOL_ICON).toBe('home_pin');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(AssignationResidenceSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 when date present (FRANCE)', () => {
    expect(AssignationResidenceSectionComponent.getPrefillCount({
      aiData: { assignationDateNotification: '2026-01-15' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(AssignationResidenceSectionComponent.getPrefillCount({
      aiData: { assignationDateNotification: '2026-01-15' },
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
    httpMock.expectOne(BASE_URL).flush(frResponse({ statut: 'EXPIRATION_PROCHE', motifAssignation: 'AUTRE', dureeAssignationJours: 30 }));
    expect(component.result()!.statut).toBe('EXPIRATION_PROCHE');
    expect(component.showForm()).toBe(false);
    expect(component.dateNotificationAssignation()).toBe('2026-01-15');
    expect(component.dureeAssignationJours()).toBe(30);
    expect(component.motifAssignation()).toBe('AUTRE');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires date + positive duree', () => {
    component.dateNotificationAssignation.set(null);
    component.dureeAssignationJours.set(45);
    expect(component.formValid()).toBe(false);
    component.dateNotificationAssignation.set('2026-01-15');
    component.dureeAssignationJours.set(0);
    expect(component.formValid()).toBe(false);
    component.dureeAssignationJours.set(45);
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + body shape', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationAssignation: '2026-01-15',
      dureeAssignationJours: 45,
      motifAssignation: 'EXECUTION_OQTF',
      obligationsPresentation: 'Présentation hebdomadaire',
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('EN_COURS');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationAssignation.set(null);
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

  // --- chip EXPIRATION_PROCHE (orange) ---

  it('EXPIRATION_PROCHE -> warning chip rendered + echeance orange class', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(
      frResponse({ statut: 'EXPIRATION_PROCHE', dateEcheanceAssignation: '2026-02-05' }),
    );
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acc-chip--warning');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('Expiration proche');
    const echeance = fixture.nativeElement.querySelector('.acc-bd-value--echeance-urgent');
    expect(echeance).not.toBeNull();
    expect(echeance.textContent).toContain('2026-02-05');
  });

  it('EXPIRE -> danger chip rendered + echeance danger class', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'EXPIRE', dateEcheanceAssignation: '2025-12-01' }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acc-chip--danger');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('expirée');
    const big = fixture.nativeElement.querySelector('.acc-jours-big--danger');
    expect(big).not.toBeNull();
  });

  it('renders renouvellement badge (ok) and recours banner with 48h', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('[data-testid="renouvellement-badge"]');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('Renouvellement possible');
    const recours = fixture.nativeElement.querySelector('[data-testid="recours-banner"]');
    expect(recours).not.toBeNull();
    expect(recours.textContent).toContain('48');
  });

  it('recoursPossible=false -> no recours banner', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ recoursPossible: false }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="recours-banner"]')).toBeNull();
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
    expect(items[0].textContent).toContain('Disproportion');
  });

  // --- Bridge échéance F-69 ---

  it('bridge F-69: EN_COURS -> creates deadline with label + echeance date', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'EN_COURS', dateEcheanceAssignation: '2026-03-01' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Échéance assignation à résidence', '2026-03-01');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: EXPIRATION_PROCHE -> no deadline (EN_COURS only)', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'EXPIRATION_PROCHE' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: EXPIRE -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'EXPIRE' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: deadline creation failure does not break the flow', () => {
    deadlineSpy.create.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'EN_COURS' }));
    expect(deadlineSpy.create).toHaveBeenCalled();
    expect(component.deadlineCreated()).toBe(false);
    expect(component.result()!.statut).toBe('EN_COURS');
  });

  it('bridge F-69: standaloneMode -> never creates a deadline', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'EN_COURS' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  // --- prefill / labels ---

  it('aiData with date -> pre-fills dateNotification + provenance IA', () => {
    component.aiData = { assignationDateNotification: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateNotificationAssignation()).toBe('2026-02-20');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { assignationDateNotification: '2099-01-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateNotificationAssignation: '2026-01-15' }));
    expect(component.dateNotificationAssignation()).toBe('2026-01-15');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('onDateNotificationChange clears provenance', () => {
    component.aiData = { assignationDateNotification: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateNotification()).toBe('IA');
    component.onDateNotificationChange('2026-03-01');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('bannerClass / bannerIcon / statutLabel cover all statuts', () => {
    expect(component.bannerClass('EN_COURS')).toContain('acc-banner--success');
    expect(component.bannerClass('EXPIRATION_PROCHE')).toContain('acc-banner--warning');
    expect(component.bannerClass('EXPIRE')).toContain('acc-banner--danger');
    expect(component.bannerIcon('EN_COURS')).toBe('check_circle');
    expect(component.bannerIcon('EXPIRATION_PROCHE')).toBe('warning');
    expect(component.bannerIcon('EXPIRE')).toBe('error');
    expect(component.statutLabel('EXPIRE')).toContain('expirée');
    expect(component.statutLabel('EN_COURS')).toContain('en cours');
  });

  it('motifLabel covers values', () => {
    expect(component.motifLabel('EXECUTION_OQTF')).toContain('OQTF');
    expect(component.motifLabel('SURVEILLANCE_MESURE_ELOIGNEMENT')).toContain('Surveillance');
    expect(component.motifLabel('AUTRE')).toContain('Autre');
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
    expect(component.dateNotificationAssignation()).toBeNull();
    component.aiData = { assignationDateNotification: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateNotificationAssignation()).toBe('2026-05-01');
    expect(component.provenanceDateNotification()).toBe('IA');
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
