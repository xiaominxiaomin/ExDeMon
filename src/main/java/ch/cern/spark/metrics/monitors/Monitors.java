package ch.cern.spark.metrics.monitors;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import ch.cern.spark.Cache;
import ch.cern.spark.Pair;
import ch.cern.spark.Properties;
import ch.cern.spark.Properties.PropertiesCache;
import ch.cern.spark.StatusStream;
import ch.cern.spark.Stream;
import ch.cern.spark.metrics.ComputeIDsForMetricsF;
import ch.cern.spark.metrics.ComputeMissingMetricResultsF;
import ch.cern.spark.metrics.Metric;
import ch.cern.spark.metrics.MonitorIDMetricIDs;
import ch.cern.spark.metrics.notifications.Notification;
import ch.cern.spark.metrics.notifications.UpdateNotificationStatusesF;
import ch.cern.spark.metrics.results.AnalysisResult;
import ch.cern.spark.metrics.results.ComputeIDsForAnalysisF;
import ch.cern.spark.metrics.store.MetricStore;
import ch.cern.spark.metrics.store.UpdateMetricStatusesF;

public class Monitors extends Cache<Map<String, Monitor>> implements Serializable{
	private static final long serialVersionUID = 2628296754660438034L;

	private transient final static Logger LOG = Logger.getLogger(Monitors.class.getName());
	
	private PropertiesCache propertiesCache;
	
	public Monitors(PropertiesCache propertiesCache) {
		this.propertiesCache = propertiesCache;
	}

	@Override
	protected Map<String, Monitor> load() throws IOException {
        Properties properties = propertiesCache.get().getSubset("monitor");
        
        Set<String> monitorNames = properties.getUniqueKeyFields();
        
        Map<String, Monitor> monitors = monitorNames.stream()
        		.map(id -> new Pair<String, Properties>(id, properties.getSubset(id)))
        		.map(info -> new Monitor(info.first).config(info.second))
        		.collect(Collectors.toMap(Monitor::getId, m -> m));
        
        LOG.info("Loaded Monitors: " + monitors);
        
        return monitors;
	}
	
	public Collection<Monitor> values() throws IOException {
		return get().values();
	}

	public Monitor get(String monitorID) throws IOException {
		return get().get(monitorID);
	}
	
	public Stream<AnalysisResult> analyze(Stream<Metric> metrics) throws Exception {
		StatusStream<MonitorIDMetricIDs, Metric, MetricStore, AnalysisResult> statuses = metrics.mapWithState("metricStores", new ComputeIDsForMetricsF(this), new UpdateMetricStatusesF(this));
        
        Stream<AnalysisResult> missingMetricsResults = statuses.getStatuses().transform((rdd, time) -> rdd.flatMap(new ComputeMissingMetricResultsF(this, time)));
        
        return statuses.union(missingMetricsResults);
	}

	public Stream<Notification> notify(Stream<AnalysisResult> results) throws IOException, ClassNotFoundException {
        return results.mapWithState("notificators", new ComputeIDsForAnalysisF(this), new UpdateNotificationStatusesF(this));
	}
	
}
