import React, {useState} from 'react';
import {
  Banknote,
  Bike,
  Bookmark,
  CheckCircle2,
  CreditCard,
  Headphones,
  Info,
  LogOut,
  MapPin,
  Navigation,
  Plus,
  ShieldCheck,
  Smartphone,
  Tag,
  Trash2,
  User,
} from 'lucide-react';
import {ByaheroWordmarkVector} from './VectorGraphics';
import {
  ActiveMode,
  FavoriteDestination,
  LocationStop,
  PromoCodeItem,
  SavedPaymentMethod,
  SettingsSection,
  SupportTicket,
  UserProfile,
  UserRole,
} from '../types/byahero';

interface SettingsPanelProps {
  profile: UserProfile;
  activeSection: SettingsSection;
  favorites: FavoriteDestination[];
  paymentMethods: SavedPaymentMethod[];
  selectedPaymentId: string;
  promoCodes: PromoCodeItem[];
  appliedPromoCode: string | null;
  onSelectSection: (section: SettingsSection) => void;
  onOpenAuthModal: () => void;
  onSwitchRole: (role: UserRole) => void;
  onSwitchActiveMode: (mode: ActiveMode) => void;
  onSelectFavoriteAsDestination: (stop: LocationStop) => void;
  onAddFavorite: (fav: FavoriteDestination) => void;
  onRemoveFavorite: (id: string) => void;
  onSelectPaymentMethod: (id: string) => void;
  onSavePaymentMethod: (method: SavedPaymentMethod) => void;
  onRemovePaymentMethod: (id: string) => void;
  onApplyPromoCode: (code: string | null) => void;
  onLogout: () => void;
}

