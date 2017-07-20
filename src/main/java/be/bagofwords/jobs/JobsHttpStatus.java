package be.bagofwords.jobs;

import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.web.BaseController;
import spark.Request;
import spark.Response;

/**
 * Created by Koen Deschacht (koendeschacht@gmail.com) on 9/8/14.
 */
public class JobsHttpStatus extends BaseController {

    @Inject
    private JobService jobService;

    public JobsHttpStatus() {
        super("/progress");
    }

    @Override
    protected Object handleRequest(Request request, Response response) throws Exception {
        return jobService.createHtmlStatus();
    }
}