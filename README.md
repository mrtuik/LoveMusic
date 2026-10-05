<h1 align="center">LoveMusic ♥</h1>
<p align="center">A YouTube Music client for Android with a brand-new look.</p>

LoveMusic is a redesigned fork of [Metrolist](https://github.com/MetrolistGroup/Metrolist) (GPL-3.0).
It talks to YouTube Music through the InnerTube API, so **no API key is needed**.

## What's new in LoveMusic
- New name, package (`com.lovemusic.app`) and launcher icon (heart + sound-wave)
- Your own logo as launcher icon (red chat-bubble + music note), adaptive + themed icon
- **Glass + Aurora design**: drifting pink/violet aurora glow behind Home and the player, translucent glass surfaces
- **Everything floats**: floating glass navigation pill, floating glass mini-player, rounded artwork with soft shadow
- Reference-style player: waveform seek bar, big circular play button, minimal prev/next
- Smaller, thinner icons everywhere (all UI icons are ~14% smaller)
- Dynamic (album-art) theme is OFF by default so the LoveMusic pink palette shows; turn it on in Settings -> Appearance
- Time-aware greeting card on Home with an animated heart
- **SVG icon pipeline**: drop any `.svg` into `svg-icons/` and it becomes an Android icon automatically

## SVG icons
All core icons (navigation, player controls, heart, shuffle, repeat, volume ...) are authored as SVG in `svg-icons/`.

```bash
python3 tools/svg2vd.py            # svg-icons/*.svg -> app/src/main/res/drawable/*.xml
python3 tools/svg2vd.py a.svg b.xml  # single file
```
- File name = drawable name (`play-circle.svg` -> `R.drawable.play_circle`)
- Use `currentColor` (or any colour) for tintable icons: they are exported white so `Icon(tint = ...)` can recolour them
- Multi-colour artwork: add `data-keep-color="1"` on `<svg>` (and `data-size="108"` for launcher-sized art)
- Supported: path, circle, ellipse, rect, line, polyline, polygon, `<g transform="translate/scale">`
- Not supported: gradients, filters, text, rotate/skew transforms

The GitHub workflow runs the converter before every build, so editing an SVG and pushing is enough.

## Build the APK with GitHub Actions
1. Create a GitHub repo and push this folder to `main`
2. Open **Actions -> LoveMusic APK** (it also runs on every push)
3. Download the APK from the run's **Artifacts**

Local build: `./gradlew assembleFossDebug`

> Original upstream workflows are kept in `.github/upstream-workflows-disabled/` (they need Metrolist's private runners and secrets).

## Optional keys
Only needed for optional extras: `LASTFM_API_KEY` / `LASTFM_SECRET` (Last.fm scrobbling) as GitHub secrets or in `local.properties`.

## License
GPL-3.0 (see `LICENSE`). Based on Metrolist by MO Agamy and contributors. Not affiliated with YouTube or Google.
