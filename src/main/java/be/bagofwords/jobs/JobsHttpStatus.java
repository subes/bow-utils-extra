package be.bagofwords.jobs;

import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.web.BaseController;
import spark.Request;
import spark.Response;

/**
 * Created by Koen Deschacht (koendeschacht@gmail.com) on 9/8/14.
 */
public class JobsHttpStatus extends BaseController {

    private JobRunner jobRunner;

    public JobsHttpStatus(ApplicationContext context) {
        super("/progress");
        jobRunner = context.getBean(JobRunner.class);
    }

    @Override
    protected Object handleRequest(Request request, Response response) throws Exception {
        return jobRunner.createHtmlStatus();
    }
}