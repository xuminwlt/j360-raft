package me.j360.raft.counter.server;

import lombok.Data;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * @author: min_xu
 */
@Data
public class StartupConf {

    private String groupId;
    private String dataPath;
    private String conf;
    private String serverAddress;

    private long minSlot;
    private long maxSlot;
    private int totalSlots = 1;

    @SneakyThrows
    public boolean loadFromFile(String file) {
        FileInputStream fis = new FileInputStream(new File(file));
        Properties props = new Properties();
        props.load(fis);
        this.groupId = props.getProperty("groups");
        this.dataPath = props.getProperty("dataPath", "/tmp/atomic");
        this.conf = props.getProperty("conf");
        this.serverAddress = props.getProperty("serverAddress");
        this.minSlot = Long.valueOf(props.getProperty("minSlot", "0"));
        this.maxSlot = Long.valueOf(props.getProperty("maxSlot", String.valueOf(Long.MAX_VALUE)));
        this.totalSlots = Integer.valueOf(props.getProperty("totalSlots", "1"));
        return this.verify();
    }


    private boolean verify() {
        return true;
    }

}
