package com.example.flowtrackerplugin.action;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.util.Collection;
import java.util.ListIterator;
import java.util.Stack;

import static com.intellij.icons.AllIcons.Actions.DiagramDiff;
import static java.util.AbstractMap.SimpleEntry;

public class FlowTrackerAction extends AnAction {
    private static final Logger logger = LogManager.getLogger(FlowTrackerAction.class.getName());
    private ConsoleView consoleView;
    private final ConsoleViewContentType classNameContentType;
    private final ConsoleViewContentType methodNameContentType;
    private final ConsoleViewContentType lineNumberContentType;
    private final ConsoleViewContentType separatorContentType;
    private final ConsoleViewContentType emptyContentType;
    private final int[] RGB_CYAN = new int[]{14, 185, 196};
    private final int[] RGB_GREEN = new int[]{105, 176, 48};
    private final int[] RGB_YELLOW = new int[]{163, 136, 47};
    private final int[] RGB_RED = new int[]{201, 72, 54};
    private final String ANSI_RESET = "\u001B[0m";
    private final String ANSI_BRIGHT_CYAN = "\u001B[1;36m";
    private final String ANSI_GREEN = "\u001B[1;32m";
    private final String ANSI_ORANGE = "\u001B[0;33m";
    private final String ANSI_RED = "\u001B[31m";
    private static final String TOOL_WINDOW_ICON = "/flowchart-consoleview-icon.png";

    public FlowTrackerAction() {
        super();

        // Initialize ConsoleViewContentTypes
        classNameContentType = getClassNameContentType();
        methodNameContentType = getMethodNameContentType();
        lineNumberContentType = getLineNumberContentType();
        separatorContentType = getSeparatorContentType();
        emptyContentType = getEmptyContentType();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();

        // Initialize the ConsoleView
        if (consoleView == null || consoleViewIsDisposed(project)) {
            consoleView = createConsoleView(project);
        }
        showConsoleView(project, consoleView);

        PsiElement psiElement = event.getData(LangDataKeys.PSI_ELEMENT);

        // Search for references to the method
        if (psiElement instanceof PsiMethod) {
            analyzeReferences(project, (PsiMethod) psiElement, null, new Stack<>());
        }
    }

    @Nullable
    private ConsoleView createConsoleView(@Nullable Project project) {
        if (project != null) {
            return TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        } else {
            return null;
        }
    }

    private void showConsoleView(Project project, ConsoleView consoleView) {
        final String toolWindowId = "FlowTrackerPlugin";

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(toolWindowId);

        if (toolWindow == null) {
            toolWindow = toolWindowManager.registerToolWindow(toolWindowId, true, ToolWindowAnchor.BOTTOM);
            toolWindow.setType(ToolWindowType.DOCKED, null);
        }

        Disposable disposable = consoleView::dispose;

        ContentManager contentManager = toolWindow.getContentManager();

        if (contentManager.getContentCount() == 0) {
            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            Content content = contentFactory.createContent(consoleView.getComponent(), "Console", true);
            content.setDisposer(disposable);
            contentManager.addContent(content);
        }

        // Load the icon image from the resources folder
        Icon icon = new ImageIcon(getClass().getResource(TOOL_WINDOW_ICON));
        toolWindow.setIcon(icon);

        toolWindow.show(consoleView::requestScrollingToEnd);
    }

    private boolean consoleViewIsDisposed(Project project) {
        if (consoleView != null)
            return project.isDisposed() || ((ConsoleViewImpl) consoleView).getEditor() == null || ((ConsoleViewImpl) consoleView).getEditor().isDisposed();
        return false;
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

            if (parentMethod.equals(method)) // This is to avoid a recursive call like with a Decorator Pattern
                continue;

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

        StringBuilder stringBuilder = new StringBuilder();

        while (listIterator.hasPrevious()) {
            stringBuilder.append(buildMessage(project, listIterator.previous(), indentation));
            indentation += 4; // The amount of spaces to be used on each line
        }

        // This is to make the code more readable
        consoleView.print("\n", emptyContentType);

        // Print the result into console
        logger.info(stringBuilder.toString());
    }

    private String buildMessage(Project project, SimpleEntry<PsiMethod, PsiElement> entry, int indentation) {
        PsiMethod method = entry.getKey();

        // Get class and method names
        String methodName = method.getName();
        String className = method.getContainingClass().getName();

        // Calculate line number of method definition
        PsiElement methodReference = entry.getValue();
        int lineNumber = calculateLineNumber(project, methodReference == null ? method : methodReference);

        String padding = new String(new char[indentation]).replace('\0', ' '); // I have to do this because the %0s format breaks

        String format = "%s" + ANSI_BRIGHT_CYAN + "%s" + ANSI_RED + "." + ANSI_GREEN + "%s" + ANSI_RED + ":" +
                ANSI_ORANGE + "%d" + ANSI_RESET + "%n";

        // Print the message into ConsoleView
        printMessage(padding, className, methodName, String.valueOf(lineNumber));

        return String.format(format, padding, className, methodName, lineNumber);
    }

    private void printMessage(String padding, String className, String methodName, String lineNumber) {
        consoleView.print(padding, emptyContentType);
        consoleView.print(className, classNameContentType);
        consoleView.print(".", separatorContentType);
        consoleView.print(methodName, methodNameContentType);
        consoleView.print(":", separatorContentType);
        consoleView.print(String.valueOf(lineNumber), lineNumberContentType);
        consoleView.print("\n", emptyContentType);
    }

    private int calculateLineNumber(Project project, PsiElement element) {
        PsiFile file = element.getContainingFile();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        return document.getLineNumber(element.getTextRange().getStartOffset()) + 1;
    }

    private ConsoleViewContentType getClassNameContentType() {
        Color color = createColor(RGB_CYAN);
        return createContentType("className", color);
    }

    private ConsoleViewContentType getMethodNameContentType() {
        Color color = createColor(RGB_GREEN);
        return createContentType("methodName", color);
    }

    private ConsoleViewContentType getLineNumberContentType() {
        Color color = createColor(RGB_YELLOW);
        return createContentType("lineNumber", color);
    }

    private ConsoleViewContentType getSeparatorContentType() {
        Color color = createColor(RGB_RED);
        return createContentType("separator", color);
    }

    private ConsoleViewContentType getEmptyContentType() {
        Color color = createColor(RGB_RED);
        return createContentType("default", color);
    }

    private Color createColor(int[] rgbColor) {
        float[] HSBValues = Color.RGBtoHSB(rgbColor[0], rgbColor[1], rgbColor[2], null);
        return Color.getHSBColor(HSBValues[0], HSBValues[1], HSBValues[2]);
    }

    private ConsoleViewContentType createContentType(String name, Color color) {
        TextAttributes textAttributes = new TextAttributes();
        textAttributes.setForegroundColor(color);
        return new ConsoleViewContentType(name, textAttributes);
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
