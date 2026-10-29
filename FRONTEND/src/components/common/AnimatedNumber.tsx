import { useEffect, useState } from 'react'

interface AnimatedNumberProps {
  value: number
  fractionDigits?: number
  suffix?: string
  className?: string
}

export function AnimatedNumber({ value, fractionDigits = 0, suffix = '', className = '' }: AnimatedNumberProps) {
  const [display, setDisplay] = useState(0)
  const reduceMotion = typeof window !== 'undefined' && (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false)
  const formatted = `${value.toFixed(fractionDigits)}${suffix}`
  const visibleValue = reduceMotion || !Number.isFinite(value) ? value : display

  useEffect(() => {
    if (reduceMotion || !Number.isFinite(value)) return
    let frame = 0
    let start: number | null = null
    const duration = 650
    const animate = (time: number) => {
      if (start === null) start = time
      const progress = Math.min(1, (time - start) / duration)
      const eased = 1 - (1 - progress) ** 3
      setDisplay(value * eased)
      if (progress < 1) frame = window.requestAnimationFrame(animate)
    }
    frame = window.requestAnimationFrame(animate)
    return () => window.cancelAnimationFrame(frame)
  }, [value, reduceMotion])

  return <span className={`animated-number ${className}`}><span aria-hidden="true">{`${visibleValue.toFixed(fractionDigits)}${suffix}`}</span><span className="sr-only">{formatted}</span></span>
}
