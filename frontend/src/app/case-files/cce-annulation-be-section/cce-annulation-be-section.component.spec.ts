import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CceAnnulationBeSectionComponent } from './cce-annulation-be-section.component';
import { CceAnnulationBeResponse } from '../../core/models/cce-annulation-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('CceAnnulationBeSectionComponent', () => {
  let component: CceAnnulationBeSectionComponent;
  let fixture: ComponentFixture<CceAnnulationBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/cce-annulation-be-analysis';

  function beResponse(
    overrides: Partial<CceAnnulationBeResponse> = {},
  ): CceAnnulationBeResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationDecision: '2026-05-01',
      typeDecision: 'REFUS_TITRE',
      recoursForme: false,
      dateRecours: null,
      dateLimiteRecours: '2026-05-31',
      joursRestants: 12,
      statut: 'DISPONIBLE',
      recommandation: null,
      delaisMemoire: 'Mémoire de synthèse dans les 8 jours de la notification du dossier administratif.',
      baseJuridique: 'Loi 15/12/1980 art. 39/2 §2 et 39/57 §1er',
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
      imports: [CceAnnulationBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CceAnnulationBeSectionComponent);
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
    expect(CceAnnulationBeSectionComponent.TOOL_LABEL).toContain('RECOURS CCE ANNULATION');
    expect(CceAnnulationBeSectionComponent.TOOL_ICON).toBe('gavel');
  });

  // ---- getPrefillCount : 0 / partiel / nominal=2 ----
  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(CceAnnulationBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (date only)', () => {
    expect(CceAnnulationBeSectionComponent.getPrefillCount({
      aiData: { recoursCceDateNotification: '2026-05-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 2 (nominal) when both real signals present (BELGIQUE)', () => {
    expect(CceAnnulationBeSectionComponent.getPrefillCount({
      aiData: {
        recoursCceDateNotification: '2026-05-01',
        recoursCceTypeDecision: 'OQT_ANNEXE13',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(CceAnnulationBeSectionComponent.getPrefillCount({
      aiData: { recoursCceDateNotification: '2026-05-01' },
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
    expect(component.dateNotificationDecision()).toBe('2026-05-01');
    expect(component.typeDecision()).toBe('REFUS_TITRE');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---- form validation ----
  it('formValid false until date + typeDecision present', () => {
    expect(component.formValid()).toBe(false);
    component.dateNotificationDecision.set('2026-05-01');
    expect(component.formValid()).toBe(false);
    component.typeDecision.set('REFUS_TITRE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid requires dateRecours when recoursForme=true', () => {
    component.dateNotificationDecision.set('2026-05-01');
    component.typeDecision.set('REFUS_TITRE');
    component.onRecoursFormeChange(true);
    expect(component.formValid()).toBe(false);
    component.onDateRecoursChange('2026-05-10');
    expect(component.formValid()).toBe(true);
  });

  it('unchecking recoursForme clears dateRecours', () => {
    component.onRecoursFormeChange(true);
    component.onDateRecoursChange('2026-05-10');
    expect(component.dateRecours()).toBe('2026-05-10');
    component.onRecoursFormeChange(false);
    expect(component.dateRecours()).toBeNull();
  });

  // ---- analyze POST ----
  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationDecision.set('2026-05-01');
    component.typeDecision.set('REFUS_TITRE');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationDecision: '2026-05-01',
      typeDecision: 'REFUS_TITRE',
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
    component.dateNotificationDecision.set('2026-05-01');
    component.typeDecision.set('OQT_ANNEXE13');
    component.onRecoursFormeChange(true);
    component.onDateRecoursChange('2026-05-10');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body).toEqual({
      dateNotificationDecision: '2026-05-01',
      typeDecision: 'OQT_ANNEXE13',
      recoursForme: true,
      dateRecours: '2026-05-10',
    });
    req.flush(beResponse({ statut: 'RECOURS_FORME', recoursForme: true, dateRecours: '2026-05-10' }));
    expect(component.result()!.statut).toBe('RECOURS_FORME');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationDecision.set('2026-05-01');
    component.typeDecision.set('REFUS_9TER');
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
      recoursCceDateNotification: '2026-04-15',
      recoursCceTypeDecision: 'DECISION_CGRA',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateNotificationDecision()).toBe('2026-04-15');
    expect(component.provenanceDateNotification()).toBe('IA');
    expect(component.typeDecision()).toBe('DECISION_CGRA');
    expect(component.provenanceTypeDecision()).toBe('IA');
  });

  it('onDateNotificationChange clears provenance', () => {
    component.aiData = { recoursCceDateNotification: '2026-04-15' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateNotification()).toBe('IA');
    component.onDateNotificationChange('2026-04-20');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.typeDecision()).toBeNull();
    component.aiData = {
      recoursCceDateNotification: '2026-03-01',
      recoursCceTypeDecision: 'REFUS_REGROUPEMENT',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.typeDecision()).toBe('REFUS_REGROUPEMENT');
    expect(component.provenanceTypeDecision()).toBe('IA');
  });

  // ---- F-IA-03 divergence ----
  it('F-IA-03: coherence alert when user date diverges from IA date', () => {
    component.aiData = { recoursCceDateNotification: '2026-05-01' } as ImmigrationExtractedData;
    // user overrides with a different date
    component.dateNotificationDecision.set('2026-04-10');
    component.provenanceDateNotification.set(null);
    const alert = component.coherenceAlerts().DATE_NOTIFICATION;
    expect(alert).toBeTruthy();
    expect(alert!.expectedDisplay).toBe('01/05/2026');
  });

  it('F-IA-03: no coherence alert when user date matches IA date', () => {
    component.aiData = { recoursCceDateNotification: '2026-05-01' } as ImmigrationExtractedData;
    component.dateNotificationDecision.set('2026-05-01');
    expect(component.coherenceAlerts().DATE_NOTIFICATION).toBeUndefined();
  });

  // ---- badge 4 états ----
  it('statutBannerClass covers all 4 statuts', () => {
    expect(component.statutBannerClass('DISPONIBLE')).toContain('cce-banner--success');
    expect(component.statutBannerClass('URGENT')).toContain('cce-banner--warning');
    expect(component.statutBannerClass('EXPIRE')).toContain('cce-banner--danger');
    expect(component.statutBannerClass('RECOURS_FORME')).toContain('cce-banner--info');
  });

  it('statutChipClass covers all 4 statuts', () => {
    expect(component.statutChipClass('DISPONIBLE')).toContain('cce-chip--success');
    expect(component.statutChipClass('URGENT')).toContain('cce-chip--warning');
    expect(component.statutChipClass('EXPIRE')).toContain('cce-chip--danger');
    expect(component.statutChipClass('RECOURS_FORME')).toContain('cce-chip--info');
  });

  it('statutLabel + statutIcon cover all 4 statuts', () => {
    expect(component.statutLabel('DISPONIBLE')).toContain('disponible');
    expect(component.statutLabel('URGENT')).toContain('urgent');
    expect(component.statutLabel('EXPIRE')).toContain('expiré');
    expect(component.statutLabel('RECOURS_FORME')).toContain('déjà formé');
    expect(component.statutIcon('EXPIRE')).toBe('event_busy');
    expect(component.statutIcon('RECOURS_FORME')).toBe('task_alt');
  });

  it('showRecommandation true only for URGENT / EXPIRE', () => {
    expect(component.showRecommandation('URGENT')).toBe(true);
    expect(component.showRecommandation('EXPIRE')).toBe(true);
    expect(component.showRecommandation('DISPONIBLE')).toBe(false);
    expect(component.showRecommandation('RECOURS_FORME')).toBe(false);
  });

  // ---- dates JJ/MM/YYYY + jours restants ----
  it('formatDateFr converts ISO to JJ/MM/YYYY', () => {
    expect(component.formatDateFr('2026-05-31')).toBe('31/05/2026');
    expect(component.formatDateFr(null)).toBe('—');
    expect(component.formatDateFr('not-a-date')).toBe('—');
  });

  it('joursRestantsClass is negative-coloured when délai dépassé', () => {
    expect(component.joursRestantsClass(12)).toBe('cce-jours');
    expect(component.joursRestantsClass(0)).toBe('cce-jours');
    expect(component.joursRestantsClass(-3)).toContain('cce-jours--negative');
  });

  it('typeDecisionLabel resolves codes and empty for null', () => {
    expect(component.typeDecisionLabel('REFUS_TITRE')).toContain('titre');
    expect(component.typeDecisionLabel('OQT_ANNEXE13')).toContain('quitter');
    expect(component.typeDecisionLabel(null)).toBe('');
  });

  // ---- rendering : FR banner + JetBrains Mono date + lien F-IM-32 ----
  it('FRANCE workspace shows BE-only info banner instead of form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="fr-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Belgique uniquement');
  });

  it('renders dateLimiteRecours in JetBrains Mono (.cce-date-mono)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse());
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const mono = fixture.nativeElement.querySelector('[data-testid="date-limite"]');
    expect(mono).not.toBeNull();
    expect(mono.classList.contains('cce-date-mono')).toBe(true);
    expect(mono.textContent.trim()).toBe('31/05/2026');
  });

  it('renders cross-link to F-IM-32 only when statut URGENT/EXPIRE', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    // DISPONIBLE -> no recommandation/lien
    component.result.set(beResponse({ statut: 'DISPONIBLE', recommandation: 'rien' }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="lien-fim32"]')).toBeNull();
    // EXPIRE -> recommandation + lien F-IM-32
    component.result.set(beResponse({
      statut: 'EXPIRE',
      joursRestants: -2,
      recommandation: 'Délai dépassé. Voir recours en extrême urgence.',
    }));
    fixture.detectChanges();
    const lien = fixture.nativeElement.querySelector('[data-testid="lien-fim32"]');
    expect(lien).not.toBeNull();
    expect(lien.textContent).toContain('F-IM-32');
  });

  it('renders negative joursRestants with negative class for EXPIRE', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ statut: 'EXPIRE', joursRestants: -5, recommandation: 'r' }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const jours = fixture.nativeElement.querySelector('[data-testid="jours-restants"]');
    expect(jours).not.toBeNull();
    expect(jours.classList.contains('cce-jours--negative')).toBe(true);
    expect(jours.textContent.trim()).toBe('-5');
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
