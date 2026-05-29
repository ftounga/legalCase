import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CceExtremeUrgenceBeSectionComponent } from './cce-extreme-urgence-be-section.component';
import { CceExtremeUrgenceBeResponse } from '../../core/models/cce-extreme-urgence-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('CceExtremeUrgenceBeSectionComponent', () => {
  let component: CceExtremeUrgenceBeSectionComponent;
  let fixture: ComponentFixture<CceExtremeUrgenceBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/cce-extreme-urgence-be-analysis';

  function beResponse(
    overrides: Partial<CceExtremeUrgenceBeResponse> = {},
  ): CceExtremeUrgenceBeResponse {
    return {
      caseFileId: 'case-1',
      dateActeExecutoire: '2026-05-01',
      typeActe: 'OQT_EXECUTE',
      recoursForme: false,
      dateRecours: null,
      dateLimiteRecours: '2026-05-08',
      joursOuvrablesRestants: 4,
      statut: 'DISPONIBLE',
      audienceEstimee: '2026-05-12',
      actionImmediate: null,
      baseJuridique: 'Loi 15/12/1980 art. 39/82',
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
      imports: [CceExtremeUrgenceBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CceExtremeUrgenceBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics (CCE = Conseil du Contentieux des Étrangers)', () => {
    expect(CceExtremeUrgenceBeSectionComponent.TOOL_LABEL).toContain('EXTRÊME URGENCE');
    expect(CceExtremeUrgenceBeSectionComponent.TOOL_ICON).toBe('gavel');
  });

  // ---- getPrefillCount : 0 / partiel / nominal=2 ----
  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(CceExtremeUrgenceBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (date only)', () => {
    expect(CceExtremeUrgenceBeSectionComponent.getPrefillCount({
      aiData: { recoursExtremeUrgenceDateActe: '2026-05-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 2 (nominal) when both real signals present (BELGIQUE)', () => {
    expect(CceExtremeUrgenceBeSectionComponent.getPrefillCount({
      aiData: {
        recoursExtremeUrgenceDateActe: '2026-05-01',
        recoursExtremeUrgenceTypeActe: 'TRANSFERT_DUBLIN',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(CceExtremeUrgenceBeSectionComponent.getPrefillCount({
      aiData: { recoursExtremeUrgenceDateActe: '2026-05-01' },
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
    expect(component.result()!.statut).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateActeExecutoire()).toBe('2026-05-01');
    expect(component.typeActe()).toBe('OQT_EXECUTE');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---- form validation ----
  it('formValid false until date + typeActe present', () => {
    expect(component.formValid()).toBe(false);
    component.dateActeExecutoire.set('2026-05-01');
    expect(component.formValid()).toBe(false);
    component.typeActe.set('OQT_EXECUTE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid requires dateRecours when recoursForme=true', () => {
    component.dateActeExecutoire.set('2026-05-01');
    component.typeActe.set('OQT_EXECUTE');
    component.onRecoursFormeChange(true);
    expect(component.formValid()).toBe(false);
    component.onDateRecoursChange('2026-05-04');
    expect(component.formValid()).toBe(true);
  });

  it('unchecking recoursForme clears dateRecours', () => {
    component.onRecoursFormeChange(true);
    component.onDateRecoursChange('2026-05-04');
    expect(component.dateRecours()).toBe('2026-05-04');
    component.onRecoursFormeChange(false);
    expect(component.dateRecours()).toBeNull();
  });

  // ---- analyze POST ----
  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.dateActeExecutoire.set('2026-05-01');
    component.typeActe.set('OQT_EXECUTE');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateActeExecutoire: '2026-05-01',
      typeActe: 'OQT_EXECUTE',
      recoursForme: false,
      dateRecours: null,
    });
    req.flush(beResponse());
    expect(component.result()!.statut).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() POST with recoursForme sends dateRecours', () => {
    component.ngOnInit();
    flush404();
    component.dateActeExecutoire.set('2026-05-01');
    component.typeActe.set('TRANSFERT_DUBLIN');
    component.onRecoursFormeChange(true);
    component.onDateRecoursChange('2026-05-04');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body).toEqual({
      dateActeExecutoire: '2026-05-01',
      typeActe: 'TRANSFERT_DUBLIN',
      recoursForme: true,
      dateRecours: '2026-05-04',
    });
    req.flush(beResponse({ statut: 'RECOURS_FORME', recoursForme: true, dateRecours: '2026-05-04' }));
    expect(component.result()!.statut).toBe('RECOURS_FORME');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dateActeExecutoire.set('2026-05-01');
    component.typeActe.set('EXPULSION_IMMEDIATE');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // ---- pré-fill IA ----
  it('aiData with both real signals -> pre-fills + provenance IA', () => {
    component.aiData = {
      recoursExtremeUrgenceDateActe: '2026-04-15',
      recoursExtremeUrgenceTypeActe: 'REFUS_ACCES_TERRITOIRE',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateActeExecutoire()).toBe('2026-04-15');
    expect(component.provenanceDateActe()).toBe('IA');
    expect(component.typeActe()).toBe('REFUS_ACCES_TERRITOIRE');
    expect(component.provenanceTypeActe()).toBe('IA');
  });

  it('onDateActeChange clears provenance', () => {
    component.aiData = { recoursExtremeUrgenceDateActe: '2026-04-15' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateActe()).toBe('IA');
    component.onDateActeChange('2026-04-20');
    expect(component.provenanceDateActe()).toBeNull();
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.typeActe()).toBeNull();
    component.aiData = {
      recoursExtremeUrgenceDateActe: '2026-03-01',
      recoursExtremeUrgenceTypeActe: 'AUTRE',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.typeActe()).toBe('AUTRE');
    expect(component.provenanceTypeActe()).toBe('IA');
  });

  // ---- F-IA-03 divergence (voie a : pas de directive popover) ----
  it('F-IA-03: coherence alert when user date diverges from IA date', () => {
    component.aiData = { recoursExtremeUrgenceDateActe: '2026-05-01' } as ImmigrationExtractedData;
    component.dateActeExecutoire.set('2026-04-10');
    component.provenanceDateActe.set(null);
    const alert = component.dateActeAlert();
    expect(alert).toBeTruthy();
    expect(alert!.expectedDisplay).toBe('01/05/2026');
  });

  it('F-IA-03: no coherence alert when user date matches IA date', () => {
    component.aiData = { recoursExtremeUrgenceDateActe: '2026-05-01' } as ImmigrationExtractedData;
    component.dateActeExecutoire.set('2026-05-01');
    expect(component.dateActeAlert()).toBeNull();
  });

  // ---- badge / statuts ----
  it('isUrgenceAbsolue true only for CRITIQUE / EXPIRE', () => {
    expect(component.isUrgenceAbsolue('CRITIQUE')).toBe(true);
    expect(component.isUrgenceAbsolue('EXPIRE')).toBe(true);
    expect(component.isUrgenceAbsolue('DISPONIBLE')).toBe(false);
    expect(component.isUrgenceAbsolue('RECOURS_FORME')).toBe(false);
  });

  it('statutBannerClass covers all 4 statuts (CRITIQUE/EXPIRE -> critical)', () => {
    expect(component.statutBannerClass('DISPONIBLE')).toContain('cce-banner--success');
    expect(component.statutBannerClass('CRITIQUE')).toContain('cce-banner--critical');
    expect(component.statutBannerClass('EXPIRE')).toContain('cce-banner--critical');
    expect(component.statutBannerClass('RECOURS_FORME')).toContain('cce-banner--info');
  });

  it('statutChipClass covers all 4 statuts', () => {
    expect(component.statutChipClass('DISPONIBLE')).toContain('cce-chip--success');
    expect(component.statutChipClass('CRITIQUE')).toContain('cce-chip--critical');
    expect(component.statutChipClass('EXPIRE')).toContain('cce-chip--critical');
    expect(component.statutChipClass('RECOURS_FORME')).toContain('cce-chip--info');
  });

  it('statutLabel + statutIcon cover all 4 statuts', () => {
    expect(component.statutLabel('DISPONIBLE')).toContain('disponible');
    expect(component.statutLabel('CRITIQUE')).toContain('URGENCE');
    expect(component.statutLabel('EXPIRE')).toContain('expiré');
    expect(component.statutLabel('RECOURS_FORME')).toContain('déjà formé');
    expect(component.statutIcon('CRITIQUE')).toBe('priority_high');
    expect(component.statutIcon('EXPIRE')).toBe('event_busy');
  });

  // ---- dates JJ/MM/YYYY + jours restants ----
  it('formatDateFr converts ISO to JJ/MM/YYYY', () => {
    expect(component.formatDateFr('2026-05-08')).toBe('08/05/2026');
    expect(component.formatDateFr(null)).toBe('—');
    expect(component.formatDateFr('not-a-date')).toBe('—');
  });

  it('joursRestantsClass is negative-coloured when délai épuisé (<= 0)', () => {
    expect(component.joursRestantsClass(4)).toBe('cce-jours');
    expect(component.joursRestantsClass(0)).toContain('cce-jours--negative');
    expect(component.joursRestantsClass(-2)).toContain('cce-jours--negative');
  });

  it('typeActeLabel resolves codes and empty for null', () => {
    expect(component.typeActeLabel('OQT_EXECUTE')).toContain('quitter');
    expect(component.typeActeLabel('TRANSFERT_DUBLIN')).toContain('Dublin');
    expect(component.typeActeLabel(null)).toBe('');
  });

  // ---- rendering ----
  it('FRANCE workspace shows BE-only info banner instead of form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="fr-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Belgique uniquement');
  });

  // Voie (a) anti-régression : le formulaire se rend SANS erreur de binding
  // (pas de directive appCoherencePopover ni de binding [coherenceAlert]).
  it('renders form fields without binding errors (voie a — no popover directive)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="date-acte-input"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="recours-forme-checkbox"]')).not.toBeNull();
  });

  it('shows F-IA-03 coherence badge inline when date diverges (no popover directive)', () => {
    component.aiData = { recoursExtremeUrgenceDateActe: '2026-05-01' } as ImmigrationExtractedData;
    component.collapsed.set(false);
    component.dateActeExecutoire.set('2026-04-10');
    component.provenanceDateActe.set(null);
    fixture.detectChanges();
    flush404();
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('[data-testid="coherence-date-acte"]');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('01/05/2026');
  });

  it('renders RED critical action banner with actionImmediate when statut CRITIQUE', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      statut: 'CRITIQUE',
      joursOuvrablesRestants: 1,
      actionImmediate: 'Déposer le recours AUJOURD\'HUI — délai expire demain.',
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="bandeau-critique"]');
    expect(banner).not.toBeNull();
    const action = fixture.nativeElement.querySelector('[data-testid="action-immediate"]');
    expect(action.textContent).toContain('AUJOURD');
  });

  it('renders RED critical banner for EXPIRE with negative jours', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      statut: 'EXPIRE',
      joursOuvrablesRestants: -2,
      actionImmediate: 'Délai dépassé.',
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="bandeau-critique"]')).not.toBeNull();
    const jours = fixture.nativeElement.querySelector('[data-testid="jours-restants"]');
    expect(jours.classList.contains('cce-jours--negative')).toBe(true);
    expect(jours.textContent.trim()).toBe('-2');
  });

  it('does NOT render red action banner for DISPONIBLE', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ statut: 'DISPONIBLE' }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="bandeau-critique"]')).toBeNull();
  });

  it('renders joursOuvrablesRestants prominently in JetBrains Mono (.cce-jours)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ joursOuvrablesRestants: 4 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const jours = fixture.nativeElement.querySelector('[data-testid="jours-restants"]');
    expect(jours).not.toBeNull();
    expect(jours.classList.contains('cce-jours')).toBe(true);
    expect(jours.textContent.trim()).toBe('4');
  });

  it('renders audience estimée info-box with JetBrains Mono date', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ audienceEstimee: '2026-05-12' }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const box = fixture.nativeElement.querySelector('[data-testid="audience-box"]');
    expect(box).not.toBeNull();
    expect(box.textContent).toContain('Audience CCE estimée');
    const date = fixture.nativeElement.querySelector('[data-testid="audience-date"]');
    expect(date.classList.contains('cce-date-mono')).toBe(true);
    expect(date.textContent.trim()).toBe('12/05/2026');
  });

  it('renders dateLimiteRecours in JetBrains Mono (.cce-date-mono)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse());
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const mono = fixture.nativeElement.querySelector('[data-testid="date-limite-detail"]');
    expect(mono).not.toBeNull();
    expect(mono.classList.contains('cce-date-mono')).toBe(true);
    expect(mono.textContent.trim()).toBe('08/05/2026');
  });

  it('standaloneMode -> no GET, form visible, simulator banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
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
});
