package s05;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;

public class Progress {
	public Progress() {
		$$$setupUI$$$();
	}
	
	
	JProgressBar bar;
	
	JLabel caption;
	JPanel root;
	JLabel action;
	
	private void $$$setupUI$$$() {
		this.root = new JPanel();
		this.root.setLayout(new GridLayoutManager(5, 3, new Insets(0, 0, 0, 0), -1, -1));
		this.root.setBackground(new Color(-16777216));
		Spacer spacer1 = new Spacer();
		this.root.add(spacer1, new GridConstraints(0, 1, 1, 1, 0, 2, 1, 4, new Dimension(-1, 8), null, null, 0, false));
		Spacer spacer2 = new Spacer();
		this.root.add(spacer2, new GridConstraints(4, 1, 1, 1, 0, 2, 1, 4, new Dimension(-1, 8), null, null, 0, false));
		this.caption = new JLabel();
		this.caption.setForeground(new Color(-2960686));
		this.caption.setHorizontalAlignment(SwingConstants.CENTER);
		this.caption.setHorizontalTextPosition(SwingConstants.CENTER);
		this.caption.setText("Scape05");
		this.root.add(this.caption, new GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, null, null, null, 0, false));
		this.bar = new JProgressBar();
		this.bar.setBorderPainted(false);
		this.root.add(this.bar, new GridConstraints(2, 1, 1, 1, 0, 1, 4, 0, new Dimension(256, 24), new Dimension(-1, 24), null, 0, false));
		Spacer spacer3 = new Spacer();
		this.root.add(spacer3, new GridConstraints(2, 2, 1, 1, 0, 1, 4, 1, new Dimension(64, -1), null, null, 0, false));
		Spacer spacer4 = new Spacer();
		this.root.add(spacer4, new GridConstraints(2, 0, 1, 1, 0, 1, 4, 1, new Dimension(64, -1), null, null, 0, false));
		this.action = new JLabel();
		this.action.setForeground(new Color(-2960686));
		this.action.setHorizontalAlignment(SwingConstants.CENTER);
		this.action.setHorizontalTextPosition(SwingConstants.CENTER);
		this.action.setText("Connecting to server...");
		this.root.add(this.action, new GridConstraints(3, 1, 1, 1, 4, 0, 0, 0, null, null, null, 0, false));
	}
	
	
	public JComponent $$$getRootComponent$$$() {
		return this.root;
	}
}
