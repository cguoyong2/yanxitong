<template>
  <RouterView />
</template>

<style>
:root {
  color: #fff7ed;
  background: #160708;
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
    radial-gradient(circle at 80% 14%, rgba(255, 210, 130, 0.18), transparent 26%),
    radial-gradient(circle at 16% 22%, rgba(228, 42, 34, 0.28), transparent 28%),
    linear-gradient(135deg, #1b0808 0%, #5f090d 46%, #120708 100%);
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
    radial-gradient(circle at 12% 12%, rgba(255, 220, 150, 0.14), transparent 10%),
    linear-gradient(rgba(255, 230, 180, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 230, 180, 0.03) 1px, transparent 1px);
  background-size: 520px 520px, 88px 88px, 88px 88px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.58), transparent 78%);
}

.panel {
  width: min(760px, 100%);
  border: 1px solid rgba(255, 230, 180, 0.18);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(42, 18, 16, 0.92), rgba(20, 8, 9, 0.9)),
    rgba(16, 20, 24, 0.86);
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.38);
  padding: 40px;
  backdrop-filter: blur(18px);
}

.eyebrow {
  margin: 0 0 12px;
  color: #f6c26b;
  font-size: 14px;
  letter-spacing: 0;
  font-weight: 800;
}

.title {
  margin: 0;
  font-size: clamp(36px, 7vw, 72px);
  line-height: 1;
  letter-spacing: 0;
}

.subtitle {
  margin: 18px 0 0;
  color: #f1d8b5;
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
  color: #ffe8c8;
  font-weight: 700;
}

.field input {
  width: 100%;
  border: 1px solid rgba(255, 230, 180, 0.2);
  border-radius: 16px;
  background: rgba(255, 248, 230, 0.08);
  color: #fff;
  padding: 14px 16px;
  outline: none;
}

.primary {
  min-height: 52px;
  border-radius: 16px;
  background: linear-gradient(135deg, #ffd98a, #d89a36);
  color: #431407;
  font-weight: 900;
}

.secondary {
  min-height: 44px;
  border-radius: 14px;
  background: rgba(255, 248, 230, 0.1);
  color: #fff7ed;
  padding: 0 18px;
  border: 1px solid rgba(255, 230, 180, 0.14);
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
  color: #f1d8b5;
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
