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
import hudson.model.Project;
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

import static com.hp.application.automation.tools.results.projectparser.performance.JobLrScenarioResult
        .DEFAULT_CONNECTION_MAX;
import static com.hp.application.automation.tools.results.projectparser.performance.JobLrScenarioResult
        .DEFAULT_SCENARIO_DURATION;

/**
 * The type Performance project action.
 */
public class PerformanceProjectAction implements Action {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
            .getLogger(PerformanceProjectAction.class.getName());
    /**
     * The Current project.
     */
    public final Project<?, ?> currentProject;
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

        this.currentProject = (Project<?, ?>) project;
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

            JSONObject scenarioData = new JSONObject();
            JSONObject scenarioStats = new JSONObject();
            LrGraphUtils.constructVuserSummary(scenarioResults.getValue().vUserSummary, scenarioStats, _workedBuilds
                    .size());
            LrGraphUtils.constructDurationSummary(scenarioResults.getValue().durationData, scenarioStats);
            LrGraphUtils.constructConnectionSummary(scenarioResults.getValue().maxConnectionsCount, scenarioStats);


            scenarioData.put("scenarioStats", scenarioStats);

            JSONObject scenarioGraphData = new JSONObject();
            //Scenario data graphs
            LrGraphUtils.constructVuserGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructConnectionsGraph(scenarioResults, scenarioGraphData);
            //Scenario SLA graphs
            LrGraphUtils.constructTotalHitsGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructAvgHitsGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructTotalThroughputGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructAverageThroughput(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructErrorGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructAvgTransactionGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructPercentileTransactionGraph(scenarioResults, scenarioGraphData);

            scenarioData.put("scenarioData", scenarioGraphData);

            String scenarioName = scenarioResults.getKey();
            projectDataSet.put(scenarioName, scenarioData);
        }
        return projectDataSet;
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
     * Is visible boolean.
     *
     * @return the boolean
     */
    boolean isVisible() {
        getUpdatedData(); // throw this our once fixes method
        if (!_workedBuilds.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Gets updated data.
     */
    public void getUpdatedData() {
        if (!isUpdateDataNeeded()) {
            return;
        }

        _workedBuilds = new ArrayList<Integer>();
        // TODO: remove after testing!

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

            int runNumber = run.getNumber();
            if (_workedBuilds.contains(runNumber)) {
                continue;
            }

            _workedBuilds.add(runNumber);
            LrJobResults jobLrResult = performanceJobReportAction.getLrResultBuildDataset();

            // get all the ran scenario results from this run and insert them into the project
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
                    scenarioGoalResult(runNumber, lrProjectScenarioResults, goalResult);
                }

                // Join sceanrio stats
                joinSceanrioConnectionsStats(runNumber, lrProjectScenarioResults, scenarioRunResult);
                joinVUserScenarioStats(runNumber, lrProjectScenarioResults, scenarioRunResult);
                joinTransactionScenarioStats(runNumber, lrProjectScenarioResults, scenarioRunResult);
                joinDurationStats(runNumber, lrProjectScenarioResults, scenarioRunResult);

            }

        }

    }

    private void joinDurationStats(int runNumber, LrProjectScenarioResults lrProjectScenarioResults,
                                   JobLrScenarioResult scenarioRunResult) {
        long scenarioConnectionMax = scenarioRunResult.getScenarioDuration();
        if (scenarioConnectionMax != DEFAULT_SCENARIO_DURATION) {
            lrProjectScenarioResults.durationData.put(runNumber, scenarioConnectionMax);
        }
    }

    private void joinTransactionScenarioStats(int runNumber, LrProjectScenarioResults lrProjectScenarioResults,
                                              JobLrScenarioResult scenarioRunResult) {
        HashMap<Integer, HashMap<String, HashMap<String, Integer>>> projectTransactionPerRun =
                lrProjectScenarioResults.transactionPerRun;
        Map<String, Integer> projectTransactionSum = lrProjectScenarioResults.transactionSum;

        final HashMap<String, HashMap<String, Integer>> scenarioTransactionData =
                scenarioRunResult.transactionData;
        final HashMap<String, Integer> scenarioTransactionSum = scenarioRunResult.transactionSum;

        if(scenarioTransactionData == null || scenarioTransactionSum == null){
            return;
        }

        if (!scenarioTransactionData.isEmpty()) {
            //store transaction state data per run
            projectTransactionPerRun.put(runNumber, scenarioTransactionData);
            //add all summary transcation states to project level summary
            for (Map.Entry<String, Integer> transactionState : scenarioTransactionSum.entrySet()) {
                int previousCount = 0;
                if(projectTransactionSum.containsKey(transactionState.getKey()))
                {
                    previousCount = projectTransactionSum.get(transactionState.getKey());
                }
                projectTransactionSum.put(transactionState.getKey(), previousCount + transactionState.getValue());
            }


            //add all per transcation states to project level per transaction summary
            Map<String, HashMap<String, Integer>> projectTransactionsData = lrProjectScenarioResults.transactionData;
            for(Map.Entry<String, HashMap<String, Integer>> scenarioTransactionDataSet :
                    scenarioTransactionData.entrySet())
            {
                String transactionName = scenarioTransactionDataSet.getKey();
                HashMap<String, Integer> TransactionStateData = scenarioTransactionDataSet.getValue();
                if(!projectTransactionsData.containsKey(transactionName)){
                    projectTransactionsData.put(transactionName, new HashMap<String, Integer>(TransactionStateData));
                    continue;
                }

                HashMap<String, Integer> projectTransactionState = projectTransactionsData.get(transactionName);
                for(Map.Entry<String, Integer> scenarioTransactionState : TransactionStateData.entrySet())
                {
                    Integer currentValue = scenarioTransactionState.getValue();
                    projectTransactionState.put(scenarioTransactionState.getKey(), currentValue +
                            scenarioTransactionState.getValue());
                }
            }
        }
    }

    private void joinVUserScenarioStats(int runNumber, LrProjectScenarioResults lrProjectScenarioResults,
                                        JobLrScenarioResult scenarioRunResult) {
        Map<Integer, Map<String, Integer>> vUserPerRun = lrProjectScenarioResults.vUserPerRun;
        if (scenarioRunResult.vUserSum != null && !scenarioRunResult.vUserSum.isEmpty()) {
            for (Map.Entry<String, Integer> vUserStat : scenarioRunResult.vUserSum.entrySet()) {
                if (!vUserPerRun.containsKey(runNumber)) {
                    vUserPerRun.put(runNumber, new HashMap<String, Integer>(0));
                    LrProjectScenarioResults.vUserMapInit(vUserPerRun.get(runNumber));
                }
                vUserPerRun.get(runNumber).put(vUserStat.getKey(), vUserStat.getValue());
                int previousCount = 0;
                if(lrProjectScenarioResults.vUserSummary.containsKey(vUserStat.getKey()))
                {
                    previousCount = lrProjectScenarioResults.vUserSummary.get(vUserStat.getKey());
                }
                lrProjectScenarioResults.vUserSummary
                        .put(vUserStat.getKey(), previousCount + vUserStat.getValue());
            }
        }
    }

    private void joinSceanrioConnectionsStats(int runNumber, LrProjectScenarioResults lrProjectScenarioResults,
                                              JobLrScenarioResult scenarioRunResult) {
        int scenarioConnectionMax = scenarioRunResult.getConnectionMax();
        if (scenarioConnectionMax != DEFAULT_CONNECTION_MAX) {
            lrProjectScenarioResults.maxConnectionsCount.put(runNumber, scenarioConnectionMax);
        }
    }

    private static void scenarioGoalResult(int runNumber, LrProjectScenarioResults lrProjectScenarioResults,
                                           GoalResult goalResult) {
        switch (goalResult.getSlaGoal()) {
            case AverageThroughput:
                lrProjectScenarioResults.averageThroughputResults
                        .put(runNumber, (WholeRunResult) goalResult);
                break;
            case TotalThroughput:
                lrProjectScenarioResults.totalThroughtputResults
                        .put(runNumber, (WholeRunResult) goalResult);
                break;
            case AverageHitsPerSecond:
                lrProjectScenarioResults.averageHitsPerSecondResults
                        .put(runNumber, (WholeRunResult) goalResult);
                break;
            case TotalHits:
                lrProjectScenarioResults.totalHitsResults.put(runNumber, (WholeRunResult) goalResult);
                break;
            case ErrorsPerSecond:
                lrProjectScenarioResults.errPerSecResults
                        .put(runNumber, (TimeRangeResult) goalResult);
                break;
            case PercentileTRT:
                if (!lrProjectScenarioResults.percentileTransactionResults.containsKey(runNumber)) {
                    lrProjectScenarioResults.percentileTransactionResults
                            .put(runNumber, new HashMap<String, PercentileTransactionWholeRun>(0));
                }
                lrProjectScenarioResults.transactions
                        .add(((PercentileTransactionWholeRun) goalResult).getName());
                lrProjectScenarioResults.percentileTransactionResults.get(runNumber)
                        .put(((PercentileTransactionWholeRun) goalResult).getName(),
                                (PercentileTransactionWholeRun) goalResult);
                break;
            case AverageTRT:
                if (!lrProjectScenarioResults.avgTransactionResponseTimeResults
                        .containsKey(runNumber)) {
                    lrProjectScenarioResults.avgTransactionResponseTimeResults
                            .put(runNumber, new HashMap<String, AvgTransactionResponseTime>(0));
                }
                lrProjectScenarioResults.transactions
                        .add(((AvgTransactionResponseTime) goalResult).getName());
                lrProjectScenarioResults.avgTransactionResponseTimeResults.get(runNumber)
                        .put(((AvgTransactionResponseTime) goalResult).getName(),
                                (AvgTransactionResponseTime) goalResult);
                break;
            default:
                break;
        }
    }

    private boolean isUpdateDataNeeded() {
//        final Run<?, ?> lastBuild = currentProject.getLastBuild();
//        if (null == lastBuild) {
//            return false;
//        }
//
//        int latestBuildNumber = lastBuild.getNumber();
//        if (latestBuildNumber == lastBuildId) {
//            return true;
//        }

        return true;
    }

}
