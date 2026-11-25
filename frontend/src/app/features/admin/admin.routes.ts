import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { LanguageService } from '../../core/i18n/language.service';
import { adminGuard } from '../../core/guards/admin.guard';

export const ADMIN_ROUTES: Routes = [
  { path: 'trajets/new', canActivate: [adminGuard], data: {roles:['GLOBAL','AGENCE']},
    loadComponent: () => import('../voyageur/pages/voyageur-create-trajet-page.component').then(m => m.VoyageurCreateTrajetPage) },
  {
	path: 'login',
	loadComponent: () => import('./pages/admin-login.page').then((m) => m.AdminLoginPage),
	title: () => `GestionTransport - ${inject(LanguageService).t('nav.adminLogin')}`
  },
  {
	path: '',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'AGENCE'] },
	loadComponent: () => import('./pages/admin-home.page').then((m) => m.AdminHomePage),
	title: () => `GestionTransport - ${inject(LanguageService).t('nav.dashboard')}`
  },
  {
	path: 'voyageurs',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'AGENCE'] },
	loadComponent: () => import('./pages/admin-voyageurs-page.component').then((m) => m.AdminVoyageursPage),
	title: () => `GestionTransport - ${inject(LanguageService).t('nav.voyageurs')}`
  },
  {
	path: 'trajets',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'AGENCE'] },
	loadComponent: () => import('./pages/admin-trajets-page.component').then((m) => m.AdminTrajetsPage),
	title: () => `GestionTransport - ${inject(LanguageService).t('nav.trajets')}`
  },
  {
	path: 'agences',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'AGENCE'] },
	loadComponent: () => import('./pages/admin-agences-page.component').then((m) => m.AdminAgencesPage),
	title: () => `GestionTransport - ${inject(LanguageService).t('nav.agences')}`
  },
  {
	path: 'vehicules',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'AGENCE'] },
	loadComponent: () => import('./pages/admin-vehicules-page.component').then((m) => m.AdminVehiculesPage),
	title: () => `GestionTransport - ${inject(LanguageService).t('nav.vehicules')}`
  },
  {
	path: 'indisponibilites',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'AGENCE'] },
	loadComponent: () => import('./pages/admin-indisponibilites-service-page.component').then((m) => m.AdminIndisponibilitesServicePage),
	title: () => `GestionTransport - ${inject(LanguageService).t('nav.indisponibilites')}`
  }
];

