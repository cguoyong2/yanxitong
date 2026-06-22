<template>
  <RouterView />
</template>

<style>
:root {
  color: #f8fafc;
  background: #101418;
  font-family:
    Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  min-width: 320px;
  min-height: 100vh;
  background:
    radial-gradient(circle at 20% 18%, rgba(250, 204, 21, 0.14), transparent 28%),
    radial-gradient(circle at 82% 8%, rgba(220, 38, 38, 0.2), transparent 26%),
    linear-gradient(135deg, #120f12 0%, #1f1111 46%, #09090b 100%);
  overflow-x: hidden;
}

button,
input {
  font: inherit;
}

button {
  border: 0;
  cursor: pointer;
}

.screen {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 48px;
  position: relative;
}

.screen::before {
  content: "";
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.42), transparent 72%);
}

.panel {
  width: min(760px, 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(16, 20, 24, 0.86);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.3);
  padding: 40px;
  backdrop-filter: blur(18px);
}

.eyebrow {
  margin: 0 0 12px;
  color: #facc15;
  font-size: 14px;
  letter-spacing: 0;
}

.title {
  margin: 0;
  font-size: clamp(36px, 7vw, 72px);
  line-height: 1;
  letter-spacing: 0;
}

.subtitle {
  margin: 18px 0 0;
  color: #cbd5e1;
  font-size: 20px;
}

.form {
  display: grid;
  gap: 18px;
  margin-top: 32px;
}

.field {
  display: grid;
  gap: 8px;
  color: #e2e8f0;
}

.field input {
  width: 100%;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  padding: 14px 16px;
  outline: none;
}

.primary {
  min-height: 52px;
  border-radius: 8px;
  background: #f59e0b;
  color: #1f1300;
  font-weight: 700;
}

.secondary {
  min-height: 44px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  padding: 0 18px;
}

.secondary:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.status-line {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 28px;
  color: #cbd5e1;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #64748b;
}

.dot.online {
  background: #22c55e;
  box-shadow: 0 0 20px rgba(34, 197, 94, 0.8);
}

.dot.warn {
  background: #f59e0b;
  box-shadow: 0 0 20px rgba(245, 158, 11, 0.7);
}

.error {
  color: #fecaca;
}

@media (max-width: 680px) {
  .screen {
    padding: 24px;
  }

  .panel {
    padding: 28px;
  }

  .subtitle {
    font-size: 17px;
  }
}
</style>
