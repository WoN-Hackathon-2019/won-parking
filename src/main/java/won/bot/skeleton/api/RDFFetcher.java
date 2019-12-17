package won.bot.skeleton.api;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.query.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.shared.NotFoundException;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class RDFFetcher {
    private String rdfURL;
    private EventListenerContext ctx;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private SkeletonBotContextWrapper botContextWrapper;
    private static final String PARKING_LOT = "result";
    private static final String getClosestParkingLotQuery;
    private static final String getClosestParkingLotQuery1;
    private Model model;

    static {
        getClosestParkingLotQuery = loadStringFromFile("/correct/getClosestParkingLot.rq");
        getClosestParkingLotQuery1 = loadStringFromFile("/correct/getClosestParkingLot1.rq");
    }


    public RDFFetcher(EventListenerContext ctx, String rdfURL) {
        this.ctx = ctx;
        this.rdfURL = rdfURL;
        if (ctx.getBotContextWrapper() instanceof SkeletonBotContextWrapper) {
            botContextWrapper = ((SkeletonBotContextWrapper) ctx.getBotContextWrapper());
        }
    }

    public Model fetch() {
        if (model != null) {
            return model;
        }
        model = ModelFactory.createDefaultModel();
        model.read(this.rdfURL);
        return model;
    }

    public static String getParkingLot(Model payload) {
        if(payload != null && !payload.isEmpty()) {
          QuerySolution solution = executeQuery(getClosestParkingLotQuery1, payload);

          if (solution != null) {
              return solution.getResource(PARKING_LOT).toString();
          }
        }
        return null;
    }

    public static String getParkingLotWithParams(Model payload, float myLong, float myLat) {
      if (payload != null && !payload.isEmpty()) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(getClosestParkingLotQuery);
        // pss.setLiteral("myLat", myLat);
        // pss.setLiteral("myLong", myLong);
        QuerySolution solution = executeQuery(
                pss.toString().replace("?myLat", "" + myLat).replace("?myLong", ""+myLong), payload);

        if (solution != null) {
            return solution.getResource(PARKING_LOT).toString();
        }
      }
      return null;
    }

    private static QuerySolution executeQuery(String queryString, Model payload) {
        Query query = QueryFactory.create(queryString);
        try(QueryExecution qexec = QueryExecutionFactory.create(query, payload)){
            ResultSet resultSet = qexec.execSelect();
            if (resultSet.hasNext()){
                QuerySolution solution = resultSet.nextSolution();
                return solution;
            }
        }
        return null;
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
    }


    public static String loadStringFromFile(String filePath) {
        InputStream is  = RDFFetcher.class.getResourceAsStream(filePath);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(is, writer, Charsets.UTF_8);
        } catch (IOException e) {
            throw new NotFoundException("failed to load resource: " + filePath);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
        return writer.toString();
    }
}
