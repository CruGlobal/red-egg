package org.cru.redegg.test;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
* @author Matt Drees
*/
@ApplicationPath(TestApplication.REST_PATH)
public class TestApplication extends Application
{

    static final String REST_PATH = "rest";
}
