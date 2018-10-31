package org.wso2.am.integration.tests.versioning;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.config.RequestConfig;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class ScenarioSampleTest {
    private final Log log = LogFactory.getLog(ScenarioSampleTest.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String publisherURLHttp;
    private String storeURLHttp;
    private APIRequest apiRequest;

    private String apiName = "Mobile_Stock_API";
    private String APIContext = "MobileStock";
    private String tags = "stock";
    private String endpointUrl, endpointUrlNew;
    private String description = "This is test API created for scenario test";
    private String APIVersion = "1.0.0";
    private String APIVersionNew = "2.0.0";
    String resourceLocation = System.getProperty("framework.resource.location");
    int timeout = 10;
    RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(timeout * 100)
            .setConnectionRequestTimeout(timeout * 1000)
            .setSocketTimeout(timeout * 1000).build();

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

//        String serverURL = "https://" + getServerURL() + ":9443/carbon";
        publisherURLHttp = "http://" + getServerURL() + ":9763/";
        storeURLHttp = "http://" + getServerURL() + ":9763/";
        endpointUrl = "http://" + getServerURL() + ":9763/am/sample/calculator/v1/api/add";

        setKeyStoreProperties();
//        AuthenticatorClient authenticatorClient = new AuthenticatorClient(serverURL);
//        String sessionCookie = authenticatorClient.login("admin", "admin", getServerURL());
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login("admin", "admin");
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login("admin", "admin");
    }

    @Test(description = "Add new version and an API")
    public void testAPINewVersionCreation() throws Exception {
        String providerName = "admin";

        apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);

        log.info("request : " + apiRequest.getProvider());

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
//        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, providerName,
                APILifeCycleState.PROTOTYPED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);


        //copy test api
        serviceResponse = apiPublisher
                .copyAPI(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(), APIVersionNew, null);


        //test the copied api
        serviceResponse = apiPublisher.getAPI(apiRequest.getName(), apiRequest.getProvider(), APIVersionNew);


        JSONObject response = new JSONObject(serviceResponse.getData());
        String version = response.getJSONObject("api").get("version").toString();
        Assert.assertEquals(version, APIVersionNew);

        //publish the api
        updateRequest = new APILifeCycleStateRequest(apiName, "admin",
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);

        Assert.assertTrue(serviceResponse.getData().contains(APILifeCycleState.PUBLISHED.getState()));
    }

    @Test(description = "Subscribe to default API and invoke with product token")
    public void testAPISubscriptionAndInvokation() throws Exception{
        String providerName = "admin";
//        apiStore.login("admin", "admin");
//        apiStore.addApplication(APP_NAME, "Unlimited", "", "");
//        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, API_PROVIDER);
//        subscriptionRequest.setApplicationName(APP_NAME);
//        apiStore.subscribe(subscriptionRequest);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.deleteAPI(apiName, APIVersion, "admin");
        apiPublisher.deleteAPI(apiName, APIVersionNew, "admin");
    }

    private String getServerURL() {
//        String bucketLocation = System.getenv("DATA_BUCKET_LOCATION");
        String url = null;

        Properties prop = new Properties();
        //InputStream input = null;
        try (InputStream input = new FileInputStream(resourceLocation + "temp/infrastructure.properties")) {
            prop.load(input);
            url = prop.getProperty("WSO2PublicIP");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //Construct the proper URL if required
        return url == null ? "localhost" : url;
    }

    private void setKeyStoreProperties() {
        System.setProperty("javax.net.ssl.trustStore", resourceLocation + "/keystores/wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }
}
