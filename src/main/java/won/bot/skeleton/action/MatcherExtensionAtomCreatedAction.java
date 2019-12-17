package won.bot.skeleton.action;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.extensions.matcher.MatcherExtensionAtomCreatedEvent;
import won.bot.skeleton.api.RDFFetcher;
import won.bot.skeleton.context.SkeletonBotContextWrapper;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import org.apache.jena.query.Dataset;
import won.protocol.model.Atom;
import won.protocol.model.Coordinate;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.WONMATCH;
import won.protocol.vocabulary.WXCHAT;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MatcherExtensionAtomCreatedAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private SkeletonBotContextWrapper botContext;
    private final RDFFetcher rdfFetcher;

    public MatcherExtensionAtomCreatedAction(EventListenerContext eventListenerContext, RDFFetcher rdfFetcher) {
        super(eventListenerContext);
        this.rdfFetcher = rdfFetcher;
    }

    @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if(!(event instanceof MatcherExtensionAtomCreatedEvent) || !(getEventListenerContext().getBotContextWrapper() instanceof SkeletonBotContextWrapper)) {
            logger.error("MatcherExtensionAtomCreatedAction can only handle MatcherExtensionAtomCreatedEvent and only works with SkeletonBotContextWrapper");
            return;
        }

        if (ctx.getBotContextWrapper() instanceof SkeletonBotContextWrapper) {
            botContext = (SkeletonBotContextWrapper) ctx.getBotContextWrapper();
        }

        SkeletonBotContextWrapper botContextWrapper = (SkeletonBotContextWrapper) ctx.getBotContextWrapper();
        MatcherExtensionAtomCreatedEvent atomCreatedEvent = (MatcherExtensionAtomCreatedEvent) event;

        // Map<URI, Set<URI>> connectedSocketsMapSet = botContextWrapper.getConnectedSockets();

        Dataset dataset = ((MatcherExtensionAtomCreatedEvent) event).getAtomData();
        if (dataset == null) {
            return;
        }

        DefaultAtomModelWrapper atom = new DefaultAtomModelWrapper(dataset);
        atom.getSeeksNodes().forEach(node -> {
            Model model = node.getModel();
            if (model == null) {
                return;
            }
            Property property = model.getProperty("https://w3id.org/won/matching#searchString");
            if (property == null) {
                return;
            }
            Statement stmt = node.getProperty(property);
            if (stmt == null) {
                return;
            }
            RDFNode rdfNode = stmt.getObject();
            if (rdfNode == null) {
                return;
            }
            String searchString = rdfNode.toString();
            if (searchString == null || !searchString.equals("parking location")) {
                return;
            }

            Coordinate locationCoordinate = atom.getLocationCoordinate(node);

            Calendar doNotMatchAfter = atom.getDoNotMatchAfter();
            Calendar validFrom = this.parseCalendar(node.getProperty(SCHEMA.VALID_FROM));
            Calendar validTrough = this.parseCalendar(node.getProperty(SCHEMA.VALID_THROUGH));

            URI atomURI = URI.create(atom.getAtomUri());
            URI socketURI = URI.create(WXCHAT.ChatSocket.getURI()); //or any other socket type you want
            LinkedDataSource linkedDataSource = getEventListenerContext().getLinkedDataSource();
            Collection<URI> sockets = WonLinkedDataUtils.getSocketsOfType(atomURI, socketURI, linkedDataSource);
            //sockets should have 0 or 1 items
            if (sockets.isEmpty()){
                return;
                //did not find a socket of that type
            }
            URI recipientSocketURI = sockets.iterator().next();
            // String parkingPosisitonWonURI = botContextWrapper.getFirstParkingPosition();
            String parkingPosisitonWonURI = RDFFetcher.getParkingLot(rdfFetcher.fetch());
            // String parkingPosisitonWonURI = RDFFetcher.getParkingLotWithParams(rdfFetcher.fetch(), locationCoordinate.getLatitude(), locationCoordinate.getLongitude());


            if (parkingPosisitonWonURI == null) {
                return;
            }
            URI ppWonUri = URI.create(parkingPosisitonWonURI);
            URI ppSocketURI = URI.create(WXCHAT.ChatSocket.getURI()); //or any other socket type you want
            LinkedDataSource ppLinkedDataSource = getEventListenerContext().getLinkedDataSource();
            Collection<URI> ppSockets = WonLinkedDataUtils.getSocketsOfType(ppWonUri, ppSocketURI, ppLinkedDataSource);
            //sockets should have 0 or 1 items
            if (ppSockets.isEmpty()){
                return;
                //did not find a socket of that type
            }
            URI senderSocketURI = ppSockets.iterator().next();


            String message = "Hello, let's connect!"; //optional welcome message
            ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(
                    senderSocketURI,recipientSocketURI, message);
            ctx.getEventBus().publish(connectCommandEvent);


            logger.info("PARKING REQUEST => " + validFrom.getTime().toString() + " - " + validTrough.getTime().toString() + " @(" + locationCoordinate.getLatitude()+", " + locationCoordinate.getLongitude() + ")");





        });
        // Object[] seekNodes = atom.getSeeksNodes().toArray();
        // Coordinate coordinate = atom.getLocationCoordinate((Resource)seekNodes[0]);

        /* for(Map.Entry<URI, Set<URI>> entry : connectedSocketsMapSet.entrySet()) {
            URI senderSocket = entry.getKey();
            Set<URI> targetSocketsSet = entry.getValue();
            for(URI targetSocket : targetSocketsSet) {
                logger.info("TODO: Send MSG("+senderSocket+"->"+targetSocket+") that we registered that an Atom was created, atomUri is: " +atomCreatedEvent.getAtomURI());
                WonMessage wonMessage = WonMessageBuilder
                                            .connectionMessage()
                                            .sockets()
                                            .sender(senderSocket)
                                            .recipient(targetSocket)
                                            .content()
                                            .text("We registered that an Atom was created, atomUri is: " + atomCreatedEvent.getAtomURI())
                                            .build();
                ctx.getWonMessageSender().prepareAndSendMessage(wonMessage);
            }
        } */
    }

    private Calendar parseCalendar(Statement prop) {
        if (prop == null) {
            return null;
        } else {
            RDFNode literal = prop.getObject();
            if (!literal.isLiteral()) {
                return null;
            } else {
                Object data = literal.asLiteral().getValue();
                return data instanceof XSDDateTime ? ((XSDDateTime)data).asCalendar() : null;
            }
        }
    }
}
