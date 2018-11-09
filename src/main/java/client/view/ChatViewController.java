package client.view;

import client.Main;
import client.controller.ClientController;
import client.utils.CustomTextArea;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

import static client.utils.Common.showAlert;

public class ChatViewController implements Initializable {

    public WebEngine webEngine;

    @FXML
    private AnchorPane messagePanel;

    @FXML
    private WebView messageWebView;

    @FXML
    private ListView<String> contactListView;

    @FXML
    private CustomTextArea messageField;

    private ObservableList<String> contactsObservList;

    private ClientController clientController;

    public ChatViewController() {
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        clientController = ClientController.getInstance();
        clientController.setChatViewController(this);
        contactsObservList = FXCollections.observableArrayList();
        fillContactListView();
        webtest();

        messageField.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode().equals(KeyCode.ENTER)) {
                String text = messageField.getText().trim();
                if (!text.isEmpty()) {
                    clientController.sendMessage(messageField.getText());
                    messageField.clear();
                    messageField.requestFocus();
                }
                event.consume();
            }
        });
    }

    private void webtest() {
        webEngine = messageWebView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        //clientController.getInstance();
        webEngine.loadContent("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +

                "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
                "</head>\n" +

                "    <body style=\"background-image: url(https://s3-alpha.figma.com/img/0f65/df21/9351ace9280e6668b235304d7ceaf426)\">\n" +

                "        <div id=\"messageArea\">" +
                "       </div>\n" +

                "    </body>\n" +
                "</html>");


    }

    public void fillContactListView() {
        contactListView.setItems(contactsObservList);
        contactListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            setText(item);
                            if (item.equals(clientController.getMyNick())) {
                                setStyle("-fx-font-weight: bold;" +
                                        " -fx-background-color: #ffead4");
                            }
                        } else {
                            setGraphic(null);
                            setText(null);
                        }
                    }
                };
            }
        });
        contactsObservList.clear();
        contactsObservList.addAll(clientController.getAllUserNames());
    }

    @FXML
    private void handleDisconnectButton() {
        Stage stage = (Stage) messagePanel.getScene().getWindow();
        stage.close();
        Main.initRootLayout();
        Main.showOverview();
    }

    @FXML
    private void handleExit() {
        clientController.dbServiceClose();
        clientController.disconnect();
        System.exit(0);
    }

    @FXML
    private void handleSendMessage() {
        clientController.sendMessage(messageField.getText());
        messageField.clear();
        messageField.requestFocus();
    }

    @FXML
    private void handleClientChoice(MouseEvent event) {
        if (event.getClickCount() == 1) {
            String receiver = contactListView.getSelectionModel().getSelectedItem();
            showAlert("Сообщения будут отправляться контакту " + receiver, Alert.AlertType.INFORMATION);
            clientController.setReceiver(receiver);
        }

        messageField.requestFocus();
        messageField.selectEnd();
    }

    @FXML
    private void handleAddContactButton() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/client/fxml/AddContactView.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Add contact");
        stage.setResizable(false);
        stage.setScene(new Scene(root));
        stage.show();
    }

    public void initFX() {

        final String EVENT_TYPE_CLICK = "click";

        webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                    EventListener listener = new EventListener() {
                        @Override
                        public void handleEvent(Event ev) {
                            String domEventType = ev.getType();
                            System.err.println("EventType: " + domEventType); // DEBUG
                            if (domEventType.equals(EVENT_TYPE_CLICK)) {
                                String href = ((Element)ev.getTarget()).getAttribute("href");
                                System.out.println(href); // DEBUG
//                                try {
                                try {
                                    Desktop.getDesktop().browse(new URI(href.contains("://") ? href : "http://" + href + "/"));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (URISyntaxException e) {
                                    e.printStackTrace();
                                }
                                System.out.println("Opening external browser.");
//                                } catch (IOException | URISyntaxException e) {
//                                    e.printStackTrace();
//                                }
                            }
                        }
                    };

                    Document doc = webEngine.getDocument();
                    NodeList nodeList = doc.getElementsByTagName("a");
                    System.out.println(nodeList.getLength());

                    for (int i = 0; i < nodeList.getLength(); i++) {
                        ((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
                        System.out.println("Add event listener.");
                    }
                }
            }
        });
    }
    
}
