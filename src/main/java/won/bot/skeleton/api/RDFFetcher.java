package won.bot.skeleton.api;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.impl.command.create.CreateAtomCommandEvent;
import won.protocol.util.DefaultAtomModelWrapper;

import java.net.URI;
import java.util.stream.Stream;

public class RDFFetcher {
    private String rdfURL;
    private EventListenerContext ctx;

    public RDFFetcher(EventListenerContext ctx, String rdfURL) {
        this.ctx = ctx;
        this.rdfURL = rdfURL;
        this.fetch();
    }

    public void fetch() {
        Model model = ModelFactory.createDefaultModel();
        model.read(this.rdfURL);
        int i = 0;
        StmtIterator it = model.listStatements(null, RDF.type, model.getResource("https://data.cityofnewyork.us/resource/kcdd-kkxy"));
        while(it.hasNext())
        {
            URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            URI atomURI = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);

            // Set atom data - here only shown for commonly used (hence 'default') properties
            DefaultAtomModelWrapper atomWrapper = new DefaultAtomModelWrapper(atomURI);

            Statement stmt = it.nextStatement();
            Resource spot = stmt.getSubject();
            Resource atomRes = atomWrapper.getAtomContentNode();
            StmtIterator it2 = spot.listProperties();
            while(it2.hasNext()) {
                Statement stmt2 = it2.nextStatement();
                atomRes.addProperty(stmt2.getPredicate(), stmt2.getObject());
            }


            //publish command
            CreateAtomCommandEvent createCommand = new CreateAtomCommandEvent(atomWrapper.getDataset());
            ctx.getEventBus().publish(createCommand);

            //System.out.println(stmt.getSubject());
            RDFDataMgr.write(System.out,atomWrapper.copyDataset(), Lang.TRIG);


        }
    }
}