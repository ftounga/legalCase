import {
  animate,
  animation,
  query,
  stagger,
  style,
  transition,
  trigger,
  useAnimation,
} from '@angular/animations';

// ─── Keyframes réutilisables ──────────────────────────────────────────────────

export const fadeInUpAnimation = animation([
  style({ opacity: 0, transform: 'translateY({{offset}}px)' }),
  animate('{{duration}} {{easing}}', style({ opacity: 1, transform: 'translateY(0)' })),
], { params: { duration: '250ms', easing: 'ease-out', offset: 8 } });

export const slideInRightAnimation = animation([
  style({ opacity: 0, transform: 'translateX(24px)' }),
  animate('200ms ease-out', style({ opacity: 1, transform: 'translateX(0)' })),
]);

// ─── Trigger : fade-in simple (page, card, section) ──────────────────────────

export const fadeInUp = trigger('fadeInUp', [
  transition(':enter', [
    useAnimation(fadeInUpAnimation),
  ]),
]);

// ─── Trigger : stagger liste (lignes de tableau, items) ──────────────────────

export const listStagger = trigger('listStagger', [
  transition('* => *', [
    query(':enter', [
      style({ opacity: 0, transform: 'translateY(6px)' }),
      stagger('35ms', [
        animate('220ms ease-out', style({ opacity: 1, transform: 'translateY(0)' })),
      ]),
    ], { optional: true }),
  ]),
]);

// ─── Trigger : slide-in depuis la droite (fichier uploadé) ───────────────────

export const slideInRight = trigger('slideInRight', [
  transition(':enter', [
    useAnimation(slideInRightAnimation),
  ]),
]);

// ─── Trigger : router page transition ────────────────────────────────────────

export const routeAnimations = trigger('routeAnimations', [
  transition('* <=> *', [
    query(':enter', [
      style({ opacity: 0, transform: 'translateY(6px)' }),
      animate('200ms ease-out', style({ opacity: 1, transform: 'translateY(0)' })),
    ], { optional: true }),
  ]),
]);
