package com.github.yankee42.aconsosync.gui

import com.github.yankee42.aconsosync.AconsoSync
import com.github.yankee42.aconsosync.LocalRepository
import com.github.yankee42.aconsosync.LoginFailedException
import com.github.yankee42.aconsosync.createHttpClient
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.nio.file.Paths
import java.util.prefs.Preferences
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTextField
import javax.swing.SwingUtilities

class Preference(val readFromPreferences: (prefs: Preferences) -> Unit, val writeToPreferences: (prefs: Preferences) -> Unit)
fun JTextField.asPreference(name: String) =
    Preference({ prefs -> prefs.get(name, null)?.also { text = it } }, { it.put(name, text) })
fun JCheckBox.asPreference(name: String) =
    Preference({ isSelected = it.getBoolean(name, isSelected) }, { it.putBoolean(name, isSelected) })

class MainFrame : JFrame("Aconso Sync") {
    private val logger = LoggerFactory.getLogger(MainFrame::class.java)
    private val urlField = JTextField("https://YOUR_COMPANY.hr-document-box.com/")
    private val usernameField = JTextField()
    private val passwordField = JPasswordField()
    private val savePasswordField = JCheckBox("", true)
    private val localDirField = JTextField()
    private val preferences = arrayOf(
        urlField.asPreference("url"),
        usernameField.asPreference("username"),
        savePasswordField.asPreference("savePassword"),
        localDirField.asPreference("localDir"),
        "password".let { name ->
            Preference(
                { passwordField.text = it.get(name, "") },
                { it.put(name, if (savePasswordField.isSelected) passwordField.password.concatToString() else "") }
            )
        }
    )

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = GridBagLayout()

        add(JLabel("URL:"), constraints(0, 0))
        add(urlField, constraints(0, 1))

        add(JLabel("Username:"), constraints(1, 0))
        add(usernameField, constraints(1, 1))

        add(JLabel("Password:"), constraints(2, 0))
        add(passwordField, constraints(2, 1))

        add(JLabel("Save Password:"), constraints(3, 0))
        add(savePasswordField, constraints(3, 1))

        add(JLabel("Local Dir:"), constraints(4, 0))
        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(localDirField)
            add(JButton("Browse").apply { addActionListener { JFileChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                if (showSaveDialog(this@MainFrame) == JFileChooser.APPROVE_OPTION) {
                    localDirField.text = selectedFile.toString()
                }
            } } })
        }, constraints(4, 1))

        add(
            JButton("Save Settings & Sync").apply { addActionListener { sync() } },
            constraints(5, 0).apply { gridwidth = 2 }
        )

        loadPreferences()
        setSize(500, 300)
        isVisible = true
    }

    private fun sync() {
        val statusFrame = StatusFrame(this)
        swingWorker {
            savePreferences()
            try {
                doSync(Url(urlField.text), statusFrame)
            } catch (e: LoginFailedException) {
                logger.error("Login failed", e)
                SwingUtilities.invokeLater { JOptionPane.showMessageDialog(
                    this, "Login failed. Check your credentials", "Login failed", JOptionPane.ERROR_MESSAGE
                ) }
            }
        }
        statusFrame.isVisible = true
    }

    private fun doSync(rootUrl: Url, statusFrame: StatusFrame) {
        runBlocking {
            createHttpClient(rootUrl).use { httpClient ->
                AconsoSync(
                    httpClient,
                    rootUrl,
                    LocalRepository(Paths.get(localDirField.text))
                ) { statusFrame.processEvent(it) }
                    .syncDocuments(usernameField.text, passwordField.password.concatToString())
            }
        }
    }

    private fun savePreferences() {
        withPreferences {
            preferences.forEach { it.writeToPreferences(this) }
        }
    }

    private fun loadPreferences() {
        withPreferences {
            preferences.forEach { it.readFromPreferences(this) }
        }
    }

    private fun withPreferences(block: Preferences.() -> Unit) {
        Preferences.userRoot().node("com.github.yankee42.aconsosync").apply(block)
    }
}

private fun constraints(x: Int, y: Int) = GridBagConstraints().apply {
    gridx = y
    gridy = x
    fill = GridBagConstraints.HORIZONTAL
    weightx = .5
    insets = Insets(3, 3, 3, 3)
}
