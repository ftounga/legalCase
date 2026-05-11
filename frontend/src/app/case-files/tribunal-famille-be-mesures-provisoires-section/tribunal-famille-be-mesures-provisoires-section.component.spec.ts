import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TribunalFamilleBeMesuresProvisoiresSectionComponent } from './tribunal-famille-be-mesures-provisoires-section.component';

describe('TribunalFamilleBeMesuresProvisoiresSectionComponent', () => {
  let component: TribunalFamilleBeMesuresProvisoiresSectionComponent;
  let fixture: ComponentFixture<TribunalFamilleBeMesuresProvisoiresSectionComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TribunalFamilleBeMesuresProvisoiresSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(TribunalFamilleBeMesuresProvisoiresSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('rendu nominal : cadre juridique CJ 1280 et notes visibles', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('1280');
    expect(html.textContent).toContain('Backend opérationnel');
  });

  it('toggle collapsed cache puis réaffiche le contenu', () => {
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    fixture.detectChanges();
    expect(component.collapsed()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).querySelector('#tfmpbe-body')).toBeNull();
    component.toggleCollapse();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('#tfmpbe-body')).not.toBeNull();
  });

  it('forceExpanded=true au mount → collapsed reste false', () => {
    const f = TestBed.createComponent(TribunalFamilleBeMesuresProvisoiresSectionComponent);
    f.componentInstance.caseFileId = 'case-1';
    f.componentInstance.collapsed.set(true);
    f.componentRef.setInput('forceExpanded', true);
    f.componentInstance.ngOnInit();
    expect(f.componentInstance.collapsed()).toBe(false);
  });

  it('expose statics TOOL_LABEL, TOOL_ICON, PREFILL_COUNT_ALWAYS_ZERO, getPrefillCount', () => {
    expect(TribunalFamilleBeMesuresProvisoiresSectionComponent.TOOL_LABEL).toBe('MESURES PROVISOIRES TRIBUNAL FAMILLE (BE)');
    expect(TribunalFamilleBeMesuresProvisoiresSectionComponent.TOOL_ICON).toBe('gavel');
    expect(TribunalFamilleBeMesuresProvisoiresSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
    expect(TribunalFamilleBeMesuresProvisoiresSectionComponent.getPrefillCount()).toBe(0);
  });
});
