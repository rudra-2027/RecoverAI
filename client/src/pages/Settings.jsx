import React, { useEffect, useState } from 'react';
import { ButtonLoader, SkeletonBlock } from '../components/LoadingSkeleton';
import {
  fetchMerchants,
  fetchRecoverySettings,
  fetchSystemStatus,
  regenerateApiKey,
  updateMerchantSettings,
  updateRecoverySettings,
} from '../services/api';

export default function Settings() {
  const [activeTab, setActiveTab] = useState('recovery'); // recovery, merchant, keys, status

  // Form states
  const [maxRetries, setMaxRetries] = useState(3);
  const [threshold, setThreshold] = useState(50.00);
  const [aiRouting, setAiRouting] = useState(true);

  // Merchant config states
  const [merchants, setMerchants] = useState([]);

  // Keys states
  const [apiKey, setApiKey] = useState('API key is managed by backend configuration');
  const [keyHidden, setKeyHidden] = useState(true);
  const [systemStatus, setSystemStatus] = useState(null);
  const [isLoadingSettings, setIsLoadingSettings] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isDiscarding, setIsDiscarding] = useState(false);
  const [isRegeneratingKey, setIsRegeneratingKey] = useState(false);
  const [togglingMerchantId, setTogglingMerchantId] = useState('');

  // Toast
  const [toast, setToast] = useState('');

  const loadSettings = () => {
    setIsLoadingSettings(true);
    return Promise.allSettled([fetchMerchants(), fetchRecoverySettings(), fetchSystemStatus()])
      .then(([merchantResult, settingsResult, statusResult]) => {
        if (merchantResult.status === 'fulfilled') {
          const data = merchantResult.value;
          setMerchants(data.map((merchant) => ({
            id: merchant.merchantId,
            name: merchant.merchantName,
            enabled: merchant.active,
          })));
        }

        if (settingsResult.status === 'fulfilled') {
          setMaxRetries(settingsResult.value.maxRetries);
          setThreshold(settingsResult.value.escalateBelowProbability);
        }

        if (statusResult.status === 'fulfilled') {
          setSystemStatus(statusResult.value);
        }
      })
      .catch(() => showToast('Could not load settings from backend.'))
      .finally(() => setIsLoadingSettings(false));
  };

  useEffect(() => {
    loadSettings();
  }, []);

  const showToast = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(''), 3000);
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      await updateRecoverySettings({
        maxRetries,
        escalateBelowProbability: Number(threshold),
      });
      showToast('Recovery configuration saved.');
    } catch {
      showToast('Could not save recovery configuration.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDiscard = async () => {
    setIsDiscarding(true);
    await loadSettings();
    setIsDiscarding(false);
    showToast('Changes discarded.');
  };

  const toggleMerchant = async (id) => {
    const merchant = merchants.find((m) => m.id === id);
    const enabled = !merchant?.enabled;
    const previousMerchants = merchants;
    setTogglingMerchantId(id);
    setMerchants(previousMerchants.map(m => m.id === id ? { ...m, enabled } : m));
    try {
      await updateMerchantSettings(id, { active: enabled });
      showToast(`Merchant ${enabled ? 'enabled' : 'disabled'}.`);
    } catch {
      setMerchants(previousMerchants);
      showToast('Could not update merchant setting.');
    } finally {
      setTogglingMerchantId('');
    }
  };

  const copyApiKey = () => {
    navigator.clipboard.writeText(apiKey);
    showToast('API Key copied to clipboard!');
  };

  const handleRegenerateApiKey = async () => {
    setIsRegeneratingKey(true);
    try {
      const result = await regenerateApiKey();
      setApiKey(result.apiKey);
      setKeyHidden(false);
      showToast(result.message || 'API key regenerated.');
    } catch {
      showToast('Could not regenerate API key.');
    } finally {
      setIsRegeneratingKey(false);
    }
  };

  return (
    <div className="flex-1 overflow-y-auto p-8 bg-[#f0f2f5] pb-20">
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 bg-inverse-surface text-inverse-on-surface px-6 py-3 rounded-lg shadow-level2 border border-outline-variant flex items-center gap-2 border-primary/20 animate-fade-in">
          <span className="material-symbols-outlined fill text-green-500">check_circle</span>
          <span className="font-semibold">{toast}</span>
        </div>
      )}

      <div className="max-w-5xl mx-auto space-y-8">
        {/* Header Section */}
        <div>
          <h2 className="font-headline-lg text-headline-lg text-on-surface">System Configuration</h2>
          <p className="font-body-md text-body-md text-on-surface-variant mt-2">Manage your automated recovery rules, merchant settings, and API access.</p>
        </div>

        {/* Bento Grid Layout for Settings */}
        <div className="grid grid-cols-12 gap-6 items-start">
          
          {/* Settings Navigation (Left Column) */}
          <div className="col-span-12 md:col-span-3 space-y-2">
            <nav className="flex flex-col gap-1 md:sticky md:top-8">
              <button 
                onClick={() => setActiveTab('recovery')}
                className={`flex items-center gap-3 px-4 py-3 rounded-lg font-title-md text-title-md transition-all text-left ${
                  activeTab === 'recovery' 
                    ? 'bg-white shadow-sm border border-[#f0f0f0] text-primary' 
                    : 'text-on-surface-variant hover:bg-white hover:shadow-sm'
                }`}
              >
                <span className="material-symbols-outlined" style={{ fontVariationSettings: activeTab === 'recovery' ? "'FILL' 1" : "'FILL' 0" }}>cycle</span>
                Recovery Config
              </button>
              
              <button 
                onClick={() => setActiveTab('merchant')}
                className={`flex items-center gap-3 px-4 py-3 rounded-lg font-title-md text-title-md transition-all text-left ${
                  activeTab === 'merchant' 
                    ? 'bg-white shadow-sm border border-[#f0f0f0] text-primary' 
                    : 'text-on-surface-variant hover:bg-white hover:shadow-sm'
                }`}
              >
                <span className="material-symbols-outlined" style={{ fontVariationSettings: activeTab === 'merchant' ? "'FILL' 1" : "'FILL' 0" }}>storefront</span>
                Merchant Config
              </button>
              
              <button 
                onClick={() => setActiveTab('keys')}
                className={`flex items-center gap-3 px-4 py-3 rounded-lg font-title-md text-title-md transition-all text-left ${
                  activeTab === 'keys' 
                    ? 'bg-white shadow-sm border border-[#f0f0f0] text-primary' 
                    : 'text-on-surface-variant hover:bg-white hover:shadow-sm'
                }`}
              >
                <span className="material-symbols-outlined" style={{ fontVariationSettings: activeTab === 'keys' ? "'FILL' 1" : "'FILL' 0" }}>key</span>
                API Keys
              </button>
              
              <button 
                onClick={() => setActiveTab('status')}
                className={`flex items-center gap-3 px-4 py-3 rounded-lg font-title-md text-title-md transition-all text-left ${
                  activeTab === 'status' 
                    ? 'bg-white shadow-sm border border-[#f0f0f0] text-primary' 
                    : 'text-on-surface-variant hover:bg-white hover:shadow-sm'
                }`}
              >
                <span className="material-symbols-outlined" style={{ fontVariationSettings: activeTab === 'status' ? "'FILL' 1" : "'FILL' 0" }}>dns</span>
                System Status
              </button>
            </nav>
          </div>

          {/* Settings Content (Right Column) */}
          <div className="col-span-12 md:col-span-9 space-y-6">
            
            {/* 1. Recovery Config Panel */}
            {activeTab === 'recovery' && (
              <div className="bg-white rounded-xl shadow-level1 border border-[#f0f0f0] p-6 relative overflow-hidden space-y-6">
                <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-[#f9f0ff] to-transparent opacity-50 pointer-events-none"></div>
                <div className="flex items-center gap-2 mb-6 border-b border-[#f0f0f0] pb-4">
                  <span className="material-symbols-outlined text-primary">cycle</span>
                  <h4 className="font-title-lg text-title-lg text-on-surface">Recovery Configuration</h4>
                </div>
                
                <div className="space-y-6">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 border-b border-[#f0f0f0] pb-6">
                    <div>
                      <label className="block font-label-md text-label-md text-on-surface-variant mb-2">MAX RETRIES</label>
                      <input 
                        value={maxRetries}
                        onChange={(e) => setMaxRetries(Number(e.target.value))}
                        className="w-full h-10 px-3 border border-[#d9d9d9] rounded-lg font-body-md text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all bg-surface-container-lowest" 
                        type="number"
                      />
                      <p className="font-body-sm text-body-sm text-outline mt-1">Maximum number of automated attempts per mandate.</p>
                    </div>
                    <div>
                      <label className="block font-label-md text-label-md text-on-surface-variant mb-2">ESCALATION THRESHOLD (%)</label>
                      <div className="relative">
                        <input 
                          value={threshold}
                          onChange={(e) => setThreshold(Number(e.target.value))}
                          className="w-full h-10 px-3 border border-[#d9d9d9] rounded-lg font-body-md text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all bg-surface-container-lowest" 
                          type="number"
                        />
                      </div>
                      <p className="font-body-sm text-body-sm text-outline mt-1">Scores below this value are escalated for review.</p>
                    </div>
                  </div>

                  {/* AI Settings Box */}
                  <div className="bg-[#f9f0ff] border border-[#e2d5f8] rounded-lg p-5">
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center gap-2">
                        <span className="material-symbols-outlined text-primary-container fill">auto_awesome</span>
                        <span className="font-title-md text-title-md text-on-surface font-bold">AI Dynamic Routing</span>
                      </div>
                      
                      {/* Toggle Button */}
                      <button 
                        onClick={() => setAiRouting(!aiRouting)}
                        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none ${
                          aiRouting ? 'bg-primary' : 'bg-outline'
                        }`}
                      >
                        <span className={`inline-block h-4 w-4 rounded-full bg-white transition-transform ${
                          aiRouting ? 'translate-x-6' : 'translate-x-1'
                        }`}></span>
                      </button>
                    </div>
                    <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">
                      Allow the machine learning model to dynamically adjust retry windows based on historical payer success rates and bank gateway traffic analysis.
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* 2. Merchant Config Panel */}
            {activeTab === 'merchant' && (
              <div className="bg-white rounded-xl shadow-level1 border border-[#f0f0f0] p-6 space-y-6">
                <div className="flex items-center gap-2 mb-6 border-b border-[#f0f0f0] pb-4">
                  <span className="material-symbols-outlined text-primary">storefront</span>
                  <h4 className="font-title-lg text-title-lg text-on-surface">Merchant Integrations</h4>
                </div>
                <p className="font-body-md text-body-md text-on-surface-variant">Enable or disable automated recovery loops for individual merchant subscriptions.</p>
                
                <div className="divide-y divide-[#f0f0f0] border border-[#f0f0f0] rounded-xl overflow-hidden bg-surface-container-lowest">
                  {isLoadingSettings ? (
                    Array.from({ length: 4 }).map((_, index) => (
                      <div key={`merchant-skeleton-${index}`} className="flex justify-between items-center p-4">
                        <div className="space-y-2">
                          <SkeletonBlock className="h-4 w-40" />
                          <SkeletonBlock className="h-3 w-28" />
                        </div>
                        <SkeletonBlock className="h-6 w-11 rounded-full" />
                      </div>
                    ))
                  ) : merchants.length === 0 ? (
                    <div className="p-4 text-center text-on-surface-variant">
                      No backend merchants configured.
                    </div>
                  ) : (
                    merchants.map(merchant => (
                    <div key={merchant.id} className="flex justify-between items-center p-4">
                      <div>
                        <span className="font-title-md text-title-md text-on-surface block">{merchant.name}</span>
                        <span className="font-code text-xs text-outline">Ref: {merchant.id}_runner_v1</span>
                      </div>
                      <button 
                        onClick={() => toggleMerchant(merchant.id)}
                        disabled={togglingMerchantId === merchant.id}
                        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none ${
                          merchant.enabled ? 'bg-primary' : 'bg-outline'
                        } disabled:opacity-50`}
                      >
                        {togglingMerchantId === merchant.id ? (
                          <ButtonLoader className="h-4 w-4 mx-auto text-white" />
                        ) : (
                          <span className={`inline-block h-4 w-4 rounded-full bg-white transition-transform ${
                            merchant.enabled ? 'translate-x-6' : 'translate-x-1'
                          }`}></span>
                        )}
                      </button>
                    </div>
                    ))
                  )}
                </div>
              </div>
            )}

            {/* 3. API Keys Panel */}
            {activeTab === 'keys' && (
              <div className="bg-white rounded-xl shadow-level1 border border-[#f0f0f0] p-6 space-y-6">
                <div className="flex items-center gap-2 mb-6 border-b border-[#f0f0f0] pb-4">
                  <span className="material-symbols-outlined text-primary">key</span>
                  <h4 className="font-title-lg text-title-lg text-on-surface">Developer API Credentials</h4>
                </div>
                
                <div className="space-y-4">
                  <p className="font-body-md text-body-md text-on-surface-variant">Use these secret keys to authenticate webhook triggers from billing platforms.</p>
                  <div className="flex items-center gap-2 bg-surface-container-low p-3 rounded-lg border border-outline-variant/30">
                    <span className="font-code text-code text-on-surface flex-1 select-all font-mono font-semibold">
                      {keyHidden ? 'Backend managed' : apiKey}
                    </span>
                    <button 
                      onClick={() => setKeyHidden(!keyHidden)}
                      className="p-1.5 text-on-surface-variant hover:text-primary rounded hover:bg-surface-container transition-colors"
                      title={keyHidden ? "Show API Key" : "Hide API Key"}
                    >
                      <span className="material-symbols-outlined text-[18px]">
                        {keyHidden ? 'visibility' : 'visibility_off'}
                      </span>
                    </button>
                    <button 
                      onClick={copyApiKey}
                      className="p-1.5 text-on-surface-variant hover:text-primary rounded hover:bg-surface-container transition-colors"
                      title="Copy Key"
                    >
                      <span className="material-symbols-outlined text-[18px]">content_copy</span>
                    </button>
                  </div>
                  <button 
                    onClick={handleRegenerateApiKey}
                    disabled={isRegeneratingKey}
                    className="font-label-md text-label-md text-error bg-error-container/20 border border-error-container hover:bg-error-container/30 px-4 py-2 rounded-lg transition-colors font-bold flex items-center gap-2 disabled:opacity-50"
                  >
                    {isRegeneratingKey && <ButtonLoader />}
                    {isRegeneratingKey ? 'Regenerating...' : 'Regenerate Secret Key'}
                  </button>
                </div>
              </div>
            )}

            {/* 4. System Status Panel */}
            {activeTab === 'status' && (
              <div className="bg-white rounded-xl shadow-level1 border border-[#f0f0f0] p-6 space-y-6">
                <div className="flex items-center gap-2 mb-6 border-b border-[#f0f0f0] pb-4">
                  <span className="material-symbols-outlined text-secondary">dns</span>
                  <h4 className="font-title-lg text-title-lg text-on-surface">System Status</h4>
                </div>
                
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {isLoadingSettings ? (
                    Array.from({ length: 4 }).map((_, index) => (
                      <div key={`status-skeleton-${index}`} className="flex items-center justify-between p-4 border border-[#f0f0f0] rounded-lg bg-surface-container-lowest shadow-sm">
                        <div className="flex items-center gap-3">
                          <SkeletonBlock className="h-6 w-6 rounded-full" />
                          <SkeletonBlock className="h-4 w-36" />
                        </div>
                        <SkeletonBlock className="h-4 w-20" />
                      </div>
                    ))
                  ) : (
                    <>
                  <div className="flex items-center justify-between p-4 border border-[#f0f0f0] rounded-lg bg-surface-container-lowest shadow-sm">
                    <div className="flex items-center gap-3">
                      <span className="material-symbols-outlined text-green-600">check_circle</span>
                      <span className="font-title-md text-title-md text-on-surface font-semibold">Payment Gateway</span>
                    </div>
                    <span className="font-code text-code text-green-600 font-bold">{systemStatus?.status || 'Unknown'}</span>
                  </div>
                  <div className="flex items-center justify-between p-4 border border-[#f0f0f0] rounded-lg bg-surface-container-lowest shadow-sm">
                    <div className="flex items-center gap-3">
                      <span className="material-symbols-outlined text-green-600">check_circle</span>
                      <span className="font-title-md text-title-md text-on-surface font-semibold">ML Prediction Engine</span>
                    </div>
                    <span className="font-code text-code text-green-600 font-bold">{systemStatus ? `${systemStatus.decisions} decisions` : 'Unknown'}</span>
                  </div>
                  <div className="flex items-center justify-between p-4 border border-[#f0f0f0] rounded-lg bg-surface-container-lowest shadow-sm">
                    <div className="flex items-center gap-3">
                      <span className="material-symbols-outlined text-green-600">check_circle</span>
                      <span className="font-title-md text-title-md text-on-surface font-semibold">Webhook Delivery Queue</span>
                    </div>
                    <span className="font-code text-code text-green-600 font-bold">{systemStatus ? `${systemStatus.batchRuns} batches` : 'Unknown'}</span>
                  </div>
                  <div className="flex items-center justify-between p-4 border border-[#f0f0f0] rounded-lg bg-surface-container-lowest shadow-sm">
                    <div className="flex items-center gap-3">
                      <span className="material-symbols-outlined text-green-600">check_circle</span>
                      <span className="font-title-md text-title-md text-on-surface font-semibold">Scheduler Service</span>
                    </div>
                    <span className="font-code text-code text-green-600 font-bold">{systemStatus?.apiKeyEnabled ? 'API key enabled' : 'API key disabled'}</span>
                  </div>
                    </>
                  )}
                </div>
              </div>
            )}

            {/* Bottom Actions Bar */}
            <div className="flex justify-end gap-4 pt-4 border-t border-outline-variant/30">
              <button 
                onClick={handleDiscard}
                disabled={isDiscarding}
                className="px-6 py-2 rounded-lg font-title-md text-title-md text-on-surface-variant border border-outline-variant hover:bg-surface-container-high transition-colors bg-white shadow-sm flex items-center gap-2 disabled:opacity-50"
              >
                {isDiscarding && <ButtonLoader />}
                {isDiscarding ? 'Discarding...' : 'Discard Changes'}
              </button>
              <button 
                onClick={handleSave}
                disabled={isSaving}
                className="px-6 py-2 rounded-lg font-title-md text-title-md bg-primary text-white hover:bg-primary/95 shadow-sm transition-colors font-semibold flex items-center gap-2 disabled:opacity-50"
              >
                {isSaving && <ButtonLoader />}
                {isSaving ? 'Saving...' : 'Save Configuration'}
              </button>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}
