package agents;

import jade.core.AID;
import sajas.core.Agent;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import launcher.RepastLauncher;
import sajas.proto.AchieveREInitiator;
import sajas.proto.AchieveREResponder;
import uchicago.src.sim.analysis.BinDataSource;
import uchicago.src.sim.engine.Schedule;

import java.sql.Date;
import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("serial")
public class RequestAgent extends Agent{
	
	private int startRequestsMade = 0;
	private int nLifts;
	private int floors;
	private Queue<Integer> inFlow = new ArrayDeque<Integer>();
	private Queue<Integer> outFlow = new ArrayDeque<Integer>();
	private ScheduledExecutorService scheduler;
	private RepastLauncher repast;
	
	public RequestAgent(int floors, int lifts, RepastLauncher repast) {
		this.floors = floors;
		this.nLifts = lifts;
		for (int i = 0; i < floors; i++) {
			this.inFlow.add(0);
			this.outFlow.add(0);
		}
		this.repast = repast;
	}
	
	public void setup() {
		scheduleNextRequest();
		addListener();
	}
	
	public void takeDown() {
		System.out.println("RequestAgent done working...");
		if(this.scheduler != null) {
			this.scheduler.shutdownNow();
		}
	}
	
	private void scheduleNextRequest() {
		repast.getSchedule().scheduleActionAt(repast.getTickCount()+repast.getTicksBetweenRequests(), this, "sendRequest", Schedule.LAST);
	}
	
	
	public void sendRequest() {
		this.scheduleNextRequest();
		Random rand = new Random();
		int randomInteger = rand.nextInt(floors + 1);
		if(startRequestsMade < nLifts) {
			randomInteger = 0;
			startRequestsMade++;
		}
		Boolean randomBoolean = rand.nextBoolean();
		
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new sajas.core.AID("floorPanelAgent" + randomInteger ,AID.ISLOCALNAME));
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        msg.setReplyByDate(new Date(System.currentTimeMillis() + 5000)); //we want to receive a reply in 5 seconds at most
        
        String content;
        
        if (randomBoolean) {
			content = "Up";
		} else {
			content = "Down";
		}
        
        if(randomInteger == 0) content = "Up";
        if(randomInteger == floors) content = "Down";
        
        msg.setContent(content);
        System.out.println("\nFloor: "+ randomInteger + "  Message: " + content);
      
