import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { AppelCphSectionComponent } from './appel-cph-section.component';
import { AppelCphResponse } from '../../core/models/appel-cph.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('AppelCphSectionComponent', () => {
  let component: AppelCphSectionComponent;
  let fixture: ComponentFixture<AppelCphSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/appel-cph-analysis';

  function response(overrides: Partial<AppelCphResponse> = {}): AppelCphResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationJugement: '2026-05-01',
      partieAppelante: 'SALARIE',
      modeNotification: 'SIGNIFICATION',
      representationConstituee: 'AVOCAT',
      jugementEnDernierRessort: false,
      dateLimiteAppel: '2026-06-01',
      joursRestants: 20,
      verdict: 'DELAI_OUVERT',
      checklist: [
        { libelle: "Déposer la déclaration d'appel", obligatoire: true, baseJuridique: 'art. 901 CPC' },
        { libelle: 'Constituer un représentant', obligatoire: true, baseJuridique: 'R. 1461-2 C. trav.' },
        { libelle: 'Notifier les conclusions', obligatoire: false, baseJuridique: 'art. 908 CPC' },
      ],
      baseJuridique: 'art. 538 CPC ; R. 1461-1 et s. C. trav.',
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function renderResult(overrides: Partial<AppelCphResponse> = {}): void {
    fixture.detectChanges();
    flush404();
    component.dateNotificationJugement.set('2026-05-01');
    component.analyze();
    httpMock.expectOne(BASE_URL).flush(response(overrides));
    fixture.detectChanges();
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [AppelCphSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AppelCphSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    component.forceExpanded = true;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(AppelCphSectionComponent.TOOL_LABEL).toContain('APPEL CPH');
    expect(AppelCphSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('renders the form with date / selects / checkbox when FRANCE (no existing analysis)', () => {
    fixture.detectChanges();
    flush404();
    fixture.detectChanges();
    const html: string = fixture.nativeElement.innerHTML;
    expect(component.showForm()).toBe(true);
    expect(html).toContain('Date de notification du jugement');
    expect(html).toContain('Partie appelante');
    expect(html).toContain('Mode de notification');
    expect(html).toContain('Représentation constituée');
    expect(html).toContain('premier et dernier ressort');
  });

  it('shows the FR gate banner when workspaceCountry=BELGIQUE (no HTTP call)', () => {
    component.workspaceCountry = 'BELGIQUE';
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('[data-testid="gate-fr"]');
    expect(el).toBeTruthy();
    httpMock.expectNone(BASE_URL);
  });

  // ── 4 verdicts ──────────────────────────────────────────────────────────
  it('verdict DELAI_OUVERT → success banner (green)', () => {
    renderResult({ verdict: 'DELAI_OUVERT', joursRestants: 20 });
    expect(component.verdictClass('DELAI_OUVERT')).toContain('acc-banner--success');
    expect(fixture.nativeElement.querySelector('.acc-banner--success')).toBeTruthy();
  });

  it('verdict DELAI_URGENT → warning banner (gold) at J-7', () => {
    renderResult({ verdict: 'DELAI_URGENT', joursRestants: 6 });
    expect(component.verdictClass('DELAI_URGENT')).toContain('acc-banner--warning');
    expect(component.verdictIcon('DELAI_URGENT')).toBe('hourglass_top');
  });

  it('verdict DELAI_EXPIRE → danger banner (red)', () => {
    renderResult({ verdict: 'DELAI_EXPIRE', joursRestants: -3 });
    expect(component.verdictClass('DELAI_EXPIRE')).toContain('acc-banner--danger');
    const big = fixture.nativeElement.querySelector('.acc-jours-big--danger');
    expect(big).toBeTruthy();
  });

  it('verdict VOIE_FERMEE → navy banner + lien vers F-DT-87', () => {
    renderResult({ verdict: 'VOIE_FERMEE', jugementEnDernierRessort: true });
    expect(component.verdictClass('VOIE_FERMEE')).toContain('acc-banner--navy');
    const link = fixture.nativeElement.querySelector('[data-testid="voie-fermee-link"]');
    expect(link).toBeTruthy();
    expect(link.textContent).toContain('F-DT-87');
  });

  // ── pré-fill IA + badge provenance ────────────────────────────────────────
  it('prefills dateNotificationJugement from aiData and sets provenance badge IA', () => {
    const aiData: TravailExtractedData = { dateNotificationJugement: '2026-05-01' };
    component.aiData = aiData;
    fixture.detectChanges();
    flush404();
    fixture.detectChanges();
    expect(component.dateNotificationJugement()).toBe('2026-05-01');
    expect(component.provenanceDateNotification()).toBe('IA');
    expect(fixture.nativeElement.querySelector('[data-testid="provenance-date"]')).toBeTruthy();
  });

  it('onDateNotificationChange clears the IA provenance badge', () => {
    component.provenanceDateNotification.set('IA');
    component.onDateNotificationChange('2026-04-01');
    expect(component.dateNotificationJugement()).toBe('2026-04-01');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('aiData via ngOnChanges triggers prefill', () => {
    fixture.detectChanges();
    flush404();
    const aiData: TravailExtractedData = { dateNotificationJugement: '2026-03-10' };
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(undefined, aiData, false) });
    expect(component.dateNotificationJugement()).toBe('2026-03-10');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  // ── getPrefillCount 0 / nominal / BE ─────────────────────────────────────
  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(AppelCphSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 0 when date absent (partiel = vide ici)', () => {
    expect(AppelCphSectionComponent.getPrefillCount({
      aiData: { appelCphEnvisage: true }, workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  it('static getPrefillCount returns 1 when dateNotificationJugement present (nominal FRANCE)', () => {
    expect(AppelCphSectionComponent.getPrefillCount({
      aiData: { dateNotificationJugement: '2026-05-01' }, workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(AppelCphSectionComponent.getPrefillCount({
      aiData: { dateNotificationJugement: '2026-05-01' }, workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  // ── checklist : item bloquant si représentation AUCUNE ───────────────────
  it('representationConstituee=AUCUNE → obligatory checklist items become blocking', () => {
    renderResult({ representationConstituee: 'AUCUNE' });
    const obligatoire = { libelle: 'Constituer un représentant', obligatoire: true, baseJuridique: 'R. 1461-2 C. trav.' };
    const facultatif = { libelle: 'Notifier les conclusions', obligatoire: false, baseJuridique: 'art. 908 CPC' };
    expect(component.itemBloquant(obligatoire)).toBe(true);
    expect(component.itemBloquant(facultatif)).toBe(false);
    const bloquant = fixture.nativeElement.querySelector('[data-testid="checklist-bloquant"]');
    expect(bloquant).toBeTruthy();
  });

  it('representationConstituee=AVOCAT → obligatory items are NOT blocking', () => {
    renderResult({ representationConstituee: 'AVOCAT' });
    const obligatoire = { libelle: 'Constituer un représentant', obligatoire: true, baseJuridique: 'R. 1461-2 C. trav.' };
    expect(component.itemBloquant(obligatoire)).toBe(false);
    expect(fixture.nativeElement.querySelector('[data-testid="checklist-bloquant"]')).toBeNull();
  });

  // ── validation F-IA-03 (coherence) ───────────────────────────────────────
  it('coherenceAlerts flags a divergence ≥ 15 days between aiData date and saisie', () => {
    component.aiData = { dateNotificationJugement: '2026-05-01' };
    fixture.detectChanges();
    flush404();
    // L'avocat corrige manuellement avec un écart > 15 jours.
    component.onDateNotificationChange('2026-03-01');
    fixture.detectChanges();
    expect(component.alertsSummary().total).toBe(1);
    expect(component.coherenceAlerts().DATE_NOTIFICATION?.expectedDisplay).toBe('2026-05-01');
    expect(fixture.nativeElement.querySelector('[data-testid="coherence-badge"]')).toBeTruthy();
  });

  it('coherenceAlerts is empty when saisie matches aiData', () => {
    component.aiData = { dateNotificationJugement: '2026-05-01' };
    fixture.detectChanges();
    flush404();
    fixture.detectChanges();
    expect(component.alertsSummary().total).toBe(0);
  });

  it('coherenceAlerts uses F-96 procedureChecks as a source', () => {
    component.procedureChecks = [
      { id: 'c1', ordre: 1, description: 'Notification du jugement prud’homal', statut: 'VERIFIED', expectedValue: '2026-05-01' },
    ];
    fixture.detectChanges();
    flush404();
    component.onDateNotificationChange('2026-02-01');
    fixture.detectChanges();
    expect(component.alertsSummary().total).toBe(1);
    expect(component.coherenceAlerts().DATE_NOTIFICATION?.expectedDisplay).toBe('2026-05-01');
  });

  // ── POST submit ──────────────────────────────────────────────────────────
  it('analyze() posts the request and switches to the result view', () => {
    fixture.detectChanges();
    flush404();
    component.dateNotificationJugement.set('2026-05-01');
    component.partieAppelante.set('EMPLOYEUR');
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.partieAppelante).toBe('EMPLOYEUR');
    req.flush(response());
    expect(component.showForm()).toBe(false);
    expect(component.result()?.verdict).toBe('DELAI_OUVERT');
  });

  it('analyze() shows a snackbar error on HTTP failure', () => {
    fixture.detectChanges();
    flush404();
    component.dateNotificationJugement.set('2026-05-01');
    component.analyze();
    httpMock.expectOne(BASE_URL).flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalled();
    expect(component.analyzing()).toBe(false);
  });
});
