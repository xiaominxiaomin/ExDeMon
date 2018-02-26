package ch.cern.spark.metrics.trigger.action.actuator.types;

import org.apache.spark.streaming.api.java.JavaDStream;

import ch.cern.components.RegisterComponent;
import ch.cern.properties.ConfigurationException;
import ch.cern.properties.Properties;
import ch.cern.spark.http.HTTPSink;
import ch.cern.spark.metrics.trigger.action.Action;
import ch.cern.spark.metrics.trigger.action.actuator.Actuator;

@RegisterComponent("http")
public class HTTPActuator extends Actuator {

	private static final long serialVersionUID = 6368509840922047167L;

	private HTTPSink sink = new HTTPSink();

	@Override
	public void tryConfig(Properties properties) throws ConfigurationException {
		properties.setPropertyIfAbsent(HTTPSink.RETRIES_PARAM, "5");
		sink.config(properties);
	}

	@Override
	protected void run(JavaDStream<Action> action) {
		sink.sink(action);	
	}

}
