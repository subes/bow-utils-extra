package be.bagofwords.application.status;

import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.ui.UI;
import be.bagofwords.util.SocketConnection;
import be.bagofwords.web.SocketRequestHandler;
import be.bagofwords.web.SocketRequestHandlerFactory;
import be.bagofwords.web.SocketServer;

import java.io.IOException;

/**
 * Created by Koen Deschacht (koendeschacht@gmail.com) on 07/10/14.
 */
public class RegisterUrlsServer implements SocketRequestHandlerFactory {

    public static byte SEND_URL = 1;

    @Inject
    private ListUrlsController listUrlsController;

    @Override
    public String getName() {
        return "RegisterUrlServer";
    }

    @Override
    public SocketRequestHandler createSocketRequestHandler(SocketConnection socketConnection) throws IOException {
        return new SocketRequestHandler(socketConnection) {

            @Override
            public void reportUnexpectedError(Exception ex) {
                UI.writeError("Unexpected error in RegisterPathServer", ex);
            }

            @Override
            public void handleRequests() throws Exception {
                byte action = connection.readByte();
                if (action == SEND_URL) {
                    String name = connection.readString();
                    String url = connection.readString();
                    listUrlsController.registerUrl(name, url);
                    connection.writeLong(SocketServer.LONG_OK);
                } else {
                    connection.writeLong(SocketServer.LONG_ERROR);
                }
                connection.flush();
            }

        };
    }

}
