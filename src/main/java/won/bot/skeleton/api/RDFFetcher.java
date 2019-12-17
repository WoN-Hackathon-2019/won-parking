package won.bot.skeleton.api;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.skeleton.context.SkeletonBotContextWrapper;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.vocabulary.WXCHAT;

import java.net.URI;

public class RDFFetcher {
    private String rdfURL;
    private EventListenerContext ctx;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private SkeletonBotContextWrapper botContextWrapper;

    public RDFFetcher(EventListenerContext ctx, String rdfURL) {
        this.ctx = ctx;
        this.rdfURL = rdfURL;
        if (ctx.getBotContextWrapper() instanceof SkeletonBotContextWrapper) {
            botContextWrapper = ((SkeletonBotContextWrapper) ctx.getBotContextWrapper());
        }
    }

    public Model fetch() {
        Model model = ModelFactory.createDefaultModel();
        model.read(this.rdfURL);
        return model;
    }

    public void importRDFtoAtom() {
        // String myAtomList = "myAtoms";

        Model model = fetch();
        int i = 0;
        int limit = 10;
        StmtIterator it = model.listStatements(null, RDF.type, model.getResource("https://data.cityofnewyork.us/resource/kcdd-kkxy"));
        while(it.hasNext())
        {
            if (i > limit) {
                break;
            }
            URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            URI atomURI = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);

            // Set atom data - here only shown for commonly used (hence 'default') properties
            DefaultAtomModelWrapper atomWrapper = new DefaultAtomModelWrapper(atomURI);
            atomWrapper.addSocket("#chatSocket", WXCHAT.ChatSocketString);
            Statement stmt = it.nextStatement();
            Resource spot = stmt.getSubject();
            if (botContextWrapper.parkingPositionExists(spot.toString()) == true) {
                continue;
            }
            i++;
            Resource atomRes = atomWrapper.getAtomContentNode();
            StmtIterator it2 = spot.listProperties();
            while(it2.hasNext()) {
                Statement stmt2 = it2.nextStatement();
                atomRes.addProperty(stmt2.getPredicate(), stmt2.getObject());
            }

            //publish command
            WonMessage msg = WonMessageBuilder
                    .createAtom()
                        .atom(atomURI)
                        .direction()
                            .fromOwner()
                        .content()
                            .dataset(atomWrapper.copyDataset())
                    .build();
            msg = ctx.getWonMessageSender().prepareMessage(msg);

            // EventBotActionUtils.rememberInList(ctx, atomURI, myAtomList);
            botContextWrapper.addParkingPosition(spot.toString(), atomURI);

            EventListener successCallback = (event12) -> {
                logger.warn("atom creation successful, new atom URI is {}", atomURI);
                // react to success
            };
            EventListener failureCallback = (event1) -> {
                //react to failure
                botContextWrapper.removeParkingPositionByRef(String.valueOf(stmt.getSubject()));
                logger.warn("atom creation failed for atom URI is {}", atomURI);
            };
            EventBotActionUtils.makeAndSubscribeResponseListener(msg, successCallback, failureCallback, ctx);
            logger.debug("registered listeners for response to message URI {}", msg.getMessageURI());

            ctx.getWonMessageSender().sendMessage(msg);
            //System.out.println(stmt.getSubject());
            RDFDataMgr.write(System.out,atomWrapper.copyDataset(), Lang.TRIG);


        }
        int test = 1;
    }
}