		addBehaviour(new AchieveREInitiator(this, msg) {
			protected void handleInform(ACLMessage inform) {
				// System.out.println("Agent " + inform.getSender().getLocalName() + " successfully performed the requested action");
			}
			protected void handleRefuse(ACLMessage refuse) {
				System.out.println("Agent " + refuse.getSender().getLocalName() + " refused to perform the requested action");
			}
			
			protected void handleFailure(ACLMessage failure) {
				if (failure.getSender().equals(myAgent.getAMS())) {
					System.out.println("Responder does not exist.");
				}
				else {
					System.out.println("Agent " + failure.getSender().getLocalName() + " failed to perform the requested action");
				}
			}
		} );
	}
	
	private void addListener() {
		System.out.println("Agent "+ getLocalName()+ " waiting for requests...");

	  	MessageTemplate template = MessageTemplate.and(
  		MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
  		MessageTemplate.MatchPerformative(ACLMessage.REQUEST) );
	  	
	  	addBehaviour(new AchieveREResponder(this, template) {
	  		
			protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
				
				
				if (checkSender(request.getSender().getName())) {
					
					ACLMessage agree = request.createReply();
					agree.setPerformative(ACLMessage.AGREE);
					return agree;
					
				}
				else {
					System.out.println("Agent " + getLocalName()+ ": Refuse");
					throw new RefuseException("check-failed");
				}
			}
			
			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException{
				
				if(request.getContent() != null) {
					ACLMessage inform = request.createReply();
					inform.setContent(createResponse(request.getContent()));
					inform.setPerformative(ACLMessage.INFORM);
					return inform;
				}
				else {
					System.out.println("Agent " + getLocalName() + ": Action failed");
					throw new FailureException("unexpected-error");
				}
			}
		} );
	}
	
	protected boolean checkSender(String name) {
		return name.contains("liftAgent") ? true : false;
	}
	
	//create response to send to liftAgent
	protected String createResponse(String request) {
		
		String response;
		
			if(request != null) {
				String[] content = request.split(":", 2);
			response = generateResponse(Integer.parseInt(content[0]),content[1]);
		}
		else {
			response = "[WARNING] Request is empty";
		}
		
		return response;
	}
	
	protected String generateResponse(int floor, String action) { 
		String response = "";
		
		switch(action){
			case "Up": 
				response = generateUpDown(floor, 1);
				break;
			case "Down": 
				response = generateUpDown(floor, -1);
				break;
			case "End": 
				response = generateEnd(floor);
				break;
			default:
				response = "[WARNING] Request is empty";
				break;
		}
		
		return response;
	}
	
	protected String generateUpDown(int floor,int op) {
		
		String response = "";
		ArrayList<Integer> floorList = new ArrayList<>();
		
		//entering mandatory
		int enteringPeople = generatePeopleNumber();
		this.addToInFlow(floor, enteringPeople);
		
		for(int i = 0; i < enteringPeople; i++) {
			int value;
			if(op == 1) {
				value = generateFloorBetweenValuesUp(floor);
			}
			else if(op == -1) {
				value = generateFloorBetweenValuesDown(floor);
			}
			else {
				value = 0;
			}
			if(!floorList.contains(value)) {
				floorList.add(value);
			}
		}
		
		//exiting optional
		int exiting = 0;
		if(generateBoolean()) {
			exiting = generatePeopleNumber();
			this.addToOutFlow(floor, exiting);
		}
		
		//build message
		response = response + "E:" + enteringPeople + "[";
		for(int j = 0; j < floorList.size(); j++) {
			if(j == floorList.size() - 1) {
				response = response + floorList.get(j);
			}
			else {
				response = response + floorList.get(j) + "-";
			}
		}
		
		response = response + "]";
		
		if(exiting != 0 ) {
			response = response + ",S:" + exiting;
		}
		
		return response;
	}
	
	protected String generateEnd(int floor) {
		String response = "";

		
		//REDACTED entering optional. Because tasklist prioritizes ups and downs over ends,
		//if its end no one has called a lift so its safe to assume no one enters	
		
		
//		//entering optional
//		if(generateBoolean()) {
//			ArrayList<Integer> floorList = new ArrayList<>();
//			int enteringPeople = generatePeopleNumber();
//			for(int i = 0; i < enteringPeople; i++) {
//				int value;
//				if(generateBoolean()) {
//					value = generateFloorBetweenValuesUp(floor);
//				}
//				else {
//					value = generateFloorBetweenValuesDown(floor);
//				}
//				
//				if(!floorList.contains(value)) {
//					floorList.add(value);
//				}
//			}
//			
//			response = response + "E:" + enteringPeople + "[";
//			
//			for(int j = 0; j < floorList.size(); j++) {
//				if(j == floorList.size() - 1) {
//					response = response + floorList.get(j);
//				}
//				else {
//					response = response + floorList.get(j) + "-";
//				}
//			}
//			response = response + "],";
//		}
		
		//exiting mandatory
		int exiting = generatePeopleNumber();
		this.addToOutFlow(floor, exiting);
		response = response + "S:" + exiting;
		return response;
	}
	
	protected int generateFloorBetweenValuesUp(int low) {
		Random r = new Random();
		if(low >= this.floors) return generateFloorBetweenValuesDown(this.floors);
		else return r.nextInt((this.floors) - low) + low + 1;
	}
	
	protected int generateFloorBetweenValuesDown(int max) {
		Random r = new Random();
		if(max <= 0) return  generateFloorBetweenValuesUp(1);
		else return r.nextInt(max);
	}
	
	protected Boolean generateBoolean() {
		Random rand = new Random();
		return rand.nextBoolean();
	}
	
	protected int generatePeopleNumber() {
		Random rand = new Random(); //generates random number of people (to be used in Enter and Exit)
		final int n = rand.nextInt(10);
		if (n > 5) { return 4; }
		else if (n > 3) { return 3; }
		else if (n > 1) { return 2; } 
		else { return 1; }
	}
	
	private void addToInFlow(int floor, int add) {
		this.inFlow.add(floor);
	}

	private void addToOutFlow(int floor, int add) {
		this.outFlow.add(floor);
	}
	
	public int getInFlow() {
		if(this.inFlow.isEmpty()) return -1;
		return this.inFlow.remove();
	}
	
	public int getOutFlow() {

		if(this.inFlow.isEmpty()) return -1;
		return this.outFlow.remove();
	}

}




