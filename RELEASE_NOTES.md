# Release v19.1.0

Pixelabs Android Calculator Release with upgraded enterprise CI/CD pipeline, offline privacy enhancements, and UI polish.

### 🚀 New Features
- Integrate direct Google Sheet webhook submission with email fallback (`daddd7f`)
- Implement two-way history swipe gestures, drag handle physics, and auto-scaling scientific number formatting (`fbfbacf`)
- Complete code audit, robust math engine, fluid UI animations, and Play Console CI/CD (`5a0e6d7`)
- Initialize Android project structure (`83c5a66`)

### 🐛 Bug Fixes & Improvements
- Polish feedback dialog header, button copy, consent checkbox, and layout overflow (`eb9f6c1`)
- Eliminate display squish and preview overlap in scientific portrait mode (`5af1916`)
- Fallback blank keystore secrets to defaults for dry-run verification (`7cd1227`)

### ⚡ Performance & Refactoring
- Remove mock google sign-in, redesign feedback dialog, and add debug apk workflow (`7ac348d`)

### 📝 Documentation & Legal
- Update feedback success copy and add voluntary feedback clauses to Privacy Policy & Terms (`3de786e`)
- Update publisher branding to PixelabsDev, support email to hello@pixelabs.in, and offline legal notices (`7f1f0f6`)

### 🔧 Maintenance & Chores
- Resilient keystore preparation and signing for release bundle (`f80f319`)
- Set Robolectric SDK config to 34 to match shadow jars (`1dd93ff`)
- Use standard gradle test task in workflow (`d5ab216`)
- Update gradle distribution to 9.3.1 for AGP 9.1.1 compatibility (`e81aa87`)
- Fix Android SDK packages in GitHub Actions workflow (`3aa3956`)
- Initial commit (`a4be7a3`)
