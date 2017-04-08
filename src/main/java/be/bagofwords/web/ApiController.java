/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-19. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.web;

import be.bagofwords.util.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public abstract class ApiController extends BaseController {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    public ApiController(String path, String method) {
        super("/api" + path, method, true, "application/json");
    }

    @Override
    protected Object handleRequest(Request request, Response response) throws UnsupportedEncodingException {
        try {
            Object object = handleAPIRequest(request, response);
            return SerializationUtils.serializeObject(object);
        } catch (Exception exp) {
            response.status(500);
            logger.error("Error while handling " + getPath(), exp);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos));
            exp.printStackTrace(writer);
            writer.close();
            return SerializationUtils.serializeObject(new ApiError(bos.toString("UTF-8")));
        }
    }

    protected abstract Object handleAPIRequest(Request request, Response response) throws Exception;

    public static class ApiError {
        public String error;

        public ApiError(String error) {
            this.error = error;
        }
    }
}
