/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-19. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.web;

import be.bagofwords.logging.Log;
import be.bagofwords.util.SerializationUtils;
import spark.Request;
import spark.Response;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public abstract class ApiController extends BaseController {

    public ApiController(String path, String method) {
        super("/api" + path, method, true, "application/json");
    }

    @Override
    protected Object handleRequest(Request request, Response response) throws UnsupportedEncodingException {
        disableCaching(response);
        try {
            Object object = handleAPIRequest(request, response);
            response.type("application/json");
            return SerializationUtils.serializeObject(object);
        } catch (Exception exp) {
            if (response.status() < 300) {
                response.status(500);
            }
            Log.e("Error while handling " + getPath(), exp);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos));
            exp.printStackTrace(writer);
            writer.close();
            return SerializationUtils.serializeObject(new ApiError(exp.getMessage(), bos.toString("UTF-8")));
        }
    }

    private void disableCaching(Response response) {
        response.header("Pragma", "no-cache");
        response.header("Cache-Control", "no-cache");
        response.header("Expires", "-1");
    }

    protected Object handleAPIRequest(Request request, Response response) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.putAll(request.params());
        for (String queryParam : request.queryParams()) {
            params.put(queryParam, request.queryParams(queryParam));
        }
        return handleAPIRequest(params, request.body());
    }

    protected Object handleAPIRequest(Map<String, String> params, String body) throws Exception {
        //Overrridden in subclasses
        return null;
    }

    public static class ApiError {
        public String message;
        public String error;

        public ApiError(String message, String error) {
            this.message = message;
            this.error = error;
        }
    }
}
