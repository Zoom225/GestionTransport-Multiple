import { describe, expect, it } from 'vitest';
import { extractApiErrorMessage } from './api-error.util';
import { TRANSLATIONS } from '../../core/i18n/translations';

it('préserve le refus de connexion sans le présenter comme une session expirée', () => {
  expect(
    extractApiErrorMessage(
      { status: 401, url: '/api/voyageurs/login', error: { message: 'Identifiants invalides.' } },
      'Connexion impossible.',
      (key) => TRANSLATIONS.fr[key] ?? key,
    ),
  ).toBe('Identifiants invalides.');
});

it('présente une reconnexion compréhensible pour une réponse 401', () => {
  expect(
    extractApiErrorMessage(
      { status: 401, error: { message: 'Unauthorized' } },
      'Erreur',
      (key) => TRANSLATIONS.fr[key] ?? key,
    ),
  ).toBe('Votre session a expiré. Reconnectez-vous pour continuer.');
});

it('présente une erreur réseau traduite en anglais', () => {
  expect(extractApiErrorMessage({ status: 0 }, 'Error', (key) => TRANSLATIONS.en[key] ?? key)).toBe(
    'Unable to connect. Check your connection and try again.',
  );
});

it('explique le refus d’accès sans afficher une erreur HTTP technique', () => {
  expect(
    extractApiErrorMessage({ status: 403 }, 'Erreur', (key) => TRANSLATIONS.fr[key] ?? key),
  ).toBe('Cette action n’est pas autorisée pour votre compte.');
});

describe('extractApiErrorMessage', () => {
  // ── Cas nominaux ──────────────────────────────────────────────────────────

  it('devrait retourner le premier message de validation de champ quand présent', () => {
    const message = extractApiErrorMessage({
      error: {
        errors: {
          email: 'Email invalide',
          password: 'Mot de passe requis',
        },
      },
    });

    expect(message).toBe('Email invalide');
  });

  it("devrait retourner le message backend quand aucune erreur de champ n'est présente", () => {
    const message = extractApiErrorMessage({
      error: {
        message: 'Reservation impossible',
      },
    });

    expect(message).toBe('Reservation impossible');
  });

  it('devrait revenir au message de secours fourni', () => {
    const message = extractApiErrorMessage(undefined, 'Erreur par defaut');

    expect(message).toBe('Erreur par defaut');
  });

  // ── Fallback par défaut ───────────────────────────────────────────────────

  it("devrait retourner le message de secours par défaut quand aucun argument n'est fourni", () => {
    const message = extractApiErrorMessage(undefined);

    expect(message).toBe('Une erreur est survenue.');
  });

  it("devrait retourner le message de secours par défaut quand l'objet erreur est null", () => {
    const message = extractApiErrorMessage(null);

    expect(message).toBe('Une erreur est survenue.');
  });

  it("devrait retourner le message de secours par défaut quand l'objet erreur est vide", () => {
    const message = extractApiErrorMessage({});

    expect(message).toBe('Une erreur est survenue.');
  });

  // ── Champs de validation ──────────────────────────────────────────────────

  it('devrait retourner le seul message de champ quand errors a une seule entrée', () => {
    const message = extractApiErrorMessage({
      error: {
        errors: { phone: 'Numéro de téléphone invalide' },
      },
    });

    expect(message).toBe('Numéro de téléphone invalide');
  });

  it("devrait ignorer l'objet errors quand il est vide et utiliser le message backend à la place", () => {
    const message = extractApiErrorMessage({
      error: {
        errors: {},
        message: 'Créneau déjà réservé',
      },
    });

    expect(message).toBe('Créneau déjà réservé');
  });

  // ── Message de niveau racine ──────────────────────────────────────────────

  it('devrait utiliser le message de niveau racine quand error.message est absent', () => {
    const message = extractApiErrorMessage({
      message: 'Timeout réseau',
    });

    expect(message).toBe('Timeout réseau');
  });

  // ── Priorité des sources ──────────────────────────────────────────────────

  it('devrait prioriser les erreurs de champ sur le message backend', () => {
    const message = extractApiErrorMessage({
      error: {
        errors: { name: 'Nom requis' },
        message: 'Validation échouée',
      },
    });

    expect(message).toBe('Nom requis');
  });

  it('devrait prioriser le message backend sur le message de niveau racine', () => {
    const message = extractApiErrorMessage({
      message: 'Erreur HTTP générique',
      error: {
        message: 'Voyageur introuvable',
      },
    });

    expect(message).toBe('Voyageur introuvable');
  });

  // ── Scénarios métier réels ────────────────────────────────────────────────

  it('devrait gérer une requête 400 Bad Request avec plusieurs erreurs de champs (inscription)', () => {
    const message = extractApiErrorMessage({
      error: {
        status: 400,
        errors: {
          email: 'Email déjà utilisé',
          password: 'Minimum 8 caractères',
        },
      },
    });

    // Seul le premier message doit être retourné
    expect(message).toBe('Email déjà utilisé');
  });

  it("devrait gérer un conflit 409 provenant d'un chevauchement de réservation", () => {
    const message = extractApiErrorMessage({
      error: {
        status: 409,
        message: 'Ce vehicule est déjà réservé sur ce créneau.',
      },
    });

    expect(message).toBe('Ce vehicule est déjà réservé sur ce créneau.');
  });

  it('devrait gérer un 403 Forbidden avec un message de secours personnalisé', () => {
    const message = extractApiErrorMessage(
      { error: { status: 403 } },
      'Accès refusé, veuillez contacter un administrateur.',
    );

    expect(message).toBe('Accès refusé, veuillez contacter un administrateur.');
  });

  it("devrait ignorer un libellé de statut HTTP générique quand aucun message métier n'existe", () => {
    const message = extractApiErrorMessage(
      { error: { status: 400, error: 'Bad Request' } },
      'Action impossible.',
    );

    expect(message).toBe('Action impossible.');
  });

  it("devrait gérer un message d'échec de paiement provenant du backend", () => {
    const message = extractApiErrorMessage({
      error: {
        message: 'Paiement refusé : solde insuffisant.',
      },
    });

    expect(message).toBe('Paiement refusé : solde insuffisant.');
  });
});
