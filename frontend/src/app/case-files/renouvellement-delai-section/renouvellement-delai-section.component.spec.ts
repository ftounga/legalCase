import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { RenouvellementDelaiSectionComponent } from './renouvellement-delai-section.component';
import { RenouvellementDelaiResponse } from '../../core/models/renouvellement-delai.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('RenouvellementDelaiSectionComponent', () => {
  let component: RenouvellementDelaiSectionComponent;
  let fixture: ComponentFixture<RenouvellementDelaiSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/renouvellement-delai-analysis';

  function frResponse(overrides: Partial<RenouvellementDelaiResponse> = {}): RenouvellementDelaiResponse {
    return {
      caseFileId: 'case-1',
      dateExpirationTitre: '2026-09-15',
      dateDepotDossier: null,
      typeTitre: 'Carte pluriannuelle',
      country: 'FRANCE',
      statut: 'A_DEPOSER',
      dateOptimalDepot: '2026-07-15',
      dateDepotImperatif: '2026-09-15',
      joursRestantsAvantOptimal: 30,
      joursRestantsAvantImperatif: 92,
      risqueIrruption: false,
      alerteRetard: false,
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateExpirationTitre.set('2026-09-15');
    component.typeTitre.set('Carte pluriannuelle');
    component.dateDepotDossier.set(null);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: 'Dépôt renouvellement titre', dueDate: '2026-07-15' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [RenouvellementDelaiSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RenouvellementDelaiSectionComponent);
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
    expect(RenouvellementDelaiSectionComponent.TOOL_LABEL).toContain('RENOUVELLEMENT');
    expect(RenouvellementDelaiSectionComponent.TOOL_ICON).toBe('event_repeat');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(RenouvellementDelaiSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 when dateExpirationTitre + typeTitreSejour present (FRANCE)', () => {
    expect(RenouvellementDelaiSectionComponent.getPrefillCount({
      aiData: { dateExpirationTitre: '2026-09-15', typeTitreSejour: 'Carte pluriannuelle' },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(RenouvellementDelaiSectionComponent.getPrefillCount({
      aiData: { dateExpirationTitre: '2026-09-15', typeTitreSejour: 'Carte pluriannuelle' },
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
    httpMock.expectOne(BASE_URL).flush(frResponse({ statut: 'DEPOSE', dateDepotDossier: '2026-07-01' }));
    expect(component.result()!.statut).toBe('DEPOSE');
    expect(component.showForm()).toBe(false);
    expect(component.dateExpirationTitre()).toBe('2026-09-15');
    expect(component.typeTitre()).toBe('Carte pluriannuelle');
    expect(component.dateDepotDossier()).toBe('2026-07-01');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires only dateExpirationTitre', () => {
    expect(component.formValid()).toBe(false);
    component.dateExpirationTitre.set('2026-09-15');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + refresh body', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateExpirationTitre: '2026-09-15',
      dateDepotDossier: null,
      typeTitre: 'Carte pluriannuelle',
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('A_DEPOSER');
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

  it('bridge F-69: A_DEPOSER -> creates deadline with label + dateOptimalDepot', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'A_DEPOSER', dateOptimalDepot: '2026-07-15' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Dépôt renouvellement titre', '2026-07-15');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: A_DEPOSER_URGENT -> creates deadline', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'A_DEPOSER_URGENT', dateOptimalDepot: '2026-07-15', joursRestantsAvantOptimal: 5, alerteRetard: true }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Dépôt renouvellement titre', '2026-07-15');
  });

  it('bridge F-69: EN_AVANCE -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'EN_AVANCE' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: EXPIRE -> no deadline created (risqueIrruption banner instead)', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'EXPIRE', joursRestantsAvantOptimal: -10, risqueIrruption: true, alerteRetard: true }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: DEPOSE -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'DEPOSE' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: deadline creation failure does not break the flow', () => {
    deadlineSpy.create.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'A_DEPOSER' }));
    expect(deadlineSpy.create).toHaveBeenCalled();
    expect(component.deadlineCreated()).toBe(false);
    expect(component.result()!.statut).toBe('A_DEPOSER');
  });

  it('bridge F-69: standaloneMode -> never creates a deadline', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'A_DEPOSER' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  // --- prefill / labels ---

  it('aiData with dateExpirationTitre + typeTitreSejour -> pre-fills both + provenance IA', () => {
    component.aiData = { dateExpirationTitre: '2026-09-20', typeTitreSejour: 'Étudiant' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateExpirationTitre()).toBe('2026-09-20');
    expect(component.provenanceDateExpiration()).toBe('IA');
    expect(component.typeTitre()).toBe('Étudiant');
    expect(component.provenanceTypeTitre()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { dateExpirationTitre: '2099-01-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateExpirationTitre: '2026-09-15' }));
    expect(component.dateExpirationTitre()).toBe('2026-09-15');
    expect(component.provenanceDateExpiration()).toBeNull();
  });

  it('onDateExpirationChange clears provenance', () => {
    component.aiData = { dateExpirationTitre: '2026-09-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateExpiration()).toBe('IA');
    component.onDateExpirationChange('2026-10-01');
    expect(component.provenanceDateExpiration()).toBeNull();
  });

  it('onTypeTitreChange clears provenance', () => {
    component.aiData = { dateExpirationTitre: '2026-09-20', typeTitreSejour: 'Salarié' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceTypeTitre()).toBe('IA');
    component.onTypeTitreChange('Visiteur');
    expect(component.provenanceTypeTitre()).toBeNull();
  });

  it('bannerClass / chipClass / bannerIcon / statutLabel cover all statuts', () => {
    expect(component.bannerClass('EN_AVANCE')).toContain('rdd-banner--success');
    expect(component.bannerClass('A_DEPOSER')).toContain('rdd-banner--info');
    expect(component.bannerClass('A_DEPOSER_URGENT')).toContain('rdd-banner--warning');
    expect(component.bannerClass('EXPIRE')).toContain('rdd-banner--danger');
    expect(component.bannerClass('DEPOSE')).toContain('rdd-banner--success');
    expect(component.chipClass('A_DEPOSER_URGENT')).toBe('rdd-chip--warning');
    expect(component.chipClass('EXPIRE')).toBe('rdd-chip--danger');
    expect(component.bannerIcon('A_DEPOSER_URGENT')).toBe('warning');
    expect(component.bannerIcon('EXPIRE')).toBe('error');
    expect(component.statutLabel('EXPIRE')).toContain('expiré');
    expect(component.statutLabel('EN_AVANCE')).toContain('confortable');
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
    expect(component.dateExpirationTitre()).toBeNull();
    component.aiData = { dateExpirationTitre: '2026-11-01' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateExpirationTitre()).toBe('2026-11-01');
    expect(component.provenanceDateExpiration()).toBe('IA');
  });

  it('EXPIRE result renders risqueIrruption alert banner', () => {
    component.forceExpanded = true;
    fixture.detectChanges(); // triggers ngOnInit -> GET
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'EXPIRE', joursRestantsAvantOptimal: -10, risqueIrruption: true, alerteRetard: true }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.rdd-irruption-block');
    expect(block).not.toBeNull();
    expect(block.textContent).toContain('irrégularité');
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.rdd-banner--info');
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
