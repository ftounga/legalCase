import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CarteAProrogationBeSectionComponent } from './carte-a-prorogation-be-section.component';
import { CarteAProrogationBeResponse } from '../../core/models/carte-a-prorogation-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('CarteAProrogationBeSectionComponent', () => {
  let component: CarteAProrogationBeSectionComponent;
  let fixture: ComponentFixture<CarteAProrogationBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/carte-a-prorogation-be-analysis';

  function beResponse(
    overrides: Partial<CarteAProrogationBeResponse> = {},
  ): CarteAProrogationBeResponse {
    return {
      caseFileId: 'case-1',
      dateExpirationCarteA: '2026-09-01',
      motifSejourPersiste: true,
      conditionsInitialesToujoursReunies: true,
      demandeDeposee: false,
      dateDemande: null,
      verdict: 'PROROGEABLE',
      joursAvantExpiration: 40,
      dateOuvertureFenetre: '2026-07-18',
      dateLimiteRecommandee: '2026-08-02',
      basesJuridiques: ['Loi du 15/12/1980 art. 13 (à vérifier par avocat)'],
      messages: ['Conditions de prorogation réunies.'],
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
      imports: [CarteAProrogationBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CarteAProrogationBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(CarteAProrogationBeSectionComponent.TOOL_LABEL).toContain('PROROGATION CARTE A');
    expect(CarteAProrogationBeSectionComponent.TOOL_ICON).toBe('event_repeat');
  });

  // ---- getPrefillCount : 0 / partiel / nominal=3 ----
  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(CarteAProrogationBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (date only)', () => {
    expect(CarteAProrogationBeSectionComponent.getPrefillCount({
      aiData: { carteAProrogationDateExpiration: '2026-09-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 3 (nominal) when all real signals present (BELGIQUE)', () => {
    expect(CarteAProrogationBeSectionComponent.getPrefillCount({
      aiData: {
        carteAProrogationDateExpiration: '2026-09-01',
        carteAProrogationMotifPersiste: true,
        carteAProrogationConditionsReunies: true,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(CarteAProrogationBeSectionComponent.getPrefillCount({
      aiData: { carteAProrogationDateExpiration: '2026-09-01' },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---- HTTP lifecycle ----
  it('BELGIQUE -> GET called on ngOnInit', () => {
    expect(component.isBelgique()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('FRANCE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse());
    expect(component.result()!.verdict).toBe('PROROGEABLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateExpirationCarteA()).toBe('2026-09-01');
    expect(component.motifSejourPersiste()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---- form validation (bouton désactivé si vide) ----
  it('formValid false until date present; true once date set', () => {
    expect(component.formValid()).toBe(false);
    component.dateExpirationCarteA.set('2026-09-01');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false when demandeDeposee but no dateDemande', () => {
    component.dateExpirationCarteA.set('2026-09-01');
    component.onDemandeDeposeeChange(true);
    expect(component.formValid()).toBe(false);
    component.dateDemande.set('2026-08-01');
    expect(component.formValid()).toBe(true);
  });

  // ---- pré-fill IA (3 champs) ----
  it('prefills the 3 real fields from aiData on ngOnChanges (BELGIQUE)', () => {
    const aiData: ImmigrationExtractedData = {
      carteAProrogationDateExpiration: '2026-10-15',
      carteAProrogationMotifPersiste: true,
      carteAProrogationConditionsReunies: false,
    } as ImmigrationExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.dateExpirationCarteA()).toBe('2026-10-15');
    expect(component.motifSejourPersiste()).toBe(true);
    expect(component.conditionsInitialesToujoursReunies()).toBe(false);
    expect(component.provenanceDateExpiration()).toBe('IA');
  });

  // ---- POST nominal ----
  it('POST on analyze() and switches to result mode', () => {
    component.dateExpirationCarteA.set('2026-09-01');
    component.motifSejourPersiste.set(true);
    component.conditionsInitialesToujoursReunies.set(true);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateExpirationCarteA).toBe('2026-09-01');
    req.flush(beResponse());
    expect(component.result()!.verdict).toBe('PROROGEABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('does not POST when form invalid', () => {
    component.analyze();
    httpMock.expectNone(BASE_URL);
  });

  // ---- verdict mapping ----
  it('maps verdict to banner/chip/icon/label classes', () => {
    expect(component.verdictChipClass('PROROGEABLE')).toContain('success');
    expect(component.verdictChipClass('A_DEPOSER_URGENT')).toContain('warning');
    expect(component.verdictChipClass('CONDITIONS_NON_REUNIES')).toContain('danger');
    expect(component.verdictChipClass('EXPIREE')).toContain('danger');
    expect(component.verdictChipClass('DEMANDE_DEPOSEE')).toContain('info');
    expect(component.verdictLabel('EXPIREE')).toBe('Carte expirée');
    expect(component.verdictIcon('PROROGEABLE')).toBe('event_available');
  });

  it('formatDateFr converts ISO to dd/mm/yyyy', () => {
    expect(component.formatDateFr('2026-09-01')).toBe('01/09/2026');
    expect(component.formatDateFr(null)).toBe('—');
  });

  it('joursClass is negative-styled when jours < 0', () => {
    expect(component.joursClass(-3)).toContain('negative');
    expect(component.joursClass(10)).not.toContain('negative');
  });
});
