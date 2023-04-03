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

import java.util.*;

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
            analyzeReferences(event.getProject(), (PsiMethod) psiElement, new Stack<>());
        }
    }

    private void analyzeReferences(Project project, PsiMethod method, Stack<PsiMethod> visitedMethods) {
        visitedMethods.push(method);

        // Search for references to the method
        Collection<PsiReference> references = ReferencesSearch.search(method).findAll();

        for (PsiReference reference : references) {
            PsiElement referenceElement = reference.getElement();

            // Get the parent method for the current reference
            PsiMethod parentMethod = PsiTreeUtil.getParentOfType(referenceElement, PsiMethod.class);

            analyzeReferences(project, parentMethod, visitedMethods);
        }

        if (references.isEmpty()) {
            printStack(project, visitedMethods);
        }

        visitedMethods.pop();
    }

    private void printStack(Project project, Stack<PsiMethod> visitedMethods) {
        int indentation = 0;

        ListIterator<PsiMethod> listIterator = visitedMethods.listIterator(visitedMethods.size());

        while (listIterator.hasPrevious()) {
            printMessage(project, listIterator.previous(), indentation);
            indentation += 4; // The amount of spaces to be used on each line
        }
    }

    private void printMessage(Project project, PsiMethod method, int indentation) {
        // Get class and method names
        String methodName = method.getName();
        String className = method.getContainingClass().getName();

        // Calculate line number of method definition
        PsiFile file = method.getContainingFile();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        int lineNumber = document.getLineNumber(method.getTextRange().getStartOffset()) + 1;

        // Print the result into console
        String padding = new String(new char[indentation]).replace('\0', ' '); // I have to do this because the %0s format breaks
        System.out.printf("%s%s.%s:%d%n", padding, className, methodName, lineNumber);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Set the availability based on whether an editor is open
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setEnabledAndVisible(editor != null);
        event.getPresentation().setIcon(DiagramDiff);
    }
}
