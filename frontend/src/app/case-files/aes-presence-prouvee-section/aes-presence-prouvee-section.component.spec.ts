import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { AesPresenceProuveeSectionComponent } from './aes-presence-prouvee-section.component';
import { AesPresenceProuveeResponse } from '../../core/models/aes-presence-prouvee.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('AesPresenceProuveeSectionComponent', () => {
  let component: AesPresenceProuveeSectionComponent;
  let fixture: ComponentFixture<AesPresenceProuveeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/aes-presence-prouvee-analysis';

  function frResponse(overrides: Partial<AesPresenceProuveeResponse> = {}): AesPresenceProuveeResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      periodesPresentees: [
        { debut: '2019-01-01', fin: '2024-01-01', typePiece: 'AVIS_IMPOSITION' },
      ],
      anneesTotalesProuvees: 5,
      eligibiliteParVoie: {
        aes_famille: true,
        aes_humanitaire: false,
        aes_etudiant: true,
        aes_metiers_tension: true,
      },
      gapsPeriodes: [],
      recommandationsPieces: [],
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
      imports: [AesPresenceProuveeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AesPresenceProuveeSectionComponent);
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
    expect(AesPresenceProuveeSectionComponent.TOOL_LABEL).toContain('PRÉSENCE PROUVÉE');
    expect(AesPresenceProuveeSectionComponent.TOOL_ICON).toBe('event_available');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(AesPresenceProuveeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 when aesDateEntreeFrance present (FR)', () => {
    expect(AesPresenceProuveeSectionComponent.getPrefillCount({
      aiData: { aesDateEntreeFrance: '2019-01-01' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(AesPresenceProuveeSectionComponent.getPrefillCount({
      aiData: { aesDateEntreeFrance: '2019-01-01' },
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
    expect(component.result()!.anneesTotalesProuvees).toBe(5);
    expect(component.showForm()).toBe(false);
    expect(component.periodes().length).toBe(1);
    expect(component.periodes()[0].debut).toBe('2019-01-01');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('addPeriode appends a new editable row', () => {
    expect(component.periodes().length).toBe(0);
    component.addPeriode();
    expect(component.periodes().length).toBe(1);
    component.addPeriode();
    expect(component.periodes().length).toBe(2);
    expect(component.periodes()[1].typePiece).toBe('AUTRE');
  });

  it('removePeriode removes the row at the given index', () => {
    component.addPeriode();
    component.addPeriode();
    component.onDebutChange(0, '2018-01-01');
    component.onDebutChange(1, '2020-01-01');
    component.removePeriode(0);
    expect(component.periodes().length).toBe(1);
    expect(component.periodes()[0].debut).toBe('2020-01-01');
  });

  it('formValid false until at least one fully valid period exists', () => {
    expect(component.formValid()).toBe(false);
    component.addPeriode();
    // debut empty -> invalid
    expect(component.formValid()).toBe(false);
    component.onDebutChange(0, '2019-01-01');
    component.onFinChange(0, '2024-01-01');
    component.onTypePieceChange(0, 'QUITTANCE_LOYER');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false when a period has debut > fin', () => {
    component.addPeriode();
    component.onDebutChange(0, '2024-06-01');
    component.onFinChange(0, '2020-01-01');
    component.onTypePieceChange(0, 'RIB_BANQUE');
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST nominal -> result + snack + 4 voies', () => {
    component.ngOnInit();
    flush404();
    component.addPeriode();
    component.onDebutChange(0, '2019-01-01');
    component.onFinChange(0, '2024-01-01');
    component.onTypePieceChange(0, 'AVIS_IMPOSITION');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      periodesPresentees: [
        { debut: '2019-01-01', fin: '2024-01-01', typePiece: 'AVIS_IMPOSITION' },
      ],
    });
    req.flush(frResponse());
    expect(component.result()!.anneesTotalesProuvees).toBe(5);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    // grille 4 éligibilités
    expect(component.voieEligible('aes_famille')).toBe(true);
    expect(component.voieEligible('aes_humanitaire')).toBe(false);
    expect(component.voieEligible('aes_etudiant')).toBe(true);
    expect(component.voieEligible('aes_metiers_tension')).toBe(true);
  });

  it('analyze() does nothing when form invalid (no periods)', () => {
    component.ngOnInit();
    flush404();
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.addPeriode();
    component.onDebutChange(0, '2019-01-01');
    component.onFinChange(0, '2024-01-01');
    component.onTypePieceChange(0, 'RIB_BANQUE');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  it('aiData with aesDateEntreeFrance -> pre-fills one initial period (provenance IA)', () => {
    component.aiData = {
      aesDateEntreeFrance: '2019-01-01',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.periodes().length).toBe(1);
    expect(component.periodes()[0].debut).toBe('2019-01-01');
    expect(component.provenanceInitialePeriode()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = {
      aesDateEntreeFrance: '2010-01-01',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expect(component.periodes()[0].debut).toBe('2019-01-01');
    expect(component.provenanceInitialePeriode()).toBeNull();
  });

  it('manual edit clears IA provenance', () => {
    component.aiData = {
      aesDateEntreeFrance: '2019-01-01',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceInitialePeriode()).toBe('IA');
    component.onDebutChange(0, '2018-06-01');
    expect(component.provenanceInitialePeriode()).toBeNull();
  });

  it('voieEligible returns false when no result', () => {
    expect(component.voieEligible('aes_famille')).toBe(false);
  });

  it('typePieceLabel maps codes to FR labels', () => {
    expect(component.typePieceLabel('RIB_BANQUE')).toContain('RIB');
    expect(component.typePieceLabel('AVIS_IMPOSITION')).toContain('imposition');
    expect(component.typePieceLabel(null)).toBe('');
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
    expect(component.periodes().length).toBe(0);
    component.aiData = {
      aesDateEntreeFrance: '2020-03-15',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.periodes().length).toBe(1);
    expect(component.periodes()[0].debut).toBe('2020-03-15');
    expect(component.provenanceInitialePeriode()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    component.aiData = {
      aesDateEntreeFrance: '2005-01-01',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.periodes()[0].debut).toBe('2019-01-01');
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.aes-banner--info');
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

  it('renders dynamic period rows + gaps + recos in result mode', () => {
    component.forceExpanded = true;
    fixture.detectChanges(); // triggers ngOnInit + GET once
    httpMock.expectOne(BASE_URL).flush(frResponse({
      gapsPeriodes: ['2021-01 à 2021-06 non couvert'],
      recommandationsPieces: ['Ajouter une quittance de loyer 2021'],
    }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="aes-gaps"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="aes-recos"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="voie-aes_famille"]')).not.toBeNull();
  });
});
