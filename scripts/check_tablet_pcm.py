"""Static regression guards for the tablet voice and full-height landscape UI."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"app/src/main/java/com/game/knowledge"
narrator=(src/"MandarinNarrator.kt").read_text(encoding="utf8")
player=(src/"BufferedSpeechPlayer.kt").read_text(encoding="utf8")
pcm=(src/"SpeechPcm.kt").read_text(encoding="utf8")
screens=(src/"Screens.kt").read_text(encoding="utf8")
layouts=(src/"TabletLayouts.kt").read_text(encoding="utf8")
duel=(src/"Duel.kt").read_text(encoding="utf8")
results=(src/"TabletResults.kt").read_text(encoding="utf8")
home=screens[:screens.index("fun StudyScreen(")]
assert "synthesizeToFile(" in narrator
assert "playCached(" in narrator and "speakDirect(" in narrator
assert "private var pcmEnabled=tablet" in narrator
assert "File(context.cacheDir," in narrator
assert "48kHz" in narrator or "48 kHz" in narrator
assert "AudioTrack.MODE_STREAM" in player and "AudioTrack.MODE_STATIC" in player
assert "const val BUFFER_MS=1_800" in player
assert "private var bufferedOutput:AudioTrack?=null" in player
assert "private fun sharedTrack():AudioTrack" in player
assert "private fun releaseShared()" in player
assert "Process.THREAD_PRIORITY_AUDIO" in player
assert "track.flush()" in player
assert "TabletPcmPolicy.canFallbackAfterFailure" in player
assert "SpeechPlaybackResult" in player
assert "result.started" in narrator
assert "pcm.prepare()" in narrator
assert "TabletPcmPolicy.bufferSize" in player
assert "TabletPcmPolicy.prefillBytes" in player
assert "SpeechPcm.to48kStereo" in player
assert "SpeechPcm.withStartupGuard" in player
assert "const val STARTUP_SILENCE_MS = 120" in pcm
assert "setSampleRate(SpeechPcm.OUTPUT_HZ)" in player
assert "runCatching {track?.release()}" in player
assert "import android.media.AudioTrack" not in pcm # software-only conversion
assert screens.count("ResponsiveWindowPolicy.tabletLandscape(")>=4
assert "MainModeTile(" in home and ".fillMaxHeight()" in home
assert "TabletStudyBody(" in screens and "TabletChallengeBody(" in screens
assert ".weight(1f)" in layouts and ".verticalScroll(rememberScrollState())" in layouts
assert ".fillMaxHeight()" in screens
assert "availablePaneHeight" in duel and "spread=true" in duel
assert "DuelResultScreen(" in duel and "QuizResultScreen(" in screens
assert results.count("ResponsiveWindowPolicy.tabletLandscape(")==2
print("TABLET_PCM_AND_LANDSCAPE_LAYOUT_REGRESSION_OK")
