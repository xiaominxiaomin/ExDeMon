package ch.cern.exdemon.monitor.trigger.action;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.junit.Test;

import ch.cern.exdemon.metrics.Metric;
import ch.cern.exdemon.metrics.value.StringValue;
import ch.cern.exdemon.metrics.value.Value;
import ch.cern.exdemon.monitor.analysis.results.AnalysisResult;
import ch.cern.exdemon.monitor.analysis.results.AnalysisResult.Status;
import ch.cern.exdemon.monitor.trigger.action.Action;
import ch.cern.exdemon.monitor.trigger.action.Template;
import ch.cern.properties.ConfigurationException;

public class TemplateTest {

    @Test
    public void templateInTag() throws ConfigurationException, AddressException, MessagingException, IOException{        
        Action action = ActionTest.DUMMY;
        Map<String, String> tags = new HashMap<>();
        tags.put("email.to", "daniel.lanza@cern.ch");
        tags.put("email.text", "Hello <tags:email.to>!");
        tags.put("cluster", "cluster1");
        action.setTags(tags);
        action.setCreation_timestamp(Instant.now());
        action.setMonitor_id("MONITOR_ID");
        action.setTrigger_id("NOTIFICATOR_ID");
        action.setReason("In ERROR for 3 hours");
        Map<String, String> metric_attributes = new HashMap<>();
        metric_attributes.put("a", "1");
        metric_attributes.put("b", "2");
        action.setMetric_attributes(metric_attributes);
        
        assertEquals("Hello daniel.lanza@cern.ch!", Template.apply("<tags:email.text>", action));
    }
    
    @Test
    public void aggMetrics() throws ConfigurationException, AddressException, MessagingException, IOException{
        Action action = ActionTest.DUMMY;

        AnalysisResult triggeringResult = new AnalysisResult();
        Value value = new StringValue("VALUE");
        List<Metric> lastSourceMetrics = new LinkedList<>();
        
        Map<String, String> ids1 = new HashMap<>();
        ids1.put("a1", "11");
        ids1.put("a2", "12");
		Metric m1 = new Metric(Instant.EPOCH, new StringValue("v1"), ids1 );
		lastSourceMetrics.add(m1);
		Map<String, String> ids2 = new HashMap<>();
		ids2.put("a1", "21");
		ids2.put("a2", "22");
		Metric m2 = new Metric(Instant.EPOCH, new StringValue("v2"), ids2 );
		lastSourceMetrics.add(m2);
		value.setLastSourceMetrics(lastSourceMetrics );
		
		Map<String, String> ids = new HashMap<>();
		Metric metric = new Metric(Instant.EPOCH, value, ids);
		triggeringResult.setAnalyzedMetric(metric);
		triggeringResult.setStatus(Status.OK, "");
		action.setTriggeringResult(triggeringResult);
        
        assertEquals("Some text: "
        				+ "1970-01-01 01:00:00:A1(=11), A2(=12):value=\"v1\":"
        				+ "\n\ta1 = 11"
        				+ "\n\ta2 = 12"
        				+ "\n 1970-01-01 01:00:00:A1(=21), A2(=22):value=\"v2\":"
        				+ "\n\ta1 = 21"
                        + "\n\ta2 = 22"
        				+ "\nOther text.", 
        		Template.apply("Some text:<agg_metrics> <datetime>:A1(=<attribute_value:a1>), A2(=<attribute_value:a2>):value=<value>:<attributes:a.+>\n</agg_metrics>Other text.", action));
    }
    
    @Test
    public void aggMetricsEmpty() throws ConfigurationException, AddressException, MessagingException, IOException{
        Action action = ActionTest.DUMMY;

        AnalysisResult triggeringResult = new AnalysisResult();
        Value value = new StringValue("VALUE");		
		Map<String, String> ids = new HashMap<>();
		Metric metric = new Metric(Instant.EPOCH, value, ids);
		triggeringResult.setAnalyzedMetric(metric);
		triggeringResult.setStatus(Status.OK, "");
		action.setTriggeringResult(triggeringResult);
        
        assertEquals("Some text: "
        				+ "No aggregated metrics."
        				+ " Other text.", 
        		Template.apply("Some text: <agg_metrics>A</agg_metrics> Other text.", action));
    }

}
