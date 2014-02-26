package com.heartpirates.twitchplaysbot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.heartpirates.twitchplaysbot.robots.TypingRobot;

/**
 * 
 * 
 * https://www.ietf.org/rfc/rfc1459.txt
 * http://www.irchelp.org/irchelp/rfc/rfc.html
 * http://help.twitch.tv/customer/portal/articles/1302780-twitch-irc
 * 
 */

public class IrcBot implements Runnable {

	private boolean logging = false;

	BufferedWriter out = null;

	Socket socket;
	BufferedWriter socketWriter = null;

	String server = "irc.twitch.tv";
	int port = 6667;

	// twitch channel (populated channel for testing)
	String channel = "#mooglebones";

	// twitch user name
	String nick = "mooglebones";

	// twitch oauth key - www.twitchapps.com/tmi
	String pass = "oauth:gd1qi7fkw98fhvwbq9rszq0pdlalamb";

	long messageCount = 0;
	long startTime = System.currentTimeMillis();
	long lastSec = System.currentTimeMillis();

	private List<MessageListener> messageListeners = new LinkedList<MessageListener>();

	private Screen screen;
	private AGB agb;
	private TypingRobot robot = null;

	public IrcBot(OutputStream os) {
		if (os != null)
			this.out = new BufferedWriter(new OutputStreamWriter(os));
	}

	public void setScreen(Screen screen) {
		this.screen = screen;
		addMessageListener(screen);
	}

	public void setAGB(AGB agb) {
		this.agb = agb;
		addMessageListener(agb);
	}

	public void start() {
		new Thread(this).start();
	}

	public void addMessageListener(MessageListener messageListener) {
		messageListeners.add(messageListener);
	}

	public void removeMessageListener(MessageListener messageListener) {
		messageListeners.remove(messageListener);
	}

	@Override
	public void run() {
		// connect to the IRC server
		try {
			print("connecting... ");
			socket = new Socket(server, port);

			// create helper classes for reading/writing
			BufferedReader socketReader = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			socketWriter = new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream()));

			// login
			socketWriter.write("PASS " + pass + "\r\n");
			socketWriter.write("NICK " + nick + "\r\n");
			// writer.write("USER " + "guest :" + nick + "\r\n");
			socketWriter.flush();

			print("logging in... ");
			String line = null;
			while ((line = socketReader.readLine()) != null) { // blocks
				if (line.indexOf("004") >= 0) {
					print("Success!\r\n");
					break;
				} else if (line.indexOf("433") >= 0) {
					print(" Fail. Nick already in use.\r\n");
					return;
				}
			}

			// auto join channel
			socketWriter.write("JOIN " + channel + "\r\n");
			socketWriter.flush();

			// get test data
			socketWriter.write("JOIN " + "#twitchplayspokemon" + "\r\n");
			socketWriter.flush();

			// read messages
			while ((line = socketReader.readLine()) != null) { // blocks
				handleMessage(parseMessage(line));
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (robot != null)
			robot.close();

		if (screen != null)
			screen.close();

		println("Socket closed: " + socket.isClosed());
		println("Exiting IrcBot.");
	}

