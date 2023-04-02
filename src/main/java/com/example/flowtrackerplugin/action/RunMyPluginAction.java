package com.example.flowtrackerplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

public class RunMyPluginAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        PsiElement psiElement = event.getData(LangDataKeys.PSI_ELEMENT);
        if (psiElement instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) psiElement;
            // Your code to analyze the method's AST and generate a report
            runMyPlugin(method);
        }
    }

    @Override
    public void update(AnActionEvent event) {
        /*PsiElement element = event.getData(LangDataKeys.PSI_ELEMENT);
        event.getPresentation().setEnabledAndVisible(element != null);*/
        PsiElement psiElement = event.getData(LangDataKeys.PSI_ELEMENT);
        boolean isMethod = psiElement instanceof PsiMethod;
        event.getPresentation().setEnabled(isMethod);
        event.getPresentation().setVisible(isMethod);
    }

    private void runMyPlugin(PsiMethod psiMethod) {
        // Call your plugin with the selected method

    }

    /*@Override
    public void actionPerformed(AnActionEvent event) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return

        // Get the selected method and pass it to your plugin
        val method = getSelectedMethod(editor, file)
        if (method != null) {
        runMyPlugin(project, method)
        }
        }

        override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val file = event.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile

        // Enable the action only when a method is selected
        event.presentation.isEnabledAndVisible = editor != null && file != null && getSelectedMethod(editor, file) != null
        }

private fun getSelectedMethod(editor: Editor, file: PsiJavaFile): PsiMethod? {
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset)
        return PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
        }

private fun runMyPlugin(project: Project, method: PsiMethod) {
        // Call your plugin with the selected method
        }*/
}

