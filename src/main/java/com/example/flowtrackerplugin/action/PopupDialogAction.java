package com.example.flowtrackerplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.intellij.icons.AllIcons.Actions.DiagramDiff;

public class PopupDialogAction extends AnAction {

    public PopupDialogAction() {
        super();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        PsiElement psiElement = event.getData(LangDataKeys.PSI_ELEMENT);

        // Search for references to the method
        if (psiElement instanceof PsiMethod) {
            analyzeReferences(event.getProject(), (PsiMethod) psiElement);
        }
    }

    private int analyzeReferences(Project project, PsiMethod method) {
        final int indentation = 4; // The amount of spaces to be used on each line

        // Search for references to the method
        Collection<PsiReference> references = ReferencesSearch.search(method).findAll();

        for (PsiReference reference : references) {
            PsiElement referenceElement = reference.getElement();

            // Get the parent method for the current reference
            PsiMethod parentMethod = PsiTreeUtil.getParentOfType(referenceElement, PsiMethod.class);

            int indentationLevel = analyzeReferences(project, parentMethod);

            // Get class and method names
            String parentMethodName = parentMethod.getName();
            String parentClassName = parentMethod.getContainingClass().getName();

            // Calculate line number of parenMethod definition
            PsiFile parentFile = parentMethod.getContainingFile();
            Document parentDocument = PsiDocumentManager.getInstance(project).getDocument(parentFile);
            int parentLineNumber = parentDocument.getLineNumber(parentMethod.getTextRange().getStartOffset()) + 1;

            // Print the result into console
            String padding = new String(new char[indentationLevel]).replace('\0', ' '); // I have to do this because the %0s format breaks
            System.out.println(String.format("%s%s.%s:%d", padding, parentClassName, parentMethodName, parentLineNumber));

            return indentationLevel + indentation;
        }

        return 0;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Set the availability based on whether an editor is open
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setEnabledAndVisible(editor != null);
        event.getPresentation().setIcon(DiagramDiff);
    }
}
