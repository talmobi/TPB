package com.heartpirates;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import com.heartpirates.IrcBot.IRCMessage;
import com.heartpirates.IrcBot.MessageListener;

public class Screen extends JFrame implements MessageListener {

	private boolean pressed = false;
	private Point startPoint;
	Font font = null;

	private final Screen screen;
	JTextArea jTextArea;

	int w = 400;
	int h = w * 9 / 16;

	long lineCount = 0;
	int lineLimit = 20;

	// colours
	Color bgColor = new Color(0x202020);
	Color fgColor = new Color(0xCFBFAD);
	Color transparentColor = new Color(0, 0, 0, 0);

	TypingRobot typingRobot = null;

	public Screen() {
		this.screen = this;
		initialize();
	}

	private void initialize() {
		this.setAlwaysOnTop(true);
		try {
			Font f = Font.createFont(Font.TRUETYPE_FONT, Screen.class
					.getClassLoader().getResourceAsStream("pixelmix.ttf"));
			font = f.deriveFont(8f);
		} catch (FontFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		MouseAdapter l = new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				Point p = e.getLocationOnScreen();

				p.x -= startPoint.x;
				p.y -= startPoint.y;

				screen.setLocation(p);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (!pressed) {
					startPoint = e.getPoint();
					pressed = true;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				pressed = false;
			}
		};

		this.addMouseListener(l);
		this.addMouseMotionListener(l);

		this.setBackground(bgColor);
		this.setForeground(fgColor);

		Dimension size = new Dimension(w, h);
		this.setSize(size);
		this.setPreferredSize(size);
		this.setMinimumSize(size);

		jTextArea = new JTextArea(20, 10);
		jTextArea.setBackground(bgColor);
		jTextArea.setForeground(fgColor);
		jTextArea.setFocusable(false);
		jTextArea.setDragEnabled(false);
		jTextArea.setEditable(false);
		jTextArea.setBounds(5, 5, w - 5, h - 5);

		jTextArea.addMouseListener(l);
		jTextArea.addMouseMotionListener(l);

		this.add(jTextArea);

		this.setUndecorated(true);
		this.setVisible(true);

		if (font != null) {
			this.setFont(font);
			jTextArea.setFont(font);
		}

		this.requestFocus();

		try {
			this.typingRobot = new TypingRobot();
		} catch (AWTException e1) {
			e1.printStackTrace();
			System.out.println("Failed to create TypingRobot.");
		}
	}

	@Override
	public void paint(Graphics g) {
		g.setColor(fgColor);
		g.drawRect(1, 1, w - 2, h - 2);
	}

	public static void main(String[] args) {
		new Screen();
	}

	@Override
	public void messageReceived(IRCMessage ircMessage) {

		if (ircMessage.command.equals("PRIVMSG")) {
			// add message to display
			insertText(ircMessage.nick + ": " + ircMessage.text);

			// read message for key input
			readKeyInput(ircMessage.text);
		} else if (ircMessage.command.equals("PART")) {
			// insertText(ircMessage.nick + " PARTED.");
		} else if (ircMessage.command.equals("JOIN")) {
			// insertText(ircMessage.nick + " JOINED.");
		} else {
			insertText(ircMessage.nick + ": " + ircMessage.text);
		}
	}

	int KEY_UP = KeyEvent.VK_U;
	int KEY_DOWN = KeyEvent.VK_J;
	int KEY_LEFT = KeyEvent.VK_H;
	int KEY_RIGHT = KeyEvent.VK_K;

	int KEY_A = KeyEvent.VK_Z;
	int KEY_B = KeyEvent.VK_X;

	int KEY_START = KeyEvent.VK_Y;
	int KEY_SELECT = KeyEvent.VK_I;

	/**
	 * readKeyInput(String text) Parses the text for key input and fires the key
	 * events if a TypingRobot is available.
	 * 
	 * @param text
	 *            text to parse
	 */
	private void readKeyInput(String text) {
		if (typingRobot == null)
			return;

		if (text.equalsIgnoreCase("up")) {
			typingRobot.fireKeyEvent(KEY_UP, false);
		} else if (text.equalsIgnoreCase("down")) {
			typingRobot.fireKeyEvent(KEY_DOWN, false);
		} else if (text.equalsIgnoreCase("left")) {
			typingRobot.fireKeyEvent(KEY_LEFT, false);
		} else if (text.equalsIgnoreCase("right")) {
			typingRobot.fireKeyEvent(KEY_RIGHT, false);
		} else

		if (text.equalsIgnoreCase("a")) {
			typingRobot.fireKeyEvent(KEY_A, false);
		} else if (text.equalsIgnoreCase("b")) {
			typingRobot.fireKeyEvent(KEY_B, false);
		} else

		if (text.equalsIgnoreCase("start")) {
			typingRobot.fireKeyEvent(KEY_START, false);
		} else if (text.equalsIgnoreCase("select")) {
			typingRobot.fireKeyEvent(KEY_SELECT, false);
		}
	}

	private void insertText(String txt) {
		lineCount++;
		jTextArea.append(" " + txt + "\r\n");

		if (lineCount > lineLimit) {
			String t = jTextArea.getText();
			int l = t.indexOf('\n') + 1;
			jTextArea.setText(t.substring(l, t.length()));
		}
	}
}