package com.game.knowledge

import java.net.URLEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Regression: all Legado community voice rules must keep balanced URI calls. */
class LegadoHttpTtsTemplateTest {
    private fun eval(expr: String, text: String = "你好", speed: Int = 15): String =
        LegadoTtsRequestCompiler.expand("{{" + expr + "}}", text, speed)

    @Test fun nestedBaiduEncoding() {
        val once = URLEncoder.encode("你好", "UTF-8")
        val twice = URLEncoder.encode(once, "UTF-8")
        assertEquals(twice, eval("java.encodeURI(java.encodeURI(speakText))"))
    }

    @Test fun singleUriEncoding() {
        val once = URLEncoder.encode("你好", "UTF-8")
        assertEquals(once, eval("java.encodeURI(speakText)"))
        assertEquals(once, eval("(java.encodeURI(speakText))"))
        assertEquals(once, eval("encodeURIComponent(speakText)"))
        assertEquals(once, eval("java.encodeURIComponent(speakText)"))
    }

    @Test fun tripleEncoding() {
        val once=URLEncoder.encode("你好","UTF-8")
        val twice=URLEncoder.encode(once,"UTF-8")
        val thrice=URLEncoder.encode(twice,"UTF-8")
        assertEquals(thrice,eval("java.encodeURI(java.encodeURI(java.encodeURI(speakText)))"))
    }

    @Test fun directTextAndSpeed() {
        assertEquals("广东话",eval("speakText","广东话"))
        assertEquals("15",eval("speakSpeed"))
        assertEquals("6",eval("(speakSpeed + 5) / 10 + 4"))
        assertEquals("6",eval("String((speakSpeed + 5) / 10 + 4)"))
        assertEquals("-100",eval("String((speakSpeed) * 20 - 400)"))
    }

    @Test fun noArbitraryJavaScript() {
        assertThrows(IllegalArgumentException::class.java) {
            eval("source.getLoginInfoMap().get('AppKey')")
        }
    }

    @Test fun embeddedUrlTemplate() {
        val once = URLEncoder.encode("香蕉", "UTF-8")
        val twice = URLEncoder.encode(once, "UTF-8")
        val rule = "http://example.org/audio?text={{java.encodeURI(java.encodeURI(speakText))}}&s={{speakSpeed}}"
        assertEquals("http://example.org/audio?text=" + twice + "&s=20",
            LegadoTtsRequestCompiler.expand(rule,"香蕉",20))
    }
}
