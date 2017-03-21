package be.bagofwords.application.status;

import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.util.Pair;
import be.bagofwords.web.BaseController;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Koen Deschacht (koendeschacht@gmail.com) on 07/10/14.
 */

public class ListUrlsController extends BaseController {

    private final List<Pair<String, String>> urls = new ArrayList<>();

    @Inject
    private RegisterUrlsServer registerUrlsServer;

    public ListUrlsController() {
        super("/paths");
    }

    @Override
    protected String handleRequest(Request request, Response response) throws Exception {
        StringBuilder result = new StringBuilder();
        synchronized (urls) {
            for (Pair<String, String> url : urls) {
                result.append("<a href=\"http://" + url.getSecond() + "\">" + url.getFirst() + " " + url.getSecond() + "</a><br>");
            }
        }
        return result.toString();
    }

    public void registerUrl(String name, String url) {
        synchronized (urls) {
            Pair<String, String> toRegister = new Pair<>(name, url);
            if (!urls.contains(toRegister)) {
                urls.add(0, toRegister);
            }
            while (urls.size() > 20) {
                urls.remove(urls.get(urls.size() - 1));
            }
        }
    }

}
