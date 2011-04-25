/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.wordpress.salaboy.procedures;

import java.util.Date;
import com.wordpress.salaboy.messaging.MessageFactory;
import com.wordpress.salaboy.messaging.MessageServerSingleton;
import org.drools.grid.SocketService;
import java.util.Map;
import com.wordpress.salaboy.services.HumanTaskServerService;
import org.jbpm.task.service.responsehandlers.BlockingTaskSummaryResponseHandler;
import com.wordpress.salaboy.grid.*;
import java.util.List;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.TaskClient;
import java.util.HashMap;
import com.wordpress.salaboy.messaging.MessageConsumer;
import com.wordpress.salaboy.model.events.PatientAtHospitalEvent;
import com.wordpress.salaboy.model.events.PatientPickUpEvent;
import com.wordpress.salaboy.services.ProceduresMGMTService;
import org.hornetq.api.core.HornetQException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author salaboy
 */
public class DefaultHeartAttackProcedureTest extends GridBaseTest{

    private MessageConsumer consumer;
    private TaskClient client;
    public DefaultHeartAttackProcedureTest() {
    
    }

   @BeforeClass
    public static void setUpClass() throws Exception {
        HumanTaskServerService.getInstance().initTaskServer();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        
        HumanTaskServerService.getInstance().stopTaskServer();
    }

    @Before
    public void setUp() throws Exception {
     MessageServerSingleton.getInstance().start();
        consumer = MessageFactory.createMessageConsumer("phoneCalls");
        
        
        this.coreServicesMap = new HashMap();
        createRemoteNode();

    }

    @After
    public void tearDown() throws Exception {
       MessageServerSingleton.getInstance().stop();
        if(remoteN1 != null){
            remoteN1.dispose();
        }
        if(grid1 != null){
            grid1.get(SocketService.class).close();
        }
    }

    @Test
    public void defaultHeartAttackSimpleTest() throws HornetQException, InterruptedException{
   
     //   MessageProducer producer = MessageFactory.createMessageProducer("phoneCalls");
      //  producer.sendMessage(new Call(1,2,new Date()));
      //  producer.stop();
        client =  HumanTaskServerService.getInstance().initTaskClient("client test defaultHeartAttackSimpleTest");
        
//        Call call = (Call) consumer.receiveMessage();
//        assertNotNull(call);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("call.id", 1L);
        
        
        
        ProceduresMGMTService.getInstance().newDefaultHeartAttackProcedure((Long)parameters.get("call.id"), parameters);
        
        Thread.sleep(4000);
        
        BlockingTaskSummaryResponseHandler handler = new BlockingTaskSummaryResponseHandler();
        client.getTasksAssignedAsPotentialOwner("garage_emergency_service", "en-UK", handler);
        List<TaskSummary> sums = handler.getResults();
        assertNotNull(sums);
        assertEquals(1, sums.size());
        TaskSummary taskSum = sums.get(0); // getting the first task
        
        
        client.start(taskSum.getId(), "garage_emergency_service", null);
        
        
        
        client.complete(taskSum.getId(), "garage_emergency_service", null, null);
        
        
        
        ProceduresMGMTService.getInstance().patientPickUpNotification(new PatientPickUpEvent(1L, 1L, new Date()));
        
        Thread.sleep(4000);
        
        handler = new BlockingTaskSummaryResponseHandler();
        client.getTasksAssignedAsPotentialOwner("doctor", "en-UK", handler);
        sums = handler.getResults();
        assertNotNull(sums);
        assertEquals(1, sums.size());
        taskSum = sums.get(0); // getting the first task
        
        client.start(taskSum.getId(), "doctor", null);
        
        
        
        client.complete(taskSum.getId(), "doctor", null, null);
        
        Thread.sleep(4000);
        
        ProceduresMGMTService.getInstance().patientAtHospitalNotification(new PatientAtHospitalEvent(1L, 1L, 1L, new Date()));
        
        Thread.sleep(4000);
        
        
    }

}