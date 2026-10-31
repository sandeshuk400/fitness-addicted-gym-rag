import { useEffect, useRef } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import type { Group } from 'three'

type DumbbellProps = {
  position: [number, number, number]
  scale?: number
  speed?: number
  tilt?: number
}

function Dumbbell({ position, scale = 1, speed = 0.3, tilt = 0 }: DumbbellProps) {
  const ref = useRef<Group>(null)

  useFrame((state, delta) => {
    const g = ref.current
    if (!g) return
    g.rotation.y += delta * speed
    g.position.y = position[1] + Math.sin(state.clock.elapsedTime * 0.6 + position[0]) * 0.25
  })

  return (
    <group ref={ref} position={position} scale={scale} rotation={[0.35, 0, tilt]}>
      {/* handle */}
      <mesh rotation={[0, 0, Math.PI / 2]}>
        <cylinderGeometry args={[0.12, 0.12, 2.8, 24]} />
        <meshStandardMaterial color="#b8bcc4" metalness={0.6} roughness={0.35} />
      </mesh>
      {[-1, 1].map((side) => (
        <group key={side} position={[side * 1.0, 0, 0]}>
          {/* big plate */}
          <mesh rotation={[0, 0, Math.PI / 2]}>
            <cylinderGeometry args={[0.75, 0.75, 0.24, 40]} />
            <meshStandardMaterial color="#a31c1b" metalness={0.5} roughness={0.4} />
          </mesh>
          {/* small plate */}
          <mesh position={[side * 0.26, 0, 0]} rotation={[0, 0, Math.PI / 2]}>
            <cylinderGeometry args={[0.55, 0.55, 0.2, 40]} />
            <meshStandardMaterial color="#861615" metalness={0.5} roughness={0.4} />
          </mesh>
          {/* collar */}
          <mesh position={[side * 0.45, 0, 0]} rotation={[0, 0, Math.PI / 2]}>
            <cylinderGeometry args={[0.18, 0.18, 0.14, 24]} />
            <meshStandardMaterial color="#b8bcc4" metalness={0.6} roughness={0.35} />
          </mesh>
        </group>
      ))}
    </group>
  )
}

// Tilts the whole scene a little toward the mouse. Listens on window, so the canvas can ignore clicks.
function Rig() {
  const rig = useRef<Group>(null)
  const mouse = useRef({ x: 0, y: 0 })

  useEffect(() => {
    const onMove = (e: PointerEvent) => {
      mouse.current.x = (e.clientX / window.innerWidth) * 2 - 1
      mouse.current.y = (e.clientY / window.innerHeight) * 2 - 1
    }
    window.addEventListener('pointermove', onMove, { passive: true })
    return () => window.removeEventListener('pointermove', onMove)
  }, [])

  useFrame(() => {
    const g = rig.current
    if (!g) return
    g.rotation.y += (mouse.current.x * 0.25 - g.rotation.y) * 0.05
    g.rotation.x += (mouse.current.y * 0.15 - g.rotation.x) * 0.05
  })

  return (
    <group ref={rig}>
      <Dumbbell position={[5, -1.2, -1]} scale={1} speed={0.35} tilt={0.25} />
      <Dumbbell position={[-5, 1.4, -2]} scale={0.75} speed={-0.28} tilt={-0.3} />
      <Dumbbell position={[0.5, 3.1, -5]} scale={0.6} speed={0.2} tilt={0.5} />
      <Dumbbell position={[-3.5, -3, -3]} scale={0.5} speed={0.4} tilt={-0.15} />
      <Dumbbell position={[3.6, 3, -4]} scale={0.45} speed={-0.3} tilt={0.35} />
    </group>
  )
}

export default function Scene3D() {
  return (
    <Canvas
      dpr={[1, 1.5]}
      camera={{ position: [0, 0, 9], fov: 45 }}
      gl={{ alpha: true, antialias: true, powerPreference: 'low-power' }}
    >
      <ambientLight intensity={0.7} />
      <hemisphereLight args={['#ffffff', '#222222', 0.6]} />
      <directionalLight position={[5, 5, 5]} intensity={1.4} />
      <pointLight position={[-6, -2, 4]} intensity={30} color="#a31c1b" />
      <Rig />
    </Canvas>
  )
}