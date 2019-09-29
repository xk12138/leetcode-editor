package com.shuzijun.leetcode.plugin.listener;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.shuzijun.leetcode.plugin.renderer.ProblemsListRenderer;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author shuzijun
 */
public class QueryDocumentListener implements DocumentListener {

    private JTextField jTextField;
    private JBScrollPane contentScrollPanel;
    private JBPopup queryPopup; // 搜索题目下拉弹窗

    public QueryDocumentListener(JTextField jTextField, JBScrollPane contentScrollPanel, ListPopupImpl queryPopup) {
        this.jTextField = jTextField;
        this.contentScrollPanel = contentScrollPanel;
        this.queryPopup = queryPopup;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        showMatchedQuestions(jTextField.getText());
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        showMatchedQuestions(jTextField.getText());
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        showMatchedQuestions(jTextField.getText());
    }

    private void showMatchedQuestions(String selectText) {

        if(StringUtils.isBlank(selectText)){
            queryPopup.dispose();
            return;
        }

        JViewport viewport = contentScrollPanel.getViewport();
        JTree tree = (JTree) viewport.getView();

        DefaultTreeModel treeMode = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeMode.getRoot();
        if (root.isLeaf() || root.getChildAt(0).isLeaf()) {
            queryPopup.dispose();
            return;
        }
        DefaultMutableTreeNode all = (DefaultMutableTreeNode) root.getChildAt(0);
        // 找到all里面所有符合条件的叶子结点，即叶子结点的字符里面包括查找字符
        DefaultListModel<DefaultMutableTreeNode> data = new DefaultListModel<>();
        for (int i = 0; i < all.getChildCount(); i++) {
            DefaultMutableTreeNode singleProblem = (DefaultMutableTreeNode) all.getChildAt(i);
            if (singleProblem.isLeaf() && singleProblem.getUserObject().toString().toUpperCase().replace(" ","").contains(selectText.toUpperCase().replace(" ",""))) {
                data.addElement(singleProblem);
            }
        }

        if(data.isEmpty()){
            queryPopup.dispose();
            return;
        }

        JBList<DefaultMutableTreeNode> list = new JBList<>(data);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(-1);
        list.setToolTipText("Double click to jump");
        list.setCellRenderer(new ProblemsListRenderer());
        JBScrollPane listScroller = new JBScrollPane(list);

        // 双击选中题目，关闭popup并跳转到相应位置
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                JBList<DefaultMutableTreeNode> list = (JBList<DefaultMutableTreeNode>) e.getSource();
                if (e.getClickCount() == 2) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        DefaultMutableTreeNode node = list.getModel().getElementAt(index);
                        jumpToTree(tree, node); // 跳转方法
                        queryPopup.dispose();
                    }
                }
            }
        });

        queryPopup.showUnderneathOf(jTextField);
    }

    private void jumpToTree(JTree tree, DefaultMutableTreeNode node) {
        if (tree == null || node == null) {
            return;
        }
        TreePath path = new TreePath(node.getPath());
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
        JViewport viewport = contentScrollPanel.getViewport();
        int selectedRow = tree.getLeadSelectionRow();
        int height = selectedRow < 3 ? 0 : (selectedRow - 3) * tree.getRowHeight();
        Point point = new Point(0, height);
        viewport.setViewPosition(point);
    }

}
