export default function Logo({ size = 16 }: { size?: number }): JSX.Element {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" className="logo">
      <rect x="2" y="2" width="60" height="60" rx="14" fill="#10161e" stroke="#22d3ee" strokeWidth="3" />
      <path d="M18 16 L32 48 L46 16" fill="none" stroke="#22d3ee" strokeWidth="7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
