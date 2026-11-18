import React, {useState} from 'react';
import {ShieldCheck, UserCheck, Bike, Car, X} from 'lucide-react';
import {UserProfile, UserRole, VehicleType} from '../types/byahero';
import {GoogleGVectorIcon, ByaheroLogoMark, ByaheroWordmarkVector} from './VectorGraphics';
import {isSupabaseConfigured, signInWithGoogleSupabase} from '../lib/supabase';

interface AuthModalProps {
  open: boolean;
  currentProfile: UserProfile;
  onClose: () => void;
  onAuthenticate: (profile: UserProfile) => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({
  open,
  currentProfile,
  onClose,
  onAuthenticate,
}) => {
  const [selectedRole, setSelectedRole] = useState<UserRole>(currentProfile.role);
  const [vehicleType, setVehicleType] = useState<VehicleType>(currentProfile.vehicleType);
  const [plateNumber, setPlateNumber] = useState<string>(currentProfile.plateNumber || 'BYH-2026');
  const [fullName, setFullName] = useState<string>(currentProfile.fullName || '');
  const [email, setEmail] = useState<string>(currentProfile.email || '');
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  if (!open) return null;

  const handleSaveAccount = async () => {
    setIsLoading(true);
    setStatusMessage(null);

    if (isSupabaseConfigured) {
      const {error} = await signInWithGoogleSupabase();
      if (error) {
        setStatusMessage(error);
        setIsLoading(false);
        return;
      }
      return;
    }

    const nextProfile: UserProfile = {
      ...currentProfile,
      fullName: fullName.trim() || currentProfile.fullName,
      email: email.trim() || currentProfile.email,
      authProvider: 'google',
      role: selectedRole,
      activeMode: selectedRole === 'rider' ? 'rider' : 'passenger',
      vehicleType,
      plateNumber: plateNumber.trim() || 'BYH-2026',
    };
    onAuthenticate(nextProfile);
    setIsLoading(false);
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40 backdrop-blur-sm p-0 sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="auth-modal-title"
    >
      <div className="w-full sm:max-w-md bg-white/90 backdrop-blur-2xl border border-white rounded-t-3xl sm:rounded-3xl text-[#101713] shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between px-5 pt-5 pb-4 border-b border-[#0F522B]/10">
          <div className="flex items-center gap-3">
            <ByaheroLogoMark className="w-9 h-9" />
            <div>
              <div id="auth-modal-title" className="flex items-center gap-2">
                <ByaheroWordmarkVector lightSurface={true} className="h-5 w-auto" />
                <span className="text-sm font-bold tracking-tight text-[#101713]">
                  Account & Role
                </span>
              </div>
              <p className="text-xs text-[#55625A] mt-0.5">
                Seamless Passenger & Rider Sync
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close account modal"
            className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl text-[#55625A] hover:text-[#101713] hover:bg-[#0F522B]/5 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-5 space-y-5 max-h-[80dvh] overflow-y-auto">
          {/* Role Selection */}
          <div>
            <label className="block text-xs font-semibold text-[#0F522B] mb-2">
              Choose Registration Role
            </label>
            <div className="grid grid-cols-3 gap-2">
              {([
                {id: 'passenger', label: 'Passenger', sub: 'Book rides'},
                {id: 'rider', label: 'Rider', sub: '+5% bonus'},
                {id: 'both', label: 'Both', sub: 'Switch anytime'},
              ] as const).map((option) => {
                const active = selectedRole === option.id;
                return (
                  <button
                    key={option.id}
                    type="button"
                    onClick={() => setSelectedRole(option.id)}
                    className={`min-h-[58px] px-2.5 py-2 rounded-xl border text-left transition-all ${
                      active
                        ? 'bg-[#0F522B] border-[#0F522B] text-white shadow-sm'
                        : 'bg-white/75 border-[#0F522B]/15 text-[#101713] hover:border-[#0F522B]/40'
                    }`}
                  >
                    <div className="text-xs font-bold whitespace-nowrap">{option.label}</div>
                    <div
                      className={`text-[11px] mt-0.5 whitespace-nowrap ${
                        active ? 'text-white/80' : 'text-[#55625A]'
                      }`}
                    >
                      {option.sub}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Vehicle Details if Rider or Both */}
          {(selectedRole === 'rider' || selectedRole === 'both') && (
            <div className="p-3.5 rounded-2xl glass-subtle space-y-3">
              <div className="text-xs font-semibold text-[#101713]">Rider Vehicle Profile</div>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setVehicleType('motorcycle')}
                  className={`min-h-[44px] px-3 py-2 rounded-xl border flex items-center justify-center gap-2 text-xs font-semibold transition-colors ${
                    vehicleType === 'motorcycle'
                      ? 'bg-[#0F522B] border-[#0F522B] text-white'
                      : 'bg-white border-[#0F522B]/15 text-[#101713]'
                  }`}
                >
                  <Bike className="w-4 h-4" />
                  <span>Motorcycle</span>
                </button>
                <button
                  type="button"
                  onClick={() => setVehicleType('taxi')}
                  className={`min-h-[44px] px-3 py-2 rounded-xl border flex items-center justify-center gap-2 text-xs font-semibold transition-colors ${
                    vehicleType === 'taxi'
                      ? 'bg-[#0F522B] border-[#0F522B] text-white'
                      : 'bg-white border-[#0F522B]/15 text-[#101713]'
                  }`}
                >
                  <Car className="w-4 h-4" />
                  <span>Taxi Cab</span>
                </button>
              </div>
              <div>
                <label htmlFor="plate-input" className="block text-[11px] text-[#55625A] mb-1">
                  Plate Number
                </label>
                <input
                  id="plate-input"
                  type="text"
                  value={plateNumber}
                  onChange={(e) => setPlateNumber(e.target.value.toUpperCase())}
                  placeholder="ABC-1234"
                  className="w-full h-11 px-3 rounded-xl bg-white border border-[#0F522B]/15 text-sm text-[#101713] focus:outline-none focus:border-[#0F522B]"
                />
              </div>
            </div>
          )}

          {!isSupabaseConfigured && (
            <div className="space-y-3">
              <div>
                <label htmlFor="google-name" className="block text-xs text-[#55625A] mb-1">
                  Full Name
                </label>
                <input
                  id="google-name"
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Alex Rivera"
                  className="w-full h-11 px-3 rounded-xl bg-white border border-[#0F522B]/15 text-sm text-[#101713] focus:outline-none focus:border-[#0F522B]"
                />
              </div>
              <div>
                <label htmlFor="google-email" className="block text-xs text-[#55625A] mb-1">
                  Email Address
                </label>
                <input
                  id="google-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="alex.rivera@gmail.com"
                  className="w-full h-11 px-3 rounded-xl bg-white border border-[#0F522B]/15 text-sm text-[#101713] focus:outline-none focus:border-[#0F522B]"
                />
              </div>
            </div>
          )}

          {statusMessage && (
            <p className="text-xs text-red-700 bg-red-50 border border-red-200 rounded-xl p-3">
              {statusMessage}
            </p>
          )}

          <button
            type="button"
            disabled={isLoading}
            onClick={handleSaveAccount}
            className="w-full h-12 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white font-bold text-sm flex items-center justify-center gap-3 shadow-md active:scale-[0.99] transition-all disabled:opacity-60"
          >
            <GoogleGVectorIcon className="w-5 h-5" />
            <span>{isLoading ? 'Saving Changes...' : 'Save & Sync Profile'}</span>
          </button>

          <div className="flex items-center justify-between text-[11px] text-[#55625A] pt-1">
            <span className="flex items-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5 text-[#0F522B]" />
              Google Account Verified
            </span>
            <span className="flex items-center gap-1.5">
              <UserCheck className="w-3.5 h-3.5 text-[#0F522B]" />
              Switch roles anytime
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
