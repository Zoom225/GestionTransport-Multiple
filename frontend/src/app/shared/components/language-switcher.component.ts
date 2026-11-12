import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { LanguageService } from '../../core/i18n/language.service';
import { Locale } from '../../core/i18n/translations';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="lang-shell">
      <button
        type="button"
        class="lang-trigger"
        [attr.aria-expanded]="open()"
        [attr.aria-label]="switcherLabel()"
        (click)="toggle()"
      >
        <span class="flag-swatch" [ngClass]="'flag-' + currentLocale()"></span>
        <span class="lang-code">{{ currentOption().code }}</span>
        <span class="chevron" aria-hidden="true"></span>
      </button>

      @if (open()) {
        <div class="lang-menu" role="group" [attr.aria-label]="switcherLabel()">
          @for (option of options; track option.locale) {
            <button
              type="button"
              class="lang-option"
              [class.is-active]="currentLocale() === option.locale"
              [attr.aria-pressed]="currentLocale() === option.locale"
              [attr.aria-label]="languageLabel(option.locale)"
              (click)="select(option.locale)"
            >
              <span class="flag-swatch" [ngClass]="'flag-' + option.locale"></span>
              <span class="lang-name">{{ languageLabel(option.locale) }}</span>
            </button>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .lang-shell {
      position: relative;
      display: inline-block;
    }

    .lang-trigger {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      min-height: 44px;
      padding: 0.35rem 0.7rem 0.35rem 0.6rem;
      border: 1px solid color-mix(in srgb, var(--ui-navy) 8%, transparent);
      border-radius: 8px;
      background: color-mix(in srgb, var(--ui-surface) 92%, transparent);
      color: var(--ui-navy);
      cursor: pointer;
      box-shadow: var(--ui-shadow);
      transition: transform 0.15s, background 0.15s, border-color 0.15s, box-shadow 0.15s;
    }

    .lang-trigger:hover {
      transform: translateY(-1px);
      background: color-mix(in srgb, var(--ui-surface) 96%, transparent);
      border-color: color-mix(in srgb, var(--ui-navy) 12%, transparent);
      box-shadow: var(--ui-shadow);
    }

    .lang-trigger:focus-visible,
    .lang-option {
      outline: 2px solid transparent;
    }

    .lang-trigger:focus-visible {
      box-shadow: var(--ui-shadow);
    }

    .lang-menu {
      position: absolute;
      right: 0;
      top: calc(100% + 0.45rem);
      display: grid;
      gap: 0.35rem;
      min-width: 190px;
      padding: 0.45rem;
      border: 1px solid color-mix(in srgb, var(--ui-navy) 8%, transparent);
      border-radius: 8px;
      background: color-mix(in srgb, var(--ui-surface) 98%, transparent);
      box-shadow: var(--ui-shadow);
      z-index: 50;
    }

    .lang-option {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      width: 100%;
      min-height: 2.3rem;
      padding: 0.45rem 0.6rem;
      border: 1px solid transparent;
      border-radius: 8px;
      background: transparent;
      color: var(--ui-text-secondary);
      font-size: 0.84rem;
      font-weight: 800;
      cursor: pointer;
      transition: transform 0.15s, background 0.15s, border-color 0.15s, box-shadow 0.15s;
    }

    .lang-option:hover {
      background: var(--ui-surface);
      border-color: color-mix(in srgb, var(--ui-navy) 6%, transparent);
    }

    .lang-option.is-active {
      background: var(--ui-surface-subtle);
      border-color: color-mix(in srgb, var(--ui-primary-strong) 12%, transparent);
      color: var(--ui-navy);
    }

    .flag-swatch {
      width: 1.55rem;
      height: 1.05rem;
      border-radius: 4px;
      border: 1px solid color-mix(in srgb, var(--ui-navy) 14%, transparent);
      flex-shrink: 0;
    }

    .flag-fr {
      background: linear-gradient(90deg, #0055a4 0 33.333%, #ffffff 33.333% 66.666%, #ef4135 66.666% 100%);
    }

    .flag-en {
      background:
        linear-gradient(180deg, transparent 41%, #ffffff 41% 59%, transparent 59%),
        linear-gradient(90deg, transparent 41%, #ffffff 41% 59%, transparent 59%),
        linear-gradient(135deg, transparent 42%, #c8102e 42% 47%, transparent 47% 53%, #c8102e 53% 58%, transparent 58%),
        linear-gradient(45deg, transparent 42%, #c8102e 42% 47%, transparent 47% 53%, #c8102e 53% 58%, transparent 58%),
        linear-gradient(90deg, #012169 0 100%);
    }

    .lang-code {
      letter-spacing: 0;
      font-size: 0.78rem;
      font-weight: 900;
      color: var(--ui-navy);
    }

    .chevron {
      width: 0.45rem;
      height: 0.45rem;
      border-right: 2px solid currentColor;
      border-bottom: 2px solid currentColor;
      transform: rotate(45deg);
      margin-left: 0.1rem;
      opacity: 0.8;
    }

    .lang-name {
      white-space: nowrap;
    }

    @media (max-width: 760px) {
      .lang-shell {
        width: 100%;
      }

      .lang-trigger {
        width: 100%;
        justify-content: space-between;
      }

      .lang-menu {
        width: 100%;
        min-width: 0;
      }

      .lang-name {
        display: none;
      }
    }
  `]
})
export class LanguageSwitcherComponent {
  private readonly language = inject(LanguageService);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  readonly open = signal(false);

  readonly options: Array<{ locale: Locale; code: string }> = [
    { locale: 'fr', code: 'FR' },
    { locale: 'en', code: 'EN' }
  ];

  readonly currentLocale = this.language.locale;
  readonly switcherLabel = computed(() => this.language.t('switcher.label'));
  readonly currentOption = computed(() => this.options.find((option) => option.locale === this.currentLocale()) ?? this.options[0]);

  languageLabel(locale: Locale): string {
    if (locale === 'fr') {
      return this.language.t('switcher.fr');
    }
    return this.language.t('switcher.en');
  }

  toggle(): void {
    this.open.update((value) => !value);
  }

  select(locale: Locale): void {
    this.language.setLocale(locale);
    this.open.set(false);
  }

  @HostListener('document:click', ['$event'])
  closeOnOutsideClick(event: MouseEvent): void {
    const target = event.target as Node | null;
    if (!target || this.elementRef.nativeElement.contains(target)) {
      return;
    }

    this.open.set(false);
  }

  @HostListener('document:keydown.escape')
  closeOnEscape(): void {
    this.open.set(false);
  }
}
