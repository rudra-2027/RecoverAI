import React, { useEffect, useState } from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { MetricCardSkeleton, SkeletonBlock } from '../components/LoadingSkeleton';
import { fetchMetrics, fetchMetricTrends } from '../services/api';

export default function Dashboard() {
  const [insightApplied, setInsightApplied] = useState(false);
  const [metrics, setMetrics] = useState(null);
  const [metricTrends, setMetricTrends] = useState([]);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    Promise.all([fetchMetrics(), fetchMetricTrends()])
      .then(([metricsResponse, trendsResponse]) => {
        setMetrics(metricsResponse);
        setMetricTrends(trendsResponse || []);
      })
      .catch(() => setError('Backend metrics are unavailable right now.'))
      .finally(() => setIsLoading(false));
  }, []);

  const money = (value) => new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(Number(value || 0));

  const percent = (value) => `${Number(value || 0).toFixed(1)}%`;

  const trendData = metricTrends.map((point) => ({
    name: point.name,
    'Total Failed': Number(point.totalFailed || 0),
    Recovered: Number(point.recovered || 0),
    'AI Recovered': Number(point.aiRecovered || 0),
  }));

  const latestTrend = trendData[trendData.length - 1] || {};
  const previousTrend = trendData[trendData.length - 2] || {};
  const trendChange = (current, previous) => {
    if (!previous && !current) return 0;
    if (!previous) return current > 0 ? 100 : 0;
    return ((current - previous) / previous) * 100;
  };
  const totalRevenueChange = trendChange(
    Number(latestTrend['Total Failed'] || 0) + Number(latestTrend.Recovered || 0),
    Number(previousTrend['Total Failed'] || 0) + Number(previousTrend.Recovered || 0),
  );
  const recoveredRevenueChange = trendChange(
    Number(latestTrend.Recovered || 0),
    Number(previousTrend.Recovered || 0),
  );
  const TrendIndicator = ({ value, label }) => {
    const isUp = value > 0;
    const isDown = value < 0;
    const color = isUp ? 'text-green-600' : isDown ? 'text-error' : 'text-secondary';
    const icon = isUp ? 'trending_up' : isDown ? 'trending_down' : 'trending_flat';

    return (
      <div className="font-body-sm text-body-sm text-secondary mt-1 flex items-center gap-1">
        <span className={`material-symbols-outlined text-[14px] ${color}`}>{icon}</span>
        <span className={`${color} font-semibold`}>{value > 0 ? '+' : ''}{Number(value || 0).toFixed(1)}%</span>
        <span>{label}</span>
      </div>
    );
  };

  const processedCount = Number(metrics?.failedMandatesCount || 0) + Number(metrics?.recoveredMandatesCount || 0);
  const failedCount = Number(metrics?.failedMandatesCount || 0);
  const recoveredCount = Number(metrics?.recoveredMandatesCount || 0);
  const retriedCount = failedCount + recoveredCount;
  const percentWidth = (value, total) => `${Math.min(100, total > 0 ? (value * 100) / total : 0)}%`;

  return (
    <div className="flex-1 p-container-padding bg-background pb-20 overflow-y-auto">
      {/* Hero Section */}
      <div className="mb-stack-lg">
        <h2 className="font-headline-lg text-headline-lg text-on-surface mb-unit">AI Revenue Recovery Agent</h2>
        <p className="font-body-lg text-body-lg text-secondary">Recover revenue from failed UPI AutoPay mandates</p>
        {error && <p className="font-body-sm text-body-sm text-error mt-2">{error}</p>}
      </div>

      {/* AI Insight Banner */}
      <div className="mb-stack-lg ai-sparkle rounded-xl p-6 flex flex-col md:flex-row items-start md:items-center gap-4 transition-all">
        <div className="p-2 bg-white rounded-lg shadow-sm border border-outline-variant/20 flex-shrink-0 flex items-center justify-center">
          <span className="material-symbols-outlined fill text-[#8a2be2]">auto_awesome</span>
        </div>
        <div className="flex-1">
          <h3 className="font-title-md text-title-md text-on-surface mb-1 flex items-center gap-2 flex-wrap">
            Automated Insight
            <span className="text-xs bg-[#f3e8ff] text-[#6b21a8] px-2 py-0.5 rounded-full font-label-md">High Confidence</span>
          </h3>
          <p className="font-body-md text-body-md text-on-surface-variant">
            {isLoading ? (
              <span className="block space-y-2">
                <SkeletonBlock className="h-4 w-full max-w-xl" />
                <SkeletonBlock className="h-4 w-64" />
              </span>
            ) : (
              <>
                Backend scoring is reporting an average recovery probability of <strong className="text-on-surface font-semibold">{percent(metrics?.averageRecoveryProbability)}</strong> with <strong className="text-on-surface font-semibold">{Number(metrics?.highValueCustomers || 0).toLocaleString()}</strong> high-value customers currently tracked.
              </>
            )}
          </p>
        </div>
        <div className="shrink-0">
          <button 
            onClick={() => setInsightApplied(!insightApplied)}
            className={`font-label-md text-label-md px-4 py-2 rounded-lg transition-all shadow-sm ${
              insightApplied 
                ? 'bg-green-600 text-white hover:bg-green-700' 
                : 'bg-primary-container text-on-primary-container hover:opacity-90'
            }`}
          >
            {insightApplied ? 'Strategy Applied' : 'Apply Strategy'}
          </button>
        </div>
      </div>

      {/* KPIs Bento Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-stack-md mb-stack-lg">
        {isLoading ? (
          Array.from({ length: 5 }).map((_, index) => <MetricCardSkeleton key={`metric-skeleton-${index}`} />)
        ) : (
          <>
        {/* Card 1 */}
        <div className="bg-surface-container-lowest rounded-xl p-5 shadow-level1 border border-outline-variant/30 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-4">
            <span className="font-body-sm text-body-sm text-secondary">Total Revenue</span>
            <span className="material-symbols-outlined text-outline-variant text-[20px]">account_balance_wallet</span>
          </div>
          <div>
            <div className="font-headline-md text-headline-md text-on-surface">{money(metrics?.revenueThisMonth)}</div>
            <TrendIndicator value={totalRevenueChange} label="vs last month" />
          </div>
        </div>

        {/* Card 2 */}
        <div className="bg-surface-container-lowest rounded-xl p-5 shadow-level1 border border-error-container flex flex-col justify-between">
          <div className="flex items-center justify-between mb-4">
            <span className="font-body-sm text-body-sm text-secondary">Revenue At Risk</span>
            <span className="material-symbols-outlined text-error text-[20px]">warning</span>
          </div>
          <div>
            <div className="font-headline-md text-headline-md text-error">{money(metrics?.revenueAtRisk)}</div>
            <div className="font-body-sm text-body-sm text-secondary mt-1">Failed mandates</div>
          </div>
        </div>

        {/* Card 3 */}
        <div className="bg-surface-container-lowest rounded-xl p-5 shadow-level1 border border-outline-variant/30 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-4">
            <span className="font-body-sm text-body-sm text-secondary">Revenue Recovered</span>
            <span className="material-symbols-outlined text-primary text-[20px]">sync_saved_locally</span>
          </div>
          <div>
            <div className="font-headline-md text-headline-md text-primary">{money(metrics?.recoveredRevenue)}</div>
            <TrendIndicator value={recoveredRevenueChange} label="vs last month" />
          </div>
        </div>

        {/* Card 4 */}
        <div className="bg-surface-container-lowest rounded-xl p-5 shadow-level1 border border-outline-variant/30 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-4">
            <span className="font-body-sm text-body-sm text-secondary">Recovery Rate</span>
            <span className="material-symbols-outlined text-outline-variant text-[20px]">percent</span>
          </div>
          <div>
            <div className="font-headline-md text-headline-md text-on-surface">{percent(metrics?.recoveryRate)}</div>
            <div className="font-body-sm text-body-sm text-secondary mt-1">Of total at-risk</div>
          </div>
        </div>

        {/* Card 5 (AI Lift) */}
        <div className="bg-[#f9f0ff] rounded-xl p-5 shadow-level1 border border-[#e0caff] flex flex-col justify-between relative overflow-hidden group">
          <div className="absolute -right-4 -top-4 w-16 h-16 bg-[#e0caff] rounded-full opacity-20 group-hover:scale-150 transition-transform duration-500"></div>
          <div className="flex items-center justify-between mb-4 relative z-10">
            <span className="font-body-sm text-body-sm text-[#581c87] font-semibold">AI Recovery Lift</span>
            <span className="material-symbols-outlined text-[#8a2be2] text-[20px]">smart_toy</span>
          </div>
          <div className="relative z-10">
            <div className="font-headline-md text-headline-md text-[#581c87]">{money(metrics?.recoveryLift)}</div>
            <div className="font-body-sm text-body-sm text-[#7e22ce] mt-1">Above baseline</div>
          </div>
        </div>
          </>
        )}
      </div>

      {/* Main Data Area */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-stack-lg mb-stack-lg">
        {/* Recharts Analytics Chart */}
        <div className="bg-surface-container-lowest rounded-xl p-6 shadow-level1 border border-outline-variant/30 lg:col-span-2">
          <div className="flex justify-between items-center mb-6">
            <h3 className="font-title-lg text-title-lg text-on-surface">Recovery Performance Trends</h3>
            <span className="text-body-sm text-secondary">Monthly backend trend</span>
          </div>
          <div className="h-72 w-full">
            {isLoading ? (
              <div className="h-full w-full flex flex-col justify-end gap-4 pt-6">
                <SkeletonBlock className="h-4 w-48" />
                <div className="grid grid-cols-6 gap-3 items-end flex-1">
                  {[45, 65, 52, 78, 60, 88].map((height, index) => (
                    <SkeletonBlock key={`chart-skeleton-${index}`} className="w-full rounded-t-lg" style={{ height: `${height}%` }} />
                  ))}
                </div>
              </div>
            ) : (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={trendData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorFailed" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#ba1a1a" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="#ba1a1a" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorRecovered" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#0038d1" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="#0038d1" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorAi" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#8a2be2" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="#8a2be2" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#c4c5d8" opacity={0.3} />
                <XAxis dataKey="name" stroke="#747687" fontSize={11} />
                <YAxis stroke="#747687" fontSize={11} />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: '#ffffff', 
                    borderRadius: '8px', 
                    border: '1px solid #c4c5d8',
                    boxShadow: '0px 2px 8px rgba(0,0,0,0.04)' 
                  }} 
                />
                <Legend iconType="circle" wrapperStyle={{ fontSize: 12, marginTop: 10 }} />
                <Area type="monotone" dataKey="Total Failed" stroke="#ba1a1a" fillOpacity={1} fill="url(#colorFailed)" strokeWidth={2} />
                <Area type="monotone" dataKey="Recovered" stroke="#0038d1" fillOpacity={1} fill="url(#colorRecovered)" strokeWidth={2} />
                <Area type="monotone" dataKey="AI Recovered" stroke="#8a2be2" fillOpacity={1} fill="url(#colorAi)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Recovery Funnel Card */}
        <div className="bg-surface-container-lowest rounded-xl p-6 shadow-level1 border border-outline-variant/30">
          <h3 className="font-title-lg text-title-lg text-on-surface mb-6">Recovery Funnel</h3>
          <div className="flex flex-col gap-6">
            {isLoading ? (
              Array.from({ length: 5 }).map((_, index) => (
                <div key={`funnel-skeleton-${index}`} className="space-y-2">
                  <div className="flex justify-between">
                    <SkeletonBlock className="h-4 w-24" />
                    <SkeletonBlock className="h-4 w-16" />
                  </div>
                  <SkeletonBlock className="h-2 w-full rounded-full" />
                </div>
              ))
            ) : (
              <>
            {/* Step 1 */}
            <div>
              <div className="flex justify-between items-baseline mb-2">
                <span className="font-body-sm text-body-sm text-secondary">Processed</span>
                <span className="font-title-md text-title-md text-on-surface">{processedCount.toLocaleString()}</span>
              </div>
              <div className="h-2 w-full bg-surface-container rounded-full overflow-hidden">
                <div className="h-full bg-outline-variant rounded-full" style={{ width: processedCount > 0 ? '100%' : '0%' }}></div>
              </div>
            </div>

            {/* Step 2 */}
            <div>
              <div className="flex justify-between items-baseline mb-2">
                <span className="font-body-sm text-body-sm text-secondary">Failed</span>
                <span className="font-title-md text-title-md text-error">{failedCount.toLocaleString()}</span>
              </div>
              <div className="h-2 w-full bg-surface-container rounded-full overflow-hidden">
                <div className="h-full bg-error rounded-full" style={{ width: percentWidth(failedCount, processedCount) }}></div>
              </div>
            </div>

            {/* Step 3 */}
            <div>
              <div className="flex justify-between items-baseline mb-2">
                <span className="font-body-sm text-body-sm text-secondary">Recoverable</span>
                <span className="font-title-md text-title-md text-on-surface">{money(metrics?.predictedRecoverableRevenue)}</span>
              </div>
              <div className="h-2 w-full bg-surface-container rounded-full overflow-hidden">
                <div className="h-full bg-secondary w-2/3 rounded-full"></div>
              </div>
            </div>

            {/* Step 4 */}
            <div>
              <div className="flex justify-between items-baseline mb-2">
                <span className="font-body-sm text-body-sm text-secondary">Retried</span>
                <span className="font-title-md text-title-md text-primary-container">{retriedCount.toLocaleString()}</span>
              </div>
              <div className="h-2 w-full bg-surface-container rounded-full overflow-hidden">
                <div className="h-full bg-primary-container rounded-full" style={{ width: percentWidth(retriedCount, processedCount) }}></div>
              </div>
            </div>

            {/* Step 5 */}
            <div>
              <div className="flex justify-between items-baseline mb-2">
                <span className="font-body-sm text-body-sm text-secondary">Recovered</span>
                <span className="font-title-md text-title-md text-primary">{recoveredCount.toLocaleString()}</span>
              </div>
              <div className="h-2 w-full bg-surface-container rounded-full overflow-hidden">
                <div className="h-full bg-primary rounded-full" style={{ width: percentWidth(recoveredCount, processedCount) }}></div>
              </div>
            </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
