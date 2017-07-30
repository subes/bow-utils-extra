package be.bagofwords.application.status;

import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.web.BaseController;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Koen Deschacht (koendeschacht@gmail.com) on 07/10/14.
 */

public class ListUrlsController extends BaseController {

    private final List<String> urls = new ArrayList<>();

    @Inject
    private RegisterUrlsServer registerUrlsServer;

    public ListUrlsController() {
        super("/paths");
    }

    @Override
    protected String handleRequest(Request request, Response response) throws Exception {
        StringBuilder result = new StringBuilder();
        synchronized (urls) {
            for (String url : urls) {
                result.append("<a href=\"http://" + url + "\">" + url + "</a><br>");
            }
        }
        return result.toString();
    }

    public void registerUrl(String url) {
        synchronized (urls) {
            if (!urls.contains(url)) {
                urls.add(0, url);
            }
            while (urls.size() > 20) {
                urls.remove(urls.get(urls.size() - 1));
            }
        }
    }

}
