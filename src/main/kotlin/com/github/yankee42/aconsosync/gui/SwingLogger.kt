package com.github.yankee42.aconsosync.gui

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import java.awt.Color
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

private fun createStyle(bold: Boolean = false, italic: Boolean = false, foreground: Color = Color.BLACK) =
    SimpleAttributeSet().apply {
        addAttribute(StyleConstants.CharacterConstants.Bold, bold)
        addAttribute(StyleConstants.CharacterConstants.Italic, italic)
        addAttribute(StyleConstants.CharacterConstants.Foreground, foreground)
    }

private val LOG_STYLES = mapOf(
    Level.ERROR to createStyle(bold = true, foreground = Color(153, 0, 0)),
    Level.WARN to createStyle(foreground = Color(153, 76, 0)),
    Level.INFO to createStyle(foreground = Color(0, 0, 153)),
    Level.DEBUG to createStyle(italic = true, foreground = Color(64, 64, 64)),
    Level.TRACE to createStyle(italic = true, foreground = Color(153, 0, 76))
)
private val LOG_STYLE_DEFAULT = createStyle(italic = true)

/**
 * Based on Appender by Rodrigo Garcia Lima (https://stackoverflow.com/a/33657637)
 */
class SwingLogger(private val lineLimit: Int = 2000) : AppenderBase<ILoggingEvent>() {
    override fun start() {
        patternLayout.context = context
        patternLayout.start()
        super.start()
    }

    override fun append(event: ILoggingEvent) {
        SwingUtilities.invokeLater {
            textPane.document.apply {
                val lineCountOverLimit = defaultRootElement.elementCount - lineLimit
                if (lineCountOverLimit > 0) {
                    remove(0, defaultRootElement.getElement(lineCountOverLimit).endOffset)
                }
            }
            textPane.document.insertString(
                textPane.document.length,
                patternLayout.doLayout(event),
                LOG_STYLES.getOrDefault(event.level, LOG_STYLE_DEFAULT)
            )
            textPane.caretPosition = textPane.document.length
        }
    }

    companion object {
        val textPane = JTextPane().apply { isEditable = false }
        private val patternLayout: PatternLayout = PatternLayout().apply {
            pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
        }
    }
}
