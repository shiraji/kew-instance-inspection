package com.github.shiraji.kewinstanceinspection.inspection

import com.github.shiraji.kewinstanceinspection.util.InspectionPsiUtil
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.KtLightClassForExplicitDeclaration
import org.jetbrains.kotlin.idea.core.getOrCreateCompanionObject
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAbstract

class KewInstanceInspectionVisitor(val holder: ProblemsHolder, val name: String) : KtVisitorVoid() {

    val QUALIFIED_NAME_OF_SUPER_CLASS = "android.app.Fragment";
    val QUALIFIED_NAME_OF_SUPER_CLASS_FOR_SUPPORT_LIBRARY = "android.support.v4.app.Fragment";

    override fun visitClass(klass: KtClass) {
        if (klass.isAbstract()) return
        val fqName = klass.fqName ?: return
        val baseClass = InspectionPsiUtil.createPsiClass(QUALIFIED_NAME_OF_SUPER_CLASS, klass.project) ?: return
        val supportFragment = InspectionPsiUtil.createPsiClass(QUALIFIED_NAME_OF_SUPER_CLASS_FOR_SUPPORT_LIBRARY, klass.project) ?: return
        val declaration = KtLightClassForExplicitDeclaration(fqName, klass)
        if (!(declaration.isInheritor(baseClass, true) || declaration.isInheritor(supportFragment, true))) return

        var hasMethod = false
        klass.getCompanionObjects().forEach {
            it.children.forEach {
                if (it is KtClassBody) {
                    it.children.forEach {
                        if (it is KtNamedFunction && it.name == name) {
                            hasMethod = true
                        }
                    }
                }
            }
        }

        if (!hasMethod) {
            holder.registerProblem(klass.nameIdentifier as PsiElement,
                    "Implement \"fun $name(): ${klass.name}\" inside companion object",
                    GenerateMethod(name))
        }
    }

    inner class GenerateMethod(val methodName: String) : LocalQuickFix {
        override fun getName() = "Implement fun $methodName()"
        override fun getFamilyName() = name
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val factory = KtPsiFactory(project)
            val klass = descriptor.psiElement.parent as KtClass

            val method = factory.createFunction(
                    """
                    fun $methodName(): ${klass.name} {
                    val fragment = ${klass.name}()
                    return fragment
                    }
                    """.trimMargin()
            )

            runWriteAction {
                klass.getOrCreateCompanionObject().getOrCreateBody().addAfter(method, klass.getOrCreateCompanionObject().getOrCreateBody().lBrace)
            }
        }
    }

}