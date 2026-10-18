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

## Twitch (servicio id 5, `services/twitch/`)
- Todo va por `https://gql.twitch.tv/gql` con el Client-ID anónimo de la web y consultas GQL
  escritas a mano (`TwitchGqlFields`). Reproducción: `streamPlaybackAccessToken` /
  `videoPlaybackAccessToken` → lista maestra HLS de `usher.ttvnw.net` (`TwitchStreamExtractor`).
- Ids de stream: `<login>` = directo, `videos/<id>` = VOD. Canal: `twitch.tv/<login>/videos`.
- Mocks: el RecordingDownloader cambia las IPs por 127.0.0.1 y eso invalida la firma del token
  de usher (403). Tras regrabar `TwitchExtractorsTest`, sustituir la respuesta 403 de usher por
  una lista HLS real descargada aparte con un token válido (IPs saneadas).

## TV (servicio id 6, `services/tv/`)
- Canales de la TDT española de la lista abierta de TDTChannels
  (`https://www.tdtchannels.com/lists/tv.json`, caché de 1 h en `TvChannelList`). Solo se
  listan canales con alguna opción: `M3U8` (se reproduce en la app), `YOUTUBE`
  (`youtube.com/channel/<id>/live`, resolver con `TvParsingHelper.resolveYoutubeLive`) o `WEB`.
- URLs propias (no son páginas reales): `https://www.tdtchannels.com/tv/<grupo>-<canal>`.
- El kiosco y la búsqueda (local, sin acentos) solo devuelven canales `M3U8`.
- Muchos canales de RTVE dan 403 fuera de España (geobloqueo): en el CI/entorno de EE. UU. no
  se puede probar la reproducción, solo que la lista y las URLs son correctas.
