import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { HarcelementProcedureInterneSectionComponent } from './harcelement-procedure-interne-section.component';
import { HarcelementProcedureInterneResponse } from '../../core/models/harcelement-procedure-interne.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('HarcelementProcedureInterneSectionComponent', () => {
  let component: HarcelementProcedureInterneSectionComponent;
  let fixture: ComponentFixture<HarcelementProcedureInterneSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/harcelement-procedure-interne-analysis';

  function conformeResponse(overrides: Partial<HarcelementProcedureInterneResponse> = {}): HarcelementProcedureInterneResponse {
    return {
      caseFileId: 'case-1',
      effectif: 50,
      referentCseDesigne: true,
      referentEmployeurDesigne: false,
      informationAffichageRealisee: true,
      signalementRecu: true,
      dateSignalement: '2026-01-01',
      dateOuvertureEnquete: '2026-01-08',
      enqueteContradictoire: true,
      mesuresConservatoiresPrises: true,
      checklist: [
        { item: 'Référent CSE harcèlement sexuel désigné', conforme: true, obligatoire: true, commentaire: 'Désigné' },
        { item: 'Information / affichage des salariés', conforme: true, obligatoire: true, commentaire: '' },
        { item: 'Enquête contradictoire et impartiale', conforme: true, obligatoire: true, commentaire: '' },
      ],
      itemsObligatoiresManquants: 0,
      statut: 'CONFORME',
      delaiReactionJours: 7,
      delaiRaisonnable: 'OUI',
      risqueResponsabiliteEmployeur: 'FAIBLE',
      country: 'FRANCE',
      baseJuridique: 'Art. L.1153-5-1, L.2314-1, L.1152-4 CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  function nonConformeResponse(): HarcelementProcedureInterneResponse {
    return conformeResponse({
      referentCseDesigne: false,
      checklist: [
        { item: 'Référent CSE harcèlement sexuel désigné', conforme: false, obligatoire: true, commentaire: 'Manquant' },
        { item: 'Information / affichage des salariés', conforme: true, obligatoire: true, commentaire: '' },
        { item: 'Enquête contradictoire et impartiale', conforme: true, obligatoire: true, commentaire: '' },
      ],
      itemsObligatoiresManquants: 1,
      statut: 'NON_CONFORME',
      risqueResponsabiliteEmployeur: 'MODERE',
    });
  }

  function carenceGraveResponse(): HarcelementProcedureInterneResponse {
    return conformeResponse({
      enqueteContradictoire: false,
      dateOuvertureEnquete: null,
      delaiReactionJours: 90,
      checklist: [
        { item: 'Référent CSE harcèlement sexuel désigné', conforme: true, obligatoire: true, commentaire: '' },
        { item: 'Enquête déclenchée à réception du signalement', conforme: false, obligatoire: true, commentaire: 'Non diligentée' },
      ],
      itemsObligatoiresManquants: 1,
      statut: 'CARENCE_GRAVE',
      delaiRaisonnable: 'NON',
      risqueResponsabiliteEmployeur: 'ELEVE',
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [HarcelementProcedureInterneSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HarcelementProcedureInterneSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // --- statics / contract ---

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(HarcelementProcedureInterneSectionComponent.TOOL_LABEL).toContain('HARCÈLEMENT');
    expect(HarcelementProcedureInterneSectionComponent.TOOL_ICON).toBe('report_problem');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(HarcelementProcedureInterneSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 (nominal) with both fields', () => {
    expect(HarcelementProcedureInterneSectionComponent.getPrefillCount({
      aiData: { pseNombreSalaries: 50, harcelementSignalementInterne: true },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(HarcelementProcedureInterneSectionComponent.getPrefillCount({
      aiData: { pseNombreSalaries: 50, harcelementSignalementInterne: true },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  // --- gate pays / lifecycle ---

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

  it('BELGIQUE workspace shows FR gate banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="country-gate-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('français uniquement');
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(conformeResponse());
    expect(component.result()!.statut).toBe('CONFORME');
    expect(component.showForm()).toBe(false);
    expect(component.effectif()).toBe(50);
    expect(component.referentCseDesigne()).toBe(true);
    expect(component.signalementRecu()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validity ---

  it('formValid requires a positive effectif', () => {
    expect(component.formValid()).toBe(false);
    component.effectif.set(0);
    expect(component.formValid()).toBe(false);
    component.effectif.set(12);
    expect(component.formValid()).toBe(true);
  });

  // --- coherence (F-IA-03) ---

  it('raises a coherence alert when effectif >= 11 sans référent CSE', () => {
    component.effectif.set(12);
    component.referentCseDesigne.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('référent harcèlement sexuel au CSE'))).toBe(true);
  });

  it('raises a coherence alert when signalement reçu sans date d\'ouverture d\'enquête', () => {
    component.signalementRecu.set(true);
    component.dateOuvertureEnquete.set(null);
    expect(component.coherenceAlerts().some(a => a.includes('aucune date d\'ouverture'))).toBe(true);
  });

  it('raises a coherence alert when date enquête antérieure au signalement', () => {
    component.dateSignalement.set('2026-02-01');
    component.dateOuvertureEnquete.set('2026-01-15');
    expect(component.coherenceAlerts().some(a => a.includes('antérieure'))).toBe(true);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    component.effectif.set(50);
    component.referentCseDesigne.set(true);
    component.referentEmployeurDesigne.set(false);
    component.informationAffichageRealisee.set(true);
    component.signalementRecu.set(true);
    component.dateSignalement.set('2026-01-01');
    component.dateOuvertureEnquete.set('2026-01-08');
    component.enqueteContradictoire.set(true);
    component.mesuresConservatoiresPrises.set(true);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      effectif: 50,
      referentCseDesigne: true,
      referentEmployeurDesigne: false,
      informationAffichageRealisee: true,
      signalementRecu: true,
      dateSignalement: '2026-01-01',
      dateOuvertureEnquete: '2026-01-08',
      enqueteContradictoire: true,
      mesuresConservatoiresPrises: true,
    });
    req.flush(conformeResponse());
    expect(component.result()!.statut).toBe('CONFORME');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid (no effectif)', () => {
    component.ngOnInit();
    flush404();
    component.effectif.set(null);
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() error -> snack error, stays in form', () => {
    component.ngOnInit();
    flush404();
    component.effectif.set(50);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : 3 statut states + checklist + risque + délai ---

  it('CONFORME -> success statut chip + risque FAIBLE + checklist ✓ + délai OUI', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(conformeResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('conforme');
    expect(chip.className).toContain('is-chip--success');
    expect(el.querySelector('[data-testid="risque-chip"]')!.textContent).toContain('faible');
    expect(el.querySelector('[data-testid="delai-chip"]')!.textContent).toContain('raisonnable');
    expect(el.querySelector('[data-testid="delai-chip"]')!.className).toContain('is-chip--success');
    expect(el.querySelector('[data-testid="delai-jours"]')!.textContent).toContain('7');
    const items = el.querySelectorAll('[data-testid="checklist"] .is-critere');
    expect(items.length).toBe(3);
    expect(el.querySelector('[data-testid="items-manquants"]')!.textContent).toContain('0');
    expect(el.querySelectorAll('[data-testid="badge-obligatoire"]').length).toBe(3);
    expect(el.querySelector('[data-testid="risque-note"]')!.textContent).toContain('L.4121-1');
  });

  it('NON_CONFORME -> warning statut chip + risque MODERE + item référent CSE ✗', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(nonConformeResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('non conforme');
    expect(chip.className).toContain('is-chip--warning');
    expect(el.querySelector('[data-testid="risque-chip"]')!.textContent).toContain('modéré');
    expect(el.querySelector('[data-testid="items-manquants"]')!.textContent).toContain('1');
    const koItems = el.querySelectorAll('[data-testid="checklist"] .is-critere--ko');
    expect(koItems.length).toBe(1);
  });

  it('CARENCE_GRAVE -> danger statut chip + risque ELEVE + délai NON', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(carenceGraveResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('Carence grave');
    expect(chip.className).toContain('is-chip--danger');
    expect(el.querySelector('[data-testid="risque-chip"]')!.textContent).toContain('élevé');
    expect(el.querySelector('[data-testid="risque-chip"]')!.className).toContain('is-chip--danger');
    const delaiChip = el.querySelector('[data-testid="delai-chip"]')!;
    expect(delaiChip.textContent).toContain('non raisonnable');
    expect(delaiChip.className).toContain('is-chip--danger');
  });

  it('statut / délai / risque chip classes map their states', () => {
    expect(component.statutChipClass('CONFORME')).toContain('success');
    expect(component.statutChipClass('NON_CONFORME')).toContain('warning');
    expect(component.statutChipClass('CARENCE_GRAVE')).toContain('danger');
    expect(component.delaiChipClass('OUI')).toContain('success');
    expect(component.delaiChipClass('LIMITE')).toContain('warning');
    expect(component.delaiChipClass('NON')).toContain('danger');
    expect(component.risqueChipClass('FAIBLE')).toContain('success');
    expect(component.risqueChipClass('MODERE')).toContain('warning');
    expect(component.risqueChipClass('ELEVE')).toContain('danger');
  });

  // --- pré-fill IA ---

  it('pré-fills effectif and signalementRecu from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      pseNombreSalaries: 120,
      harcelementSignalementInterne: true,
    };
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.effectif()).toBe(120);
    expect(component.signalementRecu()).toBe(true);
    expect(component.provenanceEffectif()).toBe('IA');
    expect(component.provenanceSignalement()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData: TravailExtractedData = { pseNombreSalaries: 30 };
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.effectif()).toBe(30);
    expect(component.provenanceEffectif()).toBe('IA');
  });

  it('onEffectifChange clears provenance', () => {
    component.provenanceEffectif.set('IA');
    component.onEffectifChange(42);
    expect(component.effectif()).toBe(42);
    expect(component.provenanceEffectif()).toBeNull();
  });

  it('onSignalementRecuChange clears provenance', () => {
    component.provenanceSignalement.set('IA');
    component.onSignalementRecuChange(false);
    expect(component.signalementRecu()).toBe(false);
    expect(component.provenanceSignalement()).toBeNull();
  });
});
