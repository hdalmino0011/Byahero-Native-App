import React from 'react';

interface LoadingSpinnerProps {
  size?: 'xs' | 'sm' | 'md' | 'lg';
  tone?: 'green' | 'white' | 'muted';
  label?: string;
  className?: string;
}

const SIZE_CLASSES: Record<NonNullable<LoadingSpinnerProps['size']>, string> = {
  xs: 'w-3.5 h-3.5 border-[2px]',
  sm: 'w-4 h-4 border-[2px]',
  md: 'w-6 h-6 border-[2.5px]',
  lg: 'w-8 h-8 border-[3px]',
};

const TONE_CLASSES: Record<NonNullable<LoadingSpinnerProps['tone']>, string> = {
  green: 'border-[#0F522B]/20 border-t-[#0F522B]',
  white: 'border-white/30 border-t-white',
  muted: 'border-[#55625A]/25 border-t-[#55625A]',
};

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'sm',
  tone = 'green',
  label,
  className = '',
}) => {
  return (
    <span
      role="status"
      aria-live="polite"
      className={`inline-flex items-center justify-center gap-2 ${className}`}
    >
      <span
        aria-hidden="true"
        className={`inline-block rounded-full animate-spin shrink-0 ${SIZE_CLASSES[size]} ${TONE_CLASSES[tone]}`}
      />
      {label ? (
        <span
          className={`text-xs font-semibold ${
            tone === 'white'
              ? 'text-white'
              : tone === 'muted'
              ? 'text-[#55625A]'
              : 'text-[#0F522B]'
          }`}
        >
          {label}
        </span>
      ) : (
        <span className="sr-only">Loading</span>
      )}
    </span>
  );
};
