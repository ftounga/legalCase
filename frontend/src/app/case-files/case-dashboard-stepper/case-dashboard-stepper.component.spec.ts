import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { CaseDashboardStepperComponent, DashboardStep } from './case-dashboard-stepper.component';

describe('CaseDashboardStepperComponent', () => {
  let component: CaseDashboardStepperComponent;
  let fixture: ComponentFixture<CaseDashboardStepperComponent>;
  let router: jest.Mocked<Router>;

  const makeStep = (overrides: Partial<DashboardStep> = {}): DashboardStep => ({
    id: 'documents',
    label: 'Documents',
    status: 'pending',
    detail: null,
    anchorId: 'section-documents',
    ...overrides,
  });

  beforeEach(async () => {
    router = { navigate: jest.fn() } as unknown as jest.Mocked<Router>;

    await TestBed.configureTestingModule({
      imports: [CaseDashboardStepperComponent],
      providers: [{ provide: Router, useValue: router }],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardStepperComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-123';
    component.steps = [];
    fixture.detectChanges();
  });

  // SF101-U-01
  it('should render one element per step', () => {
    component.steps = [makeStep(), makeStep({ id: 'analyse', label: 'Analyse IA' })];
    fixture.detectChanges();
    const stepEls = fixture.nativeElement.querySelectorAll('.step');
    expect(stepEls.length).toBe(2);
  });

  // SF101-U-02
  it('should display check_circle icon for done step', () => {
    expect(component.stepIcon('done')).toBe('check_circle');
  });

  // SF101-U-03
  it('should display radio_button_unchecked icon for pending step', () => {
    expect(component.stepIcon('pending')).toBe('radio_button_unchecked');
  });

  // SF101-U-04
  it('should display pending icon for in_progress step', () => {
    expect(component.stepIcon('in_progress')).toBe('pending');
  });

  // SF101-U-05
  it('should scroll to anchorId when step is pending with anchorId', () => {
    const mockEl = { scrollIntoView: jest.fn() };
    jest.spyOn(document, 'getElementById').mockReturnValue(mockEl as unknown as HTMLElement);

    component.onStepClick(makeStep({ status: 'pending', anchorId: 'section-documents' }));

    expect(document.getElementById).toHaveBeenCalledWith('section-documents');
    expect(mockEl.scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' });
    expect(router.navigate).not.toHaveBeenCalled();
  });

  // SF101-U-06
  it('should navigate to synthesis when step is pending without anchorId', () => {
    component.onStepClick(makeStep({ status: 'pending', anchorId: null }));

    expect(router.navigate).toHaveBeenCalledWith(['/case-files', 'case-123', 'synthesis']);
  });

  // SF101-U-07
  it('should do nothing when clicking a done step', () => {
    const mockEl = { scrollIntoView: jest.fn() };
    jest.spyOn(document, 'getElementById').mockReturnValue(mockEl as unknown as HTMLElement);

    component.onStepClick(makeStep({ status: 'done', anchorId: 'section-documents' }));

    expect(mockEl.scrollIntoView).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  // SF101-U-08
  it('should render N-1 connectors for N steps', () => {
    component.steps = [makeStep(), makeStep({ id: 'analyse' }), makeStep({ id: 'questions' })];
    fixture.detectChanges();
    const connectors = fixture.nativeElement.querySelectorAll('.step-connector');
    expect(connectors.length).toBe(2);
  });

  // SF101-U-09
  it('should apply step--done class to a done step', () => {
    component.steps = [makeStep({ status: 'done' })];
    fixture.detectChanges();
    const stepEl = fixture.nativeElement.querySelector('.step');
    expect(stepEl.classList).toContain('step--done');
  });

  // SF101-U-10
  it('should display detail text when provided', () => {
    component.steps = [makeStep({ detail: '3 documents' })];
    fixture.detectChanges();
    const detail = fixture.nativeElement.querySelector('.step-detail');
    expect(detail?.textContent?.trim()).toBe('3 documents');
  });

  // SF101-U-11
  it('should not render detail element when detail is null', () => {
    component.steps = [makeStep({ detail: null })];
    fixture.detectChanges();
    const detail = fixture.nativeElement.querySelector('.step-detail');
    expect(detail).toBeNull();
  });
});
