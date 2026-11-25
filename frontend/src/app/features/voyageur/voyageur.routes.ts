import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { LanguageService } from '../../core/i18n/language.service';
import { voyageurGuard } from '../../core/guards/voyageur.guard';

export const VOYAGEUR_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/voyageur-home-page.component').then((m) => m.VoyageurHomePage),
    title: () => `GestionTransport - ${inject(LanguageService).t('nav.voyageurArea')}`
  },
  {
    path: 'profile',
    canActivate: [voyageurGuard],
    loadComponent: () => import('./pages/voyageur-profile-page.component').then((m) => m.VoyageurProfilePage),
    title: () => `GestionTransport - ${inject(LanguageService).t('nav.profile')}`
  },
  {
    path: 'trajets',
    canActivate: [voyageurGuard],
    loadComponent: () => import('./pages/voyageur-public-trajets-page.component').then((m) => m.VoyageurPublicTrajetsPage),
    title: () => `GestionTransport - ${inject(LanguageService).t('nav.trajets')}`
  },
  {
    path: 'trajets/new',
    canActivate: [voyageurGuard],
    loadComponent: () => import('./pages/voyageur-create-trajet-page.component').then((m) => m.VoyageurCreateTrajetPage),
    title: () => `GestionTransport - ${inject(LanguageService).t('nav.create')}`
  },
  {
    path: 'reservations',
    canActivate: [voyageurGuard],
    loadComponent: () => import('./pages/voyageur-reservations-page.component').then((m) => m.VoyageurReservationsPage),
    title: () => `GestionTransport - ${inject(LanguageService).t('nav.reservations')}`
  },
  {
    path: 'payments',
    canActivate: [voyageurGuard],
    loadComponent: () => import('./pages/voyageur-payments-page.component').then((m) => m.VoyageurPaymentsPage),
    title: () => `GestionTransport - ${inject(LanguageService).t('nav.payments')}`
  }
];