	public void print(String str) {
		if (out == null)
			return;
		try {
			out.write(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void println(String str) {
		this.print(str + "\n");
	}

	/*
	 * https://www.ietf.org/rfc/rfc1459.txt
	 */
	private void handleMessage(IRCMessage message) {
		fireMessage(message);

		if (message.command.equals("PING")) {
			System.out.print("PING ");
			send("PONG " + message.params[0] + "\r\n");
			System.out.print("PONG\r\n");
		} else if (message.command.equals("PRIVMSG")) {
			messageCount++;
			log(message);
		} else if (message.command.equals("PART")) {
			if (message.nick == this.nick) {
				print("~ YOU HAVE PARTED THE CHANNEL! ~");
			}
		} else if (message.command.equals("JOIN")) {
			// ignore joins
		} else if (message.command.equals("NAMES")) {
			// ignore names
		} else if (message.command.equals("332")) {
			if (message.params.length == 1)
				print(message.params[0]);
			print("'s TOPIC: " + message.text + "\r\n");
		} else if (message.command.equals("331")) {
			if (message.params.length == 1)
				print(message.params[0]);
			print("'s has no TOPIC.\r\n");
		} else if (message.command.equals("LIST")) {
			// ignore list
		} else if (message.command.equals("MODE")) {
			// ignore mode
		} else if (message.command.equals("KICK")) {
			if (message.params.length > 0)
				print(message.params[0] + " KICKED OUT.");
		} else {
			// return text field
			print(message.nick + ": " + message.text);
		}
	}

	List<String> logList = new LinkedList<String>();
	int logLimit = 1024 << 2;
	long lastSave = System.currentTimeMillis();
	long delaySave = 1000 * 60 * 60 * 15 + 1000; // once an hour

	private void log(IRCMessage message) {
		if (!logging)
			return;

		logList.add(message.toString());

		int delta = (int) (System.currentTimeMillis() - lastSave);

		if (logList.size() >= logLimit || delta > delaySave) {
			Kryo kryo = new Kryo();
			lastSave = System.currentTimeMillis();
			try {
				File file = getLogfile();
				if (file == null) {
					println("Couldn't save log file.");
					return;
				}
				Output output = new Output(new FileOutputStream(file));

				if (logList != null && logList.size() > 0) {
					kryo.writeObject(output, logList);
				}
				output.close();
				println("Saved to file.");
			} catch (Exception e) {
				e.printStackTrace();
			}

			// clear list
			logList.clear();
		}
	}

	private File getLogfile() {
		File dir = new File(".IrcBotLogfiles");
		dir.mkdir();

		if (dir.exists()) {
			File file = null;
			int count = 0;
			while (file == null) {
				String n = "log_"
						+ Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
						+ "_" + count++;
				file = new File(".IrcBotLogfiles/" + n);
				if (file.exists())
					file = null;
			}
			return file;
		}
		return null;
	}

	class IRCMessage {
		String nick;
		String user;
		String host;

		String command;

		String[] params;

		String text;

		@Override
		public String toString() {
			String pt = "";
			if (params != null)
				for (String s : params)
					if (s != null)
						pt += "s ";
			return ("nick: " + nick + ", user: " + user + ", host: " + host
					+ ", command: " + command + ", params: " + pt);
		}
	}

	private IRCMessage parseMessage(String msg) {
		// System.out.println(msg); // debug

		int prefixEnd = -1;
		String prefix = null;

		if (msg.startsWith(":")) {
			prefixEnd = msg.indexOf(" ");
			prefix = msg.substring(1, prefixEnd);
		}

		int trailingStart = msg.indexOf(" :");
		String trailing = null;
		boolean trailingExists = false;

		if (trailingStart >= 0) {
			trailing = msg.substring(trailingStart + 2);
			trailingExists = true;
		} else {
			trailingStart = msg.length();
		}

		String[] tokens = msg.substring(prefixEnd + 1, trailingStart)
				.split(" ");

		IRCMessage ircMessage = new IRCMessage();
		ircMessage.command = tokens[0];

		if (tokens.length > 1 || trailingExists) {
			// shift tokens down and add trailing (text field) to last params
			for (int i = 0; i < tokens.length - 1; i++)
				tokens[i] = tokens[i + 1];
			tokens[tokens.length - 1] = trailing;

			// ircMessage.params = Arrays.copyOfRange(tokens, 1, tokens.length);
		}
		ircMessage.params = tokens;

		// parse the prefix
		int nickEnd = -1;
		int userEnd = -1;
		if (prefix != null) {
			nickEnd = prefix.indexOf('!');
			if (nickEnd >= 0) {
				ircMessage.nick = prefix.substring(0, nickEnd);
				userEnd = prefix.indexOf('@');
			} else {
				ircMessage.nick = prefix;
			}
			if (userEnd >= 0) {
				ircMessage.user = prefix.substring(nickEnd + 1, userEnd);
				ircMessage.host = prefix.substring(userEnd + 1);
			}
		}

		// add also text field to text field (copy of params[params.length - 1]
		ircMessage.text = trailing;
		return ircMessage;
	}

	private void fireMessage(IRCMessage ircMessage) {

		// debugIrcMessage();

		if (robot != null)
			robot.parseAndFire(ircMessage.text);

		for (MessageListener ml : messageListeners) {
			ml.messageReceived(ircMessage);
		}

		// if (System.currentTimeMillis() - lastSec > 1000) {
		// lastSec += 1000;
		// int num = (int) (messageCount / Math.max(1d,
		// ((System.currentTimeMillis() - startTime) / (1000))));
		// System.out.printf("%s messages /  SECOND.\n", num);
		// }
	}

	@SuppressWarnings("unused")
	private void debugIrcMessage(IRCMessage ircMessage) {
		System.out
				.printf("nick: %s, user: %s, host: %s, command: %s, params: %s, text: %s\r\nparams:",
						ircMessage.nick, ircMessage.user, ircMessage.host,
						ircMessage.command, ircMessage.params, ircMessage.text);

		if (ircMessage.params != null)
			for (String s : ircMessage.params)
				if (s != null)
					System.out.printf(s);
		System.out.printf("\r\n\r\n");
	}

	public void send(String msg) {
		try {
			if (socketWriter != null) {
				socketWriter.write(msg);
				socketWriter.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public interface MessageListener {
		public void messageReceived(IRCMessage ircMessage);
	}

	public void setLogging(boolean b) {
		this.logging = b;
	}

	public static void main(String[] args) {
		IrcBot ib = new IrcBot(System.out);
		ib.setLogging(false);

		ib.setScreen(new Screen());
		try {
			ib.setAGB(new AGB("VisualBoyAdvance"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		ib.start();
	}

}
