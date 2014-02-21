package com.heartpirates;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Point;
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

		jTextArea = new JTextArea();
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
			insertText(ircMessage.nick + ": " + ircMessage.text);
		} else if (ircMessage.command.equals("PART")) {
			// insertText(ircMessage.nick + " PARTED.");
		} else if (ircMessage.command.equals("JOIN")) {
			// insertText(ircMessage.nick + " JOINED.");
		} else {
			insertText(ircMessage.nick + ": " + ircMessage.text);
		}
	}

	private void insertText(String txt) {
		lineCount++;
		jTextArea.insert(" " + txt + "\r\n", 0);

		if (lineCount > lineLimit) {
			String t = jTextArea.getText();
			int l = t.lastIndexOf('\n', t.lastIndexOf('\n'));
			jTextArea.setText(t.substring(0, l));
		}
	}
}