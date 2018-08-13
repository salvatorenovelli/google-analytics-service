package com.myseotoolbox.googleanalyticsservice;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AbstractPromptReceiver;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting;
import com.google.api.services.analyticsreporting.v4.AnalyticsReportingScopes;
import com.google.api.services.analyticsreporting.v4.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class HelloAnalyticsReporting {
    private static final String APPLICATION_NAME = "website-versioning";
    private static final String CLIENT_SECRET_JSON_RESOURCE = "/client_secret.json";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String VIEW_ID = "93074237";
    public static final String REDIRECT_URI = "https://wsv.myseotoolbox.com/api/oauth2callback";

    public HelloAnalyticsReporting() {
        try {
            AnalyticsReporting service = initializeAnalyticsReporting();
            GetReportsResponse response = getReport(service);
            printResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new HelloAnalyticsReporting();
    }


    /**
     * Initializes an Analytics Reporting API V4 service object.
     *
     * @return An authorized Analytics Reporting API V4 service object.
     * @throws IOException
     * @throws GeneralSecurityException
     */

    private static final File DATA_STORE_DIR = new File(System.getProperty("user.home"), ".store/google-analytics-service");
    private static NetHttpTransport httpTransport;
    private static FileDataStoreFactory dataStoreFactory;

    private AnalyticsReporting initializeAnalyticsReporting() throws GeneralSecurityException, IOException {


        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

        // Load client secrets.
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(HelloAnalyticsReporting.class.getResourceAsStream(CLIENT_SECRET_JSON_RESOURCE)));

        // Set up authorization code flow for all authorization scopes.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, AnalyticsReportingScopes.all())
                .setDataStoreFactory(dataStoreFactory)
                .build();


        Credential credential = authorize("user1", flow);


        // Construct the Analytics Reporting service object.
        return new AnalyticsReporting.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    }


    public Credential authorize(String userId, AuthorizationCodeFlow flow) throws IOException {

        Credential credential = flow.loadCredential(userId);
        if (credential != null
                && (credential.getRefreshToken() != null ||
                credential.getExpiresInSeconds() == null ||
                credential.getExpiresInSeconds() > 60)) {
            return credential;
        }


        // Authorize.

        String authorizationUri = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
        System.out.println("GO to:" + authorizationUri);
        String code = waitForCode();
        TokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
        // store credential and return it
        return flow.createAndStoreCredential(response, userId);

    }


//    Credential credential = new AuthorizationCodeInstalledApp(flow, new AbstractPromptReceiver() {
//        public String getRedirectUri() throws IOException {
//            return "https://wsv.myseotoolbox.com/api/oauth2callback";
//        }
//    }).authorize("user");

    public String waitForCode() {
        String code;
        do {
            System.out.print("Please enter code: ");
            code = new Scanner(System.in).nextLine();
        } while (code.isEmpty());
        return code;
    }

    /**
     * Queries the Analytics Reporting API V4.
     *
     * @param service An authorized Analytics Reporting API V4 service object.
     * @return GetReportResponse The Analytics Reporting API V4 response.
     * @throws IOException
     */
    private static GetReportsResponse getReport(AnalyticsReporting service) throws IOException {
        // Create the DateRange object.
        DateRange dateRange = new DateRange().setStartDate("30DaysAgo").setEndDate("today");

        // Create the Metrics object.
        Metric sessions = new Metric()
                .setExpression("ga:sessions")
                .setAlias("sessions");

        Dimension pageTitle = new Dimension().setName("ga:landingPagePath");

        // Create the ReportRequest object.
        ReportRequest request = new ReportRequest()
                .setViewId(VIEW_ID)

                .setDateRanges(Arrays.asList(dateRange))
                .setMetrics(Arrays.asList(sessions))
                .setDimensions(Arrays.asList(pageTitle));

        ArrayList<ReportRequest> requests = new ArrayList<ReportRequest>();
        requests.add(request);

        // Create the GetReportsRequest object.
        GetReportsRequest getReport = new GetReportsRequest()
                .setReportRequests(requests);

        // Call the batchGet method.
        GetReportsResponse response = service.reports().batchGet(getReport).execute();

        // Return the response.
        return response;
    }

    /**
     * Parses and prints the Analytics Reporting API V4 response.
     *
     * @param response An Analytics Reporting API V4 response.
     */
    private static void printResponse(GetReportsResponse response) {

        for (Report report : response.getReports()) {
            ColumnHeader header = report.getColumnHeader();
            List<String> dimensionHeaders = header.getDimensions();
            List<MetricHeaderEntry> metricHeaders = header.getMetricHeader().getMetricHeaderEntries();
            List<ReportRow> rows = report.getData().getRows();

            if (rows == null) {
                System.out.println("No data found for " + VIEW_ID);
                return;
            }

            for (ReportRow row : rows) {
                List<String> dimensions = row.getDimensions();
                List<DateRangeValues> metrics = row.getMetrics();

                for (int i = 0; i < dimensionHeaders.size() && i < dimensions.size(); i++) {
                    System.out.println(dimensionHeaders.get(i) + ": " + dimensions.get(i));
                }

                for (int j = 0; j < metrics.size(); j++) {
                    System.out.print("Date Range (" + j + "): ");
                    DateRangeValues values = metrics.get(j);
                    for (int k = 0; k < values.getValues().size() && k < metricHeaders.size(); k++) {
                        System.out.println(metricHeaders.get(k).getName() + ": " + values.getValues().get(k));
                    }
                }
            }
        }
    }
}