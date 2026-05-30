import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { JournalisteStatutSectionComponent } from './journaliste-statut-section.component';
import { JournalisteStatutResponse } from '../../core/models/journaliste-statut.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('JournalisteStatutSectionComponent', () => {
  let component: JournalisteStatutSectionComponent;
  let fixture: ComponentFixture<JournalisteStatutSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/journaliste-statut-analysis';

  function cessionResponse(overrides: Partial<JournalisteStatutResponse> = {}): JournalisteStatutResponse {
    return {
      caseFileId: 'case-1',
      dateEntree: '2018-01-01',
      dateRupture: '2023-06-15',
      typeRupture: 'CLAUSE_CESSION',
      salaireMensuelMoyen: 3000,
      carteIdentiteProfessionnelle: true,
      cessionTitreConstatee: true,
      changementOrientationConstate: false,
      statutJournaliste: 'CONFIRME',
      clauseValide: 'VALIDE',
      motif: 'Cession du titre constatée (art. L.7112-5 1°)',
      indemniteCongediement: 18000,
      ancienneteAnnees: 6,
      commissionArbitraleRequise: false,
      verdictGlobal: 'RUPTURE_ASSIMILEE_LICENCIEMENT',
      country: 'FRANCE',
      baseJuridique: 'Art. L.7111-1 à L.7113-12 CT ; L.7112-3 ; L.7112-4 ; L.7112-5 (à vérifier par avocat)',
      ...overrides,
    };
  }

  function commissionResponse(): JournalisteStatutResponse {
    return cessionResponse({
      ancienneteAnnees: 18,
      indemniteCongediement: 45000,
      commissionArbitraleRequise: true,
      verdictGlobal: 'COMMISSION_ARBITRALE',
    });
  }

  function nonValideResponse(): JournalisteStatutResponse {
    return cessionResponse({
      typeRupture: 'CLAUSE_CONSCIENCE',
      cessionTitreConstatee: false,
      changementOrientationConstate: false,
      statutJournaliste: 'CONFIRME',
      clauseValide: 'NON_VALIDE',
      motif: "Changement notable d'orientation non constaté (art. L.7112-5 2°/3°)",
      indemniteCongediement: 0,
      verdictGlobal: 'INDEMNITE_NON_DUE',
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateEntree.set('2018-01-01');
    component.dateRupture.set('2023-06-15');
    component.typeRupture.set('CLAUSE_CESSION');
    component.salaireMensuelMoyen.set(3000);
    component.carteIdentiteProfessionnelle.set(true);
    component.cessionTitreConstatee.set(true);
    component.changementOrientationConstate.set(false);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [JournalisteStatutSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(JournalisteStatutSectionComponent);
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
    expect(JournalisteStatutSectionComponent.TOOL_LABEL).toContain('STATUT JOURNALISTE');
    expect(JournalisteStatutSectionComponent.TOOL_ICON).toBe('newspaper');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(JournalisteStatutSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 3 (nominal) with the three fields', () => {
    expect(JournalisteStatutSectionComponent.getPrefillCount({
      aiData: { dateEntree: '2018-01-01', dateLicenciement: '2023-06-15', journalisteCartePresse: true },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(JournalisteStatutSectionComponent.getPrefillCount({
      aiData: { dateEntree: '2018-01-01', dateLicenciement: '2023-06-15', journalisteCartePresse: true },
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
    httpMock.expectOne(BASE_URL).flush(cessionResponse());
    expect(component.result()!.verdictGlobal).toBe('RUPTURE_ASSIMILEE_LICENCIEMENT');
    expect(component.showForm()).toBe(false);
    expect(component.dateEntree()).toBe('2018-01-01');
    expect(component.typeRupture()).toBe('CLAUSE_CESSION');
    expect(component.salaireMensuelMoyen()).toBe(3000);
    expect(component.cessionTitreConstatee()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validation ---

  it('formValid requires both dates + positive salaire', () => {
    component.dateEntree.set(null);
    component.dateRupture.set('2023-06-15');
    component.salaireMensuelMoyen.set(3000);
    expect(component.formValid()).toBe(false);
    component.dateEntree.set('2018-01-01');
    expect(component.formValid()).toBe(true);
    component.salaireMensuelMoyen.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid rejects dateRupture before dateEntree + raises coherence alert', () => {
    component.dateEntree.set('2023-06-15');
    component.dateRupture.set('2018-01-01');
    component.salaireMensuelMoyen.set(3000);
    expect(component.formValid()).toBe(false);
    expect(component.coherenceAlerts().length).toBeGreaterThan(0);
  });

  it('raises a coherence alert when CLAUSE_CESSION invoked without cession constatée', () => {
    component.typeRupture.set('CLAUSE_CESSION');
    component.cessionTitreConstatee.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('cession'))).toBe(true);
  });

  it('raises a coherence alert when CLAUSE_CONSCIENCE invoked without changement constaté', () => {
    component.typeRupture.set('CLAUSE_CONSCIENCE');
    component.changementOrientationConstate.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('orientation'))).toBe(true);
  });

  // --- handlers ---

  it('onCarteChange updates value and clears provenance', () => {
    component.provenanceCarte.set('IA');
    component.onCarteChange(false);
    expect(component.carteIdentiteProfessionnelle()).toBe(false);
    expect(component.provenanceCarte()).toBeNull();
  });

  it('onCessionChange and onChangementOrientationChange update signals', () => {
    component.onCessionChange(true);
    expect(component.cessionTitreConstatee()).toBe(true);
    component.onChangementOrientationChange(true);
    expect(component.changementOrientationConstate()).toBe(true);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateEntree: '2018-01-01',
      dateRupture: '2023-06-15',
      typeRupture: 'CLAUSE_CESSION',
      salaireMensuelMoyen: 3000,
      carteIdentiteProfessionnelle: true,
      cessionTitreConstatee: true,
      changementOrientationConstate: false,
    });
    req.flush(cessionResponse());
    expect(component.result()!.verdictGlobal).toBe('RUPTURE_ASSIMILEE_LICENCIEMENT');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() error -> snack error, stays in form', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : verdict + clause + indemnité ---

  it('renders verdict chip, statut chip, clause VALIDE chip and indemnité montant', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(cessionResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="verdict-chip"]')!.textContent).toContain('assimilée');
    expect(el.querySelector('[data-testid="statut-chip"]')!.textContent).toContain('confirmé');
    const clause = el.querySelector('[data-testid="clause-chip"]')!;
    expect(clause.textContent).toContain('valide');
    expect(clause.className).toContain('js-chip--success');
    expect(el.querySelector('[data-testid="indemnite-congediement"]')!.textContent).toContain('18000');
    // pas d'encart commission arbitrale quand ancienneté <= 15 ans
    expect(el.querySelector('[data-testid="commission-arbitrale"]')).toBeNull();
  });

  it('renders the commission arbitrale encart when ancienneté > 15 ans', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(commissionResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const encart = el.querySelector('[data-testid="commission-arbitrale"]')!;
    expect(encart).not.toBeNull();
    expect(encart.textContent).toContain('commission arbitrale');
    expect(encart.textContent).toContain('15 mois');
    expect(el.querySelector('[data-testid="verdict-chip"]')!.textContent).toContain('Commission arbitrale');
  });

  it('renders NON_VALIDE clause chip (danger) with motif when clause de conscience sans constat', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(nonValideResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="clause-chip"]')!;
    expect(chip.textContent).toContain('non valide');
    expect(chip.className).toContain('js-chip--danger');
    expect(el.querySelector('[data-testid="clause-motif"]')!.textContent).toContain('orientation');
  });

  it('clauseChipClass maps VALIDE->success, NON_VALIDE->danger, SANS_OBJET->neutral', () => {
    expect(component.clauseChipClass('VALIDE')).toContain('success');
    expect(component.clauseChipClass('NON_VALIDE')).toContain('danger');
    expect(component.clauseChipClass('SANS_OBJET')).toContain('neutral');
  });

  it('chipClass maps verdict global states', () => {
    expect(component.chipClass('RUPTURE_ASSIMILEE_LICENCIEMENT')).toContain('navy');
    expect(component.chipClass('INDEMNITE_DUE')).toContain('navy');
    expect(component.chipClass('COMMISSION_ARBITRALE')).toContain('warning');
    expect(component.chipClass('INDEMNITE_NON_DUE')).toContain('danger');
  });

  it('statutChipClass maps A_QUALIFIER to warning', () => {
    expect(component.statutChipClass('CONFIRME')).toContain('success');
    expect(component.statutChipClass('A_QUALIFIER')).toContain('warning');
  });

  // --- pré-fill IA ---

  it('pré-fills dateEntree, dateRupture and carte from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      dateEntree: '2017-03-01',
      dateLicenciement: '2024-02-10',
      journalisteCartePresse: true,
    };
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.dateEntree()).toBe('2017-03-01');
    expect(component.dateRupture()).toBe('2024-02-10');
    expect(component.carteIdentiteProfessionnelle()).toBe(true);
    expect(component.provenanceDateEntree()).toBe('IA');
    expect(component.provenanceDateRupture()).toBe('IA');
    expect(component.provenanceCarte()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData: TravailExtractedData = { dateEntree: '2019-05-05' };
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.dateEntree()).toBe('2019-05-05');
    expect(component.provenanceDateEntree()).toBe('IA');
  });

  it('onDateEntreeChange clears provenance', () => {
    component.provenanceDateEntree.set('IA');
    component.onDateEntreeChange('2022-01-01');
    expect(component.dateEntree()).toBe('2022-01-01');
    expect(component.provenanceDateEntree()).toBeNull();
  });
});
