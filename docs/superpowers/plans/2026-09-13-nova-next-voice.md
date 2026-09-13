# Nova Next Voice Input Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add explicit-user-action speech-to-text that fills the guarded chat composer and always requires review before sending.

**Architecture:** Wrap browser speech recognition behind a small adapter that can be mocked in tests. The UI owns start/stop/status controls; the adapter never sends network requests or submits chat.

**Tech Stack:** Web Speech API when available, ES modules, DOM APIs, Node `node:test` with fake recognition objects.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-completion-design.md`

## Global Constraints
- No background/continuous listening.
- No automatic chat send.
- No server-side audio processing.
- Unsupported platforms render a truthful fallback.
- Current `nova/**` remains untouched.
- TDD first.

---

### Task 1: Speech recognition adapter
**Files:**
- Create: `nova-next/src/voice-input.mjs`
- Create: `nova-next/tests/voice-input.test.mjs`

**Interfaces:** `createVoiceInput({ SpeechRecognitionCtor })` -> `{ supported, start({ onTranscript, onState, onError }), stop(), destroy() }`.
- [ ] **Step 1:** Write failing tests for unsupported state, explicit start, interim/final transcript delivery, stop, error, and no auto-restart.
- [ ] **Step 2:** Run focused test and confirm RED.
- [ ] **Step 3:** Implement with `continuous = false`, `interimResults = true`, and no network/send callback.
- [ ] **Step 4:** Run focused + full tests.
- [ ] **Step 5:** Commit `feat(nova-next): add bounded voice input adapter`.

### Task 2: Voice composer controls
**Files:**
- Modify: `nova-next/index.html`
- Modify: `nova-next/app.js`
- Modify: `nova-next/live.css`
- Create: `nova-next/tests/voice-ui-contract.test.mjs`

**Interfaces:** voice button starts/stops adapter; transcript is inserted into `#novaNextChatInput` only.
- [ ] **Step 1:** Write failing static contract for mic button, `aria-pressed`, status copy, unsupported hidden/disabled state, and no form submission from recognition events.
- [ ] **Step 2:** Run test and confirm RED.
- [ ] **Step 3:** Wire voice button to adapter; preserve existing text, append transcript safely, focus composer after stop.
- [ ] **Step 4:** Add status states `Listening…`, `Transcript ready — review before sending`, and truthful unsupported/error copy.
- [ ] **Step 5:** Run full tests and syntax checks.
- [ ] **Step 6:** Commit `feat(nova-next): add review-before-send voice composer`.

### Task 3: Mobile/accessibility behavior and PR
- [ ] **Step 1:** Extend accessibility contract for mic label/state and Escape/route-change stop behavior.
- [ ] **Step 2:** Verify route changes and sign-out call `stop()`/`destroy()` and never leave recognition active.
- [ ] **Step 3:** Run all Nova Next tests.
- [ ] **Step 4:** Open and auto-merge low/medium-risk PR only when green.
