# CoupleBubble - Mediterranean Ocean & Terracotta Design Refinement

A visual and aesthetic redesign of CoupleBubble that replaces the dark navy tone with a vibrant **Mediterranean Ocean Blue** (`#1D64B4`) that pairs harmoniously with **Sunset Terracotta** (`#E2643B`), set against warm **porcelain cream** backgrounds (`#FAF8F5`).

## User Review & Critical Decisions

> [!IMPORTANT]
> The following enhancements incorporate your chosen preferences:

- **Primary Color Update**: Switching to **Mediterranean Ocean Blue** (`#1D64B4` in light mode, `#86B7FE` in dark mode). It is radiant, inviting, and sits directly opposite warm orange on the color wheel, creating vivid yet gentle harmony instead of stark dark contrast.
- **Warm Porcelain Background**: Transitioning from cold grey/white to warm cream porcelain (`#FAF8F5` / `#FFFDF9`), lending the couple space an intimate, cozy atmosphere.
- **Ambient Border Glow & Soft Gradients**: Cards feature soft radial/linear gradient fills with warm terracotta rim accents and diffused elevation shadows.
- **Avatar Ring Accents & Photo Placeholders**: Refined partner profile chips with two-tone concentric rings and interactive photo change hints.
- **Mini Memory Milestones Timeline**: Added below the hero counter card to celebrate relationship highlights (e.g. *„Erster Kuss“*, *„Erste gemeinsame Reise“*, *„Zusammengezogen“*, *„2. Jahrestag“*).

---

## 1. Overview & Visual Concept

The updated design transforms CoupleBubble from a dark corporate navy feeling into a sunlit Mediterranean sanctuary. The pairing of ocean azure, warm terracotta, and cream porcelain evokes sunlit coastlines and warmth, perfectly suiting a romantic shared space.

---

## 2. User Experience & Visual Design

### Palette Transformation
- **Mediterranean Ocean Blue**:
  - `PrimaryLight`: `#1D64B4` (Rich Aegean azure)
  - `PrimaryContainerLight`: `#E0EDFF` (Breezy sky blue tint)
  - `OnPrimaryContainerLight`: `#00295A`
- **Sunset Terracotta**:
  - `SecondaryLight`: `#E2643B` (Sunbaked terracotta orange)
  - `SecondaryContainerLight`: `#FFE8DF` (Soft apricot glow)
  - `TertiaryLight`: `#F07B52` (Vibrant warm coral)
- **Warm Cream & Porcelain Surfaces**:
  - `BackgroundLight`: `#FAF8F5` (Soft warm porcelain)
  - `SurfaceLight`: `#FFFDF9` (Luminous warm ivory card surface)
  - `SurfaceContainerLight`: `#F4EFE8` (Subtle sandy beige pill container)
  - `SurfaceContainerHighLight`: `#ECE5DA`

### Visual Hierarchy & Components

```
┌─────────────────────────────────────────────────────────────┐
│  Top Header: Mediterranean & Terracotta Ring Avatars        │
│  [ (A) ─── ❤️ ─── (S) ]   Alex & Sam • "Verbunden"         │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Hero Counter Card (Soft Cream + Terracotta Ambient Rim)    │
│  • "Zusammen seit 25. Juni 2025" (Calendar badge)           │
│  • [ 461 ] Tage voller Liebe (Display typography)          │
│  • Pill Grid: [ 1 ] Jahre  │  [ 3 ] Monate  │  [ 7 ] Tage   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Anniversary Milestone Card                                 │
│  • "2. Jahrestag in 268 Tagen"                              │
│  • Smooth terracotta-to-ocean gradient progress bar         │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Mini Memory Timeline (NEW)                                 │
│  • 📍 Erster Kuss (25. Jun 2025)                            │
│  • ✈️ Erste Reise nach Florenz (14. Okt 2025)               │
│  • 🏡 Zusammengezogen (01. Feb 2026)                        │
│  • 🌟 2. Jahrestag (In 268 Tagen)                           │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Intimate Love Note Card + Quick Floating Heart Action      │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Mediterranean Blue (`#1D64B4`) vs. Dark Navy (`#102542`)**
  - *Chosen Approach*: Brighten the primary blue to a warm chromatic blue with higher lightness (~42% vs ~16%) and saturation.
  - *Why*: Deep navy absorbed all light and clashed with vibrant orange; Mediterranean blue creates a vibrant natural complement (sky/ocean + sunset).
- **Decision 2: Memory Timeline in Dashboard**
  - *Chosen Approach*: Add `MemoryMilestone` data model to `CoupleSpace` with interactive items (with dates, titles, and icons).
  - *Why*: Gives couples tangible nostalgia and celebrates moments beyond just the raw day counter.
- **Decision 3: Avatar Ring System with Tap-to-Edit Mock Hint**
  - *Chosen Approach*: Concentric border rings combining Ocean Blue and Terracotta with a subtle camera badge.
  - *Why*: Elevates the couple avatars from flat circles into polished personal profile elements.

---

## 4. Technical Architecture & File Updates

1. **`ui/theme/Color.kt`**: Replace Navy tokens with Mediterranean Ocean Blue, warm Terracotta, and Porcelain Cream tokens.
2. **`ui/theme/Theme.kt`**: Align light and dark color schemes with the new palette and softened shadow elevations.
3. **`model/CoupleModels.kt`**: Add `MemoryMilestone(title, date, icon, isCompleted)` list to `CoupleSpace`.
4. **`ui/screens/PairingScreen.kt`**:
   - Update header brand icon with Mediterranean Blue + Sunset Terracotta linear gradient.
   - Update PIN slot borders and tab indicator to use the refreshed palette.
5. **`ui/screens/DashboardScreen.kt`**:
   - Implement new `MemoryTimelineSection` with milestone nodes and dashed connectors.
   - Refactor `SpaceTopHeader` with concentric avatar rings and status pill.
   - Add ambient border glow on `HeroCounterCard` and `MilestoneCard`.
6. **Launcher Icon Drawables (`ic_launcher_background.xml`, `ic_launcher_foreground.xml`)**:
   - Update icon background gradient to Mediterranean Ocean Blue (`#14467D` to `#1D64B4`) to match the new visual identity.
