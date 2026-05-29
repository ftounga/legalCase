import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { VlsTsValidationSectionComponent } from './vls-ts-validation-section.component';
import { VlsTsValidationResponse } from '../../core/models/vls-ts-validation.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('VlsTsValidationSectionComponent', () => {
  let component: VlsTsValidationSectionComponent;
  let fixture: ComponentFixture<VlsTsValidationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/vls-ts-validation-analysis';

  function frResponse(overrides: Partial<VlsTsValidationResponse> = {}): VlsTsValidationResponse {
    return {
      caseFileId: 'case-1',
      dateEntreeFrance: '2026-01-15',
      typeVlsTs: 'ETUDIANT',
      validationOFIIEffectuee: false,
      dateValidationOFII: null,
      country: 'FRANCE',
      statut: 'A_VALIDER',
      dateEcheanceValidation: '2026-04-15',
      joursRestantsValidation: 45,
      risqueIrregularite: false,
      procedureRecours: null,
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateEntreeFrance.set('2026-01-15');
    component.typeVlsTs.set('ETUDIANT');
    component.validationOFIIEffectuee.set(false);
    component.dateValidationOFII.set(null);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: 'Validation VLS-TS OFII', dueDate: '2026-04-15' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [VlsTsValidationSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VlsTsValidationSectionComponent);
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
    expect(VlsTsValidationSectionComponent.TOOL_LABEL).toContain('VLS-TS');
    expect(VlsTsValidationSectionComponent.TOOL_ICON).toBe('event_available');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(VlsTsValidationSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 when aesDateEntreeFrance present (FRANCE)', () => {
    expect(VlsTsValidationSectionComponent.getPrefillCount({
      aiData: { aesDateEntreeFrance: '2026-01-15' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(VlsTsValidationSectionComponent.getPrefillCount({
      aiData: { aesDateEntreeFrance: '2026-01-15' },
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
    httpMock.expectOne(BASE_URL).flush(frResponse({ statut: 'VALIDE', validationOFIIEffectuee: true, dateValidationOFII: '2026-02-01', joursRestantsValidation: null }));
    expect(component.result()!.statut).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(component.dateEntreeFrance()).toBe('2026-01-15');
    expect(component.typeVlsTs()).toBe('ETUDIANT');
    expect(component.validationOFIIEffectuee()).toBe(true);
    expect(component.dateValidationOFII()).toBe('2026-02-01');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires date + type; validation date required only if effectuee', () => {
    expect(component.formValid()).toBe(false);
    fillValidForm();
    expect(component.formValid()).toBe(true);
    component.validationOFIIEffectuee.set(true);
    component.dateValidationOFII.set(null);
    expect(component.formValid()).toBe(false);
    component.dateValidationOFII.set('2026-02-01');
    expect(component.formValid()).toBe(true);
  });

  it('onValidationEffectueeChange(false) clears dateValidationOFII', () => {
    component.validationOFIIEffectuee.set(true);
    component.dateValidationOFII.set('2026-02-01');
    component.onValidationEffectueeChange(false);
    expect(component.validationOFIIEffectuee()).toBe(false);
    expect(component.dateValidationOFII()).toBeNull();
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateEntreeFrance: '2026-01-15',
      typeVlsTs: 'ETUDIANT',
      validationOFIIEffectuee: false,
      dateValidationOFII: null,
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('A_VALIDER');
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

  it('bridge F-69: A_VALIDER -> creates deadline with label + echeance date', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'A_VALIDER', dateEcheanceValidation: '2026-04-15' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Validation VLS-TS OFII', '2026-04-15');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: URGENT -> creates deadline', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'URGENT', dateEcheanceValidation: '2026-04-15', joursRestantsValidation: 5 }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Validation VLS-TS OFII', '2026-04-15');
  });

  it('bridge F-69: VALIDE -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'VALIDE' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: EXPIRE -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'EXPIRE', joursRestantsValidation: -10, procedureRecours: 'Recours gracieux' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: deadline creation failure does not break the flow', () => {
    deadlineSpy.create.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'A_VALIDER' }));
    expect(deadlineSpy.create).toHaveBeenCalled();
    expect(component.deadlineCreated()).toBe(false);
    expect(component.result()!.statut).toBe('A_VALIDER');
  });

  it('bridge F-69: standaloneMode -> never creates a deadline', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'A_VALIDER' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  // --- prefill / labels ---

  it('aiData with aesDateEntreeFrance -> pre-fills + provenance IA', () => {
    component.aiData = { aesDateEntreeFrance: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateEntreeFrance()).toBe('2026-02-20');
    expect(component.provenanceDateEntree()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { aesDateEntreeFrance: '2099-01-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateEntreeFrance: '2026-01-15' }));
    expect(component.dateEntreeFrance()).toBe('2026-01-15');
    expect(component.provenanceDateEntree()).toBeNull();
  });

  it('onDateEntreeChange clears provenance', () => {
    component.aiData = { aesDateEntreeFrance: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateEntree()).toBe('IA');
    component.onDateEntreeChange('2026-03-01');
    expect(component.provenanceDateEntree()).toBeNull();
  });

  it('bannerClass / bannerIcon / statutLabel cover all statuts (rouge URGENT + EXPIRE)', () => {
    expect(component.bannerClass('VALIDE')).toContain('vls-banner--success');
    expect(component.bannerClass('A_VALIDER')).toContain('vls-banner--info');
    expect(component.bannerClass('URGENT')).toContain('vls-banner--warning');
    expect(component.bannerClass('EXPIRE')).toContain('vls-banner--danger');
    expect(component.bannerIcon('VALIDE')).toBe('check_circle');
    expect(component.bannerIcon('URGENT')).toBe('warning');
    expect(component.bannerIcon('EXPIRE')).toBe('error');
    expect(component.statutLabel('EXPIRE')).toContain('expiré');
    expect(component.statutLabel('VALIDE')).toContain('effectuée');
  });

  it('typeLabel maps each type code to FR label', () => {
    expect(component.typeLabel('ETUDIANT')).toBe('Étudiant');
    expect(component.typeLabel('SALARIE')).toBe('Salarié');
    expect(component.typeLabel('VISITEUR')).toBe('Visiteur');
    expect(component.typeLabel('CONJOINT_FRANCAIS')).toBe('Conjoint de Français');
    expect(component.typeLabel('AUTRE')).toBe('Autre');
    expect(component.typeLabel(null)).toBe('');
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
    expect(component.dateEntreeFrance()).toBeNull();
    component.aiData = { aesDateEntreeFrance: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateEntreeFrance()).toBe('2026-05-01');
    expect(component.provenanceDateEntree()).toBe('IA');
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.vls-banner--info');
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
