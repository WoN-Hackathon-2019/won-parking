package won.bot.skeleton.context;

import won.bot.framework.bot.context.BotContext;
import won.bot.framework.extensions.serviceatom.ServiceAtomEnabledBotContextWrapper;

import java.net.URI;
import java.util.*;

public class SkeletonBotContextWrapper extends ServiceAtomEnabledBotContextWrapper {
    private final String connectedSocketsMap;
    private final String referenceURIMap;

    public SkeletonBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
        this.connectedSocketsMap = botName + ":connectedSocketsMap";
        this.referenceURIMap = botName + ":referenceURIMap";
    }

    public void addParkingPosition(String ref, URI wonURI) {
        getBotContext().saveToObjectMap(referenceURIMap, ref, wonURI.toString());
    }

    public void removeParkingPositionByRef(String ref) {
        getBotContext().removeFromObjectMap(referenceURIMap, ref);
    }

    public String getParkingPositionWonURI(String ref) {
        return (String) getBotContext().loadFromObjectMap(referenceURIMap, ref);
    }

    public boolean parkingPositionExists(String ref) {
        return getParkingPositionWonURI(ref) != null;
    }

    public String getFirstParkingPosition() {
        Object[] arr = getBotContext().loadObjectMap(referenceURIMap).keySet().toArray();
        if (arr.length == 0) {
            return null;
        }
        return this.getParkingPositionWonURI((String) arr[0]);
    }

    public Map<URI, Set<URI>> getConnectedSockets() {
        Map<String, List<Object>> connectedSockets = getBotContext().loadListMap(connectedSocketsMap);
        Map<URI, Set<URI>> connectedSocketsMapSet = new HashMap<>(connectedSockets.size());

        for(Map.Entry<String, List<Object>> entry : connectedSockets.entrySet()) {
            URI senderSocket = URI.create(entry.getKey());
            Set<URI> targetSocketsSet = new HashSet<>(entry.getValue().size());
            for(Object o : entry.getValue()) {
                targetSocketsSet.add((URI) o);
            }
            connectedSocketsMapSet.put(senderSocket, targetSocketsSet);
        }

        return connectedSocketsMapSet;
    }

    public void addConnectedSocket(URI senderSocket, URI targetSocket) {
        getBotContext().addToListMap(connectedSocketsMap, senderSocket.toString(), targetSocket);
    }

    public void removeConnectedSocket(URI senderSocket, URI targetSocket) {
        getBotContext().removeFromListMap(connectedSocketsMap, senderSocket.toString(), targetSocket);
    }
}
