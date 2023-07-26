package com.example.flowtrackerplugin.action;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlowTrackerToolWindowFactory implements ToolWindowFactory {
    private static ConsoleView consoleView;
    private static final String TOOL_WINDOW_ID = "FlowTrackerPlugin";
    private static final String TOOL_WINDOW_TAB_ID = "Console";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (consoleView == null || projectOrEditorAreDisposed(project)) {
            consoleView = createConsoleView(project);
        }
        ContentManager contentManager = toolWindow.getContentManager();

        if (contentManager.getContentCount() == 0) {
            createTabContentContainer(contentManager);
        }

        toolWindow.hide();
    }

    private boolean projectOrEditorAreDisposed(Project project) {
        if (consoleView != null)
            return project.isDisposed() || ((ConsoleViewImpl) consoleView).getEditor() == null || ((ConsoleViewImpl) consoleView).getEditor().isDisposed();
        return false;
    }

    private ConsoleView createConsoleView(@Nullable Project project) {
        if (project != null) {
            return TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        } else {
            return null;
        }
    }

    private void createTabContentContainer(ContentManager contentManager) {
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(consoleView.getComponent(), TOOL_WINDOW_TAB_ID, false);
        content.setDisposer(consoleView);
        contentManager.addContent(content);
    }

    public static ConsoleView validateToolWindowIntegrity(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);

        if (toolWindow.getContentManager().getContentCount() == 0)
            new FlowTrackerToolWindowFactory().createToolWindowContent(project, toolWindow);

        toolWindow.show(consoleView::requestScrollingToEnd);

        return consoleView;
    }
}
