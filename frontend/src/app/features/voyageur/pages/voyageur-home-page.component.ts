import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { VoyageursApiService } from '../../../core/api/voyageurs-api.service';
import { VoyageurSessionService } from '../../../core/auth/voyageur-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { VoyageurResponse } from '../../../shared/models/voyageur.model';

@Component({
  selector: 'app-voyageur-home-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatChipsModule,
  ],
  template: `
    <div class="gestiontransport-hero">
      <div class="gestiontransport-hero-content">
        <div class="gestiontransport-ball"></div>
        <h1 class="gestiontransport-hero-title">{{ language.t('voyageur.home.title') }}</h1>
        <p class="gestiontransport-hero-sub">{{ language.t('voyageur.home.subtitle') }}</p>
      </div>
    </div>

    <section class="page-shell max-w-4xl">
      <div class="gestiontransport-login-card">
        <div class="gestiontransport-login-icon"></div>
        <h2 class="gestiontransport-login-title">
          {{ language.t('voyageur.home.identification') }}
        </h2>
        <p class="gestiontransport-login-sub">{{ language.t('voyageur.home.loginHint') }}</p>

        <form [formGroup]="form" class="gestiontransport-form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>{{ language.t('voyageur.home.matricule') }}</mat-label>
            <input
              matInput
              formControlName="matricule"
              [placeholder]="language.t('voyageur.home.matriculePlaceholder')"
              style="text-transform:uppercase; font-weight:600; letter-spacing:0.08em;"
            />
          </mat-form-field>

          <mat-form-field appearance="outline" class="w-full">
            <mat-label>{{ language.t('voyageur.home.password') }}</mat-label>
            <input
              matInput
              [type]="showVoyageurPassword ? 'text' : 'password'"
              formControlName="password"
              autocomplete="current-password"
            />
            <button
              mat-icon-button
              matSuffix
              type="button"
              class="password-toggle-button"
              [attr.aria-label]="
                showVoyageurPassword
                  ? language.t('voyageur.home.hidePassword')
                  : language.t('voyageur.home.showPassword')
              "
              (click)="toggleVoyageurPassword()"
            >
              <svg class="ui-icon" aria-hidden="true" width="24" height="24">
                <use
                  [attr.href]="
                    showVoyageurPassword ? 'images/ui-icons.svg#eye-off' : 'images/ui-icons.svg#eye'
                  "
                />
              </svg>
            </button>
          </mat-form-field>

          <div class="gestiontransport-types-grid">
            <div class="gestiontransport-type-badge gestiontransport-type-global">
              <span class="gestiontransport-type-icon" aria-hidden="true"
                ><svg class="ui-icon"><use href="images/ui-icons.svg#user" /></svg
              ></span>
              <div>
                <div class="gestiontransport-type-name">
                  {{ language.t('voyageur.home.global') }}
                </div>
                <div class="gestiontransport-type-hint">
                  {{ language.t('voyageur.home.globalHint') }}
                </div>
              </div>
            </div>
            <div class="gestiontransport-type-badge gestiontransport-type-agence">
              <span class="gestiontransport-type-icon" aria-hidden="true"
                ><svg class="ui-icon"><use href="images/ui-icons.svg#agency" /></svg
              ></span>
              <div>
                <div class="gestiontransport-type-name">
                  {{ language.t('voyageur.home.agence') }}
                </div>
                <div class="gestiontransport-type-hint">
                  {{ language.t('voyageur.home.agenceHint') }}
                </div>
              </div>
            </div>
            <div class="gestiontransport-type-badge gestiontransport-type-libre">
              <span class="gestiontransport-type-icon" aria-hidden="true"
                ><svg class="ui-icon"><use href="images/ui-icons.svg#user" /></svg
              ></span>
              <div>
                <div class="gestiontransport-type-name">
                  {{ language.t('voyageur.home.livre') }}
                </div>
                <div class="gestiontransport-type-hint">
                  {{ language.t('voyageur.home.livreHint') }}
                </div>
              </div>
            </div>
          </div>

          @if (errorMessage()) {
            <div role="alert" class="gestiontransport-error">{{ errorMessage() }}</div>
          }

          @if (foundVoyageur()) {
            <div class="gestiontransport-found-card">
              <div class="gestiontransport-found-icon"></div>
              <div>
                <div class="gestiontransport-found-name">
                  {{ foundVoyageur()!.prenom }} {{ foundVoyageur()!.nom }}
                </div>
                <div class="gestiontransport-found-details">
                  <span
                    class="gestiontransport-badge-type"
                    [class]="
                      'gestiontransport-badge-' + foundVoyageur()!.typeVoyageur.toLowerCase()
                    "
                  >
                    {{ foundVoyageur()!.typeVoyageur }}
                  </span>
                  <span
                    >·
                    {{ foundVoyageur()!.agenceNom || language.t('voyageur.home.allAgences') }}</span
                  >
                </div>
              </div>
            </div>
          }

          <div class="gestiontransport-form-actions">
            <button
              class="gestiontransport-btn-primary"
              type="submit"
              [disabled]="form.invalid || loading()"
            >
              @if (loading()) {
                <mat-spinner
                  [attr.aria-label]="language.t('transport.searching')"
                  diameter="20"
                  style="display:inline-block; margin-right:8px;"
                ></mat-spinner>
              }
              {{ language.t('voyageur.home.cta') }}
            </button>
          </div>
        </form>
      </div>
    </section>

    <style>
      .gestiontransport-hero {
        background: linear-gradient(120deg, var(--ui-navy), var(--ui-primary-strong));
        padding: 3rem 1.5rem 4rem;
        text-align: center;
        position: relative;
        overflow: hidden;
      }
      .gestiontransport-hero::before {
        content: '';
        position: absolute;
        inset: 0;
        background-image:
          repeating-linear-gradient(
            0deg,
            transparent,
            transparent 40px,
            color-mix(in srgb, var(--ui-surface) 4%, transparent) 40px,
            color-mix(in srgb, var(--ui-surface) 4%, transparent) 42px
          ),
          repeating-linear-gradient(
            90deg,
            transparent,
            transparent 40px,
            color-mix(in srgb, var(--ui-surface) 4%, transparent) 40px,
            color-mix(in srgb, var(--ui-surface) 4%, transparent) 42px
          );
      }
      .gestiontransport-hero-content {
        position: relative;
        z-index: 1;
      }
      .gestiontransport-ball {
        font-size: 4rem;
        margin-bottom: 0.5rem;
        animation: spin 8s linear infinite;
      }
      @keyframes spin {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(360deg);
        }
      }
      .gestiontransport-hero-title {
        font-size: 2.2rem;
        font-weight: 800;
        color: var(--ui-surface);
        margin: 0 0 0.5rem;
        text-shadow: 0 2px 8px color-mix(in srgb, var(--ui-navy) 30%, transparent);
      }
      .gestiontransport-hero-sub {
        color: color-mix(in srgb, var(--ui-surface) 88%, transparent);
        font-size: 1.05rem;
        margin: 0;
      }

      .gestiontransport-login-card {
        background: var(--ui-surface);
        border-radius: 1.5rem;
        box-shadow: var(--ui-shadow);
        padding: 2.5rem 2rem;
        text-align: center;
        border-top: 5px solid var(--ui-teal-strong);
        margin-top: -2.5rem;
      }
      .gestiontransport-login-icon {
        font-size: 2.5rem;
        margin-bottom: 0.5rem;
      }
      .gestiontransport-login-title {
        font-size: 1.6rem;
        font-weight: 700;
        color: var(--ui-navy);
        margin: 0 0 0.3rem;
      }
      .gestiontransport-login-sub {
        color: var(--ui-text-secondary);
        margin: 0 0 1.5rem;
      }
      .gestiontransport-form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
        text-align: left;
      }

      .gestiontransport-types-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
        gap: 0.75rem;
      }
      .gestiontransport-type-badge {
        display: flex;
        align-items: center;
        gap: 0.6rem;
        padding: 0.75rem 1rem;
        border-radius: 0.75rem;
        border: 2px solid transparent;
        transition: transform 0.15s;
      }
      .gestiontransport-type-badge:hover {
        transform: translateY(-2px);
      }
      .gestiontransport-type-global {
        background: var(--ui-surface-subtle);
        border-color: var(--ui-primary-strong);
      }
      .gestiontransport-type-agence {
        background: var(--ui-surface-subtle);
        border-color: var(--ui-teal-strong);
      }
      .gestiontransport-type-libre {
        background: var(--ui-surface-subtle);
        border-color: var(--ui-warning);
      }
      .gestiontransport-type-icon {
        font-size: 1.5rem;
      }
      .gestiontransport-type-name {
        font-weight: 700;
        font-size: 0.85rem;
        color: var(--ui-navy);
      }
      .gestiontransport-type-hint {
        font-size: 0.72rem;
        color: var(--ui-text-secondary);
      }

      .gestiontransport-error {
        background: var(--ui-surface);
        border: 1px solid var(--ui-danger);
        border-radius: 0.75rem;
        padding: 0.75rem 1rem;
        color: var(--ui-danger);
        font-size: 0.9rem;
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }
      .gestiontransport-found-card {
        background: linear-gradient(120deg, var(--ui-primary-soft), var(--ui-surface));
        border: 2px solid var(--ui-teal-strong);
        border-radius: 1rem;
        padding: 1rem 1.25rem;
        display: flex;
        align-items: center;
        gap: 1rem;
      }
      .gestiontransport-found-icon {
        font-size: 2rem;
      }
      .gestiontransport-found-name {
        font-weight: 700;
        font-size: 1.1rem;
        color: var(--ui-navy);
      }
      .gestiontransport-found-details {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 0.85rem;
        color: var(--ui-text-secondary);
        margin-top: 0.25rem;
      }

      .gestiontransport-badge-type {
        display: inline-block;
        padding: 0.15rem 0.6rem;
        border-radius: 9999px;
        font-size: 0.72rem;
        font-weight: 700;
        letter-spacing: 0.05em;
      }
      .gestiontransport-badge-global {
        background: var(--ui-primary-soft);
        color: var(--ui-primary-strong);
      }
      .gestiontransport-badge-agence {
        background: var(--ui-teal-soft);
        color: var(--ui-teal-strong);
      }
      .gestiontransport-badge-libre {
        background: var(--ui-warning-soft);
        color: var(--ui-warning);
      }

      .gestiontransport-form-actions {
        display: flex;
        justify-content: center;
        padding-top: 0.5rem;
      }
      .gestiontransport-btn-primary {
        background: var(--ui-primary);
        color: var(--ui-surface);
        border: none;
        border-radius: 0.75rem;
        padding: 0.85rem 2.5rem;
        font-size: 1rem;
        font-weight: 700;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 0.5rem;
        box-shadow: var(--ui-shadow);
        transition:
          transform 0.15s,
          box-shadow 0.15s;
      }
      .gestiontransport-btn-primary:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: var(--ui-shadow);
      }
      .gestiontransport-btn-primary:disabled {
        background: var(--ui-disabled);
        color: var(--ui-text-secondary);
        cursor: not-allowed;
      }

      .password-toggle-button {
        width: 40px;
        height: 40px;
        line-height: 40px;
        color: var(--ui-text-secondary);
      }
    </style>
  `,
})
export class VoyageurHomePage {
  private readonly voyageursApi = inject(VoyageursApiService);
  private readonly voyageurSession = inject(VoyageurSessionService);
  private readonly router = inject(Router);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly foundVoyageur = signal<VoyageurResponse | null>(null);
  showVoyageurPassword = false;

  readonly form = new FormGroup({
    matricule: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^(G\d{4}|S\d{5}|L\d{5})$/i)],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  constructor() {
    if (this.voyageurSession.isAuthenticated()) {
      this.router.navigateByUrl('/voyageur/profile');
    }
  }

  submit(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.foundVoyageur.set(null);

    const matricule = this.form.controls.matricule.getRawValue().trim().toUpperCase();
    const password = this.form.controls.password.getRawValue();

    this.voyageurSession.login(matricule, password).subscribe({
      next: (voyageur) => {
        this.foundVoyageur.set(voyageur);
        this.loading.set(false);
        setTimeout(() => this.router.navigateByUrl('/voyageur/profile'), 600);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set(this.language.t('voyageur.home.invalid'));
      },
    });
  }

  toggleVoyageurPassword(): void {
    this.showVoyageurPassword = !this.showVoyageurPassword;
  }
}
