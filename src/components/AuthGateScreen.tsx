import React, {useState} from 'react';
import {AnimatePresence, motion} from 'motion/react';
import {Bike, Car, Eye, EyeOff, Lock, Mail, User} from 'lucide-react';
import {UserProfile, UserRole, VehicleType} from '../types/byahero';
import {ByaheroLogoMark, ByaheroWordmarkVector, GoogleGVectorIcon} from './VectorGraphics';
import {LoadingSpinner} from './LoadingSpinner';

interface StoredAccount {
  id: string;
  fullName: string;
  email: string;
  password: string;
  role: UserRole;
  vehicleType: VehicleType;
  plateNumber: string;
  authProvider: 'google' | 'guest';
}

const STORAGE_KEY = 'byahero_accounts_v1';

const PREDEFINED_ACCOUNTS: StoredAccount[] = [
  {
    id: 'acc_passenger_1',
    fullName: 'Juan Dela Cruz',
    email: 'passenger@byahero.app',
    password: 'password123',
    role: 'passenger',
    vehicleType: 'motorcycle',
    plateNumber: '',
    authProvider: 'guest',
  },
  {
    id: 'acc_rider_1',
    fullName: 'Carlo Dizon',
    email: 'rider@byahero.app',
    password: 'password123',
    role: 'rider',
    vehicleType: 'motorcycle',
    plateNumber: 'MTO-8421',
    authProvider: 'guest',
  },
  {
    id: 'acc_both_1',
    fullName: 'Alex Rivera',
    email: 'alex@byahero.app',
    password: 'password123',
    role: 'both',
    vehicleType: 'taxi',
    plateNumber: 'BYH-2026',
    authProvider: 'google',
  },
];

function loadAccounts(): StoredAccount[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return PREDEFINED_ACCOUNTS;
    const parsed = JSON.parse(raw) as StoredAccount[];
    return Array.isArray(parsed) && parsed.length > 0
      ? [...parsed, ...PREDEFINED_ACCOUNTS.filter((p) => !parsed.some((x) => x.email === p.email))]
      : PREDEFINED_ACCOUNTS;
  } catch {
    return PREDEFINED_ACCOUNTS;
  }
}

function saveAccounts(accounts: StoredAccount[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(accounts));
  } catch {
    // ignore storage failures
  }
}

function toUserProfile(account: StoredAccount): UserProfile {
  return {
    id: account.id,
    email: account.email,
    fullName: account.fullName,
    authProvider: account.authProvider,
    role: account.role,
    activeMode: account.role === 'rider' ? 'rider' : 'passenger',
    vehicleType: account.vehicleType,
    plateNumber: account.plateNumber || 'BYH-774',
    isOnline: account.role !== 'passenger',
    totalRides: account.role === 'passenger' ? 4 : 14,
    riderEarningsPhp: account.role === 'passenger' ? 0 : 3840,
    riderIncentivesPhp: account.role === 'passenger' ? 0 : 192,
  };
}

interface AuthGateScreenProps {
  onAuthenticated: (profile: UserProfile) => void;
}

