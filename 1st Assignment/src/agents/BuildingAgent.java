package agents;

import agents.LiftAgent;
import agents.FloorPanelAgent;
import agents.RequestAgent;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
 
@SuppressWarnings("serial")
public class BuildingAgent extends Agent{
	
	private int nmrFloors;
	private int nmrLifts;
	private float maxWeight;
	private float maxSpeed;
	private float floorDistance;
	private ContainerController mainContainer;
	
	public BuildingAgent(String[] args, ContainerController mainContainer){
        this.nmrFloors = Integer.parseInt(args[0]);
        this.nmrLifts = Integer.parseInt(args[1]);
        this.maxWeight = Float.parseFloat(args[2]); 
        this.maxSpeed = Float.parseFloat(args[3]);
        this.floorDistance = Float.parseFloat(args[4]);
        this.mainContainer = mainContainer;
    }
	
	public void setup() {
		
		System.out.println(getLocalName() + ": started working.\n");
		System.out.println(this.toString());
		
		launchLiftAgents(this.nmrLifts);
		
		try {
			Thread.sleep(1200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		launchFloorPanelAgents(this.nmrFloors);

		try {
			Thread.sleep(1200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		launchRequestAgent(this.nmrFloors);
	}

	public void takeDown() {
		
		Codec codec = new SLCodec();    
		Ontology jmo = JADEManagementOntology.getInstance();
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(jmo);
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(getAMS());
		msg.setLanguage(codec.getName());
		msg.setOntology(jmo.getName());
		try {
		    getContentManager().fillContent(msg, new Action(getAID(), new ShutdownPlatform()));
		    send(msg);
		}
		catch (Exception e) {
			
		}
		
		System.out.println(getLocalName() + ": done working."); 
	}
	
	
	/*LiftAgents launching functions */
	
	protected void launchLiftAgents(Integer nmrLifts) {
		
		int lift;
		for(lift = 1; lift <= nmrLifts; lift++) {
			createLiftAgent(lift);
		}
	}
	
	protected void createLiftAgent(Integer lift) {
		
		AgentController liftAgent;
		
		
		try {
			liftAgent = this.mainContainer.acceptNewAgent("liftAgent" + lift, new LiftAgent(buildArgs(lift)));
			liftAgent.start();
		} catch(StaleProxyException e) {
			System.err.println("Error launching liftAgent");
			e.printStackTrace();
		}
	}
	
	protected String[] buildArgs(Integer lift) {
		String[] args = {String.valueOf(lift), "600.0", "2.5"};
		return args;
	}
	
	
	/* FloorPanelAgents launching functions */
	
	protected void launchFloorPanelAgents(Integer nmrFloors) {
		
		int floor;
		for(floor = 0; floor <= nmrFloors; floor++) {
			createFloorPanelAgent(floor);
		}
	}
	
	protected void createFloorPanelAgent(Integer floor) {
		
		AgentController floorPanelAgent;
		
		
		try {
			floorPanelAgent = this.mainContainer.acceptNewAgent("floorPanelAgent" + floor, new FloorPanelAgent(floor));
			floorPanelAgent.start();
		} catch(StaleProxyException e) {
			System.err.println("Error launching floorPanelAgent");
			e.printStackTrace();
		}
	}
	
	private void launchRequestAgent(int nmrFloors) {
		AgentController requestAgent;
		
		try {
			requestAgent = this.mainContainer.acceptNewAgent("requestAgent", new RequestAgent(nmrFloors));
			requestAgent.start();
		} catch(StaleProxyException e) {
			System.err.println("Error launching requestAgent");
			e.printStackTrace();
		}
	}
	
	@Override
    public String toString() {
        return "INFORMATION\n" + "Agent: " + getLocalName()+"\n" + "Floors: " + this.nmrFloors + "\n" + "Lifts: " + this.nmrLifts + "\n" + "Max weight: " + this.maxWeight + "\n" + "Max speed: " + this.maxSpeed + "\n" + "Floor distance: " + this.floorDistance + "\n";
    }
	
	
	
	/*getters*/
	public int getFloors() {
		return this.nmrFloors;
	}
	
	public int getLifts() {
		return this.nmrLifts;
	}
	
	public float getWeight() {
		return this.maxWeight;
	}
	
	public float getSpeed() {
		return this.maxSpeed;
	}
	
	public float getDistance() {
		return this.floorDistance;
	}
	
	public ContainerController getController() {
		return this.mainContainer;
	}
}
