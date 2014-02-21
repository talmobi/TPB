package com.heartpirates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
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
	String pass = "oauth:gd1qi7fkw98fhvwbq9rszq0pdlalamb";

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

		System.out.println("Socket closed: " + socket.isClosed());
		System.out.println("Exiting IrcBot.");
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

		// debugIrcMessage();

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
			if (writer != null) {
				writer.write(msg);
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	interface MessageListener {
		public void messageReceived(IRCMessage ircMessage);
	}

	public static void main(String[] args) {
		new IrcBot();
	}
}
