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
  styleUrl: './voyageur-home-page.component.scss',
  template: `
    <section class="traveller-login-shell">
      <div class="traveller-story">
        <img
          class="traveller-story-photo"
          src="images/car-famille-1024.webp"
          width="1024"
          height="576"
          alt=""
          fetchpriority="high"
        />
        <div class="traveller-story-copy">
          <p class="traveller-eyebrow">{{ language.t('studio.travellerEyebrow') }}</p>
          <h1>{{ language.t('studio.travellerTitle') }}</h1>
          <p>{{ language.t('studio.travellerLead') }}</p>
          <div class="traveller-promise">
            <span
              ><small>{{ language.t('transport.price') }}</small></span
            ><span
              ><svg class="ui-icon" aria-hidden="true"><use href="images/ui-icons.svg#car" /></svg>
              {{ language.t('transport.maximum') }}</span
            >
          </div>
        </div>
        <p class="traveller-story-note">
          {{ language.t('studio.travellerNote') }} <span aria-hidden="true">↗</span>
        </p>
      </div>
      <div class="traveller-login-panel">
        <div class="gestiontransport-login-card">
          <span class="gestiontransport-login-icon" aria-hidden="true"
            ><svg class="ui-icon" aria-hidden="true"><use href="images/ui-icons.svg#user" /></svg
          ></span>
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
                autocomplete="username"
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
                      showVoyageurPassword
                        ? 'images/ui-icons.svg#eye-off'
                        : 'images/ui-icons.svg#eye'
                    "
                  />
                </svg>
              </button>
            </mat-form-field>

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
                      {{
                        foundVoyageur()!.agenceNom || language.t('voyageur.home.allAgences')
                      }}</span
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
        </div>
      </div>
    </section>
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
      validators: [Validators.required, Validators.pattern(/^([VG]\d{4}|[SLF]\d{5})$/i)],
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
