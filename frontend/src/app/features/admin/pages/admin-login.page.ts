import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';

@Component({
  selector: 'app-admin-login-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  styleUrl: './admin-login.page.scss',
  template: `
    <div class="login-shell">
      <div class="login-hero">
        <div class="login-hero-content">
          <p class="login-eyebrow">{{ language.t('studio.adminEyebrow') }}</p>
          <h1 class="login-hero-title">{{ language.t('studio.adminLoginTitle') }}</h1>
          <p class="login-hero-sub">{{ language.t('studio.adminLoginLead') }}</p>
          <div class="network-illustration" aria-hidden="true">
            <span class="network-hub"
              ><svg class="ui-icon" aria-hidden="true">
                <use href="images/ui-icons.svg#agency" /></svg></span
            ><span class="network-node node-one"
              ><svg class="ui-icon" aria-hidden="true">
                <use href="images/ui-icons.svg#car" /></svg></span
            ><span class="network-node node-two"
              ><svg class="ui-icon" aria-hidden="true">
                <use href="images/ui-icons.svg#user" /></svg></span
            ><span class="network-node node-three"
              ><svg class="ui-icon" aria-hidden="true">
                <use href="images/ui-icons.svg#calendar" /></svg></span
            ><span class="network-node node-four"
              ><svg class="ui-icon" aria-hidden="true"><use href="images/ui-icons.svg#car" /></svg
            ></span>
          </div>
          <p class="login-network-note">{{ language.t('studio.adminLoginNote') }}</p>
          <div class="login-hero-badges">
            <span class="login-badge login-badge-global">{{
              language.t('admin.login.globalBadge')
            }}</span>
            <span class="login-badge login-badge-agence">{{
              language.t('admin.login.agenceBadge')
            }}</span>
          </div>
        </div>
      </div>

      <div class="login-form-panel">
        <div class="login-form-card">
          <div class="login-form-header">
            <span class="login-form-icon" aria-hidden="true"
              ><svg class="ui-icon"><use href="images/ui-icons.svg#lock" /></svg
            ></span>
            <h2 class="login-form-title">{{ language.t('admin.login.formTitle') }}</h2>
            <p class="login-form-sub">{{ language.t('admin.login.formSubtitle') }}</p>
          </div>

          <form [formGroup]="form" class="login-form" (ngSubmit)="submit()">
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>{{ language.t('admin.login.email') }}</mat-label>
              <input matInput type="email" autocomplete="username" formControlName="email" />
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>{{ language.t('admin.login.password') }}</mat-label>
              <input
                matInput
                [type]="showAdminPassword ? 'text' : 'password'"
                formControlName="password"
                autocomplete="current-password"
              />
              <button
                mat-icon-button
                matSuffix
                type="button"
                class="password-toggle-button"
                [attr.aria-label]="
                  showAdminPassword
                    ? language.t('admin.login.hidePassword')
                    : language.t('admin.login.showPassword')
                "
                (click)="toggleAdminPassword()"
              >
                <svg class="ui-icon" aria-hidden="true" width="24" height="24">
                  <use
                    [attr.href]="
                      showAdminPassword ? 'images/ui-icons.svg#eye-off' : 'images/ui-icons.svg#eye'
                    "
                  />
                </svg>
              </button>
            </mat-form-field>

            @if (errorMessage()) {
              <div role="alert" class="login-error">{{ errorMessage() }}</div>
            }

            <div class="login-actions">
              <button
                class="login-btn-primary"
                type="submit"
                [disabled]="loading() || form.invalid"
              >
                @if (loading()) {
                  <mat-spinner
                    [attr.aria-label]="language.t('transport.searching')"
                    diameter="20"
                    style="display:inline-block;margin-right:8px;"
                  ></mat-spinner>
                }
                {{ language.t('admin.login.submit') }}
              </button>
              <a routerLink="/" class="login-btn-secondary">{{ language.t('admin.login.back') }}</a>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
})
export class AdminLoginPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly errorMessage = signal('');
  showAdminPassword = false;

  readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  submit(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.loginAdmin(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl('/admin');
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set(this.language.t('admin.login.error'));
      },
    });
  }

  toggleAdminPassword(): void {
    this.showAdminPassword = !this.showAdminPassword;
  }
}
