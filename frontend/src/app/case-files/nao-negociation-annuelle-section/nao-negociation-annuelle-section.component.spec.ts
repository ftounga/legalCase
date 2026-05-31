import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { NaoNegociationAnnuelleSectionComponent } from './nao-negociation-annuelle-section.component';
import { NaoNegociationAnnuelleResponse } from '../../core/models/nao-negociation-annuelle.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('NaoNegociationAnnuelleSectionComponent', () => {
  let component: NaoNegociationAnnuelleSectionComponent;
  let fixture: ComponentFixture<NaoNegociationAnnuelleSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/nao-negociation-annuelle-analysis';

  function conformeResponse(overrides: Partial<NaoNegociationAnnuelleResponse> = {}): NaoNegociationAnnuelleResponse {
    return {
      caseFileId: 'case-1',
      effectif: 80,
      delegueSyndicalPresent: true,
      blocRemunerationNegocie: true,
      blocEgaliteQvtNegocie: true,
      accordMethodePeriodicite: false,
      dateDerniereNegociation: '2026-02-01',
      periodiciteMois: 12,
      pvDesaccordEtabli: false,
      negociationAboutie: true,
      checklist: [
        { item: 'Bloc rémunération négocié', conforme: true, obligatoire: true, commentaire: 'OK' },
        { item: 'Bloc égalité pro / QVT négocié', conforme: true, obligatoire: true, commentaire: '' },
        { item: 'Périodicité respectée', conforme: true, obligatoire: true, commentaire: '' },
      ],
      itemsObligatoiresManquants: 0,
      statut: 'CONFORME',
      dateProchaineEcheance: '2027-02-01',
      joursAvantEcheance: 200,
      statutEcheance: 'A_JOUR',
      risqueEntrave: 'FAIBLE',
      country: 'FRANCE',
      baseJuridique: 'Art. L.2242-1 à L.2242-8 CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  function nonConformeResponse(): NaoNegociationAnnuelleResponse {
    return conformeResponse({
      blocRemunerationNegocie: false,
      checklist: [
        { item: 'Bloc rémunération négocié', conforme: false, obligatoire: true, commentaire: 'Non engagé' },
        { item: 'Bloc égalité pro / QVT négocié', conforme: true, obligatoire: true, commentaire: '' },
        { item: 'Périodicité respectée', conforme: true, obligatoire: true, commentaire: '' },
      ],
      itemsObligatoiresManquants: 1,
      statut: 'NON_CONFORME',
      statutEcheance: 'DEPASSEE',
      joursAvantEcheance: -30,
      risqueEntrave: 'ELEVE',
    });
  }

  function nonApplicableResponse(): NaoNegociationAnnuelleResponse {
    return conformeResponse({
      delegueSyndicalPresent: false,
      checklist: [],
      itemsObligatoiresManquants: 0,
      statut: 'NON_APPLICABLE',
      dateProchaineEcheance: null,
      joursAvantEcheance: null,
      statutEcheance: null,
      risqueEntrave: 'FAIBLE',
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [NaoNegociationAnnuelleSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NaoNegociationAnnuelleSectionComponent);
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
    expect(NaoNegociationAnnuelleSectionComponent.TOOL_LABEL).toContain('NAO');
    expect(NaoNegociationAnnuelleSectionComponent.TOOL_ICON).toBe('handshake');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(NaoNegociationAnnuelleSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 (nominal) with both fields', () => {
    expect(NaoNegociationAnnuelleSectionComponent.getPrefillCount({
      aiData: { pseNombreSalaries: 80, delegueSyndicalPresent: true },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 1 (partiel) with only effectif', () => {
    expect(NaoNegociationAnnuelleSectionComponent.getPrefillCount({
      aiData: { pseNombreSalaries: 80 },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(NaoNegociationAnnuelleSectionComponent.getPrefillCount({
      aiData: { pseNombreSalaries: 80, delegueSyndicalPresent: true },
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
    expect(component.effectif()).toBe(80);
    expect(component.delegueSyndicalPresent()).toBe(true);
    expect(component.periodiciteMois()).toBe(12);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validity ---

  it('formValid requires a positive effectif and periodicite in [1;48]', () => {
    expect(component.formValid()).toBe(false);
    component.effectif.set(0);
    expect(component.formValid()).toBe(false);
    component.effectif.set(80);
    component.periodiciteMois.set(12);
    expect(component.formValid()).toBe(true);
    component.periodiciteMois.set(60);
    expect(component.formValid()).toBe(false);
    component.periodiciteMois.set(null);
    expect(component.formValid()).toBe(false);
  });

  // --- coherence (F-IA-03) ---

  it('raises a coherence alert when DS présent sans bloc négocié', () => {
    component.delegueSyndicalPresent.set(true);
    component.blocRemunerationNegocie.set(false);
    component.blocEgaliteQvtNegocie.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('aucun bloc obligatoire'))).toBe(true);
  });

  it('raises a coherence alert when périodicité > 12 sans accord de méthode', () => {
    component.periodiciteMois.set(24);
    component.accordMethodePeriodicite.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('dépasse 12 mois'))).toBe(true);
  });

  it('raises a coherence alert when négociation non aboutie sans PV de désaccord', () => {
    component.delegueSyndicalPresent.set(true);
    component.negociationAboutie.set(false);
    component.pvDesaccordEtabli.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('PV de désaccord'))).toBe(true);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    component.effectif.set(80);
    component.delegueSyndicalPresent.set(true);
    component.blocRemunerationNegocie.set(true);
    component.blocEgaliteQvtNegocie.set(true);
    component.accordMethodePeriodicite.set(false);
    component.dateDerniereNegociation.set('2026-02-01');
    component.periodiciteMois.set(12);
    component.pvDesaccordEtabli.set(false);
    component.negociationAboutie.set(true);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      effectif: 80,
      delegueSyndicalPresent: true,
      blocRemunerationNegocie: true,
      blocEgaliteQvtNegocie: true,
      accordMethodePeriodicite: false,
      dateDerniereNegociation: '2026-02-01',
      periodiciteMois: 12,
      pvDesaccordEtabli: false,
      negociationAboutie: true,
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
    component.effectif.set(80);
    component.periodiciteMois.set(12);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : 3 statut states + checklist + échéance + risque ---

  it('CONFORME -> success statut chip + risque FAIBLE + checklist ✓ + échéance A_JOUR', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(conformeResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('conforme');
    expect(chip.className).toContain('is-chip--success');
    expect(el.querySelector('[data-testid="risque-chip"]')!.textContent).toContain('faible');
    const echeanceChip = el.querySelector('[data-testid="echeance-chip"]')!;
    expect(echeanceChip.textContent).toContain('À jour');
    expect(echeanceChip.className).toContain('is-chip--success');
    expect(el.querySelector('[data-testid="jours-echeance"]')!.textContent).toContain('200');
    expect(el.querySelector('[data-testid="date-echeance"]')!.textContent).toContain('2027-02-01');
    const items = el.querySelectorAll('[data-testid="checklist"] .is-critere');
    expect(items.length).toBe(3);
    expect(el.querySelector('[data-testid="items-manquants"]')!.textContent).toContain('0');
    expect(el.querySelectorAll('[data-testid="badge-obligatoire"]').length).toBe(3);
    expect(el.querySelector('[data-testid="risque-note"]')!.textContent).toContain('faible');
  });

  it('NON_CONFORME -> danger statut chip + risque ELEVE + échéance DEPASSEE + item bloc ✗', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(nonConformeResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('non conforme');
    expect(chip.className).toContain('is-chip--danger');
    expect(el.querySelector('[data-testid="risque-chip"]')!.textContent).toContain('élevé');
    expect(el.querySelector('[data-testid="risque-chip"]')!.className).toContain('is-chip--danger');
    const echeanceChip = el.querySelector('[data-testid="echeance-chip"]')!;
    expect(echeanceChip.textContent).toContain('dépassée');
    expect(echeanceChip.className).toContain('is-chip--danger');
    expect(el.querySelector('[data-testid="items-manquants"]')!.textContent).toContain('1');
    const koItems = el.querySelectorAll('[data-testid="checklist"] .is-critere--ko');
    expect(koItems.length).toBe(1);
    expect(el.querySelector('[data-testid="risque-note"]')!.textContent).toContain('entrave');
  });

  it('NON_APPLICABLE -> neutral statut chip + checklist masquée + pas d\'échéance', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(nonApplicableResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('non applicable');
    expect(chip.className).toContain('is-chip--neutral');
    expect(el.querySelector('[data-testid="checklist"]')).toBeNull();
    expect(el.querySelector('[data-testid="echeance-chip"]')).toBeNull();
    expect(el.querySelector('[data-testid="risque-chip"]')).toBeNull();
  });

  it('statut / échéance / risque chip classes map their states', () => {
    expect(component.statutChipClass('CONFORME')).toContain('success');
    expect(component.statutChipClass('NON_CONFORME')).toContain('danger');
    expect(component.statutChipClass('NON_APPLICABLE')).toContain('neutral');
    expect(component.echeanceChipClass('A_JOUR')).toContain('success');
    expect(component.echeanceChipClass('ECHEANCE_PROCHE')).toContain('warning');
    expect(component.echeanceChipClass('DEPASSEE')).toContain('danger');
    expect(component.risqueChipClass('FAIBLE')).toContain('success');
    expect(component.risqueChipClass('MODERE')).toContain('warning');
    expect(component.risqueChipClass('ELEVE')).toContain('danger');
  });

  // --- pré-fill IA ---

  it('pré-fills effectif and delegueSyndicalPresent from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      pseNombreSalaries: 120,
      delegueSyndicalPresent: true,
    };
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.effectif()).toBe(120);
    expect(component.delegueSyndicalPresent()).toBe(true);
    expect(component.provenanceEffectif()).toBe('IA');
    expect(component.provenanceDelegueSyndical()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData: TravailExtractedData = { pseNombreSalaries: 60 };
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.effectif()).toBe(60);
    expect(component.provenanceEffectif()).toBe('IA');
  });

  it('onEffectifChange clears provenance', () => {
    component.provenanceEffectif.set('IA');
    component.onEffectifChange(42);
    expect(component.effectif()).toBe(42);
    expect(component.provenanceEffectif()).toBeNull();
  });

  it('onDelegueSyndicalChange clears provenance', () => {
    component.provenanceDelegueSyndical.set('IA');
    component.onDelegueSyndicalChange(false);
    expect(component.delegueSyndicalPresent()).toBe(false);
    expect(component.provenanceDelegueSyndical()).toBeNull();
  });
});
