"""Regression guard: landscape quiz next controls must not live inside the scroll.

This static check verifies composition structure, not rendered pixel positions.
Real tablet device tests remain necessary for extreme font scaling.
"""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
ui=root/"app/src/main/java/com/game/knowledge"
tablet=(ui/"TabletLayouts.kt").read_text("utf8")
study=tablet.split("internal fun TabletStudyBody(",1)[1].split("internal fun TabletChallengeBody(",1)[0]
quiz=tablet.split("internal fun TabletChallengeBody(",1)[1]
assert study.index(".verticalScroll(rememberScrollState())") < study.index("LargeAction(\"继续探索")
assert "Column(Modifier.fillMaxWidth().weight(1f)" in study
assert quiz.index(".verticalScroll(rememberScrollState())") < quiz.index("options.chunked(2)")
assert quiz.index("options.chunked(2)") < quiz.index('LargeAction("下一题')
assert "Column(Modifier.fillMaxWidth().weight(1f)" in quiz
# There are no manual next actions in the duel: progression uses coroutine delays.
duel=(ui/"Duel.kt").read_text("utf8")
assert "delay(DuelRaceNotice.SUCCESS_DISPLAY_MS)" in duel

canto=(ui/"CantonesePlayground.kt").read_text("utf8")
assert 'page in setOf(LISTEN,DIMSUM,BRIDGE,TREASURE,DIALOGUE)' in canto
assert ".then(if(tabletQuiz) Modifier else Modifier.verticalScroll(rememberScrollState()))" in canto
assert "Modifier.weight(1f) else Modifier" in canto
assert canto.count("tabletLandscape=tabletQuiz)")==5
regular=canto.split("private fun CantoQuiz(",1)[1].split("private fun CantoSpeakScreen(",1)[0]
assert "tabletLandscape:Boolean=false" in regular
assert regular.index("Modifier.weight(1f).verticalScroll(rememberScrollState())") < regular.index("FlowRow(")
assert regular.index("} // all question content stays inside the bounded scroll area") < regular.rindex('LargeAction(if(index==round.lastIndex) "🏅')
assert "StoryCard(if(tabletLandscape) Modifier.fillMaxSize()" in regular

games=(ui/"CantoneseChallenges.kt").read_text("utf8")
assert "tabletLandscape:Boolean=false" in games
assert games.index("Modifier.weight(1f).verticalScroll(rememberScrollState())") < games.index("candidates.chunked(2)")
assert games.index("} // questions, choices, feedback may scroll while action stays visible") < games.rindex('LargeAction(if(index==round.lastIndex)')
assert "StoryCard(if(tabletLandscape) Modifier.fillMaxSize()" in games
print("TABLET_NEXT_ACTIONS_PINNED_OK",
      "encyclopedia","knowledge_quiz","5_cantonese_games",
      "duel_auto_progression")