export const AuthGateScreen: React.FC<AuthGateScreenProps> = ({
  onAuthenticated,
}) => {
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [role, setRole] = useState<UserRole>('both');
  const [vehicleType, setVehicleType] = useState<VehicleType>('motorcycle');
  const [plateNumber, setPlateNumber] = useState('BYH-2026');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [submittingKind, setSubmittingKind] = useState<'form' | 'google' | null>(
    null
  );

  const completeAuth = (profile: UserProfile, kind: 'form' | 'google') => {
    setSubmittingKind(kind);
    window.setTimeout(() => {
      setSubmittingKind(null);
      onAuthenticated(profile);
    }, 260);
  };

  const handleLoginSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (submittingKind) return;
    setErrorMsg(null);
    const trimmedEmail = email.trim().toLowerCase();
    if (!trimmedEmail || !password) {
      setErrorMsg('Please enter your email address and password.');
      return;
    }

    const accounts = loadAccounts();
    const matched = accounts.find(
      (a) => a.email.toLowerCase() === trimmedEmail && a.password === password
    );

    if (!matched) {
      setErrorMsg('Incorrect email or password. Please check your credentials or sign up.');
      return;
    }

    completeAuth(toUserProfile(matched), 'form');
  };

  const handleSignUpSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (submittingKind) return;
    setErrorMsg(null);
    const trimmedName = fullName.trim();
    const trimmedEmail = email.trim().toLowerCase();

    if (!trimmedName || !trimmedEmail || password.length < 6) {
      setErrorMsg('Enter your full name, valid email, and a password of at least 6 characters.');
      return;
    }

    const accounts = loadAccounts();
    if (accounts.some((a) => a.email.toLowerCase() === trimmedEmail)) {
      setErrorMsg('An account with this email already exists. Please log in.');
      return;
    }

    const newAccount: StoredAccount = {
      id: `user_${Date.now()}`,
      fullName: trimmedName,
      email: trimmedEmail,
      password,
      role,
      vehicleType,
      plateNumber:
        role === 'passenger' ? '' : plateNumber.trim().toUpperCase() || 'BYH-2026',
      authProvider: 'guest',
    };

    saveAccounts([newAccount, ...accounts]);
    completeAuth(toUserProfile(newAccount), 'form');
  };

  const handleGoogleSignIn = () => {
    if (submittingKind) return;
    setErrorMsg(null);
    const accounts = loadAccounts();
    const existingGoogle = accounts.find((a) => a.email === 'alex@byahero.app');
    completeAuth(toUserProfile(existingGoogle || PREDEFINED_ACCOUNTS[2]), 'google');
  };

  return (
    <div
      className="min-h-[100dvh] w-full text-[#101713] flex flex-col items-center justify-center px-4 py-6 overflow-y-auto relative"
      style={{
        background:
          'radial-gradient(circle at 15% 15%, rgba(15, 82, 43, 0.14) 0%, transparent 45%), radial-gradient(circle at 85% 85%, rgba(16, 185, 129, 0.14) 0%, transparent 45%), #F4F7F5',
      }}
    >
      <motion.div
        initial={{opacity: 0, y: 14, scale: 0.985}}
        animate={{opacity: 1, y: 0, scale: 1}}
        transition={{duration: 0.28, ease: [0.16, 1, 0.3, 1]}}
        className="w-full max-w-md glass-surface rounded-[28px] p-6 sm:p-7 space-y-5"
      >
        {/* Brand Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <ByaheroLogoMark className="w-11 h-11 shrink-0" />
            <div>
              <ByaheroWordmarkVector lightSurface={true} className="h-6 w-auto" />
              <p className="text-xs font-medium text-[#0F522B] mt-1">by HDDEV</p>
            </div>
          </div>
          <span className="text-xs font-semibold text-[#55625A]">
            {mode === 'login' ? 'Sign In' : 'Register'}
          </span>
        </div>

        {/* Log In / Sign Up Switcher */}
        <div className="grid grid-cols-2 gap-1 p-1 rounded-2xl bg-[#EBF1ED]/90 border border-[#0F522B]/10">
          <button
            type="button"
            disabled={submittingKind !== null}
            onClick={() => {
              setMode('login');
              setErrorMsg(null);
            }}
            className={`h-10 rounded-xl text-xs font-bold transition-all smooth-press ${
              mode === 'login'
                ? 'bg-[#0F522B] text-white shadow-sm'
                : 'text-[#55625A] hover:text-[#101713]'
            }`}
          >
            Log In
          </button>
          <button
            type="button"
            disabled={submittingKind !== null}
            onClick={() => {
              setMode('signup');
              setErrorMsg(null);
            }}
            className={`h-10 rounded-xl text-xs font-bold transition-all smooth-press ${
              mode === 'signup'
                ? 'bg-[#0F522B] text-white shadow-sm'
                : 'text-[#55625A] hover:text-[#101713]'
            }`}
          >
            Sign Up
          </button>
        </div>

        {/* Form */}
        <form
          onSubmit={mode === 'login' ? handleLoginSubmit : handleSignUpSubmit}
          className="space-y-3.5"
        >
          <AnimatePresence initial={false}>
            {mode === 'signup' && (
              <motion.div
                key="signup-fullname"
                initial={{opacity: 0, height: 0}}
                animate={{opacity: 1, height: 'auto'}}
                exit={{opacity: 0, height: 0}}
                transition={{duration: 0.2, ease: [0.16, 1, 0.3, 1]}}
                className="overflow-hidden"
              >
                <label htmlFor="auth-name" className="block text-xs font-medium text-[#55625A] mb-1">
                  Full Name
                </label>
                <div className="flex items-center h-11 px-3.5 rounded-xl glass-subtle focus-within:border-[#0F522B]">
                  <User className="w-4 h-4 text-[#0F522B] mr-2.5 shrink-0" />
                  <input
                    id="auth-name"
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Juan Dela Cruz"
                    className="w-full bg-transparent text-sm text-[#101713] placeholder:text-[#86948C] focus:outline-none"
                  />
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          <div>
            <label htmlFor="auth-email" className="block text-xs font-medium text-[#55625A] mb-1">
              Email Address
            </label>
            <div className="flex items-center h-11 px-3.5 rounded-xl glass-subtle focus-within:border-[#0F522B]">
              <Mail className="w-4 h-4 text-[#0F522B] mr-2.5 shrink-0" />
              <input
                id="auth-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                className="w-full bg-transparent text-sm text-[#101713] placeholder:text-[#86948C] focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label htmlFor="auth-password" className="block text-xs font-medium text-[#55625A] mb-1">
              Password
            </label>
            <div className="flex items-center h-11 px-3.5 rounded-xl glass-subtle focus-within:border-[#0F522B]">
              <Lock className="w-4 h-4 text-[#0F522B] mr-2.5 shrink-0" />
              <input
                id="auth-password"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter password"
                className="w-full bg-transparent text-sm text-[#101713] placeholder:text-[#86948C] focus:outline-none"
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                className="min-h-[40px] min-w-[36px] flex items-center justify-center text-[#55625A] hover:text-[#101713]"
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <AnimatePresence initial={false}>
            {mode === 'signup' && (
              <motion.div
                key="signup-role"
                initial={{opacity: 0, height: 0}}
                animate={{opacity: 1, height: 'auto'}}
                exit={{opacity: 0, height: 0}}
                transition={{duration: 0.22, ease: [0.16, 1, 0.3, 1]}}
                className="overflow-hidden space-y-3.5"
              >
                <div>
                  <div className="text-xs font-semibold text-[#0F522B] mb-2">
                    Select Role
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
                        onClick={() => setRole(r.id)}
                        className={`h-10 rounded-xl border text-xs font-semibold transition-all smooth-press ${
                          role === r.id
                            ? 'bg-[#0F522B] border-[#0F522B] text-white shadow-sm'
                            : 'bg-white/70 border-[#0F522B]/15 text-[#101713] hover:border-[#0F522B]/40'
                        }`}
                      >
                        {r.label}
                      </button>
                    ))}
                  </div>
                </div>

                {(role === 'rider' || role === 'both') && (
                  <div className="p-3 rounded-2xl glass-subtle space-y-2.5">
                    <div className="grid grid-cols-2 gap-2">
                      <button
                        type="button"
                        onClick={() => setVehicleType('motorcycle')}
                        className={`h-10 rounded-xl border flex items-center justify-center gap-1.5 text-xs font-semibold transition-all smooth-press ${
                          vehicleType === 'motorcycle'
                            ? 'bg-[#0F522B] border-[#0F522B] text-white'
                            : 'bg-white/80 border-[#0F522B]/15 text-[#101713]'
                        }`}
                      >
                        <Bike className="w-4 h-4" />
                        <span>Motorcycle</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => setVehicleType('taxi')}
                        className={`h-10 rounded-xl border flex items-center justify-center gap-1.5 text-xs font-semibold transition-all smooth-press ${
                          vehicleType === 'taxi'
                            ? 'bg-[#0F522B] border-[#0F522B] text-white'
                            : 'bg-white/80 border-[#0F522B]/15 text-[#101713]'
                        }`}
                      >
                        <Car className="w-4 h-4" />
                        <span>Taxi</span>
                      </button>
                    </div>
                    <input
                      type="text"
                      value={plateNumber}
                      onChange={(e) => setPlateNumber(e.target.value.toUpperCase())}
                      placeholder="Plate Number (e.g. BYH-2026)"
                      aria-label="Vehicle plate number"
                      className="w-full h-10 px-3 rounded-xl bg-white/90 border border-[#0F522B]/15 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
                    />
                  </div>
                )}
              </motion.div>
            )}
          </AnimatePresence>

          <AnimatePresence>
            {errorMsg && (
              <motion.p
                initial={{opacity: 0, y: -4}}
                animate={{opacity: 1, y: 0}}
                exit={{opacity: 0, y: -4}}
                transition={{duration: 0.18}}
                className="text-xs text-red-700 bg-red-50 border border-red-200 rounded-xl px-3 py-2.5"
              >
                {errorMsg}
              </motion.p>
            )}
          </AnimatePresence>

          <button
            type="submit"
            disabled={submittingKind !== null}
            className="w-full h-12 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] disabled:opacity-75 text-white text-sm font-bold shadow-md transition-colors flex items-center justify-center gap-2 smooth-press"
          >
            {submittingKind === 'form' ? (
              <>
                <LoadingSpinner size="xs" tone="white" />
                <span>{mode === 'login' ? 'Signing In...' : 'Creating Account...'}</span>
              </>
            ) : mode === 'login' ? (
              'Log In'
            ) : (
              'Create Account'
            )}
          </button>
        </form>

        <div className="flex items-center gap-3">
          <div className="h-px flex-1 bg-[#0F522B]/15" />
          <span className="text-[11px] font-medium text-[#55625A]">or</span>
          <div className="h-px flex-1 bg-[#0F522B]/15" />
        </div>

        {/* Google Sign-In */}
        <button
          type="button"
          disabled={submittingKind !== null}
          onClick={handleGoogleSignIn}
          className="w-full h-11 rounded-xl bg-white/95 hover:bg-white disabled:opacity-75 border border-[#0F522B]/15 text-[#101713] text-xs font-bold flex items-center justify-center gap-2.5 shadow-sm transition-colors smooth-press"
        >
          {submittingKind === 'google' ? (
            <>
              <LoadingSpinner size="xs" tone="green" />
              <span>Connecting Google Account...</span>
            </>
          ) : (
            <>
              <GoogleGVectorIcon className="w-4 h-4" />
              <span>Continue with Google</span>
            </>
          )}
        </button>
      </motion.div>
    </div>
  );
};
