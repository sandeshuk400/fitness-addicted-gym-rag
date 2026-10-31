
import { Component, lazy, Suspense, useState, type ReactNode } from 'react'

const Scene3D = lazy(() => import('./Scene3D'))

function canUse3D(): boolean {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return false
  if (window.innerWidth < 768) return false
  try {
    const c = document.createElement('canvas')
    return !!(c.getContext('webgl2') || c.getContext('webgl'))
  } catch {
    return false
  }
}

class Safe extends Component<{ children: ReactNode }, { failed: boolean }> {
  state: { failed: boolean } = { failed: false }
  static getDerivedStateFromError() {
    return { failed: true }
  }
  render() {
    return this.state.failed ? null : this.props.children
  }
}

export default function Background3D() {
  const [enabled] = useState(canUse3D)
  if (!enabled) return null
  return (
    <div aria-hidden="true" className="pointer-events-none fixed inset-0 z-0">
      <div className="absolute inset-0 opacity-60">
        <Safe>
          <Suspense fallback={null}>
            <Scene3D />
          </Suspense>
        </Safe>
      </div>
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_25%,#171717_100%)]" />
    </div>
  )
}