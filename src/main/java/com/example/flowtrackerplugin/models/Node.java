package com.example.flowtrackerplugin.models;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node {
    private final PsiElement element;
    private final Map<PsiClass, List<Node>> referencesByClass = new HashMap<>();

    public Node(PsiElement element) {
        this.element = element;
    }

    public Node addReference(PsiElement element) {
        PsiClass elementClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        Node newNode = new Node(element);
        List<Node> references = referencesByClass.get(elementClass);
        if (references == null) {
            List<Node> newList = new ArrayList<>();
            newList.add(newNode);
            referencesByClass.put(elementClass, newList);
        } else {
            references.add(newNode);
        }
        return newNode;
    }
}
