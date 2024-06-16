package com.example.flowtrackerplugin.drawers.impl;

import com.example.flowtrackerplugin.action.FlowTrackerToolWindowFactory;
import com.example.flowtrackerplugin.drawers.Drawer;
import com.example.flowtrackerplugin.models.Node;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

import java.awt.Color;
import java.util.ListIterator;
import java.util.Stack;

public class DisaggregateDrawer implements Drawer {
    private final Project project;
    private final ConsoleView consoleView;
    private final ConsoleViewContentType classNameContentType = getContentType(ConsoleColor.BRIGHT_CYAN);
    private final ConsoleViewContentType methodNameContentType = getContentType(ConsoleColor.GREEN);
    private final ConsoleViewContentType lineNumberContentType = getContentType(ConsoleColor.ORANGE);
    private final ConsoleViewContentType separatorContentType = getContentType(ConsoleColor.RED);
    private final ConsoleViewContentType emptyContentType = getContentType(ConsoleColor.RED);

    public DisaggregateDrawer(Project project, ConsoleView consoleView) {
        this.project = project;
        this.consoleView = FlowTrackerToolWindowFactory.validateToolWindowIntegrity(project);
    }

    @Override
    public void draw(Node root) {
        innerDraw(root, new Stack<>());
    }
    private void innerDraw(Node node, Stack<Node> visitedMethods) {
        visitedMethods.push(node);
        if (node.getReferencesByClass().isEmpty()) {
            printStack(visitedMethods);
            visitedMethods.pop();
        } else {
            node.getReferencesByClass().forEach((psiClass, nodes) -> {
                nodes.forEach(referenceNode -> {
                    innerDraw(referenceNode, visitedMethods);
                });
            });
            visitedMethods.pop();
        }
    }

    private void printStack(Stack<Node> visitedMethods) {
        int indentation = 0;

        ListIterator<Node> listIterator = visitedMethods.listIterator(visitedMethods.size());

        StringBuilder stringBuilder = new StringBuilder();

        while (listIterator.hasPrevious()) {
            stringBuilder.append(buildMessage(project, listIterator.previous(), indentation));
            indentation += 4; // The amount of spaces to be used on each line
        }

        // This is to make the code more readable
        consoleView.print("\n", emptyContentType);

        // Print the result into console
        //logger.info(stringBuilder.toString());
    }

    private String buildMessage(Project project, Node node, int indentation) {
        PsiElement element = node.getElement();
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);

        method = method != null ? method : (PsiMethod) element;

        // Get class and method names
        String methodName = method.getName();
        String className = method.getContainingClass().getName();

        // Calculate line number of method definition
        PsiElement methodReference = element;
        int lineNumber = calculateLineNumber(project, methodReference == null ? method : methodReference);

        String padding = new String(new char[indentation]).replace('\0', ' '); // I have to do this because the %0s format breaks

        // Print the message into ConsoleView
        printMessage(padding, className, methodName, String.valueOf(lineNumber));

        String format = "%s" + ConsoleColor.BRIGHT_CYAN.ansiCode + "%s" + ConsoleColor.RED.ansiCode + "." +
                ConsoleColor.GREEN.ansiCode + "%s" + ConsoleColor.RED.ansiCode + ":" +
                ConsoleColor.ORANGE.ansiCode + "%d" + ConsoleColor.RESET.ansiCode + "%n";

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

    private ConsoleViewContentType getContentType(ConsoleColor consoleColor) {
        Color color = createColor(consoleColor.rgbCodes);
        return createContentType(consoleColor.name(), color);
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

    private enum ConsoleColor {
        RESET("\u001B[0m", new int[]{201, 72, 54}),
        BRIGHT_CYAN("\u001B[1;36m", new int[]{14, 185, 196}),
        GREEN("\u001B[1;32m", new int[]{105, 176, 48}),
        ORANGE("\u001B[0;33m", new int[]{163, 136, 47}),
        RED("\u001B[31m", new int[]{201, 72, 54});

        private final String ansiCode;
        private final int[] rgbCodes;

        ConsoleColor(String ansiCode, int[] rgbCodes) {
            this.ansiCode = ansiCode;
            this.rgbCodes = rgbCodes;
        }
    }
}
