import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { AutorisationTravailEmployeurSectionComponent } from './autorisation-travail-employeur-section.component';
import { AutorisationTravailEmployeurResponse } from '../../core/models/autorisation-travail-employeur.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('AutorisationTravailEmployeurSectionComponent', () => {
  let component: AutorisationTravailEmployeurSectionComponent;
  let fixture: ComponentFixture<AutorisationTravailEmployeurSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/autorisation-travail-employeur-analysis';

  function frResponse(overrides: Partial<AutorisationTravailEmployeurResponse> = {}): AutorisationTravailEmployeurResponse {
    return {
      caseFileId: 'case-1',
      typeContrat: 'CDI',
      posteProposes: 'Développeur',
      nationaliteCandidat: 'Algérienne',
      dureeContratMois: null,
      refusAutorisation: false,
      dateRefusAutorisation: null,
      country: 'FRANCE',
      statut: 'AUTORISATION_REQUISE',
      obligationsDemande: ['Saisine de la plateforme dématérialisée', 'Pièce justificative de l\'emploi'],
      delaiInstructionOFII: '2 mois',
      recoursPossible: false,
      delaiRecoursTa: null,
      taxeOFII: '55 % du SMIC mensuel',
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.typeContrat.set('CDI');
    component.posteProposes.set('Développeur');
    component.nationaliteCandidat.set('Algérienne');
    component.dureeContratMois.set(null);
    component.refusAutorisation.set(false);
    component.dateRefusAutorisation.set(null);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: 'Recours TA autorisation travail', dueDate: '2026-03-16' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [AutorisationTravailEmployeurSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AutorisationTravailEmployeurSectionComponent);
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
    expect(AutorisationTravailEmployeurSectionComponent.TOOL_LABEL).toContain('AUTORISATION TRAVAIL');
    expect(AutorisationTravailEmployeurSectionComponent.TOOL_ICON).toBe('badge');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(AutorisationTravailEmployeurSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 when nationalite present (FRANCE)', () => {
    expect(AutorisationTravailEmployeurSectionComponent.getPrefillCount({
      aiData: { nationalite: 'Algérienne' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(AutorisationTravailEmployeurSectionComponent.getPrefillCount({
      aiData: { nationalite: 'Algérienne' },
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
      statut: 'RECOURS_POSSIBLE', typeContrat: 'CDD', refusAutorisation: true,
      dateRefusAutorisation: '2026-01-02', dureeContratMois: 12,
    }));
    expect(component.result()!.statut).toBe('RECOURS_POSSIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.typeContrat()).toBe('CDD');
    expect(component.refusAutorisation()).toBe(true);
    expect(component.dateRefusAutorisation()).toBe('2026-01-02');
    expect(component.dureeContratMois()).toBe(12);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires posteProposes and nationaliteCandidat', () => {
    component.posteProposes.set('');
    component.nationaliteCandidat.set('Algérienne');
    expect(component.formValid()).toBe(false);
    component.posteProposes.set('Développeur');
    component.nationaliteCandidat.set('');
    expect(component.formValid()).toBe(false);
    component.nationaliteCandidat.set('Algérienne');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + body shape', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      typeContrat: 'CDI',
      posteProposes: 'Développeur',
      nationaliteCandidat: 'Algérienne',
      dureeContratMois: null,
      refusAutorisation: false,
      dateRefusAutorisation: null,
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('AUTORISATION_REQUISE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.posteProposes.set('');
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

  // --- statut AUTORISATION_NON_REQUISE (candidat UE) ---

  it('AUTORISATION_NON_REQUISE (UE) -> success chip rendered', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    component.nationaliteCandidat.set('Espagnole');
    component.posteProposes.set('Développeur');
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      statut: 'AUTORISATION_NON_REQUISE', nationaliteCandidat: 'Espagnole', obligationsDemande: [],
    }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acc-chip--success');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('non requise');
  });

  it('obligationsDemande non vide -> renders the obligations block', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      obligationsDemande: ['Saisine de la plateforme', 'Justificatif emploi', 'Salaire conforme'],
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="obligations-block"]');
    expect(block).not.toBeNull();
    const items = block.querySelectorAll('.acc-obligations-list li');
    expect(items.length).toBe(3);
  });

  it('renders delaiInstructionOFII and taxeOFII in result', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const delai = fixture.nativeElement.querySelector('.acc-bd-value--delai');
    expect(delai).not.toBeNull();
    expect(delai.textContent).toContain('2 mois');
    const base = fixture.nativeElement.querySelector('.acc-meta-base');
    expect(base.textContent).toContain('SMIC');
  });

  // --- Bridge échéance F-69 ---

  it('bridge F-69: RECOURS_POSSIBLE (refus) -> creates deadline with label + delaiRecoursTa', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.refusAutorisation.set(true);
    component.dateRefusAutorisation.set('2026-01-15');
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      statut: 'RECOURS_POSSIBLE', refusAutorisation: true, recoursPossible: true, delaiRecoursTa: '2026-03-16',
    }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Recours TA autorisation travail', '2026-03-16');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: AUTORISATION_REQUISE -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'AUTORISATION_REQUISE' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: RECOURS_PRESCRIT -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_PRESCRIT', delaiRecoursTa: null }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: deadline creation failure does not break the flow', () => {
    deadlineSpy.create.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      statut: 'RECOURS_POSSIBLE', recoursPossible: true, delaiRecoursTa: '2026-03-16',
    }));
    expect(deadlineSpy.create).toHaveBeenCalled();
    expect(component.deadlineCreated()).toBe(false);
    expect(component.result()!.statut).toBe('RECOURS_POSSIBLE');
  });

  it('bridge F-69: standaloneMode -> never creates a deadline', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      statut: 'RECOURS_POSSIBLE', recoursPossible: true, delaiRecoursTa: '2026-03-16',
    }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  // --- prefill / labels ---

  it('aiData with nationalite -> pre-fills nationaliteCandidat + provenance IA', () => {
    component.aiData = { nationalite: 'Marocaine' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.nationaliteCandidat()).toBe('Marocaine');
    expect(component.provenanceNationalite()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { nationalite: 'Tunisienne' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ nationaliteCandidat: 'Algérienne' }));
    expect(component.nationaliteCandidat()).toBe('Algérienne');
    expect(component.provenanceNationalite()).toBeNull();
  });

  it('onNationaliteCandidatChange clears provenance', () => {
    component.aiData = { nationalite: 'Marocaine' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceNationalite()).toBe('IA');
    component.onNationaliteCandidatChange('Sénégalaise');
    expect(component.provenanceNationalite()).toBeNull();
  });

  it('onRefusAutorisationChange false -> clears dateRefusAutorisation', () => {
    component.dateRefusAutorisation.set('2026-01-05');
    component.onRefusAutorisationChange(false);
    expect(component.refusAutorisation()).toBe(false);
    expect(component.dateRefusAutorisation()).toBeNull();
    component.onRefusAutorisationChange(true);
    expect(component.refusAutorisation()).toBe(true);
  });

  it('bannerClass / bannerIcon / statutLabel cover all statuts', () => {
    expect(component.bannerClass('AUTORISATION_NON_REQUISE')).toContain('acc-banner--success');
    expect(component.bannerClass('AUTORISATION_REQUISE')).toContain('acc-banner--info');
    expect(component.bannerClass('RECOURS_POSSIBLE')).toContain('acc-banner--warning');
    expect(component.bannerClass('RECOURS_PRESCRIT')).toContain('acc-banner--danger');
    expect(component.bannerIcon('AUTORISATION_NON_REQUISE')).toBe('check_circle');
    expect(component.bannerIcon('RECOURS_PRESCRIT')).toBe('error');
    expect(component.statutLabel('AUTORISATION_NON_REQUISE')).toContain('non requise');
    expect(component.statutLabel('RECOURS_PRESCRIT')).toContain('prescrit');
  });

  it('typeContratLabel covers all types', () => {
    expect(component.typeContratLabel('CDI')).toBe('CDI');
    expect(component.typeContratLabel('CDD')).toBe('CDD');
    expect(component.typeContratLabel('INTERIM')).toContain('Intérim');
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
    expect(component.nationaliteCandidat()).toBe('');
    component.aiData = { nationalite: 'Camerounaise' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.nationaliteCandidat()).toBe('Camerounaise');
    expect(component.provenanceNationalite()).toBe('IA');
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
