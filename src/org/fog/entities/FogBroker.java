package org.fog.entities;

import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;

public class FogBroker extends PowerDatacenterBroker {

	public FogBroker(String name) throws Exception {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void startEntity() {
		// TODO Auto-generated method stub

	}

	@Override
	public void processEvent(SimEvent ev) {
		// TODO Auto-generated method stub
		// System.out.print("_____________ org.fog.entities.FogBroker got event:");
		// System.out.println(ev);
	}

	@Override
	public void shutdownEntity() {
		// TODO Auto-generated method stub

	}

}
