import React, { useState, useRef, useEffect } from 'react';
import { ButtonLoader, SummaryCardSkeleton } from '../components/LoadingSkeleton';
import { askAi, fetchAiInsights, fetchAiSummary } from '../services/api';

const META_RESPONSE_MARKERS = [
  'this is an excellent example',
  'why this recoverai answer is optimal',
  'why this answer is optimal',
  'data-driven confidence',
  'transparency & accountability',
  'recoverai\'s capabilities',
];

const BLOCKED_SECTION_HEADINGS = [
  'Why this answer is optimal',
  'Why this RecoverAI Answer is Optimal',
  'Data-Driven Confidence',
  'Transparency & Accountability',
  'In summary',
];

const OPERATIONAL_FALLBACK = 'The backend response included non-operational commentary. Review the relevant mandate, latest recovery decision, and audit trail to determine the exact reason.';

function stripMarkdownEmphasis(value) {
  return value.replace(/^\*\*(.*)\*\*$/, '$1').trim();
}

function sanitizeAiAnswer(answer) {
  if (typeof answer !== 'string' || !answer.trim()) {
    return 'No backend answer returned.';
  }

  const trimmed = answer.trim();
  const normalized = trimmed.toLowerCase();
  if (META_RESPONSE_MARKERS.some(marker => normalized.includes(marker))) {
    return OPERATIONAL_FALLBACK;
  }

  return trimmed
    .split(/\r?\n/)
    .filter(line => {
      const cleanLine = stripMarkdownEmphasis(line.replace(/^#+\s*/, ''));
      return !BLOCKED_SECTION_HEADINGS.some(heading => cleanLine.toLowerCase() === heading.toLowerCase());
    })
    .join('\n')
    .trim() || OPERATIONAL_FALLBACK;
}

function AiAnswerContent({ text, compact = false }) {
  const lines = String(text || '').split(/\r?\n/).map(line => line.trim()).filter(Boolean);

  if (lines.length <= 1) {
    return <p className={`font-body-md text-body-md leading-relaxed ${compact ? 'text-on-surface-variant' : ''}`}>{text}</p>;
  }

  return (
    <div className={`font-body-md text-body-md leading-relaxed space-y-2 ${compact ? 'text-on-surface-variant' : ''}`}>
      {lines.map((line, index) => {
        const heading = stripMarkdownEmphasis(line.replace(/^#+\s*/, ''));
        const bulletMatch = line.match(/^([-*]|\d+[.)])\s+(.+)/);
        const isHeading = !bulletMatch && (
          line.startsWith('#')
          || /^\*\*.+\*\*$/.test(line)
          || /^[A-Z][A-Za-z0-9 &/-]{2,}:$/.test(line)
        );

        if (isHeading) {
          return (
            <h5 key={`${index}-${line}`} className="font-title-md text-title-md text-on-surface pt-1">
              {heading.replace(/:$/, '')}
            </h5>
          );
        }

        if (bulletMatch) {
          return (
            <div key={`${index}-${line}`} className="flex gap-2">
              <span className="font-label-md text-label-md text-primary shrink-0 min-w-5 pt-0.5">
                {bulletMatch[1]}
              </span>
              <p className="min-w-0">{stripMarkdownEmphasis(bulletMatch[2])}</p>
            </div>
          );
        }

        return <p key={`${index}-${line}`}>{stripMarkdownEmphasis(line)}</p>;
      })}
    </div>
  );
}

export default function AIInsights() {
  // Executive Summaries State
  const [summaries, setSummaries] = useState([]);

  // Chat message state
  const [messages, setMessages] = useState([
    {
      sender: 'ai',
      text: 'Ask about mandates, failures, customers, merchants, batches, retries, revenue, or recovery performance.',
    },
  ]);

  const [inputVal, setInputVal] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [activeAiAction, setActiveAiAction] = useState('');
  const [isLoadingSummaries, setIsLoadingSummaries] = useState(true);
  const chatScrollRef = useRef(null);

  // Auto-scroll to bottom of chat
  useEffect(() => {
    if (chatScrollRef.current) {
      chatScrollRef.current.scrollTop = chatScrollRef.current.scrollHeight;
    }
  }, [messages, isTyping]);

  useEffect(() => {
    Promise.allSettled([fetchAiInsights(), fetchAiSummary()]).then((results) => {
      const backendSummaries = results
        .filter((result) => result.status === 'fulfilled' && result.value?.answer)
        .map((result, index) => ({
          id: `backend-${index}`,
          title: index === 0 ? 'Backend AI Insight' : 'Backend Batch Summary',
          time: 'Generated now',
          icon: index === 0 ? 'auto_awesome' : 'summarize',
          color: 'text-primary',
          bg: 'bg-primary-container/20 hover:bg-primary-container/30',
          description: sanitizeAiAnswer(result.value.answer),
          recommendation: 'Review this backend-generated recommendation before applying operational changes.',
        }));

      if (backendSummaries.length > 0) {
        setSummaries(backendSummaries);
      }
    }).finally(() => setIsLoadingSummaries(false));
  }, []);

  const handleSend = async (text, actionKey = 'chat') => {
    if (!text.trim() || isTyping) return;
    const userMsg = { sender: 'user', text };
    setMessages(prev => [...prev, userMsg]);
    setInputVal('');
    setIsTyping(true);
    setActiveAiAction(actionKey);

    try {
      const response = await askAi(text);
      setIsTyping(false);
      setMessages(prev => [...prev, { sender: 'ai', text: sanitizeAiAnswer(response.answer) }]);
    } catch {
      setIsTyping(false);
      setMessages(prev => [...prev, { sender: 'ai', text: 'The backend AI service is unavailable right now.' }]);
    } finally {
      setActiveAiAction('');
    }
  };

  const handleDismissSummary = (id) => {
    setSummaries(summaries.filter(s => s.id !== id));
  };

  return (
    <div className="flex-1 p-container-padding flex flex-col min-h-0 bg-background overflow-hidden">
      
      {/* Page Title */}
      <div className="mb-stack-md shrink-0">
        <h2 className="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
          <span className="material-symbols-outlined text-primary fill text-[30px]">auto_awesome</span>
          AI Insights &amp; Copilot
        </h2>
        <p className="font-body-md text-body-md text-on-surface-variant mt-1">Real-time analysis and conversational intelligence for recovery operations.</p>
      </div>

      {/* Split Screen Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-gutter flex-1 min-h-0">
        
        {/* Left: AI Executive Summaries */}
        <div className="lg:col-span-5 flex flex-col min-h-0 overflow-hidden">
          <h3 className="font-title-lg text-title-lg text-on-surface py-2 shrink-0">
            Executive Summaries
          </h3>

          <div className="flex-1 min-h-0 overflow-y-auto chat-scroll pr-2 pb-stack-lg flex flex-col gap-stack-md">
            {isLoadingSummaries ? (
              <>
                <SummaryCardSkeleton />
                <SummaryCardSkeleton />
              </>
            ) : summaries.length === 0 ? (
              <div className="bg-surface rounded-xl p-6 border border-outline-variant/30 text-center text-on-surface-variant">
                All executive summaries have been processed or dismissed.
              </div>
            ) : (
              summaries.map(item => (
                <div
                  key={item.id}
                  className="bg-surface rounded-xl p-stack-md border border-outline-variant/40 shadow-level1 relative overflow-hidden group transition-all h-80 shrink-0 flex flex-col"
                >
                  <div className={`absolute top-0 right-0 w-32 h-32 rounded-bl-full -z-10 transition-colors ${item.bg}`}></div>
                  <div className="flex items-start gap-3 mb-3 shrink-0">
                    <div className="w-8 h-8 rounded-full bg-surface-container-high flex items-center justify-center shrink-0">
                      <span className={`material-symbols-outlined text-sm ${item.color}`}>
                        {item.icon}
                      </span>
                    </div>
                    <div>
                      <h4 className="font-title-md text-title-md text-on-surface">{item.title}</h4>
                      <span className="font-label-md text-label-md text-secondary">{item.time}</span>
                    </div>
                  </div>
                  <div className="flex-1 min-h-0 overflow-y-auto chat-scroll pr-1">
                    <div className="mb-2">
                      <AiAnswerContent text={item.description} compact />
                    </div>
                    <div className="mt-3 p-3 bg-surface-container/40 rounded-lg border border-outline-variant/20">
                      <span className="font-label-md text-label-md text-primary font-bold uppercase tracking-wider block mb-1">Recommendation</span>
                      <p className="text-body-sm text-on-surface-variant">{item.recommendation}</p>
                    </div>
                  </div>
                  <div className="mt-4 flex gap-2 shrink-0">
                    <button
                      onClick={() => {
                        handleSend(`Apply optimization for: ${item.title}`, `summary-${item.id}`);
                        handleDismissSummary(item.id);
                      }}
                      disabled={isTyping}
                      className="font-label-md text-label-md text-primary hover:bg-primary-container/20 px-3 py-1.5 rounded transition-colors border border-primary/20 bg-white shadow-sm font-semibold flex items-center gap-2 disabled:opacity-50"
                    >
                      {activeAiAction === `summary-${item.id}` && <ButtonLoader className="h-3 w-3" />}
                      Apply Strategy
                    </button>
                    <button
                      onClick={() => handleDismissSummary(item.id)}
                      className="font-label-md text-label-md text-secondary hover:bg-surface-container px-3 py-1.5 rounded transition-colors border border-outline-variant/30 bg-white"
                    >
                      Dismiss
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Right: AI Chat Interface / Copilot */}
        <div className="lg:col-span-7 bg-surface border border-outline-variant/30 rounded-xl shadow-level2 flex flex-col h-full overflow-hidden min-h-0">
          
          {/* Chat Header */}
          <div className="px-stack-md py-3 border-b border-outline-variant/30 bg-surface-container-low flex justify-between items-center shrink-0">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-primary fill">smart_toy</span>
              <h3 className="font-title-md text-title-md text-on-surface">Ask RecoverAI</h3>
            </div>
            <div className="flex gap-2">
              <button 
                onClick={() => setMessages([messages[0]])}
                className="p-1.5 rounded hover:bg-surface-container text-outline-variant hover:text-on-surface transition-colors" 
                title="Clear Chat History"
              >
                <span className="material-symbols-outlined text-sm">history</span>
              </button>
            </div>
          </div>

          {/* Chat History Area */}
          <div 
            ref={chatScrollRef}
            className="flex-grow overflow-y-auto chat-scroll p-stack-md flex flex-col gap-stack-lg bg-surface-bright"
          >
            {messages.map((msg, i) => (
              <div 
                key={i} 
                className={`flex gap-3 max-w-[85%] ${msg.sender === 'user' ? 'self-end flex-row-reverse' : ''}`}
              >
                {msg.sender === 'ai' ? (
                  <div className="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center shrink-0 mt-1 shadow-sm border border-primary/20">
                    <span className="material-symbols-outlined text-on-primary-container text-sm fill">auto_awesome</span>
                  </div>
                ) : (
                  <div className="w-8 h-8 rounded-full border border-outline-variant overflow-hidden shrink-0 mt-1 shadow-sm">
                    <img 
                      alt="User profile photo" 
                      className="w-full h-full object-cover" 
                      src="https://lh3.googleusercontent.com/aida-public/AB6AXuCP8XWHG29iV4UsFCsyznYiiormOuag07xAxQtmBAtWky_Yf6y9WQnIuLi5gEa64hanFjJrYaA64hLaxAAs6SleyycZgyfmfNc0wf6a45iWVINAt9N3o-FmtESK6VrimPttNzD9HBQ2qk42QOZRZlLxAR4oMKRooZa5L_RnBVvtjZ7t4qgy0w7RfgfgymQPmW2W3vSqga9f3QeaIkBEtSj9XOdBjGlMXAsXl0cDNXCKXzx9CuQ_EhhW"
                    />
                  </div>
                )}
                
                <div className={`rounded-2xl p-4 shadow-sm ${
                  msg.sender === 'ai' 
                    ? 'bg-surface-container border border-outline-variant/30 rounded-tl-sm text-on-surface' 
                    : 'bg-primary text-on-primary rounded-tr-sm'
                }`}>
                  {msg.sender === 'ai' ? (
                    <AiAnswerContent text={msg.text} />
                  ) : (
                    <p className="font-body-md text-body-md leading-relaxed">{msg.text}</p>
                  )}
                  
                  {msg.list && (
                    <div className="bg-surface rounded-lg p-3 border border-outline-variant/20 mt-2">
                      <ul className="list-disc list-inside font-body-sm text-body-sm text-on-surface-variant space-y-1">
                        {msg.list.map((li, idx) => (
                          <li key={idx} dangerouslySetInnerHTML={{ __html: li }}></li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {msg.recommendation && (
                    <p className="font-body-md text-body-md mt-2 italic font-semibold">{msg.recommendation}</p>
                  )}

                  {msg.actions && (
                    <div className="flex gap-2 mt-3">
                      <button 
                        onClick={() => handleSend('Initiate retry for the latest backend technical failures', 'latest-retry')}
                        disabled={isTyping}
                        className="font-label-md text-label-md text-primary bg-primary/10 hover:bg-primary/20 px-3 py-1 rounded border border-primary/20 transition-colors font-bold bg-white flex items-center gap-2 disabled:opacity-50"
                      >
                        {activeAiAction === 'latest-retry' && <ButtonLoader className="h-3 w-3" />}
                        Initiate Retry
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}

            {isTyping && (
              <div className="flex gap-3 max-w-[80%]">
                <div className="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center shrink-0 mt-1 shadow-sm border border-primary/20 animate-pulse">
                  <span className="material-symbols-outlined text-on-primary-container text-sm fill">auto_awesome</span>
                </div>
                <div className="bg-surface-container border border-outline-variant/30 rounded-2xl rounded-tl-sm px-4 py-3 flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-outline-variant animate-bounce" style={{ animationDelay: '0ms' }}></span>
                  <span className="w-2 h-2 rounded-full bg-outline-variant animate-bounce" style={{ animationDelay: '150ms' }}></span>
                  <span className="w-2 h-2 rounded-full bg-outline-variant animate-bounce" style={{ animationDelay: '300ms' }}></span>
                </div>
              </div>
            )}
          </div>

          {/* Input Area Container */}
          <div className="shrink-0 bg-surface-container-low border-t border-outline-variant/30 flex flex-col">
            {/* Suggested Prompts */}
            <div className="px-stack-md py-stack-sm flex gap-2 overflow-x-auto chat-scroll max-w-full">
              <button 
                onClick={() => handleSend('Why are payments failing?', 'prompt-failures')}
                disabled={isTyping}
                className="font-body-sm text-body-sm text-primary-fixed bg-inverse-surface hover:opacity-90 px-3 py-1.5 rounded-full whitespace-nowrap transition-colors flex items-center gap-1 border border-outline-variant/20 shadow-sm disabled:opacity-50"
              >
                {activeAiAction === 'prompt-failures' ? <ButtonLoader className="h-3 w-3" /> : <span className="material-symbols-outlined text-[14px]">troubleshoot</span>}
                Why are payments failing?
              </button>
              <button 
                onClick={() => handleSend('How is recovery performing?', 'prompt-performance')}
                disabled={isTyping}
                className="font-body-sm text-body-sm text-primary-fixed bg-inverse-surface hover:opacity-90 px-3 py-1.5 rounded-full whitespace-nowrap transition-colors flex items-center gap-1 border border-outline-variant/20 shadow-sm disabled:opacity-50"
              >
                {activeAiAction === 'prompt-performance' ? <ButtonLoader className="h-3 w-3" /> : <span className="material-symbols-outlined text-[14px]">monitoring</span>}
                Recovery performance
              </button>
              <button 
                onClick={() => handleSend('How did the latest batch perform?', 'prompt-batch')}
                disabled={isTyping}
                className="font-body-sm text-body-sm text-primary-fixed bg-inverse-surface hover:opacity-90 px-3 py-1.5 rounded-full whitespace-nowrap transition-colors flex items-center gap-1 border border-outline-variant/20 shadow-sm disabled:opacity-50"
              >
                {activeAiAction === 'prompt-batch' ? <ButtonLoader className="h-3 w-3" /> : <span className="material-symbols-outlined text-[14px]">summarize</span>}
                Latest batch
              </button>
              <button 
                onClick={() => handleSend('Which customers are highest risk?', 'prompt-risk')}
                disabled={isTyping}
                className="font-body-sm text-body-sm text-primary-fixed bg-inverse-surface hover:opacity-90 px-3 py-1.5 rounded-full whitespace-nowrap transition-colors flex items-center gap-1 border border-outline-variant/20 shadow-sm disabled:opacity-50"
              >
                {activeAiAction === 'prompt-risk' ? <ButtonLoader className="h-3 w-3" /> : <span className="material-symbols-outlined text-[14px]">warning</span>}
                Highest risk customers
              </button>
              <button 
                onClick={() => handleSend('Which merchants have the most failures?', 'prompt-merchants')}
                disabled={isTyping}
                className="font-body-sm text-body-sm text-primary-fixed bg-inverse-surface hover:opacity-90 px-3 py-1.5 rounded-full whitespace-nowrap transition-colors flex items-center gap-1 border border-outline-variant/20 shadow-sm disabled:opacity-50"
              >
                {activeAiAction === 'prompt-merchants' ? <ButtonLoader className="h-3 w-3" /> : <span className="material-symbols-outlined text-[14px]">storefront</span>}
                Merchant failures
              </button>
              <button 
                onClick={() => handleSend('Check Mandate M0016', 'prompt-m0016')}
                disabled={isTyping}
                className="font-body-sm text-body-sm text-primary-fixed bg-inverse-surface hover:opacity-90 px-3 py-1.5 rounded-full whitespace-nowrap transition-colors flex items-center gap-1 border border-outline-variant/20 shadow-sm disabled:opacity-50"
              >
                {activeAiAction === 'prompt-m0016' ? <ButtonLoader className="h-3 w-3" /> : <span className="material-symbols-outlined text-[14px]">assignment_late</span>}
                Check M0016
              </button>
            </div>

            {/* Input Field */}
            <div className="p-stack-md pt-2 flex items-end gap-stack-sm bg-surface-container-lowest">
              <div className="flex-1 relative">
                <input 
                  value={inputVal}
                  onChange={(e) => setInputVal(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleSend(inputVal, 'chat');
                  }}
                  className="w-full bg-surface border border-outline-variant rounded-xl pl-4 pr-10 py-3 font-body-md text-body-md text-on-surface focus:outline-none focus:border-primary transition-colors resize-none overflow-hidden block" 
                  placeholder="Ask RecoverAI about trends, batches, or specific mandates..." 
                />
                <button className="absolute right-3 top-1/2 -translate-y-1/2 text-outline-variant hover:text-primary transition-colors p-1">
                  <span className="material-symbols-outlined">attach_file</span>
                </button>
              </div>
              <button 
                onClick={() => handleSend(inputVal, 'chat')}
                disabled={isTyping || !inputVal.trim()}
                className="w-12 h-12 rounded-xl bg-primary hover:bg-on-primary-fixed-variant text-on-primary flex items-center justify-center shrink-0 transition-colors shadow-sm disabled:opacity-50"
              >
                {activeAiAction === 'chat' ? <ButtonLoader /> : <span className="material-symbols-outlined fill text-white">send</span>}
              </button>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
