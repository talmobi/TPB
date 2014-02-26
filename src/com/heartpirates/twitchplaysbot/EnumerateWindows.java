package com.heartpirates.twitchplaysbot;

import java.awt.Robot;
import java.awt.event.KeyEvent;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class EnumerateWindows {
	private static final int MAX_TITLE_LENGTH = 1024;
	public static final int PROCESS_VM_READ = 0x0010;
	public static final int PROCESS_VM_WRITE = 0x0020;
	public static final int PROCESS_VM_OPERATION = 0x0008;

	public static void main(String[] args) throws Exception {
		Robot robot = new Robot();

		char[] buffer = new char[MAX_TITLE_LENGTH * 2];
		User32DLL.GetWindowTextW(User32DLL.GetForegroundWindow(), buffer,
				MAX_TITLE_LENGTH);
		System.out.println("Active window title: " + Native.toString(buffer));

		PointerByReference pointer = new PointerByReference();
		User32DLL.GetWindowThreadProcessId(User32DLL.GetForegroundWindow(),
				pointer);
		Pointer process = Kernel32.OpenProcess(
				Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ,
				false, pointer.getValue());
		Psapi.GetModuleBaseNameW(process, null, buffer, MAX_TITLE_LENGTH);
		System.out.println("Active window process: " + Native.toString(buffer));

		HWND gbaw = null;
		HWND lastActive = null;

		while (true) {
			try {
				System.out.println("Active window: " + getActiveWindowName());

				if (!getActiveWindowName().contains("Visual")) {
					lastActive = User32DLL.GetForegroundWindow();

					System.out.println("Tring to switch -> ");

					// setActiveWindow(findWindowByName("VisualBoy"));

					setFocusWindow(findWindowByName("VisualBoy"));
					int keycode = KeyEvent.VK_Z;
					robot.keyPress(keycode);
					robot.setAutoDelay(50);
					robot.keyRelease(keycode);

					System.out.println("After: " + getActiveWindowName());
				}

				if (getActiveWindowName().contains("Visual")) {
					// save it
					gbaw = User32DLL.GetForegroundWindow();

					if (getActiveWindowName().contains("Visual")) {
						System.out.println("Sending keypress");
						int keycode = KeyEvent.VK_Z;
						robot.keyPress(keycode);
						robot.setAutoDelay(50);
						robot.keyRelease(keycode);
					}

					// restore the last active window
					setFocusWindow(lastActive);
					setActiveWindow(lastActive);
				}

				Thread.sleep(200);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}

	}

	// debug (print out current active window title + process name)
	public static void printActiveWindowInfo() throws Exception {
		char[] buffer = new char[MAX_TITLE_LENGTH * 2];
		User32DLL.GetWindowTextW(User32DLL.GetForegroundWindow(), buffer,
				MAX_TITLE_LENGTH);
		System.out.println("Active window title: " + Native.toString(buffer));

		PointerByReference pointer = new PointerByReference();
		User32DLL.GetWindowThreadProcessId(User32DLL.GetForegroundWindow(),
				pointer);
		Pointer process = Kernel32.OpenProcess(
				Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ,
				false, pointer.getValue());
		Psapi.GetModuleBaseNameW(process, null, buffer, MAX_TITLE_LENGTH);
		System.out.println("Active window process: " + Native.toString(buffer));
	}

	public static String getActiveWindowName() throws Exception {

		if (Platform.isWindows()) {
			char[] buffer = new char[MAX_TITLE_LENGTH * 2];

			User32DLL.GetWindowTextW(User32DLL.GetForegroundWindow(), buffer,
					MAX_TITLE_LENGTH);
			return Native.toString(buffer);
		}

		if (Platform.isLinux() || Platform.isFreeBSD()) {
			final X11 x11 = X11.INSTANCE;

			X11.Display display = x11.XOpenDisplay(null);
			X11.Window window = new X11.Window();
			XLib.XGetInputFocus(display, window, Pointer.NULL);
			X11.XTextProperty name = new X11.XTextProperty();
			x11.XGetWMName(display, window, name);
			return name.toString();
		}

		if (Platform.isMac()) {
			final String script = "tell application \"System Events\"\n"
					+ "\tname of application processes whose frontmost is tru\n"
					+ "end";
			ScriptEngine appleScript = new ScriptEngineManager()
					.getEngineByName("AppleScript");
			String result = (String) appleScript.eval(script);
			return result;
		}

		return null;
	}

	public static String getActiveWindowProcessName() throws Exception {
		char[] buffer = new char[MAX_TITLE_LENGTH * 2];

		PointerByReference pointer = new PointerByReference();
		User32DLL.GetWindowThreadProcessId(User32DLL.GetForegroundWindow(),
				pointer);
		Pointer process = Kernel32.OpenProcess(
				Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ,
				false, pointer.getValue());
		Psapi.GetModuleBaseNameW(process, null, buffer, MAX_TITLE_LENGTH);

		return Native.toString(buffer);
	}

	public static int getProcessId(String name) {
		HWND hWnd = findWindowByStartsWith("VisualBoyAdvance");

		IntByReference refpid = new IntByReference(0);
		User32DLL.GetWindowThreadProcessId(hWnd, refpid);
		return refpid.getValue();
	}

	public static boolean setActiveWindow(HWND hWnd) throws Exception {
		User32DLL.SetForegroundWindow(hWnd);
		return false;
	}

	public static HWND findWindow(String lpClassName, String lpWindowName)
			throws Exception {
		return User32DLL.FindWindowW(lpClassName, lpWindowName);
	}

	public static boolean setFocusWindow(HWND hWnd) throws Exception {
		User32DLL.SetFocus(hWnd);
		return false;
	}

	public static HWND findWindowByName(final String contains) {
		// pointer to the HWND
		final PointerByReference pointer = new PointerByReference();
		User32DLL.EnumWindows(new WNDENUMPROC() { // the callback function

					@Override
					public boolean callback(HWND hWnd, Pointer arg1) {
						// buffer to save window text
						char[] buffer = new char[512];
						// get window text
						User32DLL.GetWindowTextW(hWnd, buffer, 512);
						// transform buffer into native string
						String windowTitle = Native.toString(buffer);

						if (windowTitle.isEmpty()
								|| !(User32DLL.IsWindowVisible(hWnd)))
							return true; // skip empty and invisible windows

						// check the window title if it matches
						if (windowTitle.contains(contains)) {
							// save the HWND to the pointer
							pointer.setPointer(hWnd.getPointer());
							return false; // window found, stop search
						}
						return true; // not found - continue searching
					}

				}, null);

		return new HWND(pointer.getPointer());
	}

	public static HWND findWindowByStartsWith(final String startsWith) {
		// pointer to the HWND
		final PointerByReference pointer = new PointerByReference();
		User32DLL.EnumWindows(new WNDENUMPROC() { // the callback function

					@Override
					public boolean callback(HWND hWnd, Pointer arg1) {
						// buffer to save window text
						char[] buffer = new char[512];
						// get window text
						User32DLL.GetWindowTextW(hWnd, buffer, 512);
						// transform buffer into native string
						String windowTitle = Native.toString(buffer);

						if (windowTitle.isEmpty()
								|| !(User32DLL.IsWindowVisible(hWnd)))
							return true; // skip empty and invisible windows

						// check the window title if it matches
						if (windowTitle.startsWith(startsWith)) {
							// save the HWND to the pointer
							pointer.setPointer(hWnd.getPointer());
							return false; // window found, stop search
						}
						return true; // not found - continue searching
					}

				}, null);

		return new HWND(pointer.getPointer());
	}

	public static Pointer openProcess(int permissions, int pid) {
		Pointer pointer = Kernel32.OpenProcess(permissions, true, pid);
		return pointer;
	}

	public static Memory readMemory(Pointer process, long address, int size) {
		IntByReference read = new IntByReference(0);
		Memory mem = new Memory(size);
		Kernel32.ReadProcessMemory(process, address, mem, size, read);
		return mem;
	}

	public static boolean writeMemory(Pointer process, long address, byte[] data) {
		int size = data.length;
		Memory buffer = new Memory(size);

		for (int i = 0; i < size; i++) {
			buffer.setByte(i, data[i]);
		}

		return Kernel32
				.WriteProcessMemory(process, address, buffer, size, null);
	}

	public static long findDynAddress(Pointer process, int[] offsets,
			long baseAddress) {
		long pointer = baseAddress;

		int size = 4;
		Memory tmp = new Memory(size);
		long pointerAddress = 0;

		for (int i = 0; i < offsets.length; i++) {
			if (i == 0) {
				Kernel32.ReadProcessMemory(process, pointer, tmp, size, null);
			}
			pointerAddress = ((tmp.getInt(0) + offsets[i]));
			if (i != offsets.length - 1)
				Kernel32.ReadProcessMemory(process, pointerAddress, tmp, size,
						null);
		}

		return pointerAddress;
	}

	// Native API:s
	static class Psapi {
		static {
			Native.register("psapi");
		}

		public static native int GetModuleBaseNameW(Pointer hProcess,
				Pointer hmodule, char[] lpBaseName, int size);
	}

	static class Kernel32 {
		static {
			Native.register("kernel32");
		}
		public static int PROCESS_QUERY_INFORMATION = 0x0400;
		public static int PROCESS_VM_READ = 0x0010;

		public static native int GetLastError();

		public static native Pointer OpenProcess(int dwDesiredAccess,
				boolean bInheritHandle, int pid);

		public static native Pointer OpenProcess(int dwDesiredAccess,
				boolean bInheritHandle, Pointer pid);

		public static native Pointer OpenProcess(int dwDesiredAccess,
				boolean bInheritHandle, IntByReference pid);

		// (windows) write to specific memory
		public static native boolean WriteProcessMemory(Pointer p,
				long address, Pointer buffer, int size, IntByReference written);

		// (windows) read from specific memory
		public static native boolean ReadProcessMemory(Pointer hProcess,
				long intBadeAddress, Pointer outputBuffer, int nSize,
				IntByReference outNumberOfBytesRead);
	}

	static class User32DLL {
		static {
			Native.register("user32");
		}

		public static native boolean EnumWindows(WNDENUMPROC lpEnumFunc,
				Pointer data);

		public static native boolean IsWindowVisible(HWND hWnd);

		public static native int GetWindowThreadProcessId(HWND hWnd,
				PointerByReference pref);

		public static native int GetWindowThreadProcessId(HWND hWnd,
				IntByReference pid);

		public static native HWND GetForegroundWindow();

		public static native int GetWindowTextW(HWND hWnd, char[] lpString,
				int nMaxCount);

		public static native boolean SetForegroundWindow(HWND hWnd);

		public static native HWND SetFocus(HWND hWnd);

		public static native HWND FindWindowW(String lpClassName,
				String lpWindowName);
	}

	static class XLib {
		static {
			Native.register("XLib");
		}

		public static native int XGetInputFocus(X11.Display display,
				X11.Window focus_return, Pointer revert_to_return);
	}
}