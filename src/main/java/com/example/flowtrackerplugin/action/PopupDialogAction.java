package com.example.flowtrackerplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.ListIterator;
import java.util.Stack;
import static java.util.AbstractMap.SimpleEntry;

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
            analyzeReferences(event.getProject(), (PsiMethod) psiElement, null, new Stack<>());
        }
    }

    private void analyzeReferences(Project project, PsiMethod method, PsiElement methodReference, Stack<SimpleEntry<PsiMethod, PsiElement>> visitedMethods) {
        visitedMethods.push(new SimpleEntry<>(method, methodReference));

        // Search for references to the method
        Collection<PsiReference> references = ReferencesSearch.search(method).findAll();

        for (PsiReference reference : references) {
            PsiElement referenceElement = reference.getElement();

            // Get the parent method for the current reference
            PsiMethod parentMethod = PsiTreeUtil.getParentOfType(referenceElement, PsiMethod.class);

            analyzeReferences(project, parentMethod, referenceElement, visitedMethods);
        }

        if (references.isEmpty()) {
            printStack(project, visitedMethods);
        }

        visitedMethods.pop();
    }

    private void printStack(Project project, Stack<SimpleEntry<PsiMethod, PsiElement>> visitedMethods) {
        int indentation = 0;

        ListIterator<SimpleEntry<PsiMethod, PsiElement>> listIterator = visitedMethods.listIterator(visitedMethods.size());

        while (listIterator.hasPrevious()) {
            printMessage(project, listIterator.previous(), indentation);
            indentation += 4; // The amount of spaces to be used on each line
        }
    }

    private void printMessage(Project project, SimpleEntry<PsiMethod, PsiElement> entry, int indentation) {
        PsiMethod method = entry.getKey();

        // Get class and method names
        String methodName = method.getName();
        String className = method.getContainingClass().getName();

        // Calculate line number of method definition
        PsiElement methodReference = entry.getValue();
        int lineNumber = calculateLineNumber(project, methodReference == null ? method : methodReference);

        // Print the result into console
        String padding = new String(new char[indentation]).replace('\0', ' '); // I have to do this because the %0s format breaks
        System.out.printf("%s%s.%s:%d%n", padding, className, methodName, lineNumber);
    }

    private int calculateLineNumber(Project project, PsiElement element) {
        PsiFile file = element.getContainingFile();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        return document.getLineNumber(element.getTextRange().getStartOffset()) + 1;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Set the availability based on whether an editor is open
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setEnabledAndVisible(editor != null);
        event.getPresentation().setIcon(DiagramDiff);
    }
}
