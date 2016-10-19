/*
 * MIT License
 *
 * Copyright (c) 2016 Hewlett-Packard Development Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.hp.application.automation.tools;

import com.hp.application.automation.tools.results.PerformanceJobReportAction;
import com.hp.application.automation.tools.results.projectparser.performance.AvgTransactionResponseTime;
import com.hp.application.automation.tools.results.projectparser.performance.GoalResult;
import com.hp.application.automation.tools.results.projectparser.performance.JobLrScenarioResult;
import com.hp.application.automation.tools.results.projectparser.performance.LrJobResults;
import com.hp.application.automation.tools.results.projectparser.performance.LrProjectScenarioResults;
import com.hp.application.automation.tools.results.projectparser.performance.PercentileTransactionWholeRun;
import com.hp.application.automation.tools.results.projectparser.performance.ProjectLrResults;
import com.hp.application.automation.tools.results.projectparser.performance.TimeRangeResult;
import com.hp.application.automation.tools.results.projectparser.performance.WholeRunResult;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Run;
import hudson.util.RunList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The type Performance project action.
 */
public class PerformanceProjectAction implements Action {

    /**
     * The constant X_AXIS_TITLE.
     */
    public static final String X_AXIS_TITLE = "x_axis_title";
    /**
     * The constant Y_AXIS_TITLE.
     */
    public static final String Y_AXIS_TITLE = "y_axis_title";
    /**
     * The constant DESCRIPTION.
     */
    public static final String DESCRIPTION = "description";
    /**
     * The constant TITLE.
     */
    public static final String TITLE = "title";
    /**
     * The constant LABELS.
     */
    public static final String LABELS = "labels";
    /**
     * The constant BUILD_NUMBER.
     */
    public static final String BUILD_NUMBER = "Build number";
    /**
     * The constant PERCENTILE_TRANSACTION_RESPONSE_TIME.
     */
    public static final String PERCENTILE_TRANSACTION_RESPONSE_TIME = "Percentile Transaction Response TIme";
    /**
     * The constant TRANSACTIONS_RESPONSE_TIME_SECONDS.
     */
    public static final String TRANSACTIONS_RESPONSE_TIME_SECONDS = "Transactions response time (Seconds)";
    public static final String PRECENTILE_GRAPH_DESCRIPTION =
            "Displays the average time taken to perform transactions during each second of the load test." +
                    " This graph helps you determine whether the performance of the server is within " +
                    "acceptable minimum and maximum transaction performance time ranges defined for your " +
                    "system.";
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
            .getLogger(PerformanceProjectAction.class.getName());
    /**
     * The Current project.
     */
    public final AbstractProject<?, ?> currentProject;
    private final LrGraphUtils lrGraphUtils = new LrGraphUtils();
    private ArrayList<LrJobResults> jobLrResults;
    private int lastBuildId = -1;
    private ArrayList<Integer> _workedBuilds;
    private ProjectLrResults _projectResult;

    /**
     * Instantiates a new Performance project action.
     *
     * @param project the project
     */
    public PerformanceProjectAction(AbstractProject<?, ?> project) {

        this._projectResult = new ProjectLrResults();
        this._workedBuilds = new ArrayList<Integer>();
        this.jobLrResults = new ArrayList<LrJobResults>();

        this.currentProject = project;
    }

    private void updateLastBuild() {

        // TODO: The first one is the last one!!

    }

    /**
     * Gets scenario list.
     *
     * @return the scenario list
     */
    @JavaScriptMethod
    public JSONArray getScenarioList() {
        JSONArray scenarioList = new JSONArray();
        for (String scenarioName : _projectResult.getScenarioResults().keySet()) {
            JSONObject scenario = new JSONObject();
            scenario.put("ScenarioName", scenarioName);
            scenarioList.add(scenario);
        }
        return scenarioList;
    }

