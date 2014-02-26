package com.heartpirates.twitchplaysbot;

import com.heartpirates.twitchplaysbot.EnumerateWindows.Kernel32;
import com.heartpirates.twitchplaysbot.IrcBot.IRCMessage;
import com.heartpirates.twitchplaysbot.IrcBot.MessageListener;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

// Reads and writes to the emulated AGB CPU (ARM7TDMI)
public class AGB implements MessageListener, Runnable {

	int pid = -1;
	Pointer process = null;
	long dynAddress = -1;

	private boolean initialized = false;

	// base address + pointer offset for KEY_INPUT register in VGA
	long baseAddress = 0x5a8f3c;
	int[] offsets = new int[] { 0x130 };

	// 2 bytes used key_input [X,X,X,X,X,X,L,R,DWN,UP,LFT,RT,ST,SL,B,A]
	final int BUTTON_A = 0x1;
	final int BUTTON_B = 0x1 << 1;
	final int BUTTON_SL = 0x1 << 2;
	final int BUTTON_ST = 0x1 << 3;
	final int BUTTON_RT = 0x1 << 4;
	final int BUTTON_LFT = 0x1 << 5;
	final int BUTTON_UP = 0x1 << 6;
	final int BUTTON_DWN = 0x1 << 7;

	int input = 0x3ff; // no keys pressed

	private class Button {
		long time = System.currentTimeMillis();
		long wait = 250; // ms
		final int key;

		Button(int key) {
			this.key = key;
		}

		void press() {
			time = System.currentTimeMillis();
		}

		int get() {
			if (System.currentTimeMillis() - time > wait) {
				return 0;
			}
			return key;
		}
	}

	Button buttonA = new Button(BUTTON_A);
	Button buttonB = new Button(BUTTON_B);
	Button buttonSL = new Button(BUTTON_SL);
	Button buttonST = new Button(BUTTON_ST);
	Button buttonRT = new Button(BUTTON_RT);
	Button buttonLFT = new Button(BUTTON_LFT);
	Button buttonUP = new Button(BUTTON_UP);
	Button buttonDWN = new Button(BUTTON_DWN);

	public AGB(String name) throws Exception {
		initialized = init(name);
	}

	public boolean init(String name) throws Exception {
		pid = EnumerateWindows.getProcessId("VisualBoyAdvance");
		if (pid != -1) {
			// open the process with permissions
			int perm = Kernel32.PROCESS_VM_READ | Kernel32.PROCESS_VM_WRITE
					| Kernel32.PROCESS_VM_OPERATION;
			process = EnumerateWindows.openProcess(perm, pid);

			// find dynamic memory based on base address + pointer offset
			dynAddress = EnumerateWindows.findDynAddress(process, offsets,
					baseAddress);

			if (process != null && dynAddress > 0 && pid > 0) {
				new Thread(this).start();
				return true;
			}
		}

		System.out.println("Couldn't find emulator: " + name);
		return false;
	}

	private short readMemory() throws Exception {
		// 2 bytes for KEY INPUT register
		Memory readMemory = EnumerateWindows.readMemory(process, dynAddress, 2);
		return readMemory.getShort(0);
	}

	private boolean writeMemory(byte[] data) throws Exception {
		if (!initialized)
			return false;

		if (data.length > 2)
			data = new byte[] { data[0], data[1] };
		return EnumerateWindows.writeMemory(process, dynAddress, data);
	}

	private void test() {
		String msg = "nul";

		// 2 bytes used key_input [X,X,X,X,X,X,L,R,DWN,UP,LFT,RT,ST,SL,B,A]

		try {
			msg = "read: " + this.readMemory();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(msg);
	}

	private static byte[] getMoveData(int i) {
		byte[] data = new byte[2];
		data[0] = (byte) ~(i & 0xFF);
		data[1] = (byte) ~((i >> 8) & 0xFF);
		return data;
	}

	@Override
	public void messageReceived(IRCMessage ircMessage) {
		if (ircMessage == null || ircMessage.text == null)
			return;

		// 2 bytes used key_input [X,X,X,X,X,X,L,R,DWN,UP,LFT,RT,ST,SL,B,A]
		String t = ircMessage.text;

		if (t.startsWith("a") || t.startsWith("A")) {
			buttonA.press();
		} else if (t.startsWith("b") || t.startsWith("B")) {
			buttonB.press();
		} else if (t.startsWith("SELECT")) {
			buttonSL.press();
		} else if (t.startsWith("START")) {
			buttonST.press();
		} else if (t.startsWith("right")) {
			buttonRT.press();
		} else if (t.startsWith("left")) {
			buttonLFT.press();
		} else if (t.startsWith("up")) {
			buttonUP.press();
		} else if (t.startsWith("down")) {
			buttonDWN.press();
		}
	}

	byte[] updateInput() {
		byte[] data = new byte[] { (byte) 0xFF, 0x03 };

		int least = 0xFF ^ (buttonA.get() | buttonB.get() | buttonST.get()
				| buttonRT.get() | buttonLFT.get() | buttonUP.get() | buttonDWN
				.get());

		data[0] = (byte) least;
		data[1] = (byte) 0x3;

		return data;
	}

	@Override
	public void run() {
		short last = 0;
		while (true) {
			try {
				if (EnumerateWindows.processIdExists("VisualBoyAdvance") > 0) {
					writeMemory(updateInput());

					short h = readMemory();
					if (h != last) {
						last = h;
						String hex = "" + Integer.toHexString(readMemory());
						System.out.println(hex);
					}
				}
				Thread.sleep(2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}