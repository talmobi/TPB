package com.heartpirates.twitchplaysbot.robots;

import java.awt.AWTException;
import java.awt.event.KeyEvent;

public class GBARobot extends TypingRobot {

	int autoDelay = 20;

	private class Timer {
		long startTime = System.currentTimeMillis();
		int delay = 1000;

		Timer(int delay) {
			this.delay = delay;
		}

		void reset() {
			startTime = System.currentTimeMillis();
		}

		boolean isReady() {
			return (System.currentTimeMillis() - startTime) > delay;
		}

		@SuppressWarnings("unused")
		void setDelay(int delay) {
			this.delay = delay;
		}

		public boolean isReadyReset() {
			if (isReady()) {
				reset();
				return true;
			}
			return false;
		}
	}

	Timer startButtonTimer = new Timer(60 * 1000);
	Timer selectButtonTimer = new Timer(60 * 1000);

	public GBARobot() throws AWTException {
		super();
		robot.setAutoDelay(autoDelay);
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
	 * parseInput(String text) Parses the text for key input and fires the key
	 * events.
	 * 
	 * @param text
	 *            text to parse
	 */
	public void parseAndFire(String text) {
		if (text == null)
			return;

		if (text.equalsIgnoreCase("up")) {
			fireKeyEvent(KEY_UP, false);
		} else if (text.equalsIgnoreCase("down")) {
			fireKeyEvent(KEY_DOWN, false);
		} else if (text.equalsIgnoreCase("left")) {
			fireKeyEvent(KEY_LEFT, false);
		} else if (text.equalsIgnoreCase("right")) {
			fireKeyEvent(KEY_RIGHT, false);
		} else

		if (text.equalsIgnoreCase("a")) {
			fireKeyEvent(KEY_A, false);
		} else if (text.equalsIgnoreCase("b")) {
			fireKeyEvent(KEY_B, false);
		} else

		if (text.equalsIgnoreCase("start")) {
			if (startButtonTimer.isReadyReset())
				fireKeyEvent(KEY_START, false);
		} else if (text.equalsIgnoreCase("select")) {
			if (selectButtonTimer.isReadyReset())
				fireKeyEvent(KEY_SELECT, false);
		}
	}
}