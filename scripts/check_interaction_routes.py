from pathlib import Path

root=Path(__file__).resolve().parents[1]
src=root/"app/src/main/java/com/game/knowledge"
def check_ui_routes():
    screen=(src/"Screens.kt").read_text(encoding="utf-8")
    duel=(src/"Duel.kt").read_text(encoding="utf-8")
    activity=(src/"MainActivity.kt").read_text(encoding="utf-8")
    visual=(src/"Visuals.kt").read_text(encoding="utf-8")
    confirm=(src/"KidExitDialog.kt").read_text(encoding="utf-8")
    gate=(src/"ExplanationGate.kt").read_text(encoding="utf-8")
    assert "const val STUDY_WAIT_MS = 3_000L" in gate
    assert "const val QUIZ_WAIT_MS = 10_000L" in gate
    study=screen[:screen.index("@Composable\nfun ChallengeScreen")]
    quiz=screen[screen.index("@Composable\nfun ChallengeScreen"):]
    assert study.count("ExplanationGate.STUDY_WAIT_MS") == 3
    assert quiz.count("ExplanationGate.QUIZ_WAIT_MS") == 3
    assert 'TopLine("📖 科普小课堂",onBack,"${index+1}/${questions.size}",vibrateOnBack=false)' in screen
    assert 'TopLine("🏆 知识大闯关",onBack,"${index+1}/${questions.size}  ·  ⭐$score",vibrateOnBack=false)' in screen
    assert 'TopLine("🤖 AI 知识对决",onBack,"🔵 $leftScore : $rightScore 🤖",vibrateOnBack=false)' in duel
    assert 'KidLink("← 返回",onBack,vibrateOnClick=false)' in duel
    assert 'KidPill("← 退出对决",onBack,vibrateOnClick=false)' in duel
    handler=activity.split("BackHandler(enabled=!exitConfirm && !noQuestions)",1)[1].split("CompositionLocalProvider",1)[0]
    assert "feedback.tap()" not in handler
    assert "if(vibrateOnClick) feedback?.tap()" in visual
    assert "vibrateOnBack:Boolean=true" in visual
    assert 'KidPill("← 返回",onBack,vibrateOnClick=vibrateOnBack)' in visual
    assert "onConfirm, modifier = Modifier.fillMaxWidth()" in confirm
    assert "vibrateOnClick=false" not in confirm
    print("INTERACTION_ROUTES_OK study=3s quiz=10s exit_confirmation_haptic=one")
check_ui_routes()
