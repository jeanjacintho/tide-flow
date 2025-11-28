"use client"

import React, { useEffect, useId, useMemo, useRef, useState } from "react"
import { motion } from "framer-motion"
import { cn } from "@/lib/utils"

interface DotPatternProps extends React.SVGProps<SVGSVGElement> {
  width?: number
  height?: number
  x?: number
  y?: number
  cx?: number
  cy?: number
  cr?: number
  className?: string
  glow?: boolean
  [key: string]: unknown
}

export function DotPattern({
  width = 16,
  height = 16,
  cx = 1,
  cy = 1,
  cr = 1,
  className,
  glow = false,
  ...props
}: DotPatternProps) {
  const id = useId()
  const containerRef = useRef<SVGSVGElement>(null)
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 })
  const [isMounted, setIsMounted] = useState(false)
  const [randomValues] = useState(() => {

    const values: Array<{ delay: number; duration: number }> = []
    for (let i = 0; i < 10000; i++) {
      values.push({
        delay: Math.random() * 5,
        duration: Math.random() * 3 + 2,
      })
    }
    return values
  })

  useEffect(() => {
    setIsMounted(true)

    const updateDimensions = () => {
      if (containerRef.current) {
        const parent = containerRef.current.parentElement
        if (parent) {
          const { width, height } = parent.getBoundingClientRect()
          setDimensions({ width, height })
        }
      }
    }

    const rafId = requestAnimationFrame(() => {
      updateDimensions()

      requestAnimationFrame(updateDimensions)
    })

    window.addEventListener("resize", updateDimensions)
    return () => {
      cancelAnimationFrame(rafId)
      window.removeEventListener("resize", updateDimensions)
    }
  }, [])

  const dots = useMemo(() => {
    const totalDots =
      Math.ceil(dimensions.width / width) *
      Math.ceil(dimensions.height / height)

    return Array.from({ length: totalDots }, (_, i) => {
      const col = i % Math.ceil(dimensions.width / width)
      const row = Math.floor(i / Math.ceil(dimensions.width / width))
      const randomIndex = i % randomValues.length

      return {
        x: col * width + cx,
        y: row * height + cy,
        delay: randomValues[randomIndex].delay,
        duration: randomValues[randomIndex].duration,
      }
    })
  }, [dimensions.width, dimensions.height, width, height, cx, cy, randomValues])

  const shouldRender = isMounted && dimensions.width > 0 && dimensions.height > 0

  const viewBoxWidth = shouldRender ? dimensions.width : 1920
  const viewBoxHeight = shouldRender ? dimensions.height : 1080

  return (
    <svg
      ref={containerRef}
      aria-hidden="true"
      className={cn(
        "pointer-events-none absolute inset-0 h-full w-full text-neutral-400/80",
        className
      )}
      style={{
        width: '100%',
        height: '100%',
        willChange: 'auto'
      }}
      viewBox={`0 0 ${viewBoxWidth} ${viewBoxHeight}`}
      preserveAspectRatio="none"
      {...props}
    >
      <defs>
        <radialGradient id={`${id}-gradient`}>
          <stop offset="0%" stopColor="currentColor" stopOpacity="1" />
          <stop offset="100%" stopColor="currentColor" stopOpacity="0" />
        </radialGradient>
      </defs>
      {shouldRender && dots.map((dot) => (
        <motion.circle
          key={`${dot.x}-${dot.y}`}
          cx={dot.x}
          cy={dot.y}
          r={cr}
          fill={glow ? `url(#${id}-gradient)` : "currentColor"}
          initial={glow ? { opacity: 0.4, scale: 1 } : {}}
          animate={
            glow
              ? {
                  opacity: [0.4, 1, 0.4],
                  scale: [1, 1.5, 1],
                }
              : {}
          }
          transition={
            glow
              ? {
                  duration: dot.duration,
                  repeat: Infinity,
                  repeatType: "reverse",
                  delay: dot.delay,
                  ease: "easeInOut",
                }
              : {}
          }
        />
      ))}
    </svg>
  )
}
