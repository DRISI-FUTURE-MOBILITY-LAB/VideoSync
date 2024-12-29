/*
 * File: SensitivityMetric.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class intended to be a template metric for
 *          calculating sensitivity metrics based on
 *          False Positive, False Negative and True
 *          Positive event tags and car counts
 */

package VideoSync.objects.event_logger.metrics;

import VideoSync.objects.event_logger.ChannelCountProxy;
import VideoSync.objects.event_logger.Event;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensitivityMetric extends Metric
{

    public SensitivityMetric()
    {
        super();

        Map<String, String> variables = new HashMap<>();

        variables.put("False Positive", "Sensor claims to have detected vehicle in detection region; however no vehicle is present.");
        variables.put("False Negative", "Sensor fails to detect vehicle present in detection region.");
        variables.put("True Positive", "Sensor properly detects vehicle present in detection region.");

        setVariables(variables);

        setDescription("Uses false positive, false negative and true positive event tags to generate sensitivity percentages.");
    }

    @Override
    public Object calculate(ChannelCountProxy channelCountProxy)
    {

        //Get car count from a channel count
        int car_count = channelCountProxy.getCarCount();

        List<Event> eventList = channelCountProxy.getEventsByTag("False Positive");

        //Get False Positive event count
        int false_positive = 0;
        if(eventList != null)
        {
            false_positive = getEventCount(eventList);
        }

        //Get False Negative event count
        int false_negative = 0;
        eventList = channelCountProxy.getEventsByTag("False Negative");
        if(eventList != null)
            false_negative = getEventCount(eventList);

        //True Positive count calculated by subtracting false negative events from car count
        int true_positive = car_count - false_negative;

        if(true_positive < 0)
            true_positive = 0;

        setVariableValue("False Positive", false_positive);
        setVariableValue("False Negative", false_negative);
        setVariableValue("True Positive", true_positive);

        double result;

        //Actual calculation of sensitivity metric
        try
        {
            result = ((double) true_positive / (true_positive + false_negative + false_positive)) * 100.0;
        }
        catch(ArithmeticException e)
        {
            result = 0.0;
        }

        if(Double.isNaN(result))
            result = 0.0;

        result = BigDecimal.valueOf(result).setScale(2, RoundingMode.HALF_UP).doubleValue();

        return result;
    }

    @Override
    public Object calculate(Map<String, Object> variables)
    {

        try
        {

            //Set the value for each variable
            for(String variable : getVariables().keySet())
            {
                setVariableValue(variable, variables.get(variable));
            }

            int false_positive = (Integer) getVariableValue("False Positive");

            int false_negative = (Integer) getVariableValue("False Negative");

            int true_positive = (Integer) getVariableValue("True Positive");

            if(true_positive < 0)
            {
                true_positive = 0;
            }

            double result;

            //Actual calculation of sensitivity metric
            try
            {
                result = ((double) true_positive / (true_positive + false_negative + false_positive)) * 100.0;
            }
            catch(ArithmeticException e)
            {
                result = 0.0;
            }

            return result;
        }
        catch(NullPointerException e)
        {
            return Double.NaN;
        }
    }
}
