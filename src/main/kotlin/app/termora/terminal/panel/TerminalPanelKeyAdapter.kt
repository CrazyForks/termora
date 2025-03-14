package app.termora.terminal.panel

import app.termora.keymap.KeyShortcut
import app.termora.keymap.KeymapManager
import app.termora.terminal.Terminal
import com.formdev.flatlaf.util.SystemInfo
import java.awt.event.InputEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class TerminalPanelKeyAdapter(
    private val terminalPanel: TerminalPanel,
    private val terminal: Terminal,
    private val writer: TerminalWriter
) : KeyAdapter() {

    private val activeKeymap get() = KeymapManager.getInstance().getActiveKeymap()

    override fun keyTyped(e: KeyEvent) {
        if (Character.isISOControl(e.keyChar)) {
            return
        }

        terminal.getSelectionModel().clearSelection()

        writer.write(
            TerminalWriter.WriteRequest.fromBytes(
                "${e.keyChar}".toByteArray(writer.getCharset())
            )
        )

        terminal.getScrollingModel().scrollTo(Int.MAX_VALUE)

    }

    override fun keyPressed(e: KeyEvent) {
        if (e.isConsumed) return

        // remove all toast
        if (e.keyCode == KeyEvent.VK_ESCAPE) {
            terminalPanel.hideToast()
        }

        val keyStroke = KeyStroke.getKeyStrokeForEvent(e)
        for (action in terminalPanel.getTerminalActions()) {
            if (action.test(keyStroke, e)) {
                action.actionPerformed(e)
                return
            }
        }

        val encode = terminal.getKeyEncoder().encode(AWTTerminalKeyEvent(e))
        if (encode.isNotEmpty()) {
            writer.write(TerminalWriter.WriteRequest.fromBytes(encode.toByteArray(writer.getCharset())))
        }

        // https://github.com/TermoraDev/termora/issues/52
        if (SystemInfo.isWindows && e.keyCode == KeyEvent.VK_TAB && isCtrlPressedOnly(e)) {
            return
        }

        // 如果命中了全局快捷键，那么不处理
        if (keyStroke.modifiers != 0 && activeKeymap.getActionIds(KeyShortcut(keyStroke)).isNotEmpty()) {
            return
        }

        if (Character.isISOControl(e.keyChar)) {
            terminal.getSelectionModel().clearSelection()
            // 如果不为空表示已经发送过了，所以这里为空的时候再发送
            if (encode.isEmpty()) {
                writer.write(TerminalWriter.WriteRequest.fromBytes("${e.keyChar}".toByteArray(writer.getCharset())))
            }
            terminal.getScrollingModel().scrollTo(Int.MAX_VALUE)
        }

    }

    private fun isCtrlPressedOnly(e: KeyEvent): Boolean {
        val modifiersEx = e.modifiersEx
        return (modifiersEx and InputEvent.ALT_DOWN_MASK) == 0
                && (modifiersEx and InputEvent.ALT_GRAPH_DOWN_MASK) == 0
                && (modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0
                && (modifiersEx and InputEvent.SHIFT_DOWN_MASK) == 0
    }
}