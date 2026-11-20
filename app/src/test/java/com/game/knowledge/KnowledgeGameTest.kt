package com.game.knowledge

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class KnowledgeGameTest {
    @Test fun cantoneseExpandedChallengeModesFilterAgeAppropriateEntries() {
        fun e(id:String,kind:String,category:String,meaning:String="意思") =
            CantoneseWord(id,kind,category,id,id,"zi6",meaning,"🍎","小提示")
        val entries=listOf(
            e("a","word","点心茶楼"),e("b","word","交通出行"),
            e("c","word","家人称呼"),e("d","word","运动游戏"),
            e("e","phrase","日常对话"),e("f","phrase","日常对话"))
        val dialogues=CantoneseChallengeRules.eligible(CantoneseChallengeMode.DIALOGUE,entries)
        assertEquals(listOf("e","f"),dialogues.map {it.id})
        val treasure=CantoneseChallengeRules.eligible(CantoneseChallengeMode.TREASURE,entries)
        assertEquals(setOf("a","b"),treasure.map {it.id}.toSet())
        val bridge=CantoneseChallengeRules.eligible(CantoneseChallengeMode.BRIDGE,entries)
        assertEquals(setOf("a","b","d"),bridge.map {it.id}.toSet())
    }

    @Test fun cantoneseExpandedChallengeChoicesStayDistinctAndIncludeAnswer() {
        val entries=(0..7).map {i->
            CantoneseWord("w$i","word","点心茶楼","词$i","詞$i",
                "ci4","点心$i","🥟","认一认")
        }
        repeat(48) {seed->
            val target=entries[seed%entries.size]
            val options=CantoneseChallengeRules.choices(target,entries,Random(seed))
            assertEquals(4,options.size)
            assertEquals(4,options.map {it.id}.toSet().size)
            assertEquals(1,options.count {it.id==target.id})
            assertTrue(options.all {it.category==target.category})
        }
    }

    @Test fun independentCantoneseVolumeSupportsQuietNormalAndEnhancedPlayback() {
        assertEquals(150,CantoneseVolumePolicy.DEFAULT_PERCENT)
        assertEquals(0,CantoneseVolumePolicy.clamp(-25))
        assertEquals(200,CantoneseVolumePolicy.clamp(300))
        assertEquals(0f,CantoneseVolumePolicy.playerGain(0),.0001f)
        assertEquals(.5f,CantoneseVolumePolicy.playerGain(50),.0001f)
        assertEquals(1f,CantoneseVolumePolicy.playerGain(100),.0001f)
        assertEquals(1f,CantoneseVolumePolicy.playerGain(200),.0001f)
        assertEquals(0,CantoneseVolumePolicy.boostMillibels(50))
        assertEquals(0,CantoneseVolumePolicy.boostMillibels(100))
        assertTrue(CantoneseVolumePolicy.boostMillibels(150) in 350..355)
        assertTrue(CantoneseVolumePolicy.boostMillibels(200) in 600..603)
        assertEquals(75,CantoneseVolumePolicy.step(73.3f))
        assertEquals(200,CantoneseVolumePolicy.step(205f))
        assertTrue((0..200 step 5).zipWithNext().all {
            (first,second) -> CantoneseVolumePolicy.boostMillibels(first)<=
                CantoneseVolumePolicy.boostMillibels(second)
        })
        // TTS fallback cannot exceed its engine's documented 0..1 gain.
        assertEquals(1f,CantoneseVolumePolicy.ttsGain(200),.0001f)
    }

    private fun q(id:String,grade:Int,stars:Int,domain:String="自然科学",topic:String="动物分类") =
        KnowledgeQuestion(id,grade,domain,topic,"填空题",stars,
            "蝴蝶属于什么动物？","昆虫",listOf("昆虫","鱼类","鸟类","哺乳类"),
            "蝴蝶有六条腿。","")
    private val bank=listOf(
        q("a",1,1),q("b",1,2),q("c",2,1),
        q("d",3,3,"地理","国家与大洲"),
        q("e",3,1,"自然科学","植物"),
        q("f",5,5,"信息科技","网络"))
    @Test fun literaryDomainAppearsInGradeOneOnlyWhenGradeOneHasRealQuestions() {
        val questions=bank+listOf(
            q("lit1",1,1,"语文文学","古诗启蒙"),
            q("lit2",2,1,"语文文学","古诗阅读"),
            q("lit3",3,1,"语文文学","古诗作者"))
        val first=QuestionFilter(grade=1)
        val second=QuestionFilter(grade=2)
        assertTrue(QuestionFilterChoices.domains(questions,first).contains("语文文学"))
        assertTrue(QuestionFilterChoices.domains(questions,second).contains("语文文学"))
        assertFalse(QuestionFilterChoices.domains(bank,first).contains("语文文学"))
        assertEquals(listOf("古诗启蒙"),
            QuestionFilterChoices.topics(questions,first.copy(domain="语文文学")))
        assertEquals(listOf("古诗阅读"),
            QuestionFilterChoices.topics(questions,second.copy(domain="语文文学")))
    }

    @Test fun selectingLiteratureNeverSilentlyBroadensCurricularGrade() {
        val questions=bank+listOf(
            q("lit1",1,1,"语文文学","古诗启蒙"),
            q("lit2",2,1,"语文文学","古诗阅读"),
            q("lit3",3,1,"语文文学","古诗作者"))
        for(g in 1..3) {
            val selected=QuestionFilterChoices.selectDomain(
                questions,QuestionFilter(grade=g),"语文文学")
            assertEquals(g,selected.grade)
            assertEquals("语文文学",selected.domain)
            assertEquals(1,KnowledgeBank.matching(questions,selected).size)
            assertTrue(KnowledgeBank.round(questions,selected,5).all {it.grade==g})
        }
        // No high-grade literature magically appears in a grade with no content.
        val grade4=QuestionFilterChoices.selectDomain(
            questions,QuestionFilter(grade=4),"语文文学")
        assertEquals(4,grade4.grade)
        assertEquals("全部",grade4.domain)
    }

    @Test fun selectingGradeScopedDomainClearsOnlyIncompatibleKind() {
        val questions=bank+listOf(
            q("lit1",1,1,"语文文学","古诗启蒙"),
            q("lit2",2,1,"语文文学","古诗阅读"))
        val selected=QuestionFilterChoices.selectDomain(
            questions,QuestionFilter(grade=1,kind="判断题"),"语文文学")
        assertEquals(1,selected.grade)
        assertEquals("全部",selected.kind)
        assertEquals("语文文学",selected.domain)
        assertEquals(1,KnowledgeBank.matching(questions,selected).size)
        val topic=QuestionFilterChoices.selectTopic(questions,
            selected.copy(kind="判断题"),"古诗启蒙")
        assertEquals(1,topic.grade)
        assertEquals("全部",topic.kind)
        assertEquals(1,KnowledgeBank.matching(questions,topic).size)
    }

    @Test fun gradeIsTheOnlyCurricularDifficultyFilter() {
        // Both source stars are accepted: stars were almost a duplicate of grade.
        assertEquals(listOf("a","b"),KnowledgeBank.matching(bank,QuestionFilter(grade=1)).map{it.id})
    }
    @Test fun allGradesIncludeEveryOriginalStarLevel() {
        assertEquals(bank.size, KnowledgeBank.matching(bank,QuestionFilter(grade=-1)).size)
    }
    @Test fun simultaneousFiltersIntersectRatherThanUnion() {
        val list=KnowledgeBank.matching(bank,QuestionFilter(
            grade=-1,domain="自然科学",topic="动物分类"))
        assertEquals(listOf("a","b","c"),list.map{it.id})
    }
    @Test fun roundNeverRepeatsOrOverflowsPool() {
        repeat(80) {seed ->
            val round=KnowledgeBank.round(bank,QuestionFilter(grade=-1),99,Random(seed))
            assertEquals(bank.size,round.size)
            assertEquals(round.size,round.distinctBy{it.id}.size)
        }
    }
    @Test fun consecutiveRoundsPreferQuestionsNotRecentlySeen() {
        val all=QuestionFilter(grade=-1)
        val recent=setOf("a","b","c","d")
        val round=KnowledgeBank.round(bank,all,2,Random(8),avoidIds=recent)
        assertEquals(setOf("e","f"),round.map{it.id}.toSet())
        val fallback=KnowledgeBank.round(bank,all,50,Random(7),avoidIds=recent)
        assertEquals(6,fallback.size)
        assertEquals(6,fallback.map{it.id}.toSet().size)
        assertEquals(setOf("e","f"),fallback.take(2).map{it.id}.toSet())
    }
    @Test fun reactionTimeStaysInsideOriginalFourIntervals() {
        val ranges=listOf(6000L..8000L,4000L..6000L,2500L..4000L,1500L..2500L)
        ranges.forEachIndexed {level, range ->
            repeat(1000) {
                assertTrue("level=$level",DuelRules.reactionMs(level) in range)
            }
        }
    }
    @Test fun accuracyTieUsesElapsedTimeAndCanDraw() {
        assertEquals(0,DuelRules.winnerByAccuracy(7,6,40000,10000))
        assertEquals(1,DuelRules.winnerByAccuracy(7,7,40000,30000))
        assertEquals(-1,DuelRules.winnerByAccuracy(7,7,30000,30000))
    }
    @Test fun unrevealedIllustrationDoesNotReadCorrectAnswer() {
        val question=q("a",1,1)
        assertEquals(pictureFor(question),pictureFor(question.copy(answer="恐龙")))
    }
    @Test fun originalSourceKindFilterIsApplied() {
        val list=bank + q("judgment",1,1).copy(kind="判断题")
        assertEquals(listOf("judgment"),KnowledgeBank.matching(list,
            QuestionFilter(grade=-1,kind="判断题")).map { it.id })
    }
    @Test fun cantoneseJyutpingUsesSixNumberedTones() {
        assertTrue(CantoneseRules.validJyutping("sik6 faan6"))
        assertTrue(CantoneseRules.validJyutping("ng5"))
        assertTrue(CantoneseRules.validJyutping("m4 goi1"))
        assertFalse(CantoneseRules.validJyutping("ni hao"))
        assertFalse(CantoneseRules.validJyutping("m4 goi7"))
        assertFalse(CantoneseRules.validJyutping(""))
    }

    @Test fun cantoneseListeningOptionsContainUniqueTargetAndLocalDistractors() {
        val options=(0 until 10).map {
            CantoneseWord("demo$it","word","点心茶楼","词$it","詞$it",
                "zi6","例子","🥟","提示")
        }
        repeat(80) { seed ->
            val target=options[seed%options.size]
            val choices=CantoneseRules.options(options,target,Random(seed))
            assertEquals(4,choices.size)
            assertEquals(4,choices.map {it.id}.toSet().size)
            assertEquals(1,choices.count {it.id==target.id})
            assertTrue(choices.all {it.category==target.category})
        }
        assertEquals(5,CantoneseRules.newRound(options,5,Random(2)).size)
    }

    @Test fun pinyinPolicyRecognizesChineseWithoutTouchingLatinOrEmoji() {
        assertTrue(PinyinPolicy.hasHan("春眠不觉晓"))
        assertTrue(PinyinPolicy.hasHan("🔵 1年级 · 10题"))
        assertFalse(PinyinPolicy.hasHan("ABC 123 ! 😊"))
        assertTrue(PinyinPolicy.isHan('春'))
        assertFalse(PinyinPolicy.isHan('9'))
    }

    @Test fun pinyinMixedNumbersLettersAndEmojiShareGlyphCells() {
        // Regression: digits used to sit lower than adjacent annotated Han.
        val example=PinyinPolicy.glyphs("第12题 · 3年级 😊")
        assertEquals(listOf("第","1","2","题"," ","·"," ","3","年","级"," ","😊"),
            example.map {it.value})
        assertEquals(1,example[1].offset)
        assertEquals(2,example[2].offset)
        assertEquals(3,example[3].offset)
        assertTrue(PinyinPolicy.isVisibleGlyph("2"))
        assertTrue(PinyinPolicy.isVisibleGlyph("A"))
        assertTrue(PinyinPolicy.isVisibleGlyph("年"))
        assertTrue(PinyinPolicy.isVisibleGlyph("😊"))
        assertFalse(PinyinPolicy.isVisibleGlyph(" "))
        assertFalse(PinyinPolicy.isVisibleGlyph("\n"))
        val joined=PinyinPolicy.glyphs("1👩‍👧2")
        assertEquals(listOf("1","👩‍👧","2"),joined.map{it.value})
    }

    @Test fun pinyinPolyphonicOverridesCoverCommonLearningWords() {
        assertEquals(listOf("yīn","yuè"),PinyinPolicy.phraseOverrides["音乐"])
        assertEquals(listOf("chóng","yáng"),PinyinPolicy.phraseOverrides["重阳"])
        assertEquals(listOf("háng","zhǎng"),PinyinPolicy.phraseOverrides["行长"])
        assertEquals(listOf("zhǎng","dà"),PinyinPolicy.phraseOverrides["长大"])
    }

    @Test fun speedRaceNoticeMatchesMathPlanetForBothSeats() {
        assertEquals("⚡ 先答对者得分",DuelRaceNotice.forSide(true,-1,0))
        assertEquals("⚡ 先答对者得分",DuelRaceNotice.forSide(true,-1,1))
        assertEquals("✅ 你抢答成功！",DuelRaceNotice.forSide(true,0,0))
        assertEquals("⚡ 对方已抢答成功！",DuelRaceNotice.forSide(true,0,1))
        assertEquals("✅ 你抢答成功！",DuelRaceNotice.forSide(true,1,1))
        assertEquals("⚡ 对方已抢答成功！",DuelRaceNotice.forSide(true,1,0))
    }

    @Test fun raceNoticeDoesNotLeakIntoAccuracyModeOrBeforeCorrectAnswer() {
        for(winner in listOf(-1,0,1)) {
            for(side in 0..1) {
                assertEquals("",DuelRaceNotice.forSide(false,winner,side))
            }
        }
        // Wrong answers keep raceWinner=-1, so no victory is announced.
        assertFalse(DuelRaceNotice.forSide(true,-1,1).contains("抢答成功"))
        assertEquals(1_650L,DuelRaceNotice.SUCCESS_DISPLAY_MS)
    }

    @Test fun playersFaceTheirOwnPhysicalTabletEdges() {
        assertEquals(90f,DuelSeatLayout.rotationFor(0))
        assertEquals(-90f,DuelSeatLayout.rotationFor(1))
    }
    @Test fun musicPausesCompletelyForNarrationAndResumesAfterward() {
        val normal=WorldMusicLevels.gain(true,55,true,false)
        val speech=WorldMusicLevels.gain(true,55,true,true)
        assertTrue(normal>0f)
        // Volume ducking alone leaves MediaPlayer active on PA2373.
        assertEquals(0f,speech)
        assertEquals(normal,WorldMusicLevels.gain(true,55,true,false))
        assertEquals(0f,WorldMusicLevels.gain(true,100,true,true))
        assertEquals(0f,WorldMusicLevels.gain(true,55,false,false))
        assertEquals(0f,WorldMusicLevels.gain(false,100,true,false))
        assertEquals(0f,WorldMusicLevels.gain(true,0,true,false))
    }
    @Test fun studyAndQuizUseIndependentThinkingDurations() {
        val start=100_000L
        assertEquals(3_000L,ExplanationGate.STUDY_WAIT_MS)
        assertEquals(10_000L,ExplanationGate.QUIZ_WAIT_MS)
        assertEquals(3L,ExplanationGate.secondsRemaining(start,start,
            ExplanationGate.STUDY_WAIT_MS))
        assertFalse(ExplanationGate.canReveal(start,start+2_999L,
            ExplanationGate.STUDY_WAIT_MS))
        assertEquals(1L,ExplanationGate.secondsRemaining(start,start+2_999L,
            ExplanationGate.STUDY_WAIT_MS))
        assertTrue(ExplanationGate.canReveal(start,start+3_000L,
            ExplanationGate.STUDY_WAIT_MS))
        assertFalse(ExplanationGate.canReveal(start,start+3_000L,
            ExplanationGate.QUIZ_WAIT_MS))
        assertEquals(7L,ExplanationGate.secondsRemaining(start,start+3_000L,
            ExplanationGate.QUIZ_WAIT_MS))
        assertTrue(ExplanationGate.canReveal(start,start+10_000L,
            ExplanationGate.QUIZ_WAIT_MS))
    }
    @Test fun adaptiveTabletLayoutUsesAvailableWindowSize() {
        assertTrue(ResponsiveWindowPolicy.tabletLandscape(1024f,600f))
        assertTrue(ResponsiveWindowPolicy.tabletLandscape(1280f,800f))
        assertTrue(ResponsiveWindowPolicy.tabletLandscape(720f,450f))
        assertFalse(ResponsiveWindowPolicy.tabletLandscape(620f,950f))
        assertFalse(ResponsiveWindowPolicy.tabletLandscape(900f,400f))
        assertFalse(ResponsiveWindowPolicy.tabletLandscape(700f,500f))
    }

    @Test fun tabletPcmUses1800MsBufferAnd1350MsPrefill() {
        assertEquals(1_800,TabletPcmPolicy.BUFFER_MS)
        assertEquals(345_600,TabletPcmPolicy.bufferSize(4096))
        assertEquals(345_604,TabletPcmPolicy.bufferSize(345_603))
        assertEquals(259_200,TabletPcmPolicy.prefillBytes(345_600,600_000))
        assertEquals(4_000,TabletPcmPolicy.prefillBytes(345_600,4_001))
        assertEquals(0,TabletPcmPolicy.prefillBytes(345_600,0))
    }

    @Test fun noBackendRetryAfterAudioHasAlreadyStarted() {
        assertTrue(TabletPcmPolicy.canFallbackAfterFailure(false,true))
        assertFalse(TabletPcmPolicy.canFallbackAfterFailure(true,true))
        assertFalse(TabletPcmPolicy.canFallbackAfterFailure(false,false))
        assertFalse(TabletPcmPolicy.canFallbackAfterFailure(true,false))
    }

    @Test fun prebufferNeverExceedsClipOrThreeQuartersCapacity() {
        val capacity=TabletPcmPolicy.bufferSize(4096)
        for(n in listOf(0,4,8,4000,240_000,259_200,500_000,800_000)) {
            val prefill=TabletPcmPolicy.prefillBytes(capacity,n)
            assertTrue(prefill<=n)
            assertTrue(prefill<=capacity*3/4)
            assertEquals(0,prefill%TabletPcmPolicy.FRAME_BYTES)
        }
    }

    @Test fun wavSynthesizedByTtsCanBeResampledTo48kStereo() {
        val frames=160
        val pcm=ByteArray(frames*2)
        for(i in 0 until frames){
            val v=if(i%2==0) 12_000 else -12_000
            pcm[i*2]=(v and 255).toByte()
            pcm[i*2+1]=((v shr 8) and 255).toByte()
        }
        val wav=ByteBuffer.allocate(44+pcm.size).order(ByteOrder.LITTLE_ENDIAN).apply{
            put("RIFF".toByteArray(Charsets.US_ASCII));putInt(36+pcm.size)
            put("WAVEfmt ".toByteArray(Charsets.US_ASCII));putInt(16)
            putShort(1);putShort(1)
            putInt(16_000);putInt(32_000);putShort(2);putShort(16)
            put("data".toByteArray(Charsets.US_ASCII));putInt(pcm.size);put(pcm)
        }
        val temp=File.createTempFile("knowledge_tts_test_",".wav")
        try {
            temp.writeBytes(wav.array())
            val data=SpeechPcm.read(temp)
            assertEquals(1,data.channels)
            assertEquals(16_000,data.sampleRate)
            val stereo=SpeechPcm.to48kStereo(data)
            assertEquals(480*4,stereo.size)
            for(i in 0 until 480){
                assertEquals(stereo[i*4],stereo[i*4+2])
                assertEquals(stereo[i*4+1],stereo[i*4+3])
            }
        } finally {temp.delete()}
    }

    @Test fun stereoTtsWavKeepsIndependentChannels() {
        val pcm=ByteBuffer.allocate(480*4).order(ByteOrder.LITTLE_ENDIAN).apply {
            repeat(480){putShort(7000);putShort((-7000).toShort())}
        }
        val converted=SpeechPcm.to48kStereo(SpeechPcm.Wave(2,48_000,pcm.array()))
        assertEquals(480*4,converted.size)
        val middle=240*4
        val left=((converted[middle].toInt() and 255) or
            ((converted[middle+1].toInt() and 255) shl 8)).toShort().toInt()
        val right=((converted[middle+2].toInt() and 255) or
            ((converted[middle+3].toInt() and 255) shl 8)).toShort().toInt()
        assertTrue(left>0 && right<0)
    }

    @Test fun malformedTtsWavIsRejectedInsteadOfPlayingGarbage() {
        val tmp=File.createTempFile("knowledge_invalid_",".wav")
        try {
            tmp.writeText("not a RIFF file")
            assertThrows(IllegalArgumentException::class.java){SpeechPcm.read(tmp)}
        } finally {tmp.delete()}
    }

    @Test fun pcmStartupGuardProtectsFirstSyllableWithoutTrimmingSpeech() {
        val voice=ByteBuffer.allocate(48_000/10*4)
            .order(ByteOrder.LITTLE_ENDIAN).apply {
                repeat(48_000/10) {
                    putShort(10_000.toShort());putShort((-8_000).toShort())
                }
            }.array()
        val protected=SpeechPcm.withStartupGuard(voice)
        val silenceBytes=48_000*SpeechPcm.STARTUP_SILENCE_MS/1000*4
        assertEquals(120,SpeechPcm.STARTUP_SILENCE_MS)
        assertEquals(25,SpeechPcm.STARTUP_FADE_MS)
        assertEquals(silenceBytes+voice.size,protected.size)
        assertTrue(protected.take(silenceBytes).all { it==0.toByte() })
        assertEquals(0,protected[silenceBytes].toInt())
        // The first 25ms are faded in; after that the waveform is unchanged.
        val unmodifiedAt=silenceBytes+48_000*30/1000*4
        val originalAt=48_000*30/1000*4
        for(i in 0 until 100){
            assertEquals(voice[originalAt+i],protected[unmodifiedAt+i])
        }
        assertEquals(voice.last(),protected.last())
    }

    @Test fun startupGuardHonorsFrameAlignmentAndEmptyUtterances() {
        assertTrue(SpeechPcm.withStartupGuard(byteArrayOf()).isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            SpeechPcm.withStartupGuard(byteArrayOf(1,2,3))
        }
        val input=byteArrayOf(1,2,3,4,5,6,7,8)
        assertArrayEquals(input,SpeechPcm.withStartupGuard(input,0,0))
        val padded=SpeechPcm.withStartupGuard(input,100,0)
        assertEquals(input.size+19_200,padded.size)
        assertArrayEquals(input,padded.takeLast(8).toByteArray())
    }

    @Test fun revealGateHoldsForFullTenSeconds() {
        val at=123456L
        assertFalse(ExplanationGate.canReveal(at,at))
        assertFalse(ExplanationGate.canReveal(at,at+9999L))
        assertTrue(ExplanationGate.canReveal(at,at+10000L))
        assertEquals(10L,ExplanationGate.secondsRemaining(at,at))
        assertEquals(1L,ExplanationGate.secondsRemaining(at,at+9999L))
        assertEquals(0L,ExplanationGate.secondsRemaining(at,at+10000L))
    }
    @Test fun revealGateGuardsAgainstClockGoingBackwards() {
        assertEquals(10000L,ExplanationGate.remainingMs(5000L,1000L))
        assertEquals(0L,ExplanationGate.remainingMs(5000L,16000L))
    }
}
