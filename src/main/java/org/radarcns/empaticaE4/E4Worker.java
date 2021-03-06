package org.radarcns.empaticaE4;

import org.radarcns.config.PropertiesRadar;
import org.radarcns.empaticaE4.streams.E4Acceleration;
import org.radarcns.empaticaE4.streams.E4BatteryLevel;
import org.radarcns.empaticaE4.streams.E4BloodVolumePulse;
import org.radarcns.empaticaE4.streams.E4ElectroDermalActivity;
import org.radarcns.empaticaE4.streams.E4HeartRate;
import org.radarcns.empaticaE4.streams.E4InterBeatInterval;
import org.radarcns.empaticaE4.streams.E4Temperature;
import org.radarcns.empaticaE4.topic.E4Topics;
import org.radarcns.stream.aggregator.AggregatorWorker;
import org.radarcns.stream.aggregator.MasterAggregator;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Francesco Nobilia on 21/11/2016.
 */
public class E4Worker extends MasterAggregator{

    private final static Object syncObject = new Object();
    private static E4Worker instance = null;

    public static E4Worker getInstance() throws IOException{
        synchronized (syncObject) {
            if (instance == null) {
                instance = new E4Worker(PropertiesRadar.getInstance().isStandalone());
            }
        }

        return instance;
    }

    private E4Worker(boolean standalone) throws IOException{
        super(standalone,"Empatica E4");
    }

    @Override
    protected void announceTopics(Logger log){
        log.info("If AUTO.CREATE.TOPICS.ENABLE is FALSE you must create the following topics before starting: \n  - " +
                E4Topics.getInstance().getTopicNames().stream().map(Object::toString).collect(Collectors.joining("\n  - ")));
    }

    @Override
    protected void createWorker(List<AggregatorWorker> list, int low, int normal, int high) throws IOException{
        list.add(new E4Acceleration("E4Acceleration",high,this));
        list.add(new E4BatteryLevel("E4BatteryLevel",low,this));
        list.add(new E4BloodVolumePulse("E4BloodVolumePulse",high,this));
        list.add(new E4ElectroDermalActivity("E4ElectroDermalActivity",normal,this));
        list.add(new E4HeartRate("E4HeartRate",high,this));
        list.add(new E4InterBeatInterval("E4InterBeatInterval",high,this));
        list.add(new E4Temperature("E4Temperature",high,this));
    }
}