    /**
     * Collates graph data per scenario per build for the whole project.
     * Adds the respected graphs with scenario as the key
     *
     * @return the graph data
     */
    @JavaScriptMethod
    public JSONObject getGraphData() {
        JSONObject projectDataSet = new JSONObject();
        for (Map.Entry<String, LrProjectScenarioResults> scenarioResults : _projectResult.getScenarioResults()
                .entrySet()) {

            JSONObject scenarioGraphData = new JSONObject();
            String scenarioName = scenarioResults.getKey();

            lrGraphUtils.constructTotalHitsGraph(scenarioResults, scenarioGraphData);
            lrGraphUtils.constructAvgHitsGraph(scenarioResults, scenarioGraphData);
            lrGraphUtils.constructTotalThroughputGraph(scenarioResults, scenarioGraphData);
            lrGraphUtils.constructAverageThroughput(scenarioResults, scenarioGraphData);
            lrGraphUtils.constructErrorGraph(scenarioResults, scenarioGraphData);
            lrGraphUtils.constructAvgTransactionGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructPercentileTransactionGraph(scenarioResults, scenarioGraphData);

            projectDataSet.put(scenarioName, scenarioGraphData);
        }
        return projectDataSet;
    }

    /**
     * Gets total hits graph data.
     *
     * @return the total hits graph data
     */
    @JavaScriptMethod
    public JSONObject getTotalHitsGraphData() {
        JSONObject graphDataSet;
        JSONObject scenarioGraphData = new JSONObject();

        for (Map.Entry<String, LrProjectScenarioResults> scenarioResults : _projectResult.getScenarioResults()
                .entrySet()) {
            Map<Integer, WholeRunResult> graphData = scenarioResults.getValue().totalHitsResults;

            graphDataSet = lrGraphUtils.extractWholeRunSlaResult(graphData);
            scenarioGraphData.put(scenarioResults.getKey(), graphDataSet);

            return graphDataSet;

        }
        return scenarioGraphData;
    }

    /**
     * Gets avg hits per sec graph data.
     *
     * @return the avg hits per sec graph data
     */
    @JavaScriptMethod
    public JSONObject getAvgHitsPerSecGraphData() {
        JSONObject scenarioGraphData = new JSONObject();

        for (Map.Entry<String, LrProjectScenarioResults> scenarioResults : _projectResult.getScenarioResults()
                .entrySet()) {
            Map<Integer, WholeRunResult> graphData = scenarioResults.getValue().averageHitsPerSecondResults;

            JSONObject graphDataSet = lrGraphUtils.extractWholeRunSlaResult(graphData);
            scenarioGraphData.put(scenarioResults.getKey(), graphDataSet);

            return graphDataSet;

        }
        return scenarioGraphData;
    }

    /**
     * Gets total throughput graph data.
     *
     * @return the total throughput graph data
     */
    @JavaScriptMethod
    public JSONObject getTotalThroughputGraphData() {
        JSONObject graphDataSet;
        JSONObject scenarionGraphData = new JSONObject();

        for (Map.Entry<String, LrProjectScenarioResults> scenarioResults : _projectResult.getScenarioResults()
                .entrySet()) {
            Map<Integer, WholeRunResult> graphData = scenarioResults.getValue().totalThroughtputResults;

            graphDataSet = lrGraphUtils.extractWholeRunSlaResult(graphData);
            scenarionGraphData.put(scenarioResults.getKey(), graphDataSet);

            return graphDataSet;

        }
        return scenarionGraphData;
    }

    /**
     * Gets avg throughtput results graph data.
     *
     * @return the avg throughtput results graph data
     */
    @JavaScriptMethod
    public JSONObject getAvgThroughtputResultsGraphData() {
        JSONObject graphDataSet;
        JSONObject scenarionGraphData = new JSONObject();

        for (Map.Entry<String, LrProjectScenarioResults> scenarioResults : _projectResult.getScenarioResults()
                .entrySet()) {
            Map<Integer, WholeRunResult> graphData = scenarioResults.getValue().averageThroughputResults;

            graphDataSet = lrGraphUtils.extractWholeRunSlaResult(graphData);
            scenarionGraphData.put(scenarioResults.getKey(), graphDataSet);

            return graphDataSet;

        }
        return scenarionGraphData;
    }

