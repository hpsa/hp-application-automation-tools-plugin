﻿using System;
using System.Collections.Generic;
using System.Linq;
using Analysis.Api;
using Analysis.ApiLib;
using HpToolsLauncher;
using LRAnalysisLauncher.Properties;

namespace LRAnalysisLauncher
{
    public abstract class Helper
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

            var filterDimension = vUserGraph.Filter["Vuser End Status"];
            var vUserStates = new List<String>()
            {
                {"Passed"},
                {"Stopped"},
                {"Failed"},
                {"Error"}
            };
            ConsoleWriter.WriteLine("Counting vUser Results for this scenarion");
            foreach (var vuserType in vUserStates)
            {
                filterDimension.ClearValues();
                filterDimension.AddDiscreteValue(vuserType);
                vUserGraph.ApplyFilterAndGroupBy();
                double sum = 0;
                foreach (var val in vUserGraph.Series)
                    sum += val.GraphStatistics.Maximum;
                vuserDictionary[vuserType] = (int) sum;
            }


            var g = lrAnalysis.Session.OpenGraph("VuserStateGraph");
            //g.Granularity = 4;
            var filterDimensionVUser = g.Filter["Vuser Status"];
            filterDimensionVUser.ClearValues();
            filterDimensionVUser.AddDiscreteValue("Run");
            g.ApplyFilterAndGroupBy();
            int maxVUserRun = (int) Math.Round(g.Series[0].GraphStatistics.Maximum);
            vuserDictionary.Add("MaxVuserRun", maxVUserRun);
            ConsoleWriter.WriteLine(String.Format("{0} maximum vUser ran per {1} ", maxVUserRun, g.Granularity));
        
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
            }
            catch (Exception ex)
            {
                Console.Write(Resources.Helper_GetConnectionsCount_ + ex.Message);
            }
            finally
            {
                
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
    }
}