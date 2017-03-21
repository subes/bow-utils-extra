package be.bagofwords.application.status;

import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.web.BaseController;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

public class HttpApplicationStatus extends BaseController {

    private ApplicationContext applicationContext;

    public HttpApplicationStatus() {
        super("/status");
    }

    protected String getOutput() throws IOException {
        StringBuilder sb = new StringBuilder();
        List<? extends StatusViewable> statusViewables = applicationContext.getBeans(StatusViewable.class);
        for (StatusViewable statusViewable : statusViewables) {
            statusViewable.printHtmlStatus(sb);
        }
        return sb.toString();
    }

    @Override
    public String handleRequest(Request request, Response response) throws IOException {
        StringBuilder sb = new StringBuilder();
        String applicationName = applicationContext.getApplicationName();
        sb.append("<html><head><title>" + applicationName + ": application status</title></head><body>");
        sb.append(getOutput());
        sb.append("</body></html>");
        return sb.toString();
    }


}
