import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CceSuspensionBeSectionComponent } from './cce-suspension-be-section.component';
import { CceSuspensionBeResponse } from '../../core/models/cce-suspension-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('CceSuspensionBeSectionComponent', () => {
  let component: CceSuspensionBeSectionComponent;
  let fixture: ComponentFixture<CceSuspensionBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/cce-suspension-be-analysis';

  function beResponse(
    overrides: Partial<CceSuspensionBeResponse> = {},
  ): CceSuspensionBeResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationDecision: '2026-05-30',
      recoursAnnulationIntroduit: false,
      urgenceInvocable: true,
      risquePrejudiceGraveDifficilementReparable: true,
      mesureEloignementImminente: false,
      verdict: 'CONDITIONS_REUNIES',
      joursDepuisNotification: 4,
      renvoiOutil: null,
      basesJuridiques: ['Loi du 15/12/1980 art. 39/82 (à vérifier par avocat)'],
      messages: ['Conditions réunies.'],
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
      imports: [CceSuspensionBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CceSuspensionBeSectionComponent);
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
    expect(CceSuspensionBeSectionComponent.TOOL_LABEL).toContain('RECOURS CCE SUSPENSION');
    expect(CceSuspensionBeSectionComponent.TOOL_ICON).toBe('pause_circle');
  });

  // ---- getPrefillCount : 0 / partiel / nominal=3 ----
  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(CceSuspensionBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (date only)', () => {
    expect(CceSuspensionBeSectionComponent.getPrefillCount({
      aiData: { cceSuspensionDateNotification: '2026-05-30' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 3 (nominal) when all real signals present (BELGIQUE)', () => {
    expect(CceSuspensionBeSectionComponent.getPrefillCount({
      aiData: {
        cceSuspensionDateNotification: '2026-05-30',
        cceSuspensionUrgence: true,
        cceSuspensionPrejudiceGrave: false,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(CceSuspensionBeSectionComponent.getPrefillCount({
      aiData: { cceSuspensionDateNotification: '2026-05-30' },
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
    expect(component.result()!.verdict).toBe('CONDITIONS_REUNIES');
    expect(component.showForm()).toBe(false);
    expect(component.dateNotificationDecision()).toBe('2026-05-30');
    expect(component.urgenceInvocable()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---- form validation (bouton désactivé si pas de date) ----
  it('formValid false until date present; true once date set', () => {
    expect(component.formValid()).toBe(false);
    component.dateNotificationDecision.set('2026-05-30');
    expect(component.formValid()).toBe(true);
  });

  // ---- pré-fill IA (3 champs réels) ----
  it('prefills the 3 real fields from aiData on ngOnChanges (BELGIQUE)', () => {
    const aiData: ImmigrationExtractedData = {
      cceSuspensionDateNotification: '2026-05-15',
      cceSuspensionUrgence: true,
      cceSuspensionPrejudiceGrave: false,
    } as ImmigrationExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.dateNotificationDecision()).toBe('2026-05-15');
    expect(component.urgenceInvocable()).toBe(true);
    expect(component.risquePrejudiceGraveDifficilementReparable()).toBe(false);
    expect(component.provenanceDateNotification()).toBe('IA');
    expect(component.provenanceUrgence()).toBe('IA');
    expect(component.provenancePrejudice()).toBe('IA');
  });

  // ---- POST nominal ----
  it('POST on analyze() and switches to result mode', () => {
    component.dateNotificationDecision.set('2026-05-30');
    component.urgenceInvocable.set(true);
    component.risquePrejudiceGraveDifficilementReparable.set(true);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateNotificationDecision).toBe('2026-05-30');
    expect(req.request.body.urgenceInvocable).toBe(true);
    req.flush(beResponse());
    expect(component.result()!.verdict).toBe('CONDITIONS_REUNIES');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('does not POST when form invalid', () => {
    component.analyze();
    httpMock.expectNone(BASE_URL);
  });

  // ---- renvoi cross-outil extrême urgence ----
  it('exposes renvoiOutil on REORIENTER_EXTREME_URGENCE', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({
      verdict: 'REORIENTER_EXTREME_URGENCE',
      mesureEloignementImminente: true,
      renvoiOutil: 'F-IM-32-cce-extreme-urgence-5j-be',
    }));
    expect(component.result()!.verdict).toBe('REORIENTER_EXTREME_URGENCE');
    expect(component.result()!.renvoiOutil).toBe('F-IM-32-cce-extreme-urgence-5j-be');
  });

  // ---- verdict mapping ----
  it('maps verdict to banner/chip/icon/label classes', () => {
    expect(component.verdictChipClass('CONDITIONS_REUNIES')).toContain('success');
    expect(component.verdictChipClass('URGENCE_NON_DEMONTREE')).toContain('warning');
    expect(component.verdictChipClass('PREJUDICE_NON_DEMONTRE')).toContain('warning');
    expect(component.verdictChipClass('HORS_DELAI_ANNULATION')).toContain('warning');
    expect(component.verdictChipClass('REORIENTER_EXTREME_URGENCE')).toContain('info');
    expect(component.verdictLabel('REORIENTER_EXTREME_URGENCE')).toBe('Réorienter extrême urgence');
    expect(component.verdictIcon('CONDITIONS_REUNIES')).toBe('gavel');
  });

  it('formatDateFr converts ISO to dd/mm/yyyy', () => {
    expect(component.formatDateFr('2026-05-30')).toBe('30/05/2026');
    expect(component.formatDateFr(null)).toBe('—');
  });
});
