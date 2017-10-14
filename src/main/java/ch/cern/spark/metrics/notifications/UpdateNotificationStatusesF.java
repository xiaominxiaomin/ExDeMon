package ch.cern.spark.metrics.notifications;

import java.io.IOException;
import java.util.Map;

import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function4;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.Time;
import org.apache.spark.streaming.api.java.JavaPairDStream;

import ch.cern.spark.Properties;
import ch.cern.spark.Properties.PropertiesCache;
import ch.cern.spark.metrics.monitor.Monitor;
import ch.cern.spark.metrics.notificator.Notificator;
import ch.cern.spark.metrics.notificator.NotificatorID;
import ch.cern.spark.metrics.results.AnalysisResult;
import ch.cern.spark.metrics.store.Store;

public class UpdateNotificationStatusesF
        implements Function4<Time, NotificatorID, Optional<AnalysisResult>, State<Store>, Optional<Notification>> {

    private static final long serialVersionUID = 1540971922358997509L;
    
    public static String DATA_EXPIRATION_PARAM = "data.expiration";
    public static java.time.Duration DATA_EXPIRATION_DEFAULT = java.time.Duration.ofHours(3);

    private Map<String, Monitor> monitors = null;

    private Properties.PropertiesCache propertiesExp;

    public UpdateNotificationStatusesF(Properties.PropertiesCache propertiesExp) {
        this.propertiesExp = propertiesExp;
    }

    @Override
    public Optional<Notification> call(Time time, NotificatorID ids, Optional<AnalysisResult> resultOpt,
            State<Store> notificatorState) throws Exception {

        if (notificatorState.isTimingOut() || !resultOpt.isPresent())
            return Optional.absent();
        
        Monitor monitor = getMonitor(ids.getMonitorID());
        Notificator notificator = monitor.getNotificator(ids.getNotificatorID(), toOptional(notificatorState));        
        
        java.util.Optional<Notification> notification = notificator.apply(resultOpt.get());
        
        notificator.getStore().ifPresent(notificatorState::update);
        
        notification.ifPresent(n -> {
            n.setMonitorID(ids.getMonitorID());
            n.setNotificatorID(ids.getNotificatorID());
            n.setMetricIDs(ids.getMetricIDs());
            n.setTimestamp(resultOpt.get().getAnalyzedMetric().getInstant());
        });

        return notification.isPresent() ? Optional.of(notification.get()) : Optional.empty();
    }

    private java.util.Optional<Store> toOptional(State<Store> notificatorState) {
        return notificatorState.exists() ? 
        				java.util.Optional.of(notificatorState.get()) 
        				: java.util.Optional.empty();
    }

    private Monitor getMonitor(String monitorID) throws IOException {
        if (monitors == null)
            monitors = Monitor.getAll(propertiesExp);

        return monitors.get(monitorID);
    }

    public static NotificationStatusesS apply(JavaPairDStream<NotificatorID, AnalysisResult> resultsWithId,
            PropertiesCache propertiesExp, NotificationStoresRDD initialNotificationStores) throws IOException {

        java.time.Duration dataExpirationPeriod = propertiesExp.get().getPeriod(DATA_EXPIRATION_PARAM, DATA_EXPIRATION_DEFAULT);

        StateSpec<NotificatorID, AnalysisResult, Store, Notification> statusSpec = StateSpec
                .function(new UpdateNotificationStatusesF(propertiesExp)).initialState(initialNotificationStores.rdd())
                .timeout(new Duration(dataExpirationPeriod.toMillis()));

        NotificationStatusesS statuses = new NotificationStatusesS(resultsWithId.mapWithState(statusSpec));

        return statuses;
    }

}
