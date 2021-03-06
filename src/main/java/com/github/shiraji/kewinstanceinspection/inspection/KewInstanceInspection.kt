package com.github.shiraji.kewinstanceinspection.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ui.DocumentAdapter
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.text.BadLocationException

class KewInstanceInspection : AbstractKotlinInspection() {

    var methodName = "newInstance";

    override fun getGroupDisplayName() = "Android"

    override fun getDisplayName() = "Fragment should implement $methodName (Kotlin)"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) = KewInstanceInspectionVisitor(holder, methodName)

    override fun createOptionsPanel(): JComponent? {
        val panel = InspectionOptionPanel()
        panel.methodNameTextField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent?) {
                e ?: return
                val document = e.document
                try {
                    methodName = document.getText(0, document.length)
                } catch (e1: BadLocationException) {
                }
            }
        })
        panel.methodNameTextField.text = methodName
        return panel.panel
    }
}