# Changelog

## 3.3.0 (versionCode 16), 2026-11-06

### Body and muscle figure
- Redrawn front and back figure (`tools/gen_body.py` → `ui/progress/BodyArt.kt`): 8-head proportions, smoother outlines, split muscle heads (three quad heads, two hamstring heads, two calf heads, segmented abs, gluteus medius). Every shape is drawn once and mirrored, so left and right always match; muscles visible from both sides (side delts, forearms, obliques, neck) appear on both views.
- New smaller groups: forearm flexors and extensors, gastrocnemius and soleus, serratus, rotator cuff, hip flexors, neck. Deltoid heads (anterior/lateral/posterior) keep the existing `FRONT_DELTS` / `SIDE_DELTS` / `REAR_DELTS` ids and are drawn on both views where visible.
- Ids are stable: the 19 original `Muscle` names and order are unchanged (unit-tested). New entries are appended. Forearm and calf details carry `parent = FOREARMS / CALVES` and show the parent's weekly sets, so existing stats and exercise mappings are unchanged. Serratus, rotator cuff, hip flexors and neck are a new `ACCESSORY` region, which never enters the upper-to-lower ratio or "least trained".
- Tap a muscle to select it (tap again to clear). The selection is outlined on both views, the rest dims, and a label shows the name (English/Arabic), its group and its weekly sets. Small muscles snap to the nearest shape within 24dp, giving a 48dp touch target. The map has a content description for TalkBack.

### Voice assistant and AI removed
- Deleted: `core/assistant` (Gemini brain, router, limits, prompts, offline coach, recap, speech listener, voice interaction service, QS tile), `core/voice` (TTS, cloud TTS, voice catalog), `ui/assistant` (Hyper AI activity, sheet, orbs), assistant/voice settings screens, the onboarding assistant step, the workout voice coach, the AI weekly recap text, the assistant widget, the launcher AI-button router and their tests.
- Manifest: removed `RECORD_AUDIO`, the TTS/recognition `<queries>`, `AssistantActivity`, `VoiceInteractionService` and session service, the QS tile, and the assistant widget receiver.
- Strings: about 190 assistant, voice and Gemini strings removed from English and Arabic. Unused AI drawables and the `Theme.Hyper.Assistant` style removed.
- Data: on first launch the app drops the old `ai_*`, `w_voice_coach` and `w_coach_gemini` settings, the stored Gemini key, and the recap, router and limit preferences. Restoring an older backup skips them. Onboarding step numbers stay compatible with 3.2.0, so an interrupted setup resumes on the same page.
- Kept: timer sounds and vibration (`Feedback`, SoundPool) play exactly where they did, following the sound and vibration switches. The weekly recap box stays, showing your numbers only.

### Liquid Glass theme
- Tokens in one place (`ui/theme/Glass.kt`), taken from the Saja site: frosted fills (white 64% / 84%, dark 56% / 86%), specular rim (bright top-left and bottom-right), inner sheen, an outside-only two-layer soft shadow, 22dp blur, 26dp card, 18dp small and 34dp sheet radii, canvas `#F2F3F8` / `#08090C` with soft accent colour fields.
- Applied to cards, segmented controls, secondary buttons, sheets, dialogs and page backgrounds. The primary button gets a top-lit gradient and specular edge. The tab bar is now a floating glass pill with a sliding lens, and pages scroll under it.
- Blur: on Android 12+ the floating bar blurs the content under it (RenderEffect) and dialogs blur the app behind them (window blur-behind). On Android 8–11 the bar and dialogs use a dense frosted gradient instead (no RenderEffect calls).
- Light/dark mode, accent and secondary colours and the theme presets work as before. Text colours were adjusted to WCAG AA on glass: light `#4C525D` / `#59606B`, dark `#B4B9C2` / `#9EA3AD`. Switches, small pill buttons and text actions now have 48dp touch targets, and the segmented control is 48dp tall.
- Visual only: no screen logic or data changed. Large titles use slightly tighter tracking.

### Direct APK download and in-app update check
- Settings (Account) → About shows the installed version, and **Check for updates** reads `version.json` from `BuildConfig.UPDATE_MANIFEST_URL`. It is set once, from `hyperUpdateRepo` in `gradle.properties`, using the GitHub Releases `latest/download` pattern.
- The check runs off the main thread with 5/8/10-second timeouts and no retries. It never blocks startup, and offline or bad responses show only "couldn't check". It runs quietly once when Settings opens.
- When a newer `versionCode` is found (and the phone meets `minSdk`), a glass dialog shows the English or Arabic release notes. **Update** opens `apkUrl` in the browser. Nothing installs automatically; there is no install permission.
- `tools/make_release.py` packages `downloads/HyperAthlete-<version>.apk` and writes `downloads/version.json` (versionCode, versionName, apkUrl, sha256, releaseNotes en/ar, minSdk). It refuses APKs that are unsigned or signed with a key other than the release certificate.
- README: how to download and install from the phone, and how updates work.
