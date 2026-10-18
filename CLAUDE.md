# NewPipeExtractor (fork de VideoPrime) — notas para Claude

Las notas principales del proyecto (decisiones, hoja de ruta, estado, pendientes) están en
`GHD235/VideoPrime`, rama `videoprime`, archivo `CLAUDE.md`. Léelas primero.

## Convención de ramas
- `dev`: espejo del upstream `TeamNewPipe/NewPipeExtractor` (sincronizar con "Sync fork").
- `videoprime`: nuestros cambios (vigilancia de roturas, Twitch, arreglos propios).
  Los arreglos útiles para todos se proponen también upstream.

## Compilar y testear
- JDK 17. Sin jitpack.io/dl.google.com: ver `docs/videoprime/sandbox-build.md` en VideoPrime.
- `./gradlew :extractor:compileJava`
- Tests offline: `./gradlew :extractor:test -Ddownloader=MOCK --tests 'org.schabi.newpipe.extractor.services.youtube.*'`
- Tests contra servicios reales: `-Ddownloader=REAL` (requiere red a youtube.com etc.).
- Regrabar mocks: `-Ddownloader=RECORDING`.

## Dónde se rompe YouTube normalmente
`extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/`:
descifrado de firmas y parámetro `n` (`YoutubeSignaturesTest`, player JS), clientes
`youtubei` imitados, y parseo de renderers JSON en `extractors/`.
