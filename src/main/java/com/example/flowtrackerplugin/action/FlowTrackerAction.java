package com.example.flowtrackerplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
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

public class FlowTrackerAction extends AnAction {

    public FlowTrackerAction() {
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

    /**
     * The goal of the algorithm is to analyze a Java codebase and identify all the methods that are called exclusively from non-test methods, i.e., methods that are not called by test code.
     * To accomplish this, the algorithm traverses the call hierarchy of each non-test method in the codebase and identifies all the methods that it calls. If a called method is not a test
     * method itself and is only called by test methods or is not called by any other method, it is considered a leaf node in the call hierarchy. The algorithm then prints out the full call
     * stack from the leaf node back to the original non-test method. This allows developers to identify and potentially refactor methods that are only used by non-test code, which may help
     * to improve code maintainability and reduce unnecessary code complexity.
     * @param project
     * @param method
     * @param methodReference
     * @param visitedMethods
     * @return
     */
    private boolean analyzeReferences(Project project, PsiMethod method, PsiElement methodReference, Stack<SimpleEntry<PsiMethod, PsiElement>> visitedMethods) {

        if (isTestClass(method.getContainingClass()) || isTestMethod(method)) {
            return true;
        }

        visitedMethods.push(new SimpleEntry<>(method, methodReference));

        // Search for references to the method
        Collection<PsiReference> references = ReferencesSearch.search(method).findAll();

        // Search for superMethods. I have to do this because ReferencesSearch works on the AST level, and the super method is not part of the current AST being searched
        PsiMethod[] superMethods = method.findSuperMethods();

        for (PsiMethod superMethod : superMethods) {
            references.addAll(ReferencesSearch.search(superMethod).findAll());
        }

        boolean isFinalNode = true;

        for (PsiReference reference : references) {
            PsiElement referenceElement = reference.getElement();

            // Get the parent method for the current reference
            PsiMethod parentMethod = PsiTreeUtil.getParentOfType(referenceElement, PsiMethod.class);

            boolean parentIsTestNode = analyzeReferences(project, parentMethod, referenceElement, visitedMethods);

            isFinalNode = isFinalNode && parentIsTestNode;
        }

        // Print the complete path
        if (isFinalNode && visitedMethods.size() > 1) {
            printStack(project, visitedMethods);
        }

        // Go back one level
        visitedMethods.pop();
        return false;
    }

    private boolean isTestClass(PsiClass psiClass) {
        return isTestDefinition(psiClass.getName());
    }

    private boolean isTestMethod(PsiMethod psiMethod) {
        return isTestDefinition(psiMethod.getName());
    }

    private boolean isTestDefinition(String definition) {
        return definition.toLowerCase().endsWith("test");
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
        boolean isMethodSelection = event.getData(CommonDataKeys.PSI_ELEMENT) instanceof PsiMethod;
        event.getPresentation().setEnabledAndVisible(editor != null && isMethodSelection);
        event.getPresentation().setIcon(DiagramDiff);
    }
}
