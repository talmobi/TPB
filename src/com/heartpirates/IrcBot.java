package com.heartpirates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * https://www.ietf.org/rfc/rfc1459.txt
 * http://www.irchelp.org/irchelp/rfc/rfc.html
 * http://help.twitch.tv/customer/portal/articles/1302780-twitch-irc
 * 
 */

public class IrcBot implements Runnable {

	BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out));

	Socket socket;
	BufferedWriter writer = null;

	String server = "irc.twitch.tv";
	int port = 6667;

	// twitch channel (populated channel for testing)
	String channel = "#twitchplayspokemon";

	// twitch user name
	String nick = "mooglebones";

	// twitch oauth key - www.twitchapps.com/tmi
	String pass = "oauth:33a21tw2ih4lprqpoqfazzv2evzp8zl";

	// IRC regex patterns
	Pattern commandPattern = Pattern.compile("");

	long messageCount = 0;
	long startTime = System.currentTimeMillis();
	long lastSec = System.currentTimeMillis();

	private List<MessageListener> messageListeners = new LinkedList<MessageListener>();
	private Screen screen;

	public IrcBot() {
		new Thread(this).start();

		addMessageListener(new Screen());

		BufferedReader inputReader = new BufferedReader(new InputStreamReader(
				System.in));

		String inLine = null;
		try {
			while ((inLine = inputReader.readLine()) != null) {
				send(inLine);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
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
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream()));

			// login
			writer.write("PASS " + pass + "\r\n");
			writer.write("NICK " + nick + "\r\n");
			// writer.write("USER " + "guest :" + nick + "\r\n");
			writer.flush();

			print("logging in... ");
			String line = null;
			while ((line = reader.readLine()) != null) { // blocks
				if (line.indexOf("004") >= 0) {
					print("Success!\r\n");
					break;
				} else if (line.indexOf("433") >= 0) {
					print(" Fail. Nick already in use.\r\n");
					return;
				}
			}

			// auto join channel
			writer.write("JOIN " + channel + "\r\n");
			writer.flush();

			// read messages
			while ((line = reader.readLine()) != null) { // blocks
				handleMessage(parseMessage(line));
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/*
	 * https://www.ietf.org/rfc/rfc1459.txt
	 */
	private void handleMessage(IRCMessage message) {
		fireMessage(message);

		if (message.command.equals("PRIVMSG")) {
			// print(message.nick + ": " + message.text + "\r\n");
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

	class IRCMessage {
		String nick;
		String user;
		String host;

		String command;

		String[] params;

		String text;

		@Override
		public String toString() {
			return nick + " " + user + " " + host + " " + command + " "
					+ channel + " " + text;
		}
	}

	private IRCMessage parseMessage(String msg) {
		int prefixEnd = -1;
		String prefix = null;

		if (msg.startsWith(":")) {
			prefixEnd = msg.indexOf(" ");
			prefix = msg.substring(1, prefixEnd);
		}

		int trailingStart = msg.indexOf(" :");
		String trailing = null;

		if (trailingStart >= 0) {
			trailing = msg.substring(trailingStart + 2);
		} else {
			trailingStart = msg.length();
		}

		String[] tokens = msg.substring(prefixEnd + 1, trailingStart)
				.split(" ");

		IRCMessage ircMessage = new IRCMessage();
		ircMessage.command = tokens[0];

		if (tokens.length > 1) {
			ircMessage.params = Arrays.copyOfRange(tokens, 1, tokens.length);
		}

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

		ircMessage.text = trailing;
		return ircMessage;
	}

	public void print(String str) {
		// try {
		// if (out != null) {
		// out.write(str);
		// out.flush();
		// }
		//
		// } catch (IOException ioe) {
		// ioe.printStackTrace();
		// }
	}

	private void fireMessage(IRCMessage ircMessage) {
		messageCount++;

		for (MessageListener ml : messageListeners) {
			ml.messageReceived(ircMessage);
		}

		if (System.currentTimeMillis() - lastSec > 1000) {
			lastSec += 1000;
			double num = messageCount
					/ Math.max(
							1d,
							((System.currentTimeMillis() - startTime) / (1000 * 60)));
			System.out.printf("%s messages /  minute.\n", num);
		}
	}

	public void send(String msg) throws IOException {
		if (writer != null) {
			writer.write(msg);
			writer.flush();
		}
	}

	interface MessageListener {
		public void messageReceived(IRCMessage ircMessage);
	}

	public static void main(String[] args) {
		new IrcBot();

		System.out.println("Closing.");
	}
}
