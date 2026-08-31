import React from 'react';

export function SkeletonBlock({ className = '', ...props }) {
  return (
    <span
      aria-hidden="true"
      className={`block rounded bg-surface-container-high skeleton-loader ${className}`}
      {...props}
    />
  );
}

export function MetricCardSkeleton() {
  return (
    <div className="bg-surface-container-lowest rounded-xl p-5 shadow-level1 border border-outline-variant/30 h-36 flex flex-col justify-between">
      <div className="flex items-center justify-between">
        <SkeletonBlock className="h-4 w-24" />
        <SkeletonBlock className="h-5 w-5 rounded-full" />
      </div>
      <div className="space-y-2">
        <SkeletonBlock className="h-8 w-28" />
        <SkeletonBlock className="h-4 w-32" />
      </div>
    </div>
  );
}

export function TableSkeletonRows({ rows = 5, columns = 5 }) {
  return Array.from({ length: rows }).map((_, rowIndex) => (
    <tr key={`skeleton-row-${rowIndex}`} className="border-b border-outline-variant/20">
      {Array.from({ length: columns }).map((__, columnIndex) => (
        <td key={`skeleton-cell-${rowIndex}-${columnIndex}`} className="p-4">
          <SkeletonBlock className={`h-4 ${columnIndex === 0 ? 'w-20' : 'w-full'}`} />
        </td>
      ))}
    </tr>
  ));
}

export function SummaryCardSkeleton({ lines = 3 }) {
  return (
    <div className="bg-surface rounded-xl p-stack-md border border-outline-variant/40 shadow-level1">
      <div className="flex items-start gap-3 mb-4">
        <SkeletonBlock className="w-8 h-8 rounded-full shrink-0" />
        <div className="flex-1 space-y-2">
          <SkeletonBlock className="h-4 w-36" />
          <SkeletonBlock className="h-3 w-24" />
        </div>
      </div>
      <div className="space-y-2">
        {Array.from({ length: lines }).map((_, index) => (
          <SkeletonBlock
            key={`summary-line-${index}`}
            className={`h-3 ${index === lines - 1 ? 'w-2/3' : 'w-full'}`}
          />
        ))}
      </div>
    </div>
  );
}