    /**
     * Gets avg transaction results graph data.
     *
     * @return the avg transaction results graph data
     */
    @JavaScriptMethod
    public JSONObject getAvgTransactionResultsGraphData() {
        JSONObject scenarionGraphData = new JSONObject();

        for (Map.Entry<String, LrProjectScenarioResults> scenarioResults : _projectResult.getScenarioResults()
                .entrySet()) {

            Map<Integer, HashMap<String, AvgTransactionResponseTime>> graphData =
                    scenarioResults.getValue().avgTransactionResponseTimeResults;
            JSONObject graphDataSet =
                    lrGraphUtils.extractAvgTrtData(graphData, scenarioResults.getValue().transactions);
            scenarionGraphData.put(scenarioResults.getKey(), graphDataSet);
            return graphDataSet;

        }
        return scenarionGraphData;
    }

    /**
     * Gets percentelie transaction results graph data.
     *
     * @return the percentelie transaction results graph data
     */
    @JavaScriptMethod
    public JSONObject getPercentelieTransactionResultsGraphData() {
        JSONObject scenarionGraphData = new JSONObject();
        for (Map.Entry<String, LrProjectScenarioResults> scenarioResults : _projectResult.getScenarioResults()
                .entrySet()) {
            Map<Integer, HashMap<String, PercentileTransactionWholeRun>> graphData =
                    scenarioResults.getValue().percentileTransactionResults;
            JSONObject graphDataSet =
                    LrGraphUtils.extractPercentileTransactionSet(graphData, scenarioResults.getValue().transactions);
            scenarionGraphData.put(scenarioResults.getKey(), graphDataSet);
            return graphDataSet;
        }
        return scenarionGraphData;
    }

    /**
     * Gets error per sec results graph data.
     *
     * @return the error per sec results graph data
     */
    @JavaScriptMethod
    public JSONObject getErrorPerSecResultsGraphData() {
        JSONObject graphDataSet;
        JSONObject scenarioGraphData = new JSONObject();

        for (Map.Entry<String, LrProjectScenarioResults> scenarioResults : _projectResult.getScenarioResults()
                .entrySet()) {
            Map<Integer, TimeRangeResult> graphData = scenarioResults.getValue().errPerSecResults;
            graphDataSet = lrGraphUtils.extractTimeRangeResult(graphData);
            scenarioGraphData.put(scenarioResults.getKey(), graphDataSet);
        }
        return scenarioGraphData;
    }

    /**
     * Gets build performance report list.
     *
     * @return the build performance report list
     */
    public List<String> getBuildPerformanceReportList() {
        // this.buildPerformanceReportList = new ArrayList<String>(0);
        // if (null == this.currentProject) {
        // return this.buildPerformanceReportList;
        // }

        // if (null == this.currentProject.getSomeBuildWithWorkspace()) {
        // return buildPerformanceReportList;
        // }

        // List<? extends AbstractBuild<?, ?>> builds = currentProject.getBuilds();
        // int nbBuildsToAnalyze = builds.size();
        //// Range buildsLimits = getFirstAndLastBuild(request, builds);

        // for (AbstractBuild<?, ?> currentBuild : builds) {
        //
        // buildPerformanceReportList.add(currentBuild.getId());
        // }
        // return buildPerformanceReportList;
        return new ArrayList<String>(0);
    }

    @Override
    public String getIconFileName() {
        return "/plugin/hp-application-automation-tools-plugin/PerformanceReport/LoadRunner.png";
    }

    @Override
    public String getDisplayName() {
        return "Project Performance report";
    }

    @Override
    public String getUrlName() {
        return "PerformanceProjectReport";
    }

    /**
     * Add int.
     *
     * @param x the x
     * @param y the y
     * @return the int
     */
    @JavaScriptMethod
    public int add(int x, int y) {
        return x + y;
    }