export const SettingsPanel: React.FC<SettingsPanelProps> = ({
  profile,
  activeSection,
  favorites,
  paymentMethods,
  selectedPaymentId,
  promoCodes,
  appliedPromoCode,
  onSelectSection,
  onOpenAuthModal,
  onSwitchRole,
  onSwitchActiveMode,
  onSelectFavoriteAsDestination,
  onAddFavorite,
  onRemoveFavorite,
  onSelectPaymentMethod,
  onSavePaymentMethod,
  onRemovePaymentMethod,
  onApplyPromoCode,
  onLogout,
}) => {
  const [newFavLabel, setNewFavLabel] = useState('');
  const [newFavAddress, setNewFavAddress] = useState('');

  const [paymentModalType, setPaymentModalType] = useState<'gcash' | 'card' | null>(
    null
  );
  const [gcashNumber, setGcashNumber] = useState('');
  const [gcashName, setGcashName] = useState('');
  const [cardHolder, setCardHolder] = useState('');
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardCvv, setCardCvv] = useState('');
  const [paymentFeedback, setPaymentFeedback] = useState<string | null>(null);

  const [promoInput, setPromoInput] = useState('');
  const [promoMessage, setPromoMessage] = useState<string | null>(null);

  const [supportCategory, setSupportCategory] = useState('Trip & Fare Assistance');
  const [supportMessage, setSupportMessage] = useState('');
  const [tickets, setTickets] = useState<SupportTicket[]>([
    {
      id: 'TKT-2041',
      category: 'Payment & GCash Inquiry',
      message: 'Verified GCash instant settlement for weekend trips.',
      status: 'resolved',
      createdAt: 'Resolved · 2d ago',
    },
  ]);
  const [ticketSubmittedNotice, setTicketSubmittedNotice] = useState<string | null>(
    null
  );

  const sections: Array<{
    id: SettingsSection;
    label: string;
    icon: React.ComponentType<{className?: string}>;
  }> = [
    {id: 'account', label: 'Account', icon: User},
    {id: 'favorites', label: 'Favorites', icon: Bookmark},
    {id: 'payments', label: 'Payments', icon: CreditCard},
    {id: 'promos', label: 'Promo Codes', icon: Tag},
    {id: 'support', label: 'Support', icon: Headphones},
    {id: 'about', label: 'About', icon: Info},
  ];

  const handleAddFavoriteSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const label = newFavLabel.trim();
    if (!label) return;
    const address = newFavAddress.trim() || `${label}, Metro Area`;
    onAddFavorite({
      id: `fav_${Date.now()}`,
      label,
      address,
      lat: 10.3181 + (Math.random() - 0.5) * 0.02,
      lng: 123.9049 + (Math.random() - 0.5) * 0.02,
      ordersCount: 1,
    });
    setNewFavLabel('');
    setNewFavAddress('');
  };

  const handleConnectGcash = (e: React.FormEvent) => {
    e.preventDefault();
    const digits = gcashNumber.replace(/\D/g, '');
    if (digits.length < 10) {
      setPaymentFeedback('Please enter a valid 11-digit GCash mobile number.');
      return;
    }
    const cleanName = gcashName.trim() || profile.fullName;
    const last4 = digits.slice(-4);
    onSavePaymentMethod({
      id: `pm_gcash_${Date.now()}`,
      type: 'gcash',
      label: `GCash (•••• ${last4})`,
      subtitle: `${cleanName} · Connected GCash`,
      accountNumberOrLast4: last4,
      holderName: cleanName,
      isDefault: true,
    });
    setGcashNumber('');
    setGcashName('');
    setPaymentModalType(null);
    setPaymentFeedback('GCash account connected and saved as default payment.');
  };

  const handleRegisterCard = (e: React.FormEvent) => {
    e.preventDefault();
    const digits = cardNumber.replace(/\D/g, '');
    if (digits.length < 12) {
      setPaymentFeedback('Please enter a valid 16-digit card number.');
      return;
    }
    if (!cardExpiry.trim() || cardCvv.replace(/\D/g, '').length < 3) {
      setPaymentFeedback('Please enter expiry date (MM/YY) and 3-digit CVV.');
      return;
    }
    const cleanHolder = cardHolder.trim() || profile.fullName;
    const last4 = digits.slice(-4);
    const brand = digits.startsWith('4')
      ? 'Visa'
      : digits.startsWith('5')
      ? 'Mastercard'
      : 'Card';
    onSavePaymentMethod({
      id: `pm_card_${Date.now()}`,
      type: 'card',
      label: `${brand} •••• ${last4}`,
      subtitle: `${cleanHolder} · Exp ${cardExpiry.trim()}`,
      accountNumberOrLast4: last4,
      holderName: cleanHolder,
      expiry: cardExpiry.trim(),
      isDefault: true,
    });
    setCardHolder('');
    setCardNumber('');
    setCardExpiry('');
    setCardCvv('');
    setPaymentModalType(null);
    setPaymentFeedback(`${brand} ending in ${last4} registered and saved.`);
  };

  const handlePromoSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const clean = promoInput.trim().toUpperCase();
    if (!clean) return;
    const found = promoCodes.find((p) => p.code.toUpperCase() === clean);
    if (found) {
      onApplyPromoCode(found.code);
      setPromoMessage(`Promo code ${found.code} (${found.title}) applied!`);
      setPromoInput('');
    } else {
      setPromoMessage(`Code "${clean}" is not valid. Try BYAHERO10 or SATPROMO.`);
    }
  };

  const handleSupportSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const msg = supportMessage.trim();
    if (!msg) return;
    const newTicket: SupportTicket = {
      id: `TKT-${Math.floor(3000 + Math.random() * 6000)}`,
      category: supportCategory,
      message: msg,
      status: 'open',
      createdAt: 'Just now',
    };
    setTickets((prev) => [newTicket, ...prev]);
    setSupportMessage('');
    setTicketSubmittedNotice(
      `Support ticket ${newTicket.id} submitted. Our 24/7 dispatch support team will assist you shortly.`
    );
  };

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-3 gap-1.5 p-1.5 rounded-2xl bg-[#EBF1ED]/90 border border-[#0F522B]/10">
        {sections.map((sec) => {
          const Icon = sec.icon;
          const active = activeSection === sec.id;
          return (
            <button
              key={sec.id}
              type="button"
              onClick={() => onSelectSection(sec.id)}
              className={`h-9 px-2 rounded-xl text-[11px] font-bold flex items-center justify-center gap-1.5 transition-all ${
                active
                  ? 'bg-[#0F522B] text-white shadow-sm'
                  : 'text-[#55625A] hover:text-[#101713]'
              }`}
            >
              <Icon className="w-3.5 h-3.5 shrink-0" />
              <span className="truncate">{sec.label}</span>
            </button>
          );
        })}
      </div>

      {activeSection === 'account' && (
        <div className="space-y-3.5">
          <div className="p-4 rounded-2xl glass-green text-white space-y-3">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                <div className="w-11 h-11 rounded-full bg-white/15 border border-white/25 flex items-center justify-center text-white font-extrabold text-sm shrink-0">
                  {profile.fullName
                    .split(' ')
                    .map((n) => n[0])
                    .join('')
                    .slice(0, 2)
                    .toUpperCase()}
                </div>
                <div className="min-w-0">
                  <h3 className="text-sm font-bold text-white truncate">
                    {profile.fullName}
                  </h3>
                  <div className="text-xs text-white/80 truncate">{profile.email}</div>
                  <div className="inline-flex items-center gap-1 mt-1 text-[11px] text-emerald-200 font-semibold">
                    <ShieldCheck className="w-3.5 h-3.5" />
                    <span>
                      {profile.authProvider === 'google'
                        ? 'Google Connected'
                        : 'BYAHERO Member'}
                    </span>
                  </div>
                </div>
              </div>

              <button
                type="button"
                onClick={onOpenAuthModal}
                className="min-h-[36px] px-3 rounded-xl bg-white text-[#0F522B] hover:bg-emerald-50 text-xs font-bold whitespace-nowrap shrink-0 transition-colors"
              >
                Edit Profile
              </button>
            </div>
          </div>

          <div className="p-4 rounded-2xl glass-surface space-y-3">
            <div className="text-xs font-bold text-[#101713]">Account Role</div>
            <div className="grid grid-cols-3 gap-2">
              {(['passenger', 'rider', 'both'] as UserRole[]).map((r) => {
                const active = profile.role === r;
                return (
                  <button
                    key={r}
                    type="button"
                    onClick={() => onSwitchRole(r)}
                    className={`min-h-[42px] px-2.5 py-2 rounded-xl border text-xs font-semibold capitalize transition-all ${
                      active
                        ? 'bg-[#0F522B] border-[#0F522B] text-white shadow-sm'
                        : 'bg-white/70 border-[#0F522B]/15 text-[#55625A] hover:text-[#101713]'
                    }`}
                  >
                    {r === 'both' ? 'Both Roles' : r}
                  </button>
                );
              })}
            </div>

            {profile.role === 'both' && (
              <div className="pt-2 border-t border-[#0F522B]/10 space-y-2">
                <div className="text-[11px] text-[#55625A]">Active Operating Mode</div>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => onSwitchActiveMode('passenger')}
                    className={`min-h-[40px] px-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
                      profile.activeMode === 'passenger'
                        ? 'bg-[#0F522B] border-[#0F522B] text-white'
                        : 'bg-white/70 border-[#0F522B]/15 text-[#55625A]'
                    }`}
                  >
                    <User className="w-3.5 h-3.5" />
                    <span>Passenger Mode</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => onSwitchActiveMode('rider')}
                    className={`min-h-[40px] px-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
                      profile.activeMode === 'rider'
                        ? 'bg-[#0F522B] border-[#0F522B] text-white'
                        : 'bg-white/70 border-[#0F522B]/15 text-[#55625A]'
                    }`}
                  >
                    <Bike className="w-3.5 h-3.5" />
                    <span>Rider Mode</span>
                  </button>
                </div>
              </div>
            )}
          </div>

          <div className="p-4 rounded-2xl glass-surface space-y-2 text-xs">
            <div className="flex justify-between py-1 border-b border-[#0F522B]/10">
              <span className="text-[#55625A]">Registered Vehicle</span>
              <span className="font-bold text-[#101713] capitalize">
                {profile.vehicleType} · {profile.plateNumber}
              </span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-[#55625A]">Completed Trips</span>
              <span className="font-bold text-[#0F522B]">{profile.totalRides} rides</span>
            </div>
          </div>

          <button
            type="button"
            onClick={onLogout}
            className="w-full h-12 rounded-2xl bg-red-50 hover:bg-red-100 border border-red-200 text-red-700 text-xs font-bold flex items-center justify-center gap-2 transition-colors"
          >
            <LogOut className="w-4 h-4" />
            <span>Log Out of BYAHERO</span>
          </button>
        </div>
      )}

      {activeSection === 'favorites' && (
        <div className="space-y-3.5">
          <div className="p-4 rounded-2xl glass-green text-white">
            <div className="text-xs font-bold">Favorites · Always Ordered Destinations</div>
            <p className="text-[11px] text-emerald-100 mt-0.5">
              Tap Ride Here on any saved destination to set it on your booking panel immediately.
            </p>
          </div>

          <form
            onSubmit={handleAddFavoriteSubmit}
            className="p-3.5 rounded-2xl glass-surface space-y-2.5"
          >
            <div className="text-xs font-bold text-[#101713]">
              Add Favorite Destination
            </div>
            <input
              type="text"
              value={newFavLabel}
              onChange={(e) => setNewFavLabel(e.target.value)}
              placeholder="Place Name (e.g. SM Seaside City Cebu)"
              className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
            />
            <input
              type="text"
              value={newFavAddress}
              onChange={(e) => setNewFavAddress(e.target.value)}
              placeholder="Street or City Address"
              className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
            />
            <button
              type="submit"
              className="w-full h-9 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-bold inline-flex items-center justify-center gap-1.5 transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Save to Favorites</span>
            </button>
          </form>

          <div className="space-y-2.5">
            {favorites.map((fav) => (
              <div
                key={fav.id}
                className="p-3.5 rounded-2xl glass-surface flex items-center justify-between gap-2.5"
              >
                <div className="flex items-start gap-2.5 min-w-0">
                  <MapPin className="w-4 h-4 text-[#0F522B] mt-0.5 shrink-0" />
                  <div className="min-w-0">
                    <div className="text-xs font-bold text-[#101713] truncate">
                      {fav.label}
                    </div>
                    <div className="text-[11px] text-[#55625A] truncate">
                      {fav.address}
                    </div>
                    <div className="text-[10px] font-semibold text-[#0F522B] mt-0.5">
                      Ordered {fav.ordersCount} times
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-1.5 shrink-0">
                  <button
                    type="button"
                    onClick={() =>
                      onSelectFavoriteAsDestination({
                        label: fav.label,
                        address: fav.address,
                        lat: fav.lat,
                        lng: fav.lng,
                      })
                    }
                    className="h-8 px-3 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-[11px] font-bold inline-flex items-center gap-1"
                  >
                    <Navigation className="w-3 h-3" />
                    <span>Ride Here</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => onRemoveFavorite(fav.id)}
                    aria-label="Remove favorite"
                    className="h-8 w-8 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 flex items-center justify-center"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeSection === 'payments' && (
        <div className="space-y-3.5">
          <div className="p-4 rounded-2xl glass-green text-white">
            <div className="text-xs font-bold">Saved Payment Methods</div>
            <p className="text-[11px] text-emerald-100 mt-0.5">
              Choose Cash, connect your GCash account, or register a Credit/Debit Card for seamless booking.
            </p>
          </div>

          {paymentFeedback && (
            <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-xs font-semibold text-[#0F522B] flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>{paymentFeedback}</span>
            </div>
          )}

          <div className="space-y-2.5">
            {paymentMethods.map((method) => {
              const isSelected = method.id === selectedPaymentId;
              return (
                <div
                  key={method.id}
                  className={`p-3.5 rounded-2xl border transition-all flex items-center justify-between gap-2.5 ${
                    isSelected
                      ? 'bg-[#0F522B]/8 border-[#0F522B]'
                      : 'glass-surface'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => onSelectPaymentMethod(method.id)}
                    className="flex-1 flex items-center gap-3 text-left min-w-0"
                  >
                    <div className="w-9 h-9 rounded-xl bg-[#0F522B] text-white flex items-center justify-center shrink-0">
                      {method.type === 'cash' ? (
                        <Banknote className="w-4 h-4" />
                      ) : method.type === 'gcash' ? (
                        <Smartphone className="w-4 h-4" />
                      ) : (
                        <CreditCard className="w-4 h-4" />
                      )}
                    </div>
                    <div className="min-w-0">
                      <div className="text-xs font-bold text-[#101713] truncate">
                        {method.label}
                      </div>
                      <div className="text-[11px] text-[#55625A] truncate">
                        {method.subtitle}
                      </div>
                    </div>
                  </button>

                  <div className="flex items-center gap-1.5 shrink-0">
                    <button
                      type="button"
                      onClick={() => onSelectPaymentMethod(method.id)}
                      className={`h-7 px-2.5 rounded-lg text-[10px] font-bold ${
                        isSelected
                          ? 'bg-[#0F522B] text-white'
                          : 'bg-[#EBF1ED] text-[#0F522B]'
                      }`}
                    >
                      {isSelected ? 'Default' : 'Select'}
                    </button>
                    {method.type !== 'cash' && (
                      <button
                        type="button"
                        onClick={() => onRemovePaymentMethod(method.id)}
                        aria-label="Remove payment method"
                        className="h-7 w-7 rounded-lg bg-red-50 text-red-600 flex items-center justify-center hover:bg-red-100"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="grid grid-cols-2 gap-2.5">
            <button
              type="button"
              onClick={() => {
                setPaymentFeedback(null);
                setPaymentModalType(paymentModalType === 'gcash' ? null : 'gcash');
              }}
              className="h-10 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-bold inline-flex items-center justify-center gap-1.5 transition-colors"
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>Connect GCash</span>
            </button>
            <button
              type="button"
              onClick={() => {
                setPaymentFeedback(null);
                setPaymentModalType(paymentModalType === 'card' ? null : 'card');
              }}
              className="h-10 rounded-xl glass-surface hover:bg-white text-[#0F522B] border border-[#0F522B]/25 text-xs font-bold inline-flex items-center justify-center gap-1.5 transition-colors"
            >
              <CreditCard className="w-3.5 h-3.5" />
              <span>Register Card</span>
            </button>
          </div>

          {paymentModalType === 'gcash' && (
            <form
              onSubmit={handleConnectGcash}
              className="p-4 rounded-2xl glass-surface space-y-2.5"
            >
              <div className="text-xs font-bold text-[#0F522B]">
                Connect GCash Account
              </div>
              <input
                type="tel"
                value={gcashNumber}
                onChange={(e) => setGcashNumber(e.target.value)}
                placeholder="GCash Mobile Number (09XX XXX XXXX)"
                className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
              />
              <input
                type="text"
                value={gcashName}
                onChange={(e) => setGcashName(e.target.value)}
                placeholder="Registered GCash Account Name"
                className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
              />
              <button
                type="submit"
                className="w-full h-9 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-bold"
              >
                Save Connected GCash
              </button>
            </form>
          )}

          {paymentModalType === 'card' && (
            <form
              onSubmit={handleRegisterCard}
              className="p-4 rounded-2xl glass-surface space-y-2.5"
            >
              <div className="text-xs font-bold text-[#0F522B]">
                Register Credit or Debit Card
              </div>
              <input
                type="text"
                value={cardHolder}
                onChange={(e) => setCardHolder(e.target.value)}
                placeholder="Cardholder Full Name"
                className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
              />
              <input
                type="text"
                value={cardNumber}
                onChange={(e) => setCardNumber(e.target.value)}
                placeholder="16-Digit Card Number"
                maxLength={19}
                className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
              />
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="text"
                  value={cardExpiry}
                  onChange={(e) => setCardExpiry(e.target.value)}
                  placeholder="Expiry MM/YY"
                  maxLength={5}
                  className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
                />
                <input
                  type="password"
                  value={cardCvv}
                  onChange={(e) => setCardCvv(e.target.value)}
                  placeholder="CVV"
                  maxLength={4}
                  className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
                />
              </div>
              <button
                type="submit"
                className="w-full h-9 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-bold"
              >
                Save Registered Card
              </button>
            </form>
          )}
        </div>
      )}

      {activeSection === 'promos' && (
        <div className="space-y-3.5">
          <form
            onSubmit={handlePromoSubmit}
            className="p-3.5 rounded-2xl glass-surface space-y-2.5"
          >
            <div className="text-xs font-bold text-[#101713]">Redeem Promo Code</div>
            <div className="flex gap-2">
              <input
                type="text"
                value={promoInput}
                onChange={(e) => setPromoInput(e.target.value)}
                placeholder="Enter code (e.g. BYAHERO10)"
                className="flex-1 h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs font-semibold uppercase text-[#101713] focus:outline-none focus:border-[#0F522B]"
              />
              <button
                type="submit"
                className="px-4 h-9 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-bold"
              >
                Apply
              </button>
            </div>
            {promoMessage && (
              <div className="text-[11px] font-semibold text-[#0F522B]">
                {promoMessage}
              </div>
            )}
          </form>

          <div className="space-y-2.5">
            {promoCodes.map((promo) => {
              const isApplied = appliedPromoCode === promo.code;
              return (
                <div
                  key={promo.code}
                  className="p-3.5 rounded-2xl glass-surface flex items-center justify-between gap-3"
                >
                  <div className="min-w-0">
                    <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-[#EBF1ED] text-[#0F522B] text-[10px] font-extrabold">
                      <Tag className="w-3 h-3" />
                      <span>{promo.code}</span>
                    </div>
                    <div className="text-xs font-bold text-[#101713] mt-1">
                      {promo.title}
                    </div>
                    <div className="text-[11px] text-[#55625A]">{promo.description}</div>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      onApplyPromoCode(isApplied ? null : promo.code);
                      setPromoMessage(
                        isApplied
                          ? `Removed promo code ${promo.code}.`
                          : `Promo code ${promo.code} applied to your next booking!`
                      );
                    }}
                    className={`h-8 px-3 rounded-xl text-[11px] font-bold shrink-0 transition-colors ${
                      isApplied
                        ? 'bg-[#0F522B] text-white'
                        : 'bg-[#EBF1ED] hover:bg-[#DCE7E0] text-[#0F522B]'
                    }`}
                  >
                    {isApplied ? 'Applied' : 'Use Code'}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {activeSection === 'support' && (
        <div className="space-y-3.5">
          <div className="p-4 rounded-2xl glass-green text-white space-y-1">
            <div className="text-xs font-bold">BYAHERO 24/7 Support Service</div>
            <p className="text-[11px] text-emerald-100">
              Live dispatch assistance for passengers and partner riders. Hotline: +63 (032) 888-BYAH
            </p>
          </div>

          <form
            onSubmit={handleSupportSubmit}
            className="p-3.5 rounded-2xl glass-surface space-y-2.5"
          >
            <div className="text-xs font-bold text-[#101713]">Submit Support Request</div>
            <div className="grid grid-cols-2 gap-1.5">
              {[
                'Trip & Fare Assistance',
                'Payment & GCash Inquiry',
                'Lost Item on Ride',
                'Rider & Safety Report',
              ].map((cat) => (
                <button
                  key={cat}
                  type="button"
                  onClick={() => setSupportCategory(cat)}
                  className={`h-8 px-2 rounded-lg text-[10px] font-bold truncate transition-colors ${
                    supportCategory === cat
                      ? 'bg-[#0F522B] text-white'
                      : 'bg-[#EBF1ED] text-[#55625A]'
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>
            <textarea
              value={supportMessage}
              onChange={(e) => setSupportMessage(e.target.value)}
              rows={3}
              placeholder="Describe how our support team can help you..."
              className="w-full p-2.5 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
            />
            {ticketSubmittedNotice && (
              <div className="text-[11px] font-semibold text-[#0F522B]">
                {ticketSubmittedNotice}
              </div>
            )}
            <button
              type="submit"
              className="w-full h-9 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-bold"
            >
              Send to Support Team
            </button>
          </form>

          <div className="space-y-2">
            <div className="text-xs font-bold text-[#101713]">Recent Support Tickets</div>
            {tickets.map((t) => (
              <div key={t.id} className="p-3 rounded-2xl glass-surface space-y-1">
                <div className="flex items-center justify-between text-[11px]">
                  <span className="font-bold text-[#0F522B]">
                    {t.id} · {t.category}
                  </span>
                  <span className="text-[#55625A]">{t.createdAt}</span>
                </div>
                <p className="text-xs text-[#101713]">{t.message}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeSection === 'about' && (
        <div className="space-y-3.5">
          <div className="p-4 rounded-2xl glass-green text-white space-y-2">
            <div className="flex items-center gap-2.5">
              <ByaheroWordmarkVector lightSurface={false} className="h-5 w-auto" />
              <span className="text-xs font-bold text-emerald-200">
                by HDDEV · v2.6.0 (2026)
              </span>
            </div>
            <p className="text-xs text-emerald-100 leading-relaxed">
              Dual-role motorcycle and taxi ride-hailing & nearby discovery platform engineered for commuters and partner riders.
            </p>
          </div>

          <div className="p-4 rounded-2xl glass-surface space-y-2.5 text-xs">
            <div className="font-bold text-[#101713]">Platform Highlights & Fare Policy</div>
            <div className="flex justify-between py-1.5 border-b border-[#0F522B]/10">
              <span className="text-[#55625A]">BYAHERO Moto Base Fare</span>
              <span className="font-bold text-[#101713]">P40.00 + P12/km</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-[#0F522B]/10">
              <span className="text-[#55625A]">BYAHERO Cab Base Fare</span>
              <span className="font-bold text-[#101713]">P60.00 + P18/km</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-[#0F522B]/10">
              <span className="text-[#55625A]">Saturday Passenger Promo</span>
              <span className="font-bold text-[#0F522B]">10% Off All Rides</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-[#0F522B]/10">
              <span className="text-[#55625A]">Rider Partner Bonus</span>
              <span className="font-bold text-[#0F522B]">+5% Extra Incentive</span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-[#55625A]">Supported Payments</span>
              <span className="font-bold text-[#101713]">Cash, GCash, Credit/Debit</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
