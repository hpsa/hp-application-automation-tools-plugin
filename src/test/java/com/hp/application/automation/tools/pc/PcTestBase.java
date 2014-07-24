package com.hp.application.automation.tools.pc;

import com.hp.application.automation.tools.model.PostRunAction;

public interface PcTestBase {

    public static final String        PC_SERVER_NAME                  = "pcServer.hp.com";
    public static final String        ALM_USER_NAME                   = "sa";
    public static final String        ALM_PASSWORD                    = "pwd";
    public static final String        ALM_DOMAIN                      = "ALMDOM";
    public static final String        ALM_PROJECT                     = "ALMPROJ";
    public static final String        TEST_ID                         = "1";
    public static final String        TEST_INSTANCE_ID                = "2";
    public static final String        TIMESLOT_DURATION_HOURS         = "0";
    public static final String        TIMESLOT_DURATION_MINUTES       = "34";
    public static final String        TIMESLOT_ID                     = "56";
    public static final PostRunAction POST_RUN_ACTION                 = PostRunAction.COLLATE_AND_ANALYZE;
    public static final boolean       VUDS_MODE                       = false;
    public static final String        DESCRIPTION                     = "Testing HP Performance Center Jenkins plugin";
    public static final String        RUN_ID                          = "7";
    public static final String        RUN_ID_WAIT                     = "8";
    public static final String        REPORT_ID                       = "9";
    public static final String        STOP_MODE                       = "stop";

    public static final MockPcModel   pcModel                         = new MockPcModel(PC_SERVER_NAME, ALM_USER_NAME,
                                                                          ALM_PASSWORD, ALM_DOMAIN, ALM_PROJECT,
                                                                          TEST_ID, TEST_INSTANCE_ID,
                                                                          TIMESLOT_DURATION_HOURS,
                                                                          TIMESLOT_DURATION_MINUTES, POST_RUN_ACTION,
                                                                          VUDS_MODE, DESCRIPTION);
    
    public static final String          runResponseEntity  = "<Run xmlns=\"http://www.hp.com/PC/REST/API\">" +
    		                                                    "<TestID>" + TEST_ID + "</TestID>" +
	                                                    		"<TestInstanceID>" + TEST_INSTANCE_ID + "</TestInstanceID>" +
                                                				"<PostRunAction>" + POST_RUN_ACTION.getValue() + "</PostRunAction>" +
                                        						"<TimeslotID>1076</TimeslotID>" +
                                        						"<VudsMode>false</VudsMode>" +
                                        						"<ID>" + RUN_ID + "</ID>" +
                                								"<Duration>" + TIMESLOT_DURATION_MINUTES + "</Duration>" +
                        										"<RunState>*</RunState>" +
                        										"<RunSLAStatus />" + 
                                                             "</Run>";
    
    public static final String          emptyResultsEntity = "<RunResults xmlns=\"http://www.hp.com/PC/REST/API\" />";
    
    public static final String          runResultsEntity   = "<RunResults xmlns=\"http://www.hp.com/PC/REST/API\">" +
    		                                                    "<RunResult>" +
    		                                                        "<ID>1302</ID>" +
        		                                                    "<Name>output.mdb.zip</Name>" +
        		                                                    "<Type>Output Log</Type>" +
        		                                                    "<RunID>" + RUN_ID + "</RunID>" +
    		                                                    "</RunResult>" +
    		                                                    "<RunResult>" +
    		                                                        "<ID>1303</ID>" +
    		                                                        "<Name>RawResults.zip</Name>" +
    		                                                        "<Type>Raw Results</Type>" +
    		                                                        "<RunID>" + RUN_ID + "</RunID>" +
		                                                        "</RunResult>" +
		                                                        "<RunResult>" +
		                                                            "<ID>1304</ID>" +
		                                                            "<Name>Results.zip</Name>" +
		                                                            "<Type>Analyzed Result</Type>" +
		                                                            "<RunID>" + RUN_ID + "</RunID>" +
	                                                            "</RunResult>" +
	                                                            "<RunResult>" +
	                                                                "<ID>" + REPORT_ID + "</ID>" +
	                                                                "<Name>Reports.zip</Name>" +
	                                                                "<Type>HTML Report</Type>" +
	                                                                "<RunID>" + RUN_ID + "</RunID>" +
                                                                "</RunResult>" +
                                                                "<RunResult>" +
                                                                    "<ID>1306</ID>" +
                                                                    "<Name>HighLevelReport_7.xls</Name>" +
                                                                    "<Type>Rich Report</Type>" +
                                                                    "<RunID>" + RUN_ID + "</RunID>" +
                                                                "</RunResult>" +
                                                             "</RunResults>";

    public static final String        pcAuthenticationFailureMessage    = "Exception of type 'HP.PC.API.Model.Exceptions.InvalidAuthenticationDataException' was thrown. Error code: 1100";

    public static final String        pcNoTimeslotExceptionMessage      = "Failed to retrieve reservation information for reservation " + TIMESLOT_ID + ". Error code: 1202";

    public static final String        pcStopNonExistRunFailureMessage   = "Failed to retrieve run " + RUN_ID + " information from domain " + ALM_DOMAIN + ", project " + ALM_PROJECT + ". Error code: 1300";
    
}