    /**
     * Is visible boolean.
     *
     * @return the boolean
     */
    boolean isVisible() {
        getUpdatedData(); // throw this our once fixes method
        if (_workedBuilds.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Gets updated data.
     */
    public void getUpdatedData() {
        if (isUpdateDataNeeded()) {
            return;
        }

        _workedBuilds = new ArrayList<Integer>(); // TODO: remove after testing!

        RunList<? extends Run> projectBuilds = currentProject.getBuilds();

        // updateLastBuild();

        for (Run run : projectBuilds) {
            PerformanceJobReportAction performanceJobReportAction = run.getAction(PerformanceJobReportAction.class);
            if (performanceJobReportAction == null) {
                continue;
            }
            if (run.isBuilding()) {
                continue;
            }

            if (_workedBuilds.contains(run.getNumber())) {
                continue;
            }

            _workedBuilds.add(run.getNumber());
            LrJobResults jobLrResult = performanceJobReportAction.getLrResultBuildDataset();

            // get all the ran scenario results
            for (Map.Entry<String, JobLrScenarioResult> runResult : jobLrResult.getLrScenarioResults().entrySet()) {
                // add the scenario if it's the first time it's ran in this build (allows scenarios to be also added
                // at diffrent time)
                if (!_projectResult.getScenarioResults().containsKey(runResult.getKey())) {
                    _projectResult.addScenario(new LrProjectScenarioResults(runResult.getKey()));
                }

                // Join the SLA rule results
                LrProjectScenarioResults lrProjectScenarioResults =
                        _projectResult.getScenarioResults().get(runResult.getKey());
                JobLrScenarioResult scenarioRunResult = runResult.getValue();
                for (GoalResult goalResult : scenarioRunResult.scenarioSlaResults) {
                    switch (goalResult.getSlaGoal()) {
                        case AverageThroughput:
                            lrProjectScenarioResults.averageThroughputResults
                                    .put(run.getNumber(), (WholeRunResult) goalResult);
                            break;
                        case TotalThroughput:
                            lrProjectScenarioResults.totalThroughtputResults
                                    .put(run.getNumber(), (WholeRunResult) goalResult);
                            break;
                        case AverageHitsPerSecond:
                            lrProjectScenarioResults.averageHitsPerSecondResults
                                    .put(run.getNumber(), (WholeRunResult) goalResult);
                            break;
                        case TotalHits:
                            lrProjectScenarioResults.totalHitsResults.put(run.getNumber(), (WholeRunResult) goalResult);
                            break;
                        case ErrorsPerSecond:
                            lrProjectScenarioResults.errPerSecResults
                                    .put(run.getNumber(), (TimeRangeResult) goalResult);
                            break;
                        case PercentileTRT:
                            if (!lrProjectScenarioResults.percentileTransactionResults.containsKey(run.getNumber())) {
                                lrProjectScenarioResults.percentileTransactionResults
                                        .put(run.getNumber(), new HashMap<String, PercentileTransactionWholeRun>(0));
                            }
                            lrProjectScenarioResults.transactions
                                    .add(((PercentileTransactionWholeRun) goalResult).getName());
                            lrProjectScenarioResults.percentileTransactionResults.get(run.getNumber())
                                    .put(((PercentileTransactionWholeRun) goalResult).getName(),
                                            (PercentileTransactionWholeRun) goalResult);
                            break;
                        case AverageTRT:
                            if (!lrProjectScenarioResults.avgTransactionResponseTimeResults
                                    .containsKey(run.getNumber())) {
                                lrProjectScenarioResults.avgTransactionResponseTimeResults
                                        .put(run.getNumber(), new HashMap<String, AvgTransactionResponseTime>(0));
                            }
                            lrProjectScenarioResults.transactions
                                    .add(((AvgTransactionResponseTime) goalResult).getName());
                            lrProjectScenarioResults.avgTransactionResponseTimeResults.get(run.getNumber())
                                    .put(((AvgTransactionResponseTime) goalResult).getName(),
                                            (AvgTransactionResponseTime) goalResult);
                            break;
                        default:
                            break;
                    }
                }

                // Join sceanrio stats

            }
        }
    }

    private boolean isUpdateDataNeeded() {
        int latestBuildNumber = currentProject.getLastBuild().getNumber();
        if (lastBuildId == latestBuildNumber) {
            return true;
        }
        return false;
    }

}
