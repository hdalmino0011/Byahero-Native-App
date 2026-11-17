import React from 'react';

interface IconProps {
  className?: string;
  lightSurface?: boolean;
}

export const ByaheroLogoMark: React.FC<IconProps> = ({className = 'w-8 h-8'}) => (
  <svg
    viewBox="0 0 64 64"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={className}
    aria-hidden="true"
  >
    <rect width="64" height="64" rx="16" fill="#0F522B" />
    <path
      d="M14 44C18.5 34 27 24 44 20L37 31H48L24 48L29 36H19L14 44Z"
      fill="#FFFFFF"
    />
    <circle cx="48" cy="18" r="5" fill="#34D399" />
  </svg>
);

export const ByaheroMotorcycleVector: React.FC<IconProps> = ({
  className = 'w-44 h-24',
  lightSurface = false,
}) => {
  const strokeMain = lightSurface ? '#101713' : '#FFFFFF';
  const bodyFill = lightSurface ? '#0F522B' : '#047857';
  const accent = lightSurface ? '#0F522B' : '#10B981';

  return (
    <svg
      viewBox="0 0 220 120"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      {/* Speed motion lines */}
      <path d="M12 68H42" stroke={accent} strokeWidth="2.5" strokeLinecap="round" strokeOpacity="0.85" />
      <path d="M4 80H32" stroke={strokeMain} strokeWidth="2" strokeLinecap="round" strokeOpacity="0.35" />
      <path d="M18 92H40" stroke={accent} strokeWidth="2" strokeLinecap="round" strokeOpacity="0.6" />

      {/* Rear wheel */}
      <circle cx="58" cy="86" r="22" stroke={strokeMain} strokeWidth="5.5" />
      <circle cx="58" cy="86" r="13" stroke={accent} strokeWidth="2.5" strokeDasharray="6 4" />
      <circle cx="58" cy="86" r="4.5" fill={strokeMain} />

      {/* Front wheel */}
      <circle cx="166" cy="86" r="22" stroke={strokeMain} strokeWidth="5.5" />
      <circle cx="166" cy="86" r="13" stroke={accent} strokeWidth="2.5" strokeDasharray="6 4" />
      <circle cx="166" cy="86" r="4.5" fill={strokeMain} />

      {/* Swingarm & Engine Block */}
      <path
        d="M58 86L96 82L122 86"
        stroke={lightSurface ? '#34694C' : '#A7F3D0'}
        strokeWidth="4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <rect x="92" y="64" width="28" height="20" rx="5" fill="#0F522B" stroke={accent} strokeWidth="2.5" />

      {/* Aerodynamic Chassis & Seat */}
      <path
        d="M44 58C56 54 78 52 94 56L114 42C125 36 140 36 152 44L162 56L136 68H86L56 66L44 58Z"
        fill={bodyFill}
        stroke={strokeMain}
        strokeWidth="3"
        strokeLinejoin="round"
      />
      {/* Seat cushion */}
      <path
        d="M66 53C78 51 92 51 102 55L92 61H66V53Z"
        fill="#101713"
        stroke={strokeMain}
        strokeWidth="2"
        strokeLinejoin="round"
      />
      {/* Front fork & Handlebars */}
      <path d="M142 36L166 86" stroke={strokeMain} strokeWidth="5" strokeLinecap="round" />
      <path d="M132 32H148L156 42" stroke={accent} strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" />
      {/* Headlight beam */}
      <path d="M158 46L172 48L165 57L154 53Z" fill="#FFFFFF" stroke={strokeMain} strokeWidth="1.5" />
      <path d="M174 47L210 38L206 60L172 52Z" fill={accent} fillOpacity="0.22" />
    </svg>
  );
};

export const ByaheroTaxiVector: React.FC<IconProps> = ({
  className = 'w-28 h-16',
  lightSurface = false,
}) => {
  const strokeMain = lightSurface ? '#101713' : '#FFFFFF';
  const accent = lightSurface ? '#34D399' : '#10B981';

  return (
    <svg
      viewBox="0 0 180 96"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      {/* Roof taxi sign */}
      <rect x="76" y="18" width="28" height="10" rx="3" fill={accent} stroke={strokeMain} strokeWidth="2" />
      {/* Car body */}
      <path
        d="M22 64L30 46L52 28H116L142 46L160 52C164 53.5 166 57 166 61V68H18V64H22Z"
        fill="#0F522B"
        stroke={strokeMain}
        strokeWidth="3"
        strokeLinejoin="round"
      />
      {/* Windows */}
      <path
        d="M55 33H86V46H38L55 33Z"
        fill="#101713"
        stroke="#A7F3D0"
        strokeWidth="2"
        strokeLinejoin="round"
      />
      <path
        d="M92 33H114L132 46H92V33Z"
        fill="#101713"
        stroke="#A7F3D0"
        strokeWidth="2"
        strokeLinejoin="round"
      />
      {/* Headlight */}
      <rect x="156" y="54" width="8" height="6" rx="2" fill="#FFFFFF" />
      {/* Wheels */}
      <circle cx="48" cy="70" r="14" fill="#101713" stroke={strokeMain} strokeWidth="4" />
      <circle cx="48" cy="70" r="5" fill={accent} />
      <circle cx="136" cy="70" r="14" fill="#101713" stroke={strokeMain} strokeWidth="4" />
      <circle cx="136" cy="70" r="5" fill={accent} />
    </svg>
  );
};

export const GoogleGVectorIcon: React.FC<IconProps> = ({className = 'w-5 h-5'}) => (
  <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
    <path
      fill="#4285F4"
      d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17Z"
    />
    <path
      fill="#34A853"
      d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.11-6.72-4.96H1.29v3.14C3.26 21.3 7.31 24 12 24Z"
    />
    <path
      fill="#FBBC05"
      d="M5.28 14.24c-.24-.72-.38-1.49-.38-2.24s.14-1.52.38-2.24V6.62H1.29C.47 8.24 0 10.06 0 12s.47 3.76 1.29 5.38l3.99-3.14Z"
    />
    <path
      fill="#EA4335"
      d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.31 0 3.26 2.7 1.29 6.62l3.99 3.14c.95-2.85 3.6-4.96 6.72-4.96Z"
    />
  </svg>
);

export const FastFoodVectorIcon: React.FC<IconProps> = ({className = 'w-5 h-5'}) => (
  <svg viewBox="0 0 24 24" fill="none" className={className} aria-hidden="true">
    <path
      d="M4 11C4 7.686 7.582 5 12 5C16.418 5 20 7.686 20 11H4Z"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinejoin="round"
    />
    <path d="M3 14.5H21" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    <path
      d="M5 17.5H19C19 18.88 17.88 20 16.5 20H7.5C6.12 20 5 18.88 5 17.5Z"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinejoin="round"
    />
  </svg>
);

export const ResortBeachVectorIcon: React.FC<IconProps> = ({className = 'w-5 h-5'}) => (
  <svg viewBox="0 0 24 24" fill="none" className={className} aria-hidden="true">
    <path
      d="M12 3C7.582 3 4 6.582 4 11H20C20 6.582 16.418 3 12 3Z"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinejoin="round"
    />
    <path d="M12 11V19" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    <path
      d="M3 20C5.5 18.5 8 18.5 10.5 20C13 21.5 15.5 21.5 18 20C19.5 19.1 20.5 19.1 21 19.4"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
    />
  </svg>
);
