﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using Analysis.Api;
using Analysis.ApiLib;
using HpToolsLauncher;
using LRAnalysisLauncher.Properties;

namespace LRAnalysisLauncher
{
    public static class Helper
    {
  
        /// <summary>
        /// Returns the number of run vusers from VuserStateGraph graph
        /// </summary>
        /// <returns>Number of run vusers per catagory</returns>
        public static Dictionary<string, int> GetVusersCountByStatus(LrAnalysis lrAnalysis)
        {

            
            var vuserDictionary = new Dictionary<string, int>(4)
            {
                {"Passed", 0},
                {"Stopped", 0},
                {"Failed", 0},
                {"Error", 0}
            };

            var vUserGraph = lrAnalysis.Session.OpenGraph("VuserSummary");
            if (vUserGraph == null)
            {
                return vuserDictionary;
            }

            //FilterItem filterDimension = vUserGraph.Filter["Vuser End Status"];
            List<String> vUserStates = new List<String>()
            {
                {"Passed"},
                {"Stopped"},
                {"Failed"},
                {"Error"}
            };
            ConsoleWriter.WriteLine("Counting vUser Results for this scenario");
            foreach (Series vUserType in vUserGraph.Series)
            {
                //filterDimension.ClearValues();
                //filterDimension.AddDiscreteValue(vUserType.Name);
                vUserGraph.ApplyFilterAndGroupBy();
                double vUserTypeMax = vUserType.GraphStatistics.Maximum;
                if (!HasValue(vUserTypeMax)){
                    continue;
                }
                
                vuserDictionary[vUserType.Name] = (int)Math.Round(vUserTypeMax);
            }

            ConsoleWriter.WriteLine("Getting maximum ran vUsers this scenarion");
            var vUserStateGraph = lrAnalysis.Session.OpenGraph("VuserStateGraph");
            if (vUserStateGraph == null)
            {
                return vuserDictionary;
            }
            vUserStateGraph.Granularity = 4;
            FilterItem filterDimensionVUser = vUserStateGraph.Filter["Vuser Status"];
            foreach (Series vUserType in vUserStateGraph.Series)
            {
                if (vUserType.Name.Equals("Run"))
                {
                    filterDimensionVUser.ClearValues();
                    //filterDimensionVUser.AddDiscreteValue("Run");
                    vUserGraph.ApplyFilterAndGroupBy();
                    double vUserMax = vUserType.GraphStatistics.Maximum;
                    if (!HasValue(vUserMax))
                    {
                        vUserMax = -1;
                    }
                    vuserDictionary.Add("MaxVuserRun", (int)Math.Round(vUserMax));
                    ConsoleWriter.WriteLine(String.Format("{0} maximum vUser ran per {1} seconds", vUserMax, vUserStateGraph.Granularity));

                    break;
                }
            }
            return vuserDictionary;
        }


        /// <summary>
        /// Calculating the number of transactions by status
        /// </summary>
        /// <returns>Transactions by status</returns>
        public static Dictionary<string, Dictionary<string, double>> CalcFailedTransPercent(LrAnalysis lrAnalysis)
        { 
          
            var transactionGraph = lrAnalysis.Session.OpenGraph("TransactionSummary");

            foreach (FilterItem fi in transactionGraph.Filter)
            {
                fi.ClearValues();
                fi.IsActive = false;
                transactionGraph.ApplyFilterAndGroupBy();
            }


            var transDictionary = new Dictionary<string, Dictionary<string, double> > () ;

            transactionGraph.Granularity = 4;
            
            var filterDimension = transactionGraph.Filter["Transaction End Status"];
            foreach (var series in transactionGraph.Series)
            {
                SeriesAttributeValue a;
                if (!series.Attributes.TryGetValue("Event Name", out a)) continue;
                SeriesAttributeValue transEndStatusAttr;

                if (!series.Attributes.TryGetValue("Transaction End Status", out transEndStatusAttr)) continue;

                Dictionary<string, double> value;
                if (!transDictionary.TryGetValue(a.Value.ToString(), out value))
                {
                    transDictionary.Add(a.Value.ToString(),
                        new Dictionary<string, double>() {{"Pass", 0}, {"Fail", 0}, {"Stop", 0}});
                }
                (transDictionary[a.Value.ToString()])[transEndStatusAttr.Value.ToString()] = series.Points[0].Value;
            }
        
            return transDictionary;
        }

        ///<summary>
        /// Get Connections count
        /// </summary>
        public static double GetConnectionsCount(LrAnalysis lrAnalysis)
        {
            double connectionsCount = 0;

            try
            {
                Graph g;
                // check if Connections graph has data
                if (lrAnalysis.Session.Runs[0].Graphs.TryGetValue("Connections", out g) != true)
                {
                    throw new Exception("Failed to retrieve values from Connections graph");
                }
                if (g.Series.Count == 0)
                {
                    throw new Exception("No data exists in Connections graph");
                }

                g.Granularity = 1;

                foreach (FilterItem fi in g.Filter)
                {
                    fi.ClearValues();
                    fi.IsActive = false;
                    g.ApplyFilterAndGroupBy();
                }

                g.ApplyFilterAndGroupBy();
                connectionsCount = g.Series["Connections"].GraphStatistics.Maximum;
                if (!HasValue(connectionsCount))
                {
                    connectionsCount = -1;
                }
            }
            catch (Exception ex)
            {
                Console.Write(Resources.Helper_GetConnectionsCount_ + ex.Message);
            }

            return connectionsCount;
        }


        /// <summary>
        /// Returns scenario duration
        /// </summary>
        /// <returns>Scenario duration</returns>
        public static String GetScenarioDuration(LrAnalysis lrAnalysis)
        {
            var testDuration = lrAnalysis.Session.Runs[0].EndTime - lrAnalysis.Session.Runs[0].StartTime;
            return testDuration.ToString();
        }


        public static DateTime FromUnixTime(long unixTime)
        {
            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Local);
            return epoch.AddSeconds(unixTime);
        }

        public static bool HasValue(double value)
        {
            return !Double.IsNaN(value) && !Double.IsInfinity(value);
        }

    }
}