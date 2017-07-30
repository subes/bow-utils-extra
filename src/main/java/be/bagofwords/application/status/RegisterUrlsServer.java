package be.bagofwords.application.status;

import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.util.SocketConnection;
import be.bagofwords.web.SocketRequestHandler;
import be.bagofwords.web.SocketRequestHandlerFactory;

import java.io.IOException;

import static be.bagofwords.util.SocketMessage.ERROR;
import static be.bagofwords.util.SocketMessage.OK;

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
                Log.e("Unexpected error in RegisterPathServer", ex);
            }

            @Override
            public void handleRequests() throws Exception {
                byte action = connection.readByte();
                if (action == SEND_URL) {
                    String url = connection.readString();
                    listUrlsController.registerUrl(url);
                    connection.writeValue(OK);
                } else {
                    connection.writeValue(ERROR);
                }
                connection.flush();
            }

        };
    }

}
