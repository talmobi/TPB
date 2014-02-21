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

public class IrcBot {

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
	String oauth = "oauth:33a21tw2ih4lprqpoqfazzv2evzp8zl"; // get you

	public IrcBot() throws UnknownHostException, IOException {
		connect();
	}

	private void connect() throws UnknownHostException, IOException {

		// connect to the irc server
		socket = new Socket(server, port);

		// create helper classes for reading/writing
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(
				socket.getOutputStream()));

		// login
		writer.write("PASS " + oauth + "\r\n");
		writer.write("NICK " + nick + "\r\n");

		String readLine = null;
		while ((readLine = reader.readLine()) != null) { // blocks
			handleMessage(readLine);
		}
	}

	private void handleMessage(String msg) {

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

	}

}
