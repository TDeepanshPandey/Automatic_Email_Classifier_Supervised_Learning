package com.webmail.controllers;



import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class InputDialogController implements Initializable {
	@FXML public TextField tbInput;
	@FXML public Button btnCancel, btnOk;

	ButtonType btn = ButtonType.CANCEL;

	Stage getStage() {
		return (Stage) tbInput.getScene().getWindow();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		btnCancel.setOnAction(ev -> {
			btn = ButtonType.CANCEL;
			getStage().close();
		});

		btnOk.disableProperty().bind(tbInput.textProperty().isEmpty());
		btnOk.setOnAction(ev -> {
			btn = ButtonType.OK;
			getStage().close();
		});

		tbInput.setOnKeyReleased(ev -> {
			if (ev.getCode() == KeyCode.ENTER) {
				btnOk.fire();
			}
		});
	}

	public static String show(Stage parent) {
		try {
			Stage stage = new Stage();
			FXMLLoader loader = new FXMLLoader(InputDialogController.class.getResource("newfolder.fxml"));
			Parent pane = loader.load();
			stage.setTitle("New folder");
			stage.setScene(new Scene(pane));
			stage.setResizable(false);
			stage.showAndWait();

			if (((InputDialogController) loader.getController()).btn == ButtonType.OK) {
				return ((InputDialogController) loader.getController()).tbInput.getText();
			}

			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
