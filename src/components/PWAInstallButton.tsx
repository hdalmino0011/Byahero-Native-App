import React, {useState} from 'react';
import {Download} from 'lucide-react';
import {usePWAInstall} from '../hooks/usePWAInstall';

export const PWAInstallButton: React.FC = () => {
  const {isInstallable, isInstalled, isIOS, install} = usePWAInstall();
  const [showIOSGuide, setShowIOSGuide] = useState(false);

  if (isInstalled) {
    return null;
  }

  if (isInstallable) {
    return (
      <button
        type="button"
        onClick={() => void install()}
        className="w-full h-11 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-bold flex items-center justify-center gap-2 shadow-sm transition-colors"
      >
        <Download className="w-4 h-4" />
        <span>Install BYAHERO App on Phone</span>
      </button>
    );
  }

  if (isIOS) {
    return (
      <>
        <button
          type="button"
          onClick={() => setShowIOSGuide(true)}
          className="w-full h-11 rounded-xl glass-surface hover:bg-white text-xs font-bold text-[#101713] flex items-center justify-center gap-2 transition-colors"
        >
          <Download className="w-4 h-4 text-[#0F522B]" />
          <span>Add BYAHERO Icon to Home Screen</span>
        </button>

        {showIOSGuide && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#101713]/50 backdrop-blur-xs p-4">
            <div className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-2xl space-y-3 border border-[#0F522B]/15">
              <h3 className="text-sm font-bold text-[#101713]">Install BYAHERO on iPhone</h3>
              <p className="text-xs text-[#55625A] leading-relaxed">
                1. Tap the <strong>Share</strong> button in Safari.<br />
                2. Scroll down and tap <strong>Add to Home Screen</strong>.
              </p>
              <button
                type="button"
                onClick={() => setShowIOSGuide(false)}
                className="w-full h-10 rounded-xl bg-[#0F522B] text-white text-xs font-bold"
              >
                Got it
              </button>
            </div>
          </div>
        )}
      </>
    );
  }

  return null;
};
