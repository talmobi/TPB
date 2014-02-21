package com.heartpirates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * https://www.ietf.org/rfc/rfc1459.txt
 * http://www.irchelp.org/irchelp/rfc/rfc.html
 * http://help.twitch.tv/customer/portal/articles/1302780-twitch-irc
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

	public IrcBot() {
		new Thread(this).start();

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
			writer.write("USER " + "guest :" + nick + "\r\n");
			writer.flush();

			print("logging in... ");
			String line = null;
			while ((line = reader.readLine()) != null) { // blocks
				if (line.indexOf("004") >= 0) {
					print("Success!\r\n");
					break;
				}
			}

			// auto join channel
			writer.write("JOIN " + channel + "\r\n");
			writer.flush();

			// read messages
			while ((line = reader.readLine()) != null) { // blocks
				if (line.startsWith("PING ")) {
					print("PING ");
					writer.write("PONG " + line.substring(5) + "\r\n");
					writer.flush();
					print("PONG\r\n");
				} else {
					handleMessage(line);
				}
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	class IRCMessage {
		String nick;
		String user;
		String host;
		String command;
		String channel;
		String text;

		@Override
		public String toString() {
			return nick + " " + user + " " + host + " " + command + " "
					+ channel + " " + text;
		}
	}

	private void handleMessage(String line) {

		String[] tokens = line.split(" ", 4);

		if (tokens[1].equals("PRIVMSG")) {
			String nick = "";
			if (line.indexOf('!') >= 0)
				nick = line.substring(1, line.indexOf('!'));
			String text = "";
			if (tokens.length >= 3)
				text = tokens[3].substring(1);

			System.out.println(nick + ": " + text);
		} else {
			System.out.println(line);
		}
	}

	public void print(String str) throws IOException {
		if (out != null) {
			out.write(str);
			out.flush();
		}
	}

	public void send(String msg) throws IOException {
		if (writer != null) {
			writer.write(msg);
			writer.flush();
		}
	}

	public static void main(String[] args) {
		new IrcBot();

		System.out.println("Closing.");
	}

}
