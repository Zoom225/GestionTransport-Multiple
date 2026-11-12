import { CommonModule } from '@angular/common';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';
import { VoyageursApiService } from '../../../core/api/voyageurs-api.service';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { VoyageurRequest, VoyageurResponse } from '../../../shared/models/voyageur.model';
import { TypeVoyageur } from '../../../shared/models/enums.model';
import { AgenceResponse } from '../../../shared/models/agence-vehicule.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';
import { LanguageService } from '../../../core/i18n/language.service';

@Component({
  selector: 'app-admin-voyageurs-page',
  standalone: true,
  imports: [
    CommonModule,
    MatCheckboxModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <!-- En-tête gestiontransport admin -->
    <div class="adm-header">
      <div class="adm-header-content">
        <div class="adm-header-left">
          <span class="adm-icon">🚗</span>
          <div>
            <h1 class="adm-title ds-section-title">{{ language.t('admin.voyageurs.title') }}</h1>
            <p class="adm-sub ds-subtitle">{{ language.t('admin.voyageurs.subtitle') }}</p>
          </div>
        </div>
        <a mat-stroked-button routerLink="/admin" class="adm-back-btn">{{
          language.t('common.back')
        }}</a>
      </div>
    </div>

    <section class="page-shell">
      <!-- Formulaire création/édition -->
      <div class="adm-form-card">
        <div class="adm-form-header">
          <span class="adm-form-icon">{{ editingId() ? '✏️' : '➕' }}</span>
          <h2 class="adm-form-title">
            {{ editingId() ? language.t('common.edit') : language.t('common.create') }}
          </h2>
        </div>

        <form [formGroup]="form" class="grid gap-4 pt-4 md:grid-cols-2" (ngSubmit)="save()">
          <mat-checkbox formControlName="peutProposerTrajet" class="md:col-span-2"
            >Autoriser ce voyageur à proposer des trajets comme chauffeur</mat-checkbox
          >
          @if (!editingId()) {
            <mat-form-field appearance="outline"
              ><mat-label>Mot de passe initial personnel</mat-label>
              <input
                matInput
                type="password"
                formControlName="password"
                autocomplete="new-password"
                minlength="12"
                maxlength="128"
              />
              <mat-hint
                >Entre 12 et 128 caractères. Les mots de passe existants ne sont pas
                modifiés.</mat-hint
              >
            </mat-form-field>
          }
          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.voyageurs.matricule') }}</mat-label>
            <input
              matInput
              formControlName="matricule"
              [readonly]="editingId() !== null"
              [placeholder]="editingId() ? '' : 'G1234 / S12345 / L12345'"
              class="uppercase font-semibold tracking-wider"
            />
            @if (!editingId()) {
              <mat-hint>{{ language.t('admin.voyageurs.matriculeHint') }}</mat-hint>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.voyageurs.type') }}</mat-label>
            <mat-select formControlName="typeVoyageur" [disabled]="editingId() !== null">
              <mat-option value="VIP">{{ language.t('admin.voyageurs.typeGlobal') }}</mat-option>
              <mat-option value="LOCAL">{{ language.t('admin.voyageurs.typeAgence') }}</mat-option>
              <mat-option value="LIBRE">{{ language.t('admin.voyageurs.typeLibre') }}</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.voyageurs.nom') }}</mat-label>
            <input matInput formControlName="nom" placeholder="Dupont" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.voyageurs.prenom') }}</mat-label>
            <input matInput formControlName="prenom" placeholder="Jean" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.voyageurs.email') }}</mat-label>
            <input
              matInput
              type="email"
              formControlName="email"
              placeholder="jean.dupont@email.com"
            />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.voyageurs.agence') }}</mat-label>
            <mat-select formControlName="agenceId">
              <mat-option [value]="null">{{ language.t('admin.voyageurs.agenceNone') }}</mat-option>
              @for (agence of agences(); track agence.id) {
                <mat-option [value]="agence.id">🏢 {{ agence.nom }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          @if (message()) {
            <div class="status-success md:col-span-2">✅ {{ message() }}</div>
          }
          @if (errorMessage()) {
            <div class="status-error md:col-span-2">❌ {{ errorMessage() }}</div>
          }

          <div class="flex items-center gap-3 md:col-span-2">
            <button class="adm-btn-primary" type="submit" [disabled]="loading() || form.invalid">
              {{ editingId() ? language.t('common.save') : language.t('common.create') }}
            </button>
            <button class="adm-btn-secondary" type="button" (click)="resetForm()">
              {{ language.t('common.reset') }}
            </button>
            @if (loading()) {
              <mat-spinner diameter="24"></mat-spinner>
            }
          </div>
        </form>
      </div>

      <!-- Recherche par matricule -->
      <div class="adm-search-bar">
        <span class="adm-search-icon">🔍</span>
        <input
          class="adm-search-input"
          type="text"
          [placeholder]="language.t('admin.voyageurs.searchPlaceholder')"
          [value]="searchQuery()"
          (input)="searchQuery.set($any($event.target).value)"
        />
        @if (searchQuery()) {
          <button class="adm-search-clear" (click)="searchQuery.set('')">✕</button>
        }
        <span class="adm-search-count"
          >{{ displayedVoyageurs().length }} {{ language.t('admin.voyageurs.count') }}</span
        >
      </div>

      <!-- Filtres par type -->
      <div class="adm-type-filters">
        <button
          class="adm-filter-btn"
          [class.active]="typeFilter() === ''"
          (click)="typeFilter.set('')"
        >
          {{ language.t('admin.voyageurs.filterAll') }} ({{ filteredVoyageurs().length }})
        </button>
        <button
          class="adm-filter-btn adm-filter-global"
          [class.active]="typeFilter() === 'VIP'"
          (click)="typeFilter.set('VIP')"
        >
          🌍 {{ language.t('admin.voyageurs.filterGlobal') }} ({{ countByType('VIP') }})
        </button>
        <button
          class="adm-filter-btn adm-filter-agence"
          [class.active]="typeFilter() === 'LOCAL'"
          (click)="typeFilter.set('LOCAL')"
        >
          🏢 {{ language.t('admin.voyageurs.filterAgence') }} ({{ countByType('LOCAL') }})
        </button>
        <button
          class="adm-filter-btn adm-filter-libre"
          [class.active]="typeFilter() === 'LIBRE'"
          (click)="typeFilter.set('LIBRE')"
        >
          ⚡ {{ language.t('admin.voyageurs.filterLibre') }} ({{ countByType('LIBRE') }})
        </button>
      </div>

      <!-- Liste des voyageurs -->
      @if (displayedVoyageurs().length === 0) {
        <div class="adm-empty">
          <span class="adm-empty-icon">🚗</span>
          <p class="adm-empty-title">{{ language.t('admin.voyageurs.empty') }}</p>
          <p class="adm-empty-sub">
            {{
              searchQuery()
                ? language.t('admin.voyageurs.searchNoResult', { query: searchQuery() })
                : language.t('admin.voyageurs.emptySub')
            }}
          </p>
        </div>
      }

      <div class="grid gap-4 md:grid-cols-2">
        @for (voyageur of displayedVoyageurs(); track voyageur.id) {
          <div
            class="adm-voyageur-card"
            [class]="'adm-voyageur-' + voyageur.typeVoyageur.toLowerCase()"
          >
            <div class="adm-voyageur-top">
              <div class="adm-voyageur-avatar">
                {{
                  voyageur.typeVoyageur === 'VIP'
                    ? '🌍'
                    : voyageur.typeVoyageur === 'LOCAL'
                      ? '🏢'
                      : '⚡'
                }}
              </div>
              <div class="adm-voyageur-info">
                <div class="adm-voyageur-name">{{ voyageur.prenom }} {{ voyageur.nom }}</div>
                <div class="adm-voyageur-matricule">{{ voyageur.matricule }}</div>
              </div>
              <span class="ds-badge" [class]="typeBadgeClass(voyageur.typeVoyageur)">
                {{ voyageur.typeVoyageur }}
              </span>
              @if (hasActiveRestriction(voyageur.id)) {
                <span class="adm-restriction-badge">{{
                  language.t('admin.voyageurs.restriction')
                }}</span>
              }
            </div>

            <div class="adm-voyageur-details ds-data-list">
              <div class="adm-detail-row ds-data-row">
                <span class="ds-data-key">{{ language.t('admin.voyageurs.email') }}</span>
                <span class="ds-data-value">{{
                  voyageur.email || language.t('admin.voyageurs.emailMissing')
                }}</span>
              </div>
              <div class="adm-detail-row ds-data-row">
                <span class="ds-data-key">{{ language.t('admin.voyageurs.agence') }}</span>
                <span class="ds-data-value">{{
                  voyageur.agenceNom || language.t('admin.voyageurs.agenceAll')
                }}</span>
              </div>
              <div class="adm-detail-row ds-data-row">
                <span class="ds-data-key">{{ language.t('admin.voyageurs.balance') }}</span>
                <span class="ds-data-value">{{ voyageur.solde }} EUR</span>
              </div>
            </div>

            <div class="adm-voyageur-actions">
              <button mat-button type="button" (click)="voirRestrictions(voyageur.id)">Historique des restrictions</button>
              <button class="adm-action-edit" (click)="edit(voyageur)">
                ✏️ {{ language.t('admin.voyageurs.edit') }}
              </button>
              <button class="adm-action-delete" (click)="remove(voyageur.id)">
                🗑️ {{ language.t('admin.voyageurs.delete') }}
              </button>
            </div>
            @if (historiqueVoyageurId() === voyageur.id) {
              <div class="p-4" aria-live="polite">
                @for (restriction of historiqueRestrictions(); track restriction.id) {
                  <p class="break-words">{{ restriction.motif }} — {{ restriction.dateDebut ? (restriction.dateDebut | date:'dd/MM/yyyy') : 'Début historique non renseigné' }} → {{ restriction.dateFin | date:'dd/MM/yyyy' }} : {{ restriction.active ? 'Active' : 'Inactive' }}</p>
                } @empty { <p>Aucune restriction enregistrée.</p> }
              </div>
            }
          </div>
        }
      </div>
    </section>

    <style>
      .adm-header {
        background: linear-gradient(135deg, #14532d 0%, #15803d 50%, #166534 100%);
        padding: 1.5rem 2rem;
      }
      .adm-header-content {
        display: flex;
        align-items: center;
        justify-content: space-between;
        max-width: 1200px;
        margin: 0 auto;
      }
      .adm-header-left {
        display: flex;
        align-items: center;
        gap: 1rem;
      }
      .adm-icon {
        font-size: 2.5rem;
      }
      .adm-title {
        font-size: 1.6rem;
        font-weight: 800;
        color: #fff;
        margin: 0;
      }
      .adm-sub {
        color: rgba(255, 255, 255, 0.75);
        font-size: 0.85rem;
        margin: 0;
      }
      .adm-back-btn {
        color: #fff !important;
        border-color: rgba(255, 255, 255, 0.4) !important;
      }

      .adm-form-card {
        background: #fff;
        border-radius: 1.25rem;
        box-shadow: 0 4px 20px rgba(21, 128, 61, 0.1);
        padding: 1.5rem;
        border-left: 5px solid #16a34a;
      }
      .adm-form-header {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        margin-bottom: 0.5rem;
      }
      .adm-form-icon {
        font-size: 1.5rem;
      }
      .adm-form-title {
        font-size: 1.2rem;
        font-weight: 700;
        color: #14532d;
        margin: 0;
      }

      .adm-success {
        background: #f0fdf4;
        border: 1px solid #86efac;
        border-radius: 0.75rem;
        padding: 0.75rem 1rem;
        color: #14532d;
        font-size: 0.9rem;
      }
      .adm-error {
        background: #fef2f2;
        border: 1px solid #fca5a5;
        border-radius: 0.75rem;
        padding: 0.75rem 1rem;
        color: #991b1b;
        font-size: 0.9rem;
      }

      .adm-btn-primary {
        background: linear-gradient(135deg, #15803d, #16a34a);
        color: #fff;
        border: none;
        border-radius: 0.6rem;
        padding: 0.65rem 1.5rem;
        font-weight: 700;
        cursor: pointer;
        box-shadow: 0 3px 10px rgba(21, 128, 61, 0.3);
        transition: transform 0.15s;
      }
      .adm-btn-primary:hover:not(:disabled) {
        transform: translateY(-1px);
      }
      .adm-btn-primary:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
      .adm-btn-secondary {
        background: #f8fafc;
        color: #475569;
        border: 1px solid #cbd5e1;
        border-radius: 0.6rem;
        padding: 0.65rem 1.25rem;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.15s;
      }
      .adm-btn-secondary:hover {
        background: #f1f5f9;
      }

      .adm-search-bar {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        background: #fff;
        border: 2px solid #86efac;
        border-radius: 1rem;
        padding: 0.6rem 1rem;
        box-shadow: 0 2px 8px rgba(21, 128, 61, 0.1);
      }
      .adm-search-icon {
        font-size: 1.1rem;
      }
      .adm-search-input {
        flex: 1;
        border: none;
        outline: none;
        font-size: 0.95rem;
        background: transparent;
        color: #1e293b;
      }
      .adm-search-clear {
        background: #fee2e2;
        border: none;
        border-radius: 50%;
        width: 1.5rem;
        height: 1.5rem;
        cursor: pointer;
        font-size: 0.75rem;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .adm-search-count {
        font-size: 0.8rem;
        color: #64748b;
        white-space: nowrap;
        background: #f0fdf4;
        padding: 0.2rem 0.6rem;
        border-radius: 9999px;
      }

      .adm-type-filters {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }
      .adm-filter-btn {
        padding: 0.4rem 1rem;
        border-radius: 9999px;
        border: 2px solid #e2e8f0;
        background: #fff;
        font-size: 0.82rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.15s;
        color: #475569;
      }
      .adm-filter-btn.active,
      .adm-filter-btn:hover {
        border-color: #16a34a;
        background: #f0fdf4;
        color: #14532d;
      }
      .adm-filter-global.active {
        border-color: #3b82f6;
        background: #eff6ff;
        color: #1d4ed8;
      }
      .adm-filter-agence.active {
        border-color: #16a34a;
        background: #f0fdf4;
        color: #14532d;
      }
      .adm-filter-libre.active {
        border-color: #eab308;
        background: #fefce8;
        color: #a16207;
      }

      .adm-empty {
        text-align: center;
        padding: 3rem 1rem;
      }
      .adm-empty-icon {
        font-size: 3rem;
        display: block;
        margin-bottom: 0.75rem;
      }
      .adm-empty-title {
        font-size: 1.1rem;
        font-weight: 700;
        color: #374151;
        margin: 0 0 0.35rem;
      }
      .adm-empty-sub {
        color: #9ca3af;
        font-size: 0.9rem;
        margin: 0;
      }

      .adm-voyageur-card {
        background: #fff;
        border-radius: 1.25rem;
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.07);
        overflow: hidden;
        transition:
          transform 0.15s,
          box-shadow 0.15s;
        border-top: 4px solid #e2e8f0;
      }
      .adm-voyageur-card:hover {
        transform: translateY(-3px);
        box-shadow: 0 10px 28px rgba(0, 0, 0, 0.12);
      }
      .adm-voyageur-global {
        border-top-color: #3b82f6;
      }
      .adm-voyageur-agence {
        border-top-color: #16a34a;
      }
      .adm-voyageur-libre {
        border-top-color: #eab308;
      }

      .adm-voyageur-top {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 1rem 1.25rem 0.5rem;
      }
      .adm-voyageur-avatar {
        font-size: 2rem;
      }
      .adm-voyageur-info {
        flex: 1;
      }
      .adm-voyageur-name {
        font-size: 1rem;
        font-weight: 700;
        color: #1e293b;
      }
      .adm-voyageur-matricule {
        font-size: 0.85rem;
        color: #64748b;
        font-family: monospace;
        letter-spacing: 0.05em;
      }

      .adm-voyageur-badge {
        display: inline-block;
        padding: 0.2rem 0.7rem;
        border-radius: 9999px;
        font-size: 0.72rem;
        font-weight: 800;
        letter-spacing: 0.06em;
      }
      .adm-badge-global {
        background: #dbeafe;
        color: #1d4ed8;
      }
      .adm-badge-agence {
        background: #dcfce7;
        color: #15803d;
      }
      .adm-badge-libre {
        background: #fef9c3;
        color: #a16207;
      }
      .adm-restriction-badge {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        background: #fee2e2;
        color: #991b1b;
        border: 1px solid #fecaca;
        padding: 0.2rem 0.65rem;
        border-radius: 9999px;
        font-size: 0.72rem;
        font-weight: 800;
        white-space: nowrap;
      }

      .adm-voyageur-details {
        padding: 0.5rem 1.25rem 0.75rem;
        display: flex;
        flex-direction: column;
        gap: 0.3rem;
      }
      .adm-detail-row {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 0.85rem;
        color: #475569;
      }

      .adm-voyageur-actions {
        display: flex;
        gap: 0.5rem;
        padding: 0.75rem 1.25rem;
        border-top: 1px solid #f1f5f9;
      }
      .adm-action-edit {
        flex: 1;
        background: #eff6ff;
        color: #1d4ed8;
        border: 1px solid #bfdbfe;
        border-radius: 0.5rem;
        padding: 0.4rem;
        font-size: 0.82rem;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.15s;
      }
      .adm-action-edit:hover {
        background: #dbeafe;
      }
      .adm-action-delete {
        flex: 1;
        background: #fef2f2;
        color: #dc2626;
        border: 1px solid #fecaca;
        border-radius: 0.5rem;
        padding: 0.4rem;
        font-size: 0.82rem;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.15s;
      }
      .adm-action-delete:hover {
        background: #fee2e2;
      }
    </style>
  `,
})
export class AdminVoyageursPage {
  readonly historiqueVoyageurId = signal<number | null>(null);
  readonly historiqueRestrictions = signal<import('../../../shared/models/voyageur.model').RestrictionResponse[]>([]);

  voirRestrictions(id: number): void {
    this.historiqueVoyageurId.set(null);
    this.voyageursApi.getRestrictions(id).subscribe({
      next: rows => { this.historiqueRestrictions.set(rows); this.historiqueVoyageurId.set(id); },
      error: error => this.errorMessage.set(extractApiErrorMessage(error, "L'historique est indisponible."))
    });
  }
  private readonly voyageursApi = inject(VoyageursApiService);
  private readonly agencesApi = inject(AgencesApiService);
  private readonly adminSession = inject(AdminSessionService);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly editingId = signal<number | null>(null);
  readonly voyageurs = signal<VoyageurResponse[]>([]);
  readonly agences = signal<AgenceResponse[]>([]);
  readonly searchQuery = signal('');
  readonly typeFilter = signal('');
  readonly restrictedVoyageurIds = signal<Set<number>>(new Set());

  readonly filteredVoyageurs = computed(() => {
    const agenceId = this.adminSession.agenceId();
    if (this.adminSession.isGlobalAdmin() || !agenceId) {
      return this.voyageurs();
    }
    return this.voyageurs().filter((m) => m.agenceId === agenceId || m.agenceId === null);
  });

  readonly displayedVoyageurs = computed(() => {
    let list = this.filteredVoyageurs();
    const q = this.searchQuery().trim().toUpperCase();
    const t = this.typeFilter();
    if (q) {
      list = list.filter(
        (m) =>
          m.matricule.toUpperCase().includes(q) ||
          m.nom.toUpperCase().includes(q) ||
          m.prenom.toUpperCase().includes(q),
      );
    }
    if (t) {
      list = list.filter((m) => m.typeVoyageur === t);
    }
    return list;
  });

  countByType(type: string): number {
    return this.filteredVoyageurs().filter((m) => m.typeVoyageur === type).length;
  }

  typeBadgeClass(type: TypeVoyageur): string {
    if (type === 'VIP') {
      return 'ds-badge-info';
    }
    if (type === 'LOCAL') {
      return 'ds-badge-success';
    }
    return 'ds-badge-warning';
  }

  hasActiveRestriction(voyageurId: number): boolean {
    return this.restrictedVoyageurIds().has(voyageurId);
  }

  readonly form = new FormGroup({
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.minLength(12), Validators.maxLength(128)],
    }),
    peutProposerTrajet: new FormControl(false, { nonNullable: true }),
    matricule: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    nom: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    prenom: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.email] }),
    typeVoyageur: new FormControl<TypeVoyageur>('VIP', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    agenceId: new FormControl<number | null>(null),
  });

  constructor() {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.restrictedVoyageurIds.set(new Set());
    forkJoin({
      voyageurs: this.voyageursApi.getAll(),
      agences: this.agencesApi.getAll(),
    }).subscribe({
      next: ({ voyageurs, agences }) => {
        this.voyageurs.set(voyageurs);
        this.agences.set(
          this.adminSession.isGlobalAdmin()
            ? agences
            : agences.filter((s) => s.id === this.adminSession.agenceId()),
        );
        this.loadRestrictionStatuses(voyageurs);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.restrictedVoyageurIds.set(new Set());
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('admin.voyageurs.loadError')),
        );
      },
    });
  }

  private loadRestrictionStatuses(voyageurs: VoyageurResponse[]): void {
    if (voyageurs.length === 0) {
      this.restrictedVoyageurIds.set(new Set());
      return;
    }

    forkJoin(
      voyageurs.map((voyageur) =>
        this.voyageursApi.hasRestriction(voyageur.id).pipe(
          map((hasRestriction) => ({ id: voyageur.id, hasRestriction })),
          catchError(() => of({ id: voyageur.id, hasRestriction: false })),
        ),
      ),
    ).subscribe((statuses) => {
      this.restrictedVoyageurIds.set(
        new Set(statuses.filter((status) => status.hasRestriction).map((status) => status.id)),
      );
    });
  }

  edit(voyageur: VoyageurResponse): void {
    this.editingId.set(voyageur.id);
    this.form.patchValue({
      peutProposerTrajet: !!voyageur.peutProposerTrajet,
      matricule: voyageur.matricule,
      nom: voyageur.nom,
      prenom: voyageur.prenom,
      email: voyageur.email,
      typeVoyageur: voyageur.typeVoyageur,
      agenceId: voyageur.agenceId,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  resetForm(): void {
    this.editingId.set(null);
    this.message.set('');
    this.errorMessage.set('');
    this.form.reset({
      password: '',
      peutProposerTrajet: false,
      matricule: '',
      nom: '',
      prenom: '',
      email: '',
      typeVoyageur: this.adminSession.isAgenceAdmin() ? 'LOCAL' : 'VIP',
      agenceId: this.adminSession.isAgenceAdmin() ? this.adminSession.agenceId() : null,
    });
  }

  save(): void {
    if (this.form.invalid || this.loading()) return;

    this.loading.set(true);
    this.message.set('');
    this.errorMessage.set('');

    const payload = this.form.getRawValue() as VoyageurRequest;
    if (this.editingId()) delete payload.password;
    else if (!payload.password || payload.password.length < 12) {
      this.loading.set(false);
      this.errorMessage.set('Un mot de passe initial de 12 caractères minimum est obligatoire.');
      return;
    }
    if (this.adminSession.isAgenceAdmin()) {
      payload.agenceId = this.adminSession.agenceId() ?? undefined;
      payload.typeVoyageur = 'LOCAL';
    }

    const request$ = this.editingId()
      ? this.voyageursApi.update(this.editingId()!, payload)
      : this.voyageursApi.create(payload);

    request$.subscribe({
      next: () => {
        const wasEditing = this.editingId() !== null;
        this.loading.set(false);
        this.resetForm();
        this.message.set(
          wasEditing
            ? this.language.t('admin.voyageurs.updateSuccess')
            : this.language.t('admin.voyageurs.createSuccess'),
        );
        this.loadData();
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('admin.voyageurs.saveError')),
        );
      },
    });
  }

  remove(id: number): void {
    if (!confirm(this.language.t('admin.voyageurs.deleteConfirm'))) return;
    this.loading.set(true);
    this.message.set('');
    this.errorMessage.set('');

    this.voyageursApi.delete(id).subscribe({
      next: () => {
        this.loading.set(false);
        this.message.set(this.language.t('admin.voyageurs.deleteSuccess'));
        this.loadData();
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('admin.voyageurs.deleteError')),
        );
      },
    });
  }
}
