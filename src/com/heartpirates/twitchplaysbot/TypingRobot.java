package com.heartpirates.twitchplaysbot;

import java.awt.AWTException;
import java.awt.Robot;

import com.heartpirates.twitchplaysbot.EnumerateWindows.User32DLL;
import com.sun.jna.platform.win32.WinDef.HWND;

/**
 * TypingRobot fires off key events to designated window (Windows).
 * 
 * @author Mollie
 * 
 */
public class TypingRobot {

	protected Robot robot;
	protected String windowName = "VisualBoyAdvance";

	protected boolean tryForceSwitch = false; // false recommended
	protected HWND windowHandle = null; // JNA window handle

	public TypingRobot() throws AWTException {
		robot = new Robot();
		robot.setAutoDelay(50);
	}

	/**
	 * fireKeyEvent(int keycode)
	 * 
	 * @param keycode
	 *            keycode to fire (see KeyEvent).
	 * @return returns true if successful.
	 */
	public boolean fireKeyEvent(int keycode, boolean force) {

		try {
			if (EnumerateWindows.getActiveWindowName().contains(windowName)) {
				System.out.println("Firing key event.");
				windowHandle = User32DLL.GetForegroundWindow();
				robot.keyPress(keycode);
				robot.keyRelease(keycode);
				return true;
			} else if (force) {
				try { // try to force switch (not recommended)
					HWND hWnd = EnumerateWindows.findWindowByName(windowName);
					EnumerateWindows.setActiveWindow(hWnd);
					if (EnumerateWindows.getActiveWindowName().contains(
							windowName)) {
						System.out.println("Firing key event.");
						robot.keyPress(keycode);
						robot.keyRelease(keycode);
						return true;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Key event ignored. Wrong Active Window.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out
					.println("Can't check active window. Linux/Mac OS incompatible?");
			// TODO linux/mac support
		}
		return false;
	}

	public void parseAndFire(String text) {
	}

	public void close() {
	}
}