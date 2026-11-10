'use client';

import { Button } from 'antd-mobile';

export function EmptyState({
  title,
  desc,
  actionText,
  onAction,
}: {
  title: string;
  desc: string;
  actionText?: string;
  onAction?: () => void;
}) {
  return (
    <div className="empty-state">
      <svg className="empty-art" viewBox="0 0 120 120" role="img" aria-hidden="true">
        <rect x="26" y="16" width="68" height="92" rx="10" fill="#EAF3FF" />
        <rect x="26" y="16" width="68" height="20" rx="10" fill="#1677FF" />
        <line x1="38" y1="26" x2="82" y2="26" stroke="#FFFFFF" strokeWidth="4" strokeLinecap="round" />
        <circle cx="46" cy="52" r="9" fill="none" stroke="#1677FF" strokeWidth="5" />
        <line x1="60" y1="50" x2="84" y2="50" stroke="#C9C9C9" strokeWidth="4" strokeLinecap="round" />
        <line x1="60" y1="64" x2="84" y2="64" stroke="#C9C9C9" strokeWidth="4" strokeLinecap="round" />
        <line x1="60" y1="78" x2="78" y2="78" stroke="#C9C9C9" strokeWidth="4" strokeLinecap="round" />
      </svg>
      <p className="empty-title">{title}</p>
      <p className="empty-desc">{desc}</p>
      {onAction && actionText ? (
        <Button color="primary" size="small" onClick={onAction}>
          {actionText}
        </Button>
      ) : null}
    </div>
  );
}
