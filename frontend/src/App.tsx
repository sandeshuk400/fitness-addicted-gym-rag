function App() {
  return (
    <div className="min-h-screen bg-charcoal flex items-center justify-center p-6">
      <div className="w-full max-w-md rounded-2xl bg-forest border border-white/10 p-8 text-center shadow-xl">
        <h1 className="text-3xl font-bold text-white">Fitness Addicted Gym</h1>
        <p className="mt-2 text-neutral-400">Memberships, attendance and more.</p>
        <button className="mt-6 rounded-lg bg-crimson px-6 py-2 font-semibold text-white transition hover:bg-crimson-dark">
          Sign in
        </button>
      </div>
    </div>
  )
}

export default App