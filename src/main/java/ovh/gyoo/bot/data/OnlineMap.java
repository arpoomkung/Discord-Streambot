package ovh.gyoo.bot.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnlineMap {
    private static OnlineMap ourInstance = new OnlineMap();
    private Map<String, List<String>> online;

    public static synchronized OnlineMap getInstance() {
        return ourInstance;
    }

    private OnlineMap() {
        online = new HashMap<>();
    }

    public synchronized List<String> getNameList(String serverId){
        return online.get(serverId);
    }

    public synchronized void addToList(String serverId, String name){
        online.get(serverId).add(name);
    }

    public synchronized void removeFromList(String serverId, String name){
        if(online.get(serverId).contains(name)) online.get(serverId).remove(name);
    }

    public synchronized void addServer(String serverId){
        online.put(serverId, new ArrayList<>());
    }

    public synchronized Map<String, List<String>> getMap(){
        return online;
    }
}