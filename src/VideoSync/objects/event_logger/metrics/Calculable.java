/*
 * File: Calculable.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Interface class used by metric class
 */


package VideoSync.objects.event_logger.metrics;

import VideoSync.objects.event_logger.ChannelCountProxy;

import java.util.Map;

public interface Calculable
{

    /**
     * Method call used for calculating a metric for one channel count
     *
     * @param object channel count object
     * @return intended to return an Integer or Double metric result value
     */
    Object calculate(ChannelCountProxy object);

    /**
     * Method call used for calculating a metric based on a map event tag/variable
     * counts - used when generating cumulative metrics for a collection of channel counts
     *
     * @param objectMap map of cumulative event tag/variable values
     * @return inteded to return an Integer or Double metric result value
     */
    Object calculate(Map<String, Object> objectMap);
}
