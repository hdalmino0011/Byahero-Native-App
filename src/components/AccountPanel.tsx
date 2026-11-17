import React from 'react';
import {Bike, Car, LogOut, Percent, TrendingUp, UserCheck} from 'lucide-react';
import {ActiveMode, UserProfile, UserRole} from '../types/byahero';
import {GoogleGVectorIcon} from './VectorGraphics';
import {PWAInstallButton} from './PWAInstallButton';

interface AccountPanelProps {
  profile: UserProfile;
  onOpenAuthModal: () => void;
  onSwitchRole: (role: UserRole) => void;
  onSwitchActiveMode: (mode: ActiveMode) => void;
  onLogout: () => void;
}

export const AccountPanel: React.FC<AccountPanelProps> = ({
  profile,
  onOpenAuthModal,
  onSwitchRole,
  onSwitchActiveMode,
  onLogout,
}) => {
  const hasRiderAccess = profile.role === 'rider' || profile.role === 'both';

  return (
    <div className="space-y-4">
      {/* User Identity Hero Card */}
      <div className="p-4 rounded-2xl glass-green text-white space-y-3">
        <div className="flex items-center justify-between gap-3">
          <div className="min-w-0">
            <div className="text-sm font-bold text-white truncate">{profile.fullName}</div>
            <div className="text-xs text-white/80 truncate">{profile.email}</div>
          </div>
          <span className="text-[11px] font-semibold text-emerald-200 shrink-0">
            {profile.authProvider === 'google' ? 'Google Account' : 'Member'}
          </span>
        </div>

        <button
          type="button"
          onClick={onOpenAuthModal}
          className="w-full h-11 rounded-xl bg-white hover:bg-[#F4F7F5] text-[#101713] text-xs font-bold flex items-center justify-center gap-2.5 shadow-sm transition-colors"
        >
          <GoogleGVectorIcon className="w-4 h-4" />
          <span>Manage Account & Vehicle</span>
        </button>
      </div>

      {/* Role Registration & Instant Mode Switching */}
      <div className="p-4 rounded-2xl glass-surface space-y-3">
        <div className="flex items-center gap-2 text-xs font-bold text-[#101713]">
          <UserCheck className="w-4 h-4 text-[#0F522B]" />
          <span>Account Role</span>
        </div>

        <div className="grid grid-cols-3 gap-2">
          {([
            {id: 'passenger', label: 'Passenger'},
            {id: 'rider', label: 'Rider'},
            {id: 'both', label: 'Both'},
          ] as const).map((r) => (
            <button
              key={r.id}
              type="button"
              onClick={() => onSwitchRole(r.id)}
              className={`h-10 rounded-xl border text-xs font-semibold transition-all ${
                profile.role === r.id
                  ? 'bg-[#0F522B] border-[#0F522B] text-white shadow-sm'
                  : 'bg-white/70 border-[#0F522B]/15 text-[#101713] hover:border-[#0F522B]/40'
              }`}
            >
              {r.label}
            </button>
          ))}
        </div>

        {profile.role === 'both' && (
          <div className="pt-2.5 border-t border-[#0F522B]/10 flex items-center justify-between gap-2">
            <span className="text-xs font-medium text-[#55625A]">Active Mode</span>
            <div className="flex items-center gap-1 p-1 rounded-xl bg-[#EBF1ED] border border-[#0F522B]/10">
              <button
                type="button"
                onClick={() => onSwitchActiveMode('passenger')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                  profile.activeMode === 'passenger'
                    ? 'bg-[#0F522B] text-white'
                    : 'text-[#55625A] hover:text-[#101713]'
                }`}
              >
                Passenger
              </button>
              <button
                type="button"
                onClick={() => onSwitchActiveMode('rider')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                  profile.activeMode === 'rider'
                    ? 'bg-[#0F522B] text-white'
                    : 'text-[#55625A] hover:text-[#101713]'
                }`}
              >
                Rider
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Vehicle Info (only when Rider or Both) */}
      {hasRiderAccess && (
        <div className="p-4 rounded-2xl glass-surface flex items-center justify-between text-xs">
          <span className="flex items-center gap-2 font-semibold text-[#101713]">
            {profile.vehicleType === 'motorcycle' ? (
              <Bike className="w-4 h-4 text-[#0F522B]" />
            ) : (
              <Car className="w-4 h-4 text-[#0F522B]" />
            )}
            <span>
              {profile.vehicleType === 'motorcycle' ? 'Motorcycle' : 'Taxi Cab'}
            </span>
          </span>
          <span className="text-[#55625A] font-semibold tabular-nums">
            Plate {profile.plateNumber || 'BYH-774'}
          </span>
        </div>
      )}

      {/* Member Benefits */}
      <div className="p-4 rounded-2xl glass-surface space-y-2.5 text-xs">
        <div className="flex items-center justify-between">
          <span className="flex items-center gap-2 font-semibold text-[#101713]">
            <Percent className="w-4 h-4 text-[#0F522B]" />
            <span>Saturday Ride Discount</span>
          </span>
          <span className="text-[#0F522B] font-bold">10% Off</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="flex items-center gap-2 font-semibold text-[#101713]">
            <TrendingUp className="w-4 h-4 text-[#0F522B]" />
            <span>Rider Trip Bonus</span>
          </span>
          <span className="text-[#0F522B] font-bold">+5% Extra</span>
        </div>
      </div>

      {/* Install on Mobile Home Screen */}
      <PWAInstallButton />

      {/* Log Out */}
      <button
        type="button"
        onClick={onLogout}
        className="w-full h-11 rounded-xl glass-surface hover:bg-white text-xs font-bold text-[#101713] flex items-center justify-center gap-2 transition-colors"
      >
        <LogOut className="w-4 h-4 text-[#0F522B]" />
        <span>Log Out</span>
      </button>
    </div>
  );
};
