/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wordpress.salaboy.services;

import com.wordpress.salaboy.model.Call;
import com.wordpress.salaboy.model.events.PatientAtHospitalEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactoryService;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderErrors;
import org.drools.builder.KnowledgeBuilderFactoryService;
import org.drools.builder.ResourceType;
import org.drools.grid.ConnectionFactoryService;
import org.drools.grid.GridConnection;
import org.drools.grid.GridNode;
import org.drools.grid.GridServiceDescription;
import org.drools.grid.conf.GridPeerServiceConfiguration;
import org.drools.grid.conf.impl.GridPeerConfiguration;
import org.drools.grid.impl.GridImpl;
import org.drools.grid.service.directory.Address;
import org.drools.grid.service.directory.WhitePages;
import org.drools.grid.service.directory.impl.CoreServicesLookupConfiguration;
import org.drools.grid.service.directory.impl.GridServiceDescriptionImpl;
import org.drools.grid.service.directory.impl.WhitePagesRemoteConfiguration;
import org.drools.io.impl.ByteArrayResource;
import org.drools.io.impl.ClassPathResource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.jbpm.process.workitem.wsht.CommandBasedWSHumanTaskHandler;
import com.wordpress.salaboy.model.ProcedureRequest;
import com.wordpress.salaboy.model.events.PatientPickUpEvent;

/**
 *
 * @author salaboy
 */
public class ProceduresMGMTService {

    private static ProceduresMGMTService instance;
    private Map<Long, StatefulKnowledgeSession> procedureSessions;

    private ProceduresMGMTService() {
        procedureSessions = new HashMap<Long, StatefulKnowledgeSession>();

    }

    public static ProceduresMGMTService getInstance() {
        if (instance == null) {
            instance = new ProceduresMGMTService();
        }
        return instance;
    }

    public void newDefaultHeartAttackProcedure(final Long callId, Map<String, Object> parameters) {
        try {
            procedureSessions.put(callId, createDefaultHeartAttackProcedureSession(callId));
            setWorkItemHandlers(procedureSessions.get(callId));
            
            new Thread(new Runnable() {

                public void run() {
                    procedureSessions.get(callId).fireUntilHalt();
                }
            }).start();
        } catch (IOException ex) {
            Logger.getLogger(ProceduresMGMTService.class.getName()).log(Level.SEVERE, null, ex);
        }
       procedureSessions.get(callId).getWorkingMemoryEntryPoint("procedure request").insert(new ProcedureRequest("DefaultHeartAttackProcedure", parameters));
       
    }
    
    public void patientPickUpNotification(PatientPickUpEvent event){
       procedureSessions.get(event.getCallId()).signalEvent("com.wordpress.salaboy.model.events.PatientPickUpEvent", event);         
    }

    private StatefulKnowledgeSession createDefaultHeartAttackProcedureSession(Long callId) throws IOException {
        Map<String, GridServiceDescription> coreServicesMap = new HashMap<String, GridServiceDescription>();
        GridServiceDescriptionImpl gsd = new GridServiceDescriptionImpl(WhitePages.class.getName());
        Address addr = gsd.addAddress("socket");
        addr.setObject(new InetSocketAddress[]{new InetSocketAddress("localhost", 8000)});
        coreServicesMap.put(WhitePages.class.getCanonicalName(), gsd);

        GridImpl grid = new GridImpl(new ConcurrentHashMap<String, Object>());

        GridPeerConfiguration conf = new GridPeerConfiguration();
        GridPeerServiceConfiguration coreSeviceConf = new CoreServicesLookupConfiguration(coreServicesMap);
        conf.addConfiguration(coreSeviceConf);

        GridPeerServiceConfiguration wprConf = new WhitePagesRemoteConfiguration();
        conf.addConfiguration(wprConf);

        conf.configure(grid);

        GridServiceDescription<GridNode> n1Gsd = grid.get(WhitePages.class).lookup("n1");
        GridConnection<GridNode> conn = grid.get(ConnectionFactoryService.class).createConnection(n1Gsd);
        GridNode remoteN1 = conn.connect();


        KnowledgeBuilder kbuilder = remoteN1.get(KnowledgeBuilderFactoryService.class).newKnowledgeBuilder();

        kbuilder.add(new ByteArrayResource(IOUtils.toByteArray(new ClassPathResource("processes/DefaultHeartAttackProcedure.bpmn").getInputStream())), ResourceType.BPMN2);

        kbuilder.add(new ByteArrayResource(IOUtils.toByteArray(new ClassPathResource("rules/proceduresRequestHandler.drl").getInputStream())), ResourceType.DRL);
        
        //   kbuilder.add(new ByteArrayResource(IOUtils.toByteArray(new ClassPathResource("rules/startProcedures.drl").getInputStream())), ResourceType.DRL);

        KnowledgeBuilderErrors errors = kbuilder.getErrors();
        if (errors != null && errors.size() > 0) {
            for (KnowledgeBuilderError error : errors) {
                System.out.println(">>>>>>> Error: " + error.getMessage());

            }
            throw new IllegalStateException("Failed to parse knowledge!");
        }

        KnowledgeBase kbase = remoteN1.get(KnowledgeBaseFactoryService.class).newKnowledgeBase();

        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());

        StatefulKnowledgeSession session = kbase.newStatefulKnowledgeSession();

        remoteN1.set("DefaultHeartAttackProcedureSession" + callId, session);

        return session;

    }

    private void setWorkItemHandlers(StatefulKnowledgeSession session) {
        session.getWorkItemManager().registerWorkItemHandler("Human Task", new CommandBasedWSHumanTaskHandler(session));
    }

    public void patientAtHospitalNotification(PatientAtHospitalEvent event) {
        procedureSessions.get(event.getCallId()).signalEvent("com.wordpress.salaboy.model.events.PatientAtHospitalEvent", event);         
    }
}