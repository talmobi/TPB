package com.heartpirates;

import java.awt.AWTException;
import java.awt.Robot;

import com.sun.jna.platform.win32.WinDef.HWND;

/**
 * TypingRobot fires off key events to designated window (Windows).
 * 
 * @author Mollie
 * 
 */
public class TypingRobot {

	Robot robot;
	String windowName = "VisualBoyAdvance";

	boolean tryForceSwitch = false;
	HWND windowHandle; // JNA window handle

	public TypingRobot() throws AWTException {
		robot = new Robot();
		robot.setAutoDelay(250);
	}

	/**
	 * fireKeyEvent(int keycode)
	 * 
	 * @param keycode
	 *            keycode to fire (see KeyEvent).
	 * @return returns true if successful.
	 */
	public boolean fireKeyEvent(int keycode, boolean force) {
		System.out.println("Firing key event.");
		
		try {
			if (EnumerateWindows.getActiveWindowName().contains(windowName)) {
				robot.keyPress(keycode);
				robot.keyRelease(keycode);
				return true;
			} else if (force) {
				try { // try to force switch (not recommended)
					HWND hWnd = EnumerateWindows.findWindowByName(windowName);
					EnumerateWindows.setActiveWindow(hWnd);
					if (EnumerateWindows.getActiveWindowName().contains(
							windowName)) {
						robot.keyPress(keycode);
						robot.keyRelease(keycode);
						return true;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out
					.println("Can't check active window. Linux/Mac OS incompatible?");
			// TODO linux/mac support
		}
		return false;
	}
}