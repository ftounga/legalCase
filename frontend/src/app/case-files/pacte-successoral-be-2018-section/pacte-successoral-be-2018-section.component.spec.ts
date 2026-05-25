import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PacteSuccessoralBe2018SectionComponent } from './pacte-successoral-be-2018-section.component';

describe('PacteSuccessoralBe2018SectionComponent', () => {
  let component: PacteSuccessoralBe2018SectionComponent;
  let fixture: ComponentFixture<PacteSuccessoralBe2018SectionComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PacteSuccessoralBe2018SectionComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(PacteSuccessoralBe2018SectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('rendu nominal : loi 31/07/2017 et notes visibles', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('31 juillet 2017');
    expect(html.textContent).toContain('Backend opérationnel');
  });

  it('toggle collapsed cache puis réaffiche le contenu', () => {
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    fixture.detectChanges();
    expect(component.collapsed()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).querySelector('#psbe-body')).toBeNull();
    component.toggleCollapse();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('#psbe-body')).not.toBeNull();
  });

  it('forceExpanded=true au mount → collapsed reste false', () => {
    const f = TestBed.createComponent(PacteSuccessoralBe2018SectionComponent);
    f.componentInstance.caseFileId = 'case-1';
    f.componentInstance.collapsed.set(true);
    f.componentRef.setInput('forceExpanded', true);
    f.componentInstance.ngOnInit();
    expect(f.componentInstance.collapsed()).toBe(false);
  });

  it('expose statics TOOL_LABEL, TOOL_ICON, PREFILL_COUNT_ALWAYS_ZERO, getPrefillCount', () => {
    expect(PacteSuccessoralBe2018SectionComponent.TOOL_LABEL).toBe('PACTE SUCCESSORAL 2018 (BE)');
    expect(PacteSuccessoralBe2018SectionComponent.TOOL_ICON).toBe('description');
    expect(PacteSuccessoralBe2018SectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
    expect(PacteSuccessoralBe2018SectionComponent.getPrefillCount()).toBe(0);
  });
});
