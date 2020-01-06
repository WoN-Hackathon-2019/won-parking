package won.bot.skeleton.action;


import at.apf.easycli.CliEngine;
import at.apf.easycli.annotation.Command;
import at.apf.easycli.annotation.Meta;
import at.apf.easycli.exception.CommandNotFoundException;
import at.apf.easycli.impl.EasyEngine;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.WonRdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Date;

/**
 * Listener that responds to open and message events with automatic messages.
 * Can be configured to apply a timeout (non-blocking) before sending messages.
 */
public class RespondToMessageAction extends BaseEventBotAction {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private long millisTimeoutBeforeReply = 0;

    public RespondToMessageAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    public RespondToMessageAction(final EventListenerContext eventListenerContext,
                                  final long millisTimeoutBeforeReply) {
        super(eventListenerContext);
        this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
    }


    private String createMessage(String inMessage, DefaultAtomModelWrapper model) {
        boolean jsonFlag = false;
        CliEngine engine = new EasyEngine();
        engine.register(new Object() {
            @Command("help")
            String helpMessage(@Meta DefaultAtomModelWrapper model) {
                return "Sorry I have no idea how to help you :(";
            }
            @Command("info")
            String infoMessage(@Meta DefaultAtomModelWrapper model) {
                Property property = model.getAtomModel().getProperty("https://data.cityofnewyork.us/resource/kcdd-kkxy/address_street_name");
                if (property == null) {
                    return "Sorry, i have no info for this location :/";
                }
                // TODO: make output more readable
                String result = "street name: " + model.getContentPropertyStringValue(property);
                logger.info(result);
                return result;
            }
            @Command("confirm")
            String confirmMessage(@Meta DefaultAtomModelWrapper model) {
                // TODO: handle a parking position reservation state
                return "parking position has been reserved";
            }
            @Command("cancel")
            String cancelMessage(@Meta DefaultAtomModelWrapper model) {
                // TODO: handle a parking position reservation state
                return "Reservation canceled";
            }
        });
        if (inMessage != null && model != null) {
            try {
                return (String) engine.parse(inMessage, model);
            } catch (CommandNotFoundException e) {
                return e.getMessage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "Command not registered";

    }

    @Override
    protected void doRun(final Event event, EventListener executingListener) throws Exception {
        if (event instanceof ConnectionSpecificEvent) {
            handleMessageEvent((ConnectionSpecificEvent) event);
        }
    }

    private String extractTextMessageFromWonMessage(WonMessage wonMessage) {
        if (wonMessage == null) return null;
        return WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
    }


    private void handleMessageEvent(final ConnectionSpecificEvent messageEvent) {
        getEventListenerContext().getTaskScheduler().schedule(() -> {
            URI atomURI = ((MessageFromOtherAtomEvent) messageEvent).getAtomURI();
            Dataset atomData = this.getEventListenerContext().getLinkedDataSource().getDataForResource(atomURI);
            DefaultAtomModelWrapper model = new DefaultAtomModelWrapper(atomData);
            String message = createMessage(extractTextMessageFromWonMessage(((MessageEvent) messageEvent).getWonMessage()), model);
            URI connectionUri = messageEvent.getConnectionURI();
            URI senderSocket = messageEvent.getSocketURI();
            URI targetSocket = messageEvent.getTargetSocketURI();
            try {
                EventListenerContext ctx = getEventListenerContext();

                WonMessage wonMessage =
                        WonMessageBuilder.connectionMessage().sockets().sender(senderSocket).recipient(targetSocket).content().text(message).build();
                ctx.getWonMessageSender().prepareAndSendMessage(wonMessage);

            } catch (Exception e) {
                logger.warn("could not send message via connection {}", connectionUri, e);
            }
        }, new Date(System.currentTimeMillis() + this.millisTimeoutBeforeReply));
    }


}