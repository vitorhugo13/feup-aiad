package utils;

import agents.FloorPanelAgent;

import java.util.ArrayList;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class SenderFloorLift {
	
	private ArrayList<String> listeners;
	private FloorPanelAgent floorPanelAgent;
	private String type;
	private int floor;
	
	public SenderFloorLift(ArrayList<String> listeners, FloorPanelAgent floorPanelAgent, String type, Integer floor) { 		
		this.listeners = listeners;
		this.floorPanelAgent = floorPanelAgent;
		this.type = type;
		this.floor = floor;

	}
	
	public void sendToLift() {
		
		for(String listener : this.listeners) {
			 ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
	         msg.addReceiver(new AID(listener,AID.ISLOCALNAME));
	         switch(this.type) {
	         	case "Down":
	         		msg.setContent(Integer.toString(-1 * this.floor));
	         		break;
	         	case "Up":
	         		msg.setContent(Integer.toString(this.floor));
	         		break;
	         	default:
	         		break;
	         }
	         
	         this.floorPanelAgent.send(msg);
		}
	}
}
