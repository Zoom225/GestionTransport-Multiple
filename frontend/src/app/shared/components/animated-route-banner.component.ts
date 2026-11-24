import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LanguageService } from '../../core/i18n/language.service';

@Component({
  selector: 'app-animated-route-banner',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    :host {
      display: block;
      margin: 3.5rem 0 2rem;
    }

    .banner-container {
      position: relative;
      background: linear-gradient(135deg, #0b1f33 0%, #102a43 55%, #0f3b5c 100%);
      border-radius: var(--ui-radius-large, 24px);
      overflow: hidden;
      box-shadow: 0 16px 36px -8px rgba(11, 31, 51, 0.35);
      border: 1px solid rgba(255, 255, 255, 0.12);
      color: #ffffff;
      padding: 2.25rem 2rem;
      display: grid;
      grid-template-columns: 1.1fr 1.4fr;
      align-items: center;
      gap: 2rem;
    }

    .banner-text {
      z-index: 2;
    }

    .banner-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.75rem;
      font-weight: 750;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      color: #5eead4;
      background: rgba(13, 148, 136, 0.22);
      border: 1px solid rgba(94, 234, 212, 0.4);
      padding: 0.35rem 0.85rem;
      border-radius: 9999px;
      margin-bottom: 1rem;
    }

    .badge-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: #14b8a6;
      box-shadow: 0 0 10px #2dd4bf;
      animation: pulseDot 2s ease-in-out infinite;
    }

    .banner-title {
      font-size: clamp(1.4rem, 2.4vw, 2rem);
      font-weight: 800;
      line-height: 1.2;
      color: #ffffff;
      margin: 0 0 0.75rem;
      letter-spacing: -0.025em;
    }

    .banner-title span {
      color: #38bdf8;
    }

    .banner-lead {
      font-size: 0.95rem;
      line-height: 1.6;
      color: #cbd5e1;
      margin: 0 0 1.5rem;
      max-width: 44ch;
    }

    .banner-cta {
      display: inline-flex;
      align-items: center;
      gap: 0.65rem;
      background: #2563eb;
      color: #ffffff;
      text-decoration: none;
      padding: 0.75rem 1.4rem;
      border-radius: 12px;
      font-weight: 700;
      font-size: 0.9rem;
      transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.4);
    }

    .banner-cta:hover {
      background: #1d4ed8;
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(37, 99, 235, 0.55);
    }

    .banner-stage {
      position: relative;
      width: 100%;
      height: 100%;
      min-height: 190px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .svg-scene {
      width: 100%;
      height: auto;
      max-height: 230px;
      overflow: visible;
    }

    /* Animations fluides en CSS pur */
    .road-stripes {
      stroke-dasharray: 20 20;
      animation: roadScroll 1s linear infinite;
    }

    .car-group {
      animation: carBobbing 1.8s ease-in-out infinite alternate;
    }

    .wheel-rear, .wheel-front {
      transform-origin: center;
      animation: wheelSpin 0.75s linear infinite;
    }

    .city-pin {
      animation: pinHop 2.4s ease-in-out infinite;
    }
    .city-pin:nth-child(2) {
      animation-delay: 0.8s;
    }
    .city-pin:nth-child(3) {
      animation-delay: 1.6s;
    }

    .pulse-ring {
      transform-origin: center;
      animation: ringExpand 2s cubic-bezier(0.2, 0.8, 0.4, 1) infinite;
    }

    .cloud-slow {
      animation: cloudDrift 18s linear infinite;
    }

    @keyframes roadScroll {
      from { stroke-dashoffset: 0; }
      to { stroke-dashoffset: -40; }
    }

    @keyframes carBobbing {
      0% { transform: translateY(0px) rotate(0deg); }
      50% { transform: translateY(-2px) rotate(-0.3deg); }
      100% { transform: translateY(1.5px) rotate(0.4deg); }
    }

    @keyframes wheelSpin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    @keyframes pulseDot {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.45; transform: scale(0.85); }
    }

    @keyframes pinHop {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-4px); }
    }

    @keyframes ringExpand {
      0% { transform: scale(0.6); opacity: 0.9; }
      100% { transform: scale(2.2); opacity: 0; }
    }

    @keyframes cloudDrift {
      from { transform: translateX(0); }
      to { transform: translateX(-90px); }
    }

    @media (max-width: 900px) {
      .banner-container {
        grid-template-columns: 1fr;
        padding: 2rem 1.5rem;
        gap: 1.5rem;
      }
      .banner-stage {
        min-height: 160px;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .road-stripes, .car-group, .wheel-rear, .wheel-front, .city-pin, .pulse-ring, .cloud-slow, .badge-dot {
        animation: none !important;
      }
    }
  `],
  template: `
    <aside class="banner-container" aria-label="Réseau de covoiturage interurbain animé">
      <div class="banner-text">
        <span class="banner-badge">
          <span class="badge-dot" aria-hidden="true"></span>
          {{ language.t('carousel.label') }}
        </span>
        <h2 class="banner-title">
          Le réseau qui <span>connecte</span> les villes belges
        </h2>
        <p class="banner-lead">
          Des trajets partagés réguliers, 4 places confortables par véhicule et un tarif clair de 25 € par voyageur.
        </p>
        <a routerLink="/voyageur" class="banner-cta">
          {{ language.t('transport.find') }} <span aria-hidden="true">→</span>
        </a>
      </div>

      <div class="banner-stage" aria-hidden="true">
        <svg class="svg-scene" viewBox="0 0 520 220" fill="none" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="roadGradient" x1="0" y1="0" x2="520" y2="0" gradientUnits="userSpaceOnUse">
              <stop stop-color="#0f3b5c" stop-opacity="0.3" />
              <stop offset="0.5" stop-color="#1e293b" />
              <stop offset="1" stop-color="#0f3b5c" stop-opacity="0.3" />
            </linearGradient>
            <linearGradient id="carBodyGrad" x1="180" y1="80" x2="340" y2="170" gradientUnits="userSpaceOnUse">
              <stop stop-color="#38bdf8" />
              <stop offset="0.5" stop-color="#2563eb" />
              <stop offset="1" stop-color="#1e3a8a" />
            </linearGradient>
            <linearGradient id="headlightCone" x1="330" y1="126" x2="480" y2="140" gradientUnits="userSpaceOnUse">
              <stop stop-color="#fef08a" stop-opacity="0.35" />
              <stop offset="1" stop-color="#fef08a" stop-opacity="0" />
            </linearGradient>
          </defs>

          <!-- Arrière-plan urbain stylisé / silhouettes -->
          <g opacity="0.22" fill="#64748b" class="cloud-slow">
            <rect x="40" y="85" width="22" height="40" rx="2" />
            <rect x="68" y="70" width="28" height="55" rx="2" />
            <polygon points="120,60 110,85 130,85" />
            <rect x="114" y="85" width="12" height="40" />
            <rect x="390" y="75" width="26" height="50" rx="2" />
            <rect x="424" y="62" width="32" height="63" rx="2" />
            <circle cx="475" cy="80" r="14" />
          </g>

          <!-- Ligne de route ondulée -->
          <path d="M 0 160 Q 140 145, 260 155 T 520 150" stroke="#334155" stroke-width="32" stroke-linecap="round" fill="none" />
          <path d="M 0 160 Q 140 145, 260 155 T 520 150" stroke="#0ea5e9" stroke-width="2" stroke-opacity="0.35" fill="none" />

          <!-- Bandes blanches animées de la route -->
          <path class="road-stripes" d="M 0 160 Q 140 145, 260 155 T 520 150" stroke="#f8fafc" stroke-width="3" stroke-linecap="round" fill="none" />

          <!-- Halo lumineux phare avant -->
          <polygon points="332,126 470,105 480,165 330,140" fill="url(#headlightCone)" />

          <!-- Voiture en mouvement avec amorti -->
          <g class="car-group" transform="translate(45, 0)">
            <!-- Ombre portée de la voiture -->
            <ellipse cx="255" cy="162" rx="75" ry="9" fill="#020617" opacity="0.6" />

            <!-- Carrosserie principale -->
            <path d="M 185 134 C 185 125 192 118 202 118 L 222 118 L 245 92 C 250 86 258 84 266 84 L 305 84 C 314 84 322 89 326 97 L 340 118 L 350 122 C 358 125 362 131 362 138 L 362 144 C 362 147 359 150 355 150 L 192 150 C 188 150 185 147 185 144 Z" fill="url(#carBodyGrad)" />

            <!-- Vitres profil avec passagers stylisés -->
            <path d="M 245 94 L 227 116 L 270 116 L 270 94 Z" fill="#e0f2fe" opacity="0.85" />
            <path d="M 276 94 L 276 116 L 332 116 L 322 97 C 320 95 316 94 312 94 Z" fill="#e0f2fe" opacity="0.85" />

            <!-- Silhouettes de 2 passagers visibles à bord -->
            <circle cx="254" cy="107" r="5" fill="#0369a1" />
            <circle cx="295" cy="107" r="5" fill="#0369a1" />

            <!-- Phare avant et arrière -->
            <rect x="358" y="128" width="5" height="10" rx="2" fill="#fef08a" />
            <rect x="184" y="128" width="4" height="9" rx="1.5" fill="#ef4444" />

            <!-- Roue arrière -->
            <g class="wheel-rear" style="transform-origin: 220px 150px;">
              <circle cx="220" cy="150" r="16" fill="#0f172a" stroke="#475569" stroke-width="2" />
              <circle cx="220" cy="150" r="7" fill="#94a3b8" />
              <line x1="220" y1="137" x2="220" y2="163" stroke="#cbd5e1" stroke-width="1.5" />
              <line x1="207" y1="150" x2="233" y2="150" stroke="#cbd5e1" stroke-width="1.5" />
            </g>

            <!-- Roue avant -->
            <g class="wheel-front" style="transform-origin: 325px 150px;">
              <circle cx="325" cy="150" r="16" fill="#0f172a" stroke="#475569" stroke-width="2" />
              <circle cx="325" cy="150" r="7" fill="#94a3b8" />
              <line x1="325" y1="137" x2="325" y2="163" stroke="#cbd5e1" stroke-width="1.5" />
              <line x1="312" y1="150" x2="338" y2="150" stroke="#cbd5e1" stroke-width="1.5" />
            </g>
          </g>

          <!-- Villes jalons avec repères animés -->
          <g class="city-pin" transform="translate(60, 105)">
            <circle class="pulse-ring" cx="12" cy="12" r="10" fill="#14b8a6" />
            <circle cx="12" cy="12" r="7" fill="#0d9488" stroke="#ffffff" stroke-width="2" />
            <text x="12" y="32" text-anchor="middle" fill="#94a3b8" font-size="10" font-weight="700">Mons</text>
          </g>

          <g class="city-pin" transform="translate(245, 30)">
            <circle class="pulse-ring" cx="12" cy="12" r="12" fill="#38bdf8" />
            <circle cx="12" cy="12" r="8" fill="#0284c7" stroke="#ffffff" stroke-width="2.5" />
            <text x="12" y="-6" text-anchor="middle" fill="#38bdf8" font-size="11" font-weight="800">Bruxelles</text>
          </g>

          <g class="city-pin" transform="translate(450, 95)">
            <circle class="pulse-ring" cx="12" cy="12" r="10" fill="#14b8a6" />
            <circle cx="12" cy="12" r="7" fill="#0d9488" stroke="#ffffff" stroke-width="2" />
            <text x="12" y="32" text-anchor="middle" fill="#94a3b8" font-size="10" font-weight="700">Liège</text>
          </g>
        </svg>
      </div>
    </aside>
  `,
})
export class AnimatedRouteBannerComponent {
  readonly language = inject(LanguageService);
}
