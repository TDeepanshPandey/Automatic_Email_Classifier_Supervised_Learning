package com.webmail.utils;

import com.webmail.LoadMoreMessage;
import javafx.application.Platform;
import javafx.scene.control.ListCell;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ListViewCell extends ListCell<Message> {
	private static ExecutorService threadPool = Executors.newCachedThreadPool();
	Future task;

	@Override
	protected void updateItem(Message item, boolean empty) {
		super.updateItem(item, empty);
		setText("");

		if (task != null && !task.isDone())
			task.cancel(true);

		if (empty) {
			Platform.runLater(() -> setGraphic(null));
		} else {
			if (item instanceof LoadMoreMessage) {
				setText("<Load next 35>");
			} else {
				setText("Loading...");

				try {
					setText(item.getSubject());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
