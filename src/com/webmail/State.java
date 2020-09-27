package com.webmail;

import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;

public class State {
	public static String username, password;
	public static Session session;
	public static Store store;
	public static Folder inbox;
        public static String nw="no";
	public static void clear() {
		username = password = null;
		session = null;
		store = null;
		inbox = null;
	}
}